package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;
import com.childrengreens.disruptor.consumer.ExceptionHandlerSupport;
import com.childrengreens.disruptor.consumer.HandlerAdapter;
import com.childrengreens.disruptor.consumer.SubscriberDefinition;
import com.childrengreens.disruptor.consumer.SubscriberRegistry;
import com.childrengreens.disruptor.consumer.WorkerPoolSupport;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.childrengreens.disruptor.properties.RingProperties;
import com.childrengreens.disruptor.properties.ShutdownStrategy;
import com.lmax.disruptor.EventHandler;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisruptorManagerTest {
    @Test
    void startCreatesDefaultRingWhenNoConfigurationProvided() {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        manager.start();
        assertThat(manager.isRunning()).isTrue();
        assertThat(manager.getRingBuffer("default")).isNotNull();
        manager.stop(Duration.ofMillis(100), ShutdownStrategy.HALT);
    }

    @Test
    void startAddsRingFromSubscriberDefinitions() {
        DisruptorProperties properties = new DisruptorProperties();
        SubscriberRegistry registry = new SubscriberRegistry();
        registry.register(new SubscriberDefinition(
                new NoopEventHandler(),
                "noopHandler",
                null,
                DisruptorEvent.class,
                "alpha",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.DELEGATE));

        DisruptorManager manager = newManager(properties, registry);
        manager.start();

        assertThat(manager.getRingBuffer("default")).isNotNull();
        assertThat(manager.getRingBuffer("alpha")).isNotNull();
        manager.stop(Duration.ofMillis(100), ShutdownStrategy.HALT);
    }

    @Test
    void startRejectsNonPowerOfTwoBufferSize() {
        DisruptorProperties properties = new DisruptorProperties();
        RingProperties ring = new RingProperties();
        ring.setBufferSize(1000);
        properties.setRings(Map.of("bad", ring));
        DisruptorManager manager = newManager(properties, new SubscriberRegistry());

        assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bufferSize must be a power of two");
    }

    @Test
    void startRejectsNonPositiveThreadCount() {
        DisruptorProperties properties = new DisruptorProperties();
        RingProperties ring = new RingProperties();
        ring.setThreads(0);
        properties.setRings(Map.of("bad", ring));
        DisruptorManager manager = newManager(properties, new SubscriberRegistry());

        assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threads must be positive");
    }

    @Test
    void stopIsNoopWhenNotRunning() {
        DisruptorManager manager = newManager(new DisruptorProperties(), new SubscriberRegistry());
        manager.stop(Duration.ofMillis(10), ShutdownStrategy.DRAIN);
        assertThat(manager.isRunning()).isFalse();
    }

    private DisruptorManager newManager(
            DisruptorProperties properties, SubscriberRegistry registry) {
        HandlerAdapter handlerAdapter = new HandlerAdapter(new DisruptorMetrics());
        ExceptionHandlerSupport exceptionHandlerSupport = new ExceptionHandlerSupport();
        WorkerPoolSupport workerPoolSupport = new WorkerPoolSupport();
        return new DisruptorManager(
                properties, registry, handlerAdapter, exceptionHandlerSupport, workerPoolSupport);
    }

    static class NoopEventHandler implements EventHandler<DisruptorEvent> {
        @Override
        public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
        }
    }
}
