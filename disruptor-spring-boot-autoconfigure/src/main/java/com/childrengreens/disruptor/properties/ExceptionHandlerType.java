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
package com.childrengreens.disruptor.properties;

/**
 * Exception handling strategies for Disruptor event processing.
 *
 * <p>Defines how exceptions thrown by event handlers should be handled
 * at the ring level. This determines whether the Disruptor continues
 * processing subsequent events or stops entirely when an error occurs.</p>
 *
 * <p>Configuration example:</p>
 * <pre>{@code
 * spring:
 *   disruptor:
 *     rings:
 *       order:
 *         exception-handler: LOG_AND_CONTINUE
 * }</pre>
 *
 * @see RingProperties#getExceptionHandler()
 * @see com.childrengreens.disruptor.annotation.ExceptionPolicy
 */
public enum ExceptionHandlerType {

    /**
     * Log the exception and continue processing subsequent events.
     * <p>The exception details (including ring name, sequence number, and
     * event information) are logged at ERROR level, but the Disruptor
     * continues to process the next events in the RingBuffer.</p>
     * <p>Recommended for most production scenarios where event processing
     * failures should not block the entire pipeline.</p>
     */
    LOG_AND_CONTINUE,

    /**
     * Log the exception and halt the Disruptor.
     * <p>The exception is logged at ERROR level, and then the Disruptor
     * is stopped, preventing any further event processing.</p>
     * <p>Use this strategy when event processing failures indicate a
     * critical error that requires immediate attention and the system
     * should not continue processing potentially corrupted data.</p>
     */
    LOG_AND_HALT
}
