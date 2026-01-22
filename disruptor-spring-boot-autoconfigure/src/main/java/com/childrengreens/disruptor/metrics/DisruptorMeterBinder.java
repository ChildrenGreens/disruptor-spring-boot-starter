package com.childrengreens.disruptor.metrics;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.lmax.disruptor.RingBuffer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.util.List;

import org.springframework.lang.NonNull;

/**
 * Micrometer binder for basic ring buffer gauges.
 */
public class DisruptorMeterBinder implements MeterBinder {
    private final DisruptorManager manager;
    private final DisruptorMetrics metrics;
    private final DisruptorProperties properties;

    public DisruptorMeterBinder(
            DisruptorManager manager, DisruptorMetrics metrics, DisruptorProperties properties) {
        this.manager = manager;
        this.metrics = metrics;
        this.properties = properties;
    }

    /**
     * Register gauge metrics for each ring buffer.
     */
    @Override
    public void bindTo(@NonNull MeterRegistry registry) {
        for (String ring : resolveRings()) {
            Gauge.builder(
                            "disruptor.ringbuffer.remainingCapacity",
                            () -> remainingCapacity(ring))
                    .tag("ring", ring)
                    .register(registry);
            Gauge.builder("disruptor.ringbuffer.cursor", () -> cursor(ring))
                    .tag("ring", ring)
                    .register(registry);
            Gauge.builder("disruptor.ringbuffer.backlog", () -> backlog(ring))
                    .tag("ring", ring)
                    .register(registry);
            FunctionCounter.builder(
                            "disruptor.publish.count",
                            metrics,
                            m -> m.getPublishCount(ring))
                    .tag("ring", ring)
                    .register(registry);
            FunctionCounter.builder(
                            "disruptor.consume.count",
                            metrics,
                            m -> m.getConsumeCount(ring))
                    .tag("ring", ring)
                    .register(registry);
            Gauge.builder(
                            "disruptor.event.latency.avg",
                            metrics,
                            m -> m.getAverageLatencyMillis(ring))
                    .tag("ring", ring)
                    .register(registry);
        }
    }

    private List<String> resolveRings() {
        if (properties.getRings() == null || properties.getRings().isEmpty()) {
            return List.of("default");
        }
        return List.copyOf(properties.getRings().keySet());
    }

    private double remainingCapacity(String ring) {
        RingBuffer<DisruptorEvent> buffer = manager.getRingBuffer(ring);
        return buffer == null ? 0 : buffer.remainingCapacity();
    }

    private double cursor(String ring) {
        RingBuffer<DisruptorEvent> buffer = manager.getRingBuffer(ring);
        return buffer == null ? 0 : buffer.getCursor();
    }

    private double backlog(String ring) {
        RingBuffer<DisruptorEvent> buffer = manager.getRingBuffer(ring);
        if (buffer == null) {
            return 0;
        }
        return buffer.getBufferSize() - buffer.remainingCapacity();
    }
}
