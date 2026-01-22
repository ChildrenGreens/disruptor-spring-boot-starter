package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * Helper to register worker pool handlers on a Disruptor instance.
 */
public class WorkerPoolSupport {
    /**
     * Register worker pool handlers on a Disruptor instance.
     */
    public void handleWithWorkerPool(
            Disruptor<DisruptorEvent> disruptor, WorkHandler<DisruptorEvent>[] handlers) {
        disruptor.handleEventsWithWorkerPool(handlers);
    }
}
