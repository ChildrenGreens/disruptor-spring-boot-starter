package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.consumer.ExceptionHandlerSupport;
import com.childrengreens.disruptor.consumer.HandlerAdapter;
import com.childrengreens.disruptor.consumer.SubscriberDefinition;
import com.childrengreens.disruptor.consumer.SubscriberRegistry;
import com.childrengreens.disruptor.consumer.WorkerPoolSupport;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.childrengreens.disruptor.properties.ProducerType;
import com.childrengreens.disruptor.properties.RingProperties;
import com.childrengreens.disruptor.properties.ShutdownStrategy;
import com.childrengreens.disruptor.properties.WaitStrategyType;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * Central manager that builds, starts, and stops Disruptor instances per ring.
 *
 * <p>It wires handler chains, worker pools, and exception strategies based on
 * {@link com.childrengreens.disruptor.properties.DisruptorProperties}.</p>
 */
public class DisruptorManager {
    private static final Logger log = LoggerFactory.getLogger(DisruptorManager.class);

    private final DisruptorProperties properties;
    private final SubscriberRegistry registry;
    private final HandlerAdapter handlerAdapter;
    private final ExceptionHandlerSupport exceptionHandlerSupport;
    private final WorkerPoolSupport workerPoolSupport;
    private final Map<String, Disruptor<DisruptorEvent>> disruptors = new LinkedHashMap<>();
    private final Map<String, RingBuffer<DisruptorEvent>> ringBuffers = new LinkedHashMap<>();
    private volatile boolean running = false;

    public DisruptorManager(
            DisruptorProperties properties,
            SubscriberRegistry registry,
            HandlerAdapter handlerAdapter,
            ExceptionHandlerSupport exceptionHandlerSupport,
            WorkerPoolSupport workerPoolSupport) {
        this.properties = properties;
        this.registry = registry;
        this.handlerAdapter = handlerAdapter;
        this.exceptionHandlerSupport = exceptionHandlerSupport;
        this.workerPoolSupport = workerPoolSupport;
    }

    /**
     * Build and start all configured Disruptor rings.
     */
    public synchronized void start() {
        if (!disruptors.isEmpty()) {
            return;
        }
        Map<String, RingProperties> rings = resolveRings();
        validateRings(rings);

        Map<String, Map<Integer, List<EventHandler<DisruptorEvent>>>> orderedHandlers =
                groupEventHandlersByOrder();
        Map<String, List<WorkHandler<DisruptorEvent>>> workHandlers =
                handlerAdapter.adaptWorkHandlers(registry);

        for (Map.Entry<String, RingProperties> entry : rings.entrySet()) {
            String ringName = entry.getKey();
            RingProperties ringProperties = entry.getValue();
            Disruptor<DisruptorEvent> disruptor = buildDisruptor(ringName, ringProperties);
            ExceptionHandler<DisruptorEvent> exceptionHandler =
                    exceptionHandlerSupport.create(ringProperties.getExceptionHandler(), ringName);
            disruptor.setDefaultExceptionHandler(exceptionHandler);

            Map<Integer, List<EventHandler<DisruptorEvent>>> ringEventHandlers =
                    orderedHandlers.get(ringName);
            List<WorkHandler<DisruptorEvent>> ringWorkHandlers = workHandlers.get(ringName);
            if (ringEventHandlers != null && !ringEventHandlers.isEmpty()) {
                EventHandlerGroup<DisruptorEvent> group = null;
                for (Map.Entry<Integer, List<EventHandler<DisruptorEvent>>> orderedGroup :
                        ringEventHandlers.entrySet()) {
                    @SuppressWarnings("unchecked")
                    EventHandler<DisruptorEvent>[] handlers =
                            orderedGroup.getValue().toArray(new EventHandler[0]);
                    if (group == null) {
                        group = disruptor.handleEventsWith(handlers);
                    } else {
                        group = group.then(handlers);
                    }
                }
            }

            if (ringWorkHandlers != null && !ringWorkHandlers.isEmpty()) {
                if (ringEventHandlers != null && !ringEventHandlers.isEmpty()) {
                    log.warn(
                            "Ring {} has both handler and worker subscribers. WorkerPool will run in parallel.",
                            ringName);
                }
                @SuppressWarnings("unchecked")
                WorkHandler<DisruptorEvent>[] handlers =
                        ringWorkHandlers.toArray(new WorkHandler[0]);
                workerPoolSupport.handleWithWorkerPool(disruptor, handlers);
            }

            if ((ringEventHandlers == null || ringEventHandlers.isEmpty())
                    && (ringWorkHandlers == null || ringWorkHandlers.isEmpty())) {
                log.info("Ring {} has no subscribers registered.", ringName);
            }

            disruptor.start();
            disruptors.put(ringName, disruptor);
            ringBuffers.put(ringName, disruptor.getRingBuffer());
        }
        running = true;
    }

