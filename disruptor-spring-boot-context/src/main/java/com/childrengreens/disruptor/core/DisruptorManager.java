/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.consumer.ExceptionHandlerSupport;
import com.childrengreens.disruptor.consumer.HandlerAdapter;
import com.childrengreens.disruptor.consumer.SubscriberDefinition;
import com.childrengreens.disruptor.consumer.SubscriberRegistry;
import com.childrengreens.disruptor.consumer.WorkerPoolSupport;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.childrengreens.disruptor.properties.RingProperties;
import com.childrengreens.disruptor.properties.ShutdownStrategy;
import com.childrengreens.disruptor.properties.WaitStrategyType;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
        if (running) {
            return;
        }
        Map<String, RingProperties> rings = resolveRings();
        validateRings(rings);

        Map<String, Map<Integer, List<EventHandler<DisruptorEvent>>>> orderedHandlers =
                groupEventHandlersByOrder();
        Map<String, List<WorkHandler<DisruptorEvent>>> workHandlers =
                handlerAdapter.adaptWorkHandlers(registry);

        Map<String, Disruptor<DisruptorEvent>> started = new LinkedHashMap<>();
        try {
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
                started.put(ringName, disruptor);
            }
            running = true;
        } catch (Exception ex) {
            for (Disruptor<DisruptorEvent> disruptor : started.values()) {
                try {
                    disruptor.halt();
                } catch (Exception haltEx) {
                    log.warn("Failed to halt ring during startup rollback.", haltEx);
                }
            }
            disruptors.clear();
            ringBuffers.clear();
            running = false;
            throw ex;
        }
    }

    /**
     * Stop all rings with the given timeout and strategy.
     */
    public synchronized void stop(Duration timeout, ShutdownStrategy strategy) {
        if (!running) {
            return;
        }
        try {
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
                    try {
                        disruptor.halt();
                    } catch (Exception haltEx) {
                        log.error("Failed to halt ring {} after shutdown failure.", entry.getKey(), haltEx);
                    }
                }
            }
        } finally {
            disruptors.clear();
            ringBuffers.clear();
            running = false;
        }
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
     * Return resolved ring names, including those discovered from subscribers.
     */
    public List<String> getResolvedRingNames() {
        return List.copyOf(resolveRings().keySet());
    }

    /**
     * Resolve ring configurations, including defaults and rings discovered by subscribers.
     */
    private Map<String, RingProperties> resolveRings() {
        Map<String, RingProperties> rings = properties.getRings();
        rings = rings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rings);
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
                    != Concurrency.MODE_HANDLER) {
                continue;
            }
            EventHandler<DisruptorEvent> handler = handlerAdapter.adaptEventHandler(definition);
            if (handler == null) {
                continue;
            }
            result
                    .computeIfAbsent(definition.ring(), key -> new TreeMap<>())
                    .computeIfAbsent(definition.order(), key -> new ArrayList<>())
                    .add(handler);
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
                props.getProducerType(),
                toWaitStrategy(props.getWaitStrategy(), props));
    }

    /**
     * Map configured wait strategy to Disruptor wait strategy.
     */
    private WaitStrategy toWaitStrategy(WaitStrategyType waitStrategyType, RingProperties props) {
        RingProperties.WaitStrategyConfig config =
                props.getWaitStrategyConfig() == null
                        ? new RingProperties.WaitStrategyConfig()
                        : props.getWaitStrategyConfig();
        return switch (waitStrategyType) {
            case TIMEOUT_BLOCKING -> new TimeoutBlockingWaitStrategy(
                    toTimeout(config.getTimeoutBlockingTimeout(), Duration.ofMillis(1)),
                    TimeUnit.NANOSECONDS);
            case LITE_BLOCKING -> new LiteBlockingWaitStrategy();
            case LITE_TIMEOUT_BLOCKING -> new LiteTimeoutBlockingWaitStrategy(
                    toTimeout(config.getLiteTimeoutBlockingTimeout(), Duration.ofMillis(1)),
                    TimeUnit.NANOSECONDS);
            case SLEEPING -> new SleepingWaitStrategy();
            case YIELDING -> new YieldingWaitStrategy();
            case BUSY_SPIN -> new BusySpinWaitStrategy();
            case PHASED_BACKOFF -> {
                Duration spinTimeout = config.getPhasedBackoffSpinTimeout();
                Duration yieldTimeout = config.getPhasedBackoffYieldTimeout();
                WaitStrategy fallback =
                        toFallbackWaitStrategy(config.getPhasedBackoffFallback(), config);
                yield new PhasedBackoffWaitStrategy(
                        toTimeout(spinTimeout, Duration.ofNanos(1000)),
                        toTimeout(yieldTimeout, Duration.ofNanos(1_000_000)),
                        TimeUnit.NANOSECONDS,
                        fallback);
            }
            default -> new BlockingWaitStrategy();
        };
    }

    private WaitStrategy toFallbackWaitStrategy(
            WaitStrategyType waitStrategyType, RingProperties.WaitStrategyConfig config) {
        WaitStrategyType type = waitStrategyType == null ? WaitStrategyType.YIELDING : waitStrategyType;
        if (type == WaitStrategyType.PHASED_BACKOFF) {
            log.warn("PHASED_BACKOFF cannot be used as a phasedBackoffFallback. Using YIELDING.");
            type = WaitStrategyType.YIELDING;
        }
        return switch (type) {
            case TIMEOUT_BLOCKING -> new TimeoutBlockingWaitStrategy(
                    toTimeout(config.getTimeoutBlockingTimeout(), Duration.ofMillis(1)),
                    TimeUnit.NANOSECONDS);
            case LITE_BLOCKING -> new LiteBlockingWaitStrategy();
            case LITE_TIMEOUT_BLOCKING -> new LiteTimeoutBlockingWaitStrategy(
                    toTimeout(config.getLiteTimeoutBlockingTimeout(), Duration.ofMillis(1)),
                    TimeUnit.NANOSECONDS);
            case SLEEPING -> new SleepingWaitStrategy();
            case BUSY_SPIN -> new BusySpinWaitStrategy();
            case BLOCKING -> new BlockingWaitStrategy();
            default -> new YieldingWaitStrategy();
        };
    }

    private long toTimeout(Duration value, Duration defaultValue) {
        Duration effective = value;
        if (effective == null || effective.isZero() || effective.isNegative()) {
            effective = defaultValue;
        }
        return effective.toNanos();
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger index = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(prefix + index.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
