package com.childrengreens.disruptor.core;

public interface EventPublisher {
    /**
     * Publish an event payload to the given ring.
     */
    void publish(String ring, Object event);
}
