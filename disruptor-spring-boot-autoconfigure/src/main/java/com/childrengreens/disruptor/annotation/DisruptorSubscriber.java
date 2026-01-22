package com.childrengreens.disruptor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DisruptorSubscriber {
    /**
     * Target ring name (default ring if not specified).
     */
    String ring() default "default";

    /**
     * Concurrency model: handler = broadcast, worker = competing consumers.
     */
    Concurrency mode() default Concurrency.MODE_HANDLER;

    /**
     * Order within handler chain; lower values execute first.
     */
    int order() default 0;

    /**
     * Enable batch delivery (handler mode only).
     */
    boolean batch() default false;

    /**
     * Max batch size; 0 means flush only on endOfBatch.
     */
    int batchSize() default 0;

    /**
     * Optional event type filter; empty means no filtering.
     */
    String eventType() default "";

    /**
     * Per-subscriber exception policy.
     */
    ExceptionPolicy exceptionPolicy() default ExceptionPolicy.DELEGATE;
}
