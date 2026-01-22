package com.childrengreens.disruptor.metrics;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.core.DisruptorEventFactory;
import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.lmax.disruptor.RingBuffer;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisruptorMeterBinderTest {
    @Test
    void registersMetersForDefaultRing() {
        RingBuffer<DisruptorEvent> ringBuffer =
                RingBuffer.createMultiProducer(new DisruptorEventFactory(), 8);

        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.getRingBuffer("default")).thenReturn(ringBuffer);

        DisruptorMetrics metrics = new DisruptorMetrics();
        metrics.recordPublish("default");
        metrics.recordConsume("default", "handlerA");

        DisruptorProperties properties = new DisruptorProperties();
        DisruptorMeterBinder binder = new DisruptorMeterBinder(manager, metrics, properties);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        binder.bindTo(registry);

        Gauge backlog = registry.get("disruptor.ringbuffer.backlog")
                .tag("ring", "default")
                .gauge();
        assertThat(backlog.value()).isGreaterThanOrEqualTo(0);

        FunctionCounter publishCount = registry.get("disruptor.publish.count")
                .tag("ring", "default")
                .functionCounter();
        assertThat(publishCount.count()).isEqualTo(1.0);
    }
}
