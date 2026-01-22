package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkerPoolSupportTest {
    @Test
    void delegatesToDisruptorWorkerPoolRegistration() {
        WorkerPoolSupport support = new WorkerPoolSupport();
        @SuppressWarnings("unchecked")
        Disruptor<DisruptorEvent> disruptor = mock(Disruptor.class);
        @SuppressWarnings("unchecked")
        WorkHandler<DisruptorEvent>[] handlers = new WorkHandler[]{event -> {
        }};

        support.handleWithWorkerPool(disruptor, handlers);

        verify(disruptor).handleEventsWithWorkerPool(handlers);
    }
}
