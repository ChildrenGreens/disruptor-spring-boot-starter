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
