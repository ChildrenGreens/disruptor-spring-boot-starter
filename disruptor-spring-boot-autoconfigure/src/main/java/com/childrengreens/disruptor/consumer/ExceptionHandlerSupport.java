package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.properties.ExceptionHandlerType;
import com.lmax.disruptor.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for Disruptor {@link com.lmax.disruptor.ExceptionHandler} strategies.
 */
public class ExceptionHandlerSupport {
    /**
     * Create an exception handler for the given ring and strategy.
     */
    public ExceptionHandler<DisruptorEvent> create(ExceptionHandlerType type, String ring) {
        if (type == null) {
            return new LoggingExceptionHandler(ring, false);
        }
        return switch (type) {
            case LOG_AND_HALT -> new LoggingExceptionHandler(ring, true);
            default -> new LoggingExceptionHandler(ring, false);
        };
    }

    /**
     * Logging exception handler with optional fail-fast behavior.
     */
    private static final class LoggingExceptionHandler implements ExceptionHandler<DisruptorEvent> {
        private static final Logger log = LoggerFactory.getLogger(LoggingExceptionHandler.class);
        private final String ring;
        private final boolean halt;

        private LoggingExceptionHandler(String ring, boolean halt) {
            this.ring = ring;
            this.halt = halt;
        }

        @Override
        public void handleEventException(Throwable ex, long sequence, DisruptorEvent event) {
            log.error(
                    "Disruptor event exception on ring={} sequence={} eventType={} payload={}",
                    ring,
                    sequence,
                    event == null ? null : event.getEventType(),
                    event == null ? null : event.getPayload(),
                    ex);
            if (halt) {
                throw new RuntimeException("Disruptor handler halted ring=" + ring, ex);
            }
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            log.error("Disruptor ring {} start exception", ring, ex);
            if (halt) {
                throw new RuntimeException("Disruptor start halted ring=" + ring, ex);
            }
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            log.error("Disruptor ring {} shutdown exception", ring, ex);
        }
    }
}
