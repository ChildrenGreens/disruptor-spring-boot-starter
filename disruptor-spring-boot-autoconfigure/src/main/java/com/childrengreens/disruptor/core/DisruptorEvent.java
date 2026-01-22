package com.childrengreens.disruptor.core;

/**
 * Generic event container stored in the RingBuffer.
 */
public class DisruptorEvent {
    // User payload object.
    private Object payload;
    // Logical event type for routing/metrics.
    private String eventType;
    // Creation timestamp for latency tracking.
    private long createdAt;

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
