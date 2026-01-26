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

/**
 * Generic event container stored in the Disruptor RingBuffer.
 *
 * <p>This class serves as a wrapper for all events published through the
 * Disruptor starter. It follows the Disruptor's pre-allocation pattern where
 * event objects are created once and reused, with only their contents being
 * updated on each publish.</p>
 *
 * <p>The event contains:</p>
 * <ul>
 *   <li>{@link #payload} - The actual business event object</li>
 *   <li>{@link #eventType} - Logical type identifier for routing and filtering</li>
 *   <li>{@link #createdAt} - Timestamp for latency tracking and metrics</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is not thread-safe by design.
 * The Disruptor framework ensures that only one thread accesses an event slot
 * at a time during publishing, and events are safely handed off to consumers.</p>
 *
 * <p><strong>Memory Management:</strong> After an event is consumed, the
 * payload reference should ideally be cleared to allow garbage collection
 * of the business object. However, the DisruptorEvent instance itself remains
 * in the RingBuffer for reuse.</p>
 *
 * @see DisruptorEventFactory
 * @see DisruptorTemplate#publish(String, Object)
 */
public class DisruptorEvent {

    /**
     * The actual business payload object.
     * <p>This can be any object type. The payload is set during event
     * publishing via {@link DisruptorTemplate} and accessed by subscribers
     * annotated with {@link com.childrengreens.disruptor.annotation.DisruptorSubscriber}.</p>
     */
    private Object payload;

    /**
     * Logical event type identifier for routing and filtering.
     * <p>This value is determined by:</p>
     * <ol>
     *   <li>The {@link com.childrengreens.disruptor.annotation.DisruptorEventType}
     *       annotation on the payload class, if present</li>
     *   <li>Otherwise, the fully qualified class name of the payload</li>
     * </ol>
     * <p>Subscribers can filter events by type using
     * {@link com.childrengreens.disruptor.annotation.DisruptorSubscriber#eventType()}.</p>
     */
    private String eventType;

    /**
     * Event creation timestamp in milliseconds since epoch.
     * <p>Set automatically when the event is published. Used for:</p>
     * <ul>
     *   <li>Calculating event processing latency in metrics</li>
     *   <li>Monitoring queue delays via Actuator endpoint</li>
     * </ul>
     *
     * @see com.childrengreens.disruptor.core.DisruptorMetrics
     */
    private long createdAt;

    /**
     * Returns the business payload object.
     *
     * @return the payload, may be {@code null}
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * Sets the business payload object.
     * <p>Called internally by {@link DisruptorTemplate} during event publishing.</p>
     *
     * @param payload the business event object
     */
    public void setPayload(Object payload) {
        this.payload = payload;
    }

    /**
     * Returns the logical event type identifier.
     *
     * @return the event type string, never {@code null} after publishing
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Sets the logical event type identifier.
     * <p>Called internally by {@link DisruptorTemplate} during event publishing.</p>
     *
     * @param eventType the event type identifier
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * Returns the event creation timestamp.
     *
     * @return milliseconds since epoch when the event was published
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the event creation timestamp.
     * <p>Called internally by {@link DisruptorTemplate} during event publishing.
     * Typically set to {@link System#currentTimeMillis()}.</p>
     *
     * @param createdAt milliseconds since epoch
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Clear references after consumption to help GC.
     * <p>Call only when no downstream handlers need the event instance.</p>
     */
    public void clear() {
        this.payload = null;
        this.eventType = null;
        this.createdAt = 0L;
    }
}
