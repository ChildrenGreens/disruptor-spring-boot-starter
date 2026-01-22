package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.properties.ExceptionHandlerType;
import com.lmax.disruptor.ExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionHandlerSupportTest {
    @Test
    void logAndContinueDoesNotThrow() {
        ExceptionHandlerSupport support = new ExceptionHandlerSupport();
        ExceptionHandler<DisruptorEvent> handler =
                support.create(ExceptionHandlerType.LOG_AND_CONTINUE, "ring");
        handler.handleEventException(new RuntimeException("boom"), 1L, null);
        handler.handleOnStartException(new RuntimeException("boom"));
        handler.handleOnShutdownException(new RuntimeException("boom"));
    }

    @Test
    void logAndHaltThrowsOnEventAndStart() {
        ExceptionHandlerSupport support = new ExceptionHandlerSupport();
        ExceptionHandler<DisruptorEvent> handler =
                support.create(ExceptionHandlerType.LOG_AND_HALT, "ring");
        assertThatThrownBy(() -> handler.handleEventException(new RuntimeException("boom"), 1L, null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> handler.handleOnStartException(new RuntimeException("boom")))
                .isInstanceOf(RuntimeException.class);
    }
}
