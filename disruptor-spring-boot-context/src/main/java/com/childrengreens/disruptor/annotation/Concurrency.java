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

/**
 * Subscriber concurrency models for Disruptor event processing.
 *
 * <p>Defines how subscribers consume events from the RingBuffer. The choice
 * affects event distribution and processing guarantees.</p>
 *
 * <h3>Model Comparison:</h3>
 * <table border="1">
 *   <tr><th>Model</th><th>Distribution</th><th>Use Case</th></tr>
 *   <tr><td>MODE_HANDLER</td><td>Broadcast (all handlers receive every event)</td><td>Event monitoring, logging, multi-stage processing</td></tr>
 *   <tr><td>MODE_WORKER</td><td>Competing (each event processed by one worker)</td><td>Load balancing, parallel task processing</td></tr>
 * </table>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Handler mode: both methods receive every event
 * @DisruptorSubscriber(mode = Concurrency.MODE_HANDLER)
 * public void logEvent(MyEvent event) { ... }
 *
 * @DisruptorSubscriber(mode = Concurrency.MODE_HANDLER)
 * public void processEvent(MyEvent event) { ... }
 *
 * // Worker mode: events are distributed among workers
 * @DisruptorSubscriber(mode = Concurrency.MODE_WORKER)
 * public void worker1(MyEvent event) { ... }
 *
 * @DisruptorSubscriber(mode = Concurrency.MODE_WORKER)
 * public void worker2(MyEvent event) { ... }
 * }</pre>
 *
 * @see DisruptorSubscriber#mode()
 */
public enum Concurrency {

    /**
     * Handler mode: broadcast event delivery.
     * <p>Every registered handler receives every event from the RingBuffer.
     * Handlers can be ordered using {@link DisruptorSubscriber#order()} to
     * create processing pipelines.</p>
     * <p>Best for:</p>
     * <ul>
     *   <li>Event logging and monitoring</li>
     *   <li>Multi-stage processing pipelines</li>
     *   <li>Scenarios where multiple consumers need the same event</li>
     * </ul>
     */
    MODE_HANDLER,

    /**
     * Worker mode: competing consumer delivery.
     * <p>Each event is processed by exactly one worker from the pool.
     * Events are distributed among available workers for parallel processing.</p>
     * <p>Best for:</p>
     * <ul>
     *   <li>Load balancing across multiple consumers</li>
     *   <li>Parallel task execution</li>
     *   <li>Scaling event processing horizontally</li>
     * </ul>
     * <p><strong>Note:</strong> Batch mode is not supported in worker mode.</p>
     */
    MODE_WORKER
}
