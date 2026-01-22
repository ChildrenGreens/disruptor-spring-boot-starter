package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.annotation.DisruptorEventType;
import com.lmax.disruptor.RingBuffer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Template-style publisher for sending events to a named Disruptor ring.
 */
public class DisruptorTemplate implements EventPublisher {
    private final DisruptorManager manager;
    private final List<EventConverter<?>> converters;
    private final DisruptorMetrics metrics;

    public DisruptorTemplate(
            DisruptorManager manager, List<EventConverter<?>> converters, DisruptorMetrics metrics) {
        this.manager = manager;
        this.converters = new ArrayList<>(converters);
        AnnotationAwareOrderComparator.sort(this.converters);
        this.metrics = metrics;
    }

    /**
     * Publish payload into the target ring via Disruptor RingBuffer.
     */
    @Override
    public void publish(String ring, Object event) {
        if (!manager.isRunning()) {
            throw new IllegalStateException("Disruptor is not running.");
        }
        String targetRing = (ring == null || ring.isBlank()) ? "default" : ring;
        RingBuffer<DisruptorEvent> ringBuffer = manager.getRingBuffer(targetRing);
        if (ringBuffer == null) {
            throw new IllegalStateException("Ring not found: " + targetRing);
        }
        Object convertedPayload = convert(event);
        String eventType = resolveEventType(convertedPayload);
        ringBuffer.publishEvent(
                (disruptorEvent, sequence, payload) -> {
                    disruptorEvent.setPayload(payload);
                    disruptorEvent.setEventType(eventType);
                    disruptorEvent.setCreatedAt(System.currentTimeMillis());
                },
                convertedPayload);
        if (metrics != null) {
            metrics.recordPublish(targetRing);
        }
    }

    /**
     * Resolve logical event type from annotation or class name.
     */
    private String resolveEventType(Object event) {
        if (event == null) {
            return "null";
        }
        DisruptorEventType annotation = event.getClass().getAnnotation(DisruptorEventType.class);
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }
        return event.getClass().getName();
    }

    /**
     * Convert payload using the first matching converter.
     */
    private Object convert(Object payload) {
        for (EventConverter<?> converter : converters) {
            if (converter.supports(payload)) {
                return converter.convert(payload);
            }
        }
        return payload;
    }
}