    /**
     * Stop all rings with the given timeout and strategy.
     */
    public synchronized void stop(Duration timeout, ShutdownStrategy strategy) {
        if (!running) {
            return;
        }
        for (Map.Entry<String, Disruptor<DisruptorEvent>> entry : disruptors.entrySet()) {
            Disruptor<DisruptorEvent> disruptor = entry.getValue();
            try {
                if (strategy == ShutdownStrategy.HALT) {
                    disruptor.halt();
                } else {
                    disruptor.shutdown(timeout.toMillis(), TimeUnit.MILLISECONDS);
                }
            } catch (Exception ex) {
                log.warn("Failed to shutdown ring {} gracefully, forcing halt.", entry.getKey(), ex);
                disruptor.halt();
            }
        }
        disruptors.clear();
        ringBuffers.clear();
        running = false;
    }

    /**
     * Whether Disruptor rings are running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Return an immutable view of RingBuffers by ring name.
     */
    public Map<String, RingBuffer<DisruptorEvent>> getRingBuffers() {
        return Collections.unmodifiableMap(ringBuffers);
    }

    /**
     * Return RingBuffer for the given ring, or {@code null} when not available.
     */
    public RingBuffer<DisruptorEvent> getRingBuffer(String name) {
        return ringBuffers.get(name);
    }

    /**
     * Resolve ring configurations, including defaults and rings discovered by subscribers.
     */
    private Map<String, RingProperties> resolveRings() {
        Map<String, RingProperties> rings = properties.getRings();
        if (rings == null || rings.isEmpty()) {
            Map<String, RingProperties> defaults = new LinkedHashMap<>();
            defaults.put("default", new RingProperties());
            rings = defaults;
        }
        for (String ringName : registry.getDefinitions().stream()
                .map(SubscriberDefinition::ring)
                .distinct()
                .toList()) {
            rings.putIfAbsent(ringName, new RingProperties());
        }
        return rings;
    }

    /**
     * Build ordered handler chains grouped by ring and order.
     */
    private Map<String, Map<Integer, List<EventHandler<DisruptorEvent>>>> groupEventHandlersByOrder() {
        Map<String, Map<Integer, List<EventHandler<DisruptorEvent>>>> result =
                new LinkedHashMap<>();
        for (SubscriberDefinition definition : registry.getDefinitions()) {
            if (definition.mode()
                    != com.childrengreens.disruptor.annotation.Concurrency.MODE_HANDLER) {
                continue;
            }
            EventHandler<DisruptorEvent> handler = handlerAdapter.adaptEventHandler(definition);
            if (handler == null) {
                continue;
            }
            result
                    .computeIfAbsent(definition.ring(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(definition.order(), key -> new java.util.ArrayList<>())
                    .add(handler);
        }
        for (Map<Integer, List<EventHandler<DisruptorEvent>>> ordered : result.values()) {
            Map<Integer, List<EventHandler<DisruptorEvent>>> sorted =
                    ordered.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .collect(
                                    java.util.stream.Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (a, b) -> a,
                                            LinkedHashMap::new));
            ordered.clear();
            ordered.putAll(sorted);
        }
        return result;
    }

    /**
     * Validate ring configuration before Disruptor creation.
     */
    private void validateRings(Map<String, RingProperties> rings) {
        for (Map.Entry<String, RingProperties> entry : rings.entrySet()) {
            RingProperties props = entry.getValue();
            int bufferSize = props.getBufferSize();
            if (bufferSize <= 0 || (bufferSize & (bufferSize - 1)) != 0) {
                throw new IllegalArgumentException(
                        "Ring "
                                + entry.getKey()
                                + " bufferSize must be a power of two, but was "
                                + bufferSize);
            }
            if (props.getThreads() <= 0) {
                throw new IllegalArgumentException(
                        "Ring " + entry.getKey() + " threads must be positive.");
            }
        }
    }

    /**
     * Create a Disruptor instance for the given ring.
     */
    private Disruptor<DisruptorEvent> buildDisruptor(String ring, RingProperties props) {
        ThreadFactory threadFactory = new NamedThreadFactory("disruptor-" + ring + "-");
        return new Disruptor<>(
                new DisruptorEventFactory(),
                props.getBufferSize(),
                threadFactory,
                toProducerType(props.getProducerType()),
                toWaitStrategy(props.getWaitStrategy()));
    }

    /**
     * Map configured producer type to Disruptor producer type.
     */
    private com.lmax.disruptor.dsl.ProducerType toProducerType(ProducerType producerType) {
        return producerType == ProducerType.SINGLE
                ? com.lmax.disruptor.dsl.ProducerType.SINGLE
                : com.lmax.disruptor.dsl.ProducerType.MULTI;
    }

    /**
     * Map configured wait strategy to Disruptor wait strategy.
     */
    private WaitStrategy toWaitStrategy(WaitStrategyType waitStrategyType) {
        return switch (waitStrategyType) {
            case SLEEPING -> new SleepingWaitStrategy();
            case YIELDING -> new YieldingWaitStrategy();
            case BUSY_SPIN -> new BusySpinWaitStrategy();
            case PHASED_BACKOFF -> new PhasedBackoffWaitStrategy(
                    1, 1000, TimeUnit.MICROSECONDS, new YieldingWaitStrategy());
            default -> new BlockingWaitStrategy();
        };
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private int index = 1;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public synchronized Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + index++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
