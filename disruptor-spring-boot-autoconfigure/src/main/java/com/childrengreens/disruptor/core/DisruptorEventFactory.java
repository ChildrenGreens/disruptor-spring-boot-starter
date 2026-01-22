package com.childrengreens.disruptor.core;

import com.lmax.disruptor.EventFactory;

/**
 * Factory for allocating {@link DisruptorEvent} instances in the RingBuffer.
 */
public class DisruptorEventFactory implements EventFactory<DisruptorEvent> {
    @Override
    public DisruptorEvent newInstance() {
        return new DisruptorEvent();
    }
}
