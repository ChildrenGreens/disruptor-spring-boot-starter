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
 * Shutdown strategies for Disruptor rings.
 *
 * <p>Defines how the Disruptor should behave when the Spring application
 * context is closed. The choice affects whether remaining events in the
 * RingBuffer are processed or discarded.</p>
 *
 * <p>Configuration example:</p>
 * <pre>{@code
 * spring:
 *   disruptor:
 *     shutdown-timeout: 30s
 *     shutdown-strategy: DRAIN
 * }</pre>
 *
 * @see DisruptorProperties#getShutdownStrategy()
 * @see DisruptorProperties#getShutdownTimeout()
 */
public enum ShutdownStrategy {

    /**
     * Graceful shutdown: attempt to process all remaining events.
     * <p>When shutdown is triggered, the Disruptor will:</p>
     * <ol>
     *   <li>Stop accepting new events</li>
     *   <li>Wait for consumers to process all remaining events in the RingBuffer</li>
     *   <li>Respect the configured {@code shutdown-timeout}</li>
     *   <li>Force halt if timeout is exceeded</li>
     * </ol>
     * <p>Recommended for most production scenarios to prevent data loss.</p>
     */
    DRAIN,

    /**
     * Immediate shutdown: stop without processing remaining events.
     * <p>When shutdown is triggered, the Disruptor will:</p>
     * <ol>
     *   <li>Immediately halt all event processing</li>
     *   <li>Discard any unprocessed events in the RingBuffer</li>
     * </ol>
     * <p>Use when:</p>
     * <ul>
     *   <li>Fast shutdown is critical</li>
     *   <li>Remaining events can be safely discarded</li>
     *   <li>Events are persisted elsewhere and can be replayed</li>
     * </ul>
     * <p><strong>Warning:</strong> Events in the RingBuffer will be lost.</p>
     */
    HALT
}
