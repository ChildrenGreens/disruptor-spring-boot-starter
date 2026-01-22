package com.childrengreens.disruptor.annotation;

/**
 * Per-subscriber exception handling policy.
 */
public enum ExceptionPolicy {
    /**
     * Defer to ring-level exception handler.
     */
    DELEGATE,
    /**
     * Log and continue processing.
     */
    LOG_AND_CONTINUE,
    /**
     * Throw and let Disruptor exception handler decide.
     */
    THROW
}
