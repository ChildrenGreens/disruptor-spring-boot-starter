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
 * Wait strategies for Disruptor consumers.
 *
 * <p>The wait strategy determines how consumers wait for new events when the
 * RingBuffer is empty. Different strategies offer trade-offs between CPU usage
 * and latency.</p>
 *
 * <h3>Strategy Selection Guide:</h3>
 * <table border="1">
 *   <tr><th>Strategy</th><th>CPU Usage</th><th>Latency</th><th>Use Case</th></tr>
 *   <tr><td>BLOCKING</td><td>Lowest</td><td>Highest</td><td>Low throughput, CPU conservation</td></tr>
 *   <tr><td>SLEEPING</td><td>Low</td><td>Medium</td><td>Balanced performance</td></tr>
 *   <tr><td>YIELDING</td><td>Medium</td><td>Low</td><td>Low latency with some CPU overhead</td></tr>
 *   <tr><td>BUSY_SPIN</td><td>Highest</td><td>Lowest</td><td>Ultra-low latency, dedicated cores</td></tr>
 * </table>
 *
 * <p>Configuration example:</p>
 * <pre>{@code
 * spring:
 *   disruptor:
 *     rings:
 *       order:
 *         wait-strategy: YIELDING
 * }</pre>
 *
 * @see RingProperties#getWaitStrategy()
 * @see RingProperties.WaitStrategyConfig
 */
public enum WaitStrategyType {

    /**
     * Lock-based blocking wait strategy.
     * <p>Uses a lock and condition variable to wait for events.
     * Provides the lowest CPU usage but highest latency.</p>
     * <p>Best for: Systems where CPU resources are limited or when
     * low latency is not critical.</p>
     *
     * @see com.lmax.disruptor.BlockingWaitStrategy
     */
    BLOCKING,

    /**
     * Blocking wait strategy with configurable timeout.
     * <p>Similar to BLOCKING but wakes up periodically based on timeout
     * to check for new events or handle interrupts.</p>
     * <p>Best for: Scenarios requiring periodic wake-ups or interruptibility.</p>
     * <p>Configure timeout via {@code wait-strategy-config.timeout-blocking-timeout}</p>
     *
     * @see com.lmax.disruptor.TimeoutBlockingWaitStrategy
     * @see RingProperties.WaitStrategyConfig#getTimeoutBlockingTimeout()
     */
    TIMEOUT_BLOCKING,

    /**
     * Lightweight blocking wait strategy.
     * <p>A variation of BLOCKING that avoids signaling when not necessary,
     * reducing contention in some scenarios.</p>
     * <p>Best for: Moderate throughput with lower lock contention.</p>
     *
     * @see com.lmax.disruptor.LiteBlockingWaitStrategy
     */
    LITE_BLOCKING,

    /**
     * Lightweight blocking wait strategy with configurable timeout.
     * <p>Combines the benefits of LITE_BLOCKING with periodic timeout wake-ups.</p>
     * <p>Configure timeout via {@code wait-strategy-config.lite-timeout-blocking-timeout}</p>
     *
     * @see com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy
     * @see RingProperties.WaitStrategyConfig#getLiteTimeoutBlockingTimeout()
     */
    LITE_TIMEOUT_BLOCKING,

    /**
     * Sleeping wait strategy with progressive backoff.
     * <p>Initially spins, then yields, then sleeps. Provides a good balance
     * between latency and CPU usage.</p>
     * <p>Best for: General-purpose use where balanced performance is desired.</p>
     *
     * @see com.lmax.disruptor.SleepingWaitStrategy
     */
    SLEEPING,

    /**
     * Yielding wait strategy.
     * <p>Spins then yields to other threads. Provides low latency while
     * still allowing other threads to make progress.</p>
     * <p>Best for: Low latency requirements with acceptable CPU overhead.
     * Good for systems with hyper-threading.</p>
     *
     * @see com.lmax.disruptor.YieldingWaitStrategy
     */
    YIELDING,

    /**
     * Busy spin wait strategy.
     * <p>Continuously spins without yielding. Provides the lowest latency
     * but consumes 100% CPU on the waiting thread.</p>
     * <p>Best for: Ultra-low latency systems with dedicated CPU cores.
     * Not recommended for general use.</p>
     * <p><strong>Warning:</strong> This strategy should only be used when
     * threads can be bound to specific CPU cores.</p>
     *
     * @see com.lmax.disruptor.BusySpinWaitStrategy
     */
    BUSY_SPIN,

    /**
     * Phased backoff wait strategy.
     * <p>Progressively backs off from spinning to yielding to a fallback
     * strategy. Provides adaptive latency based on contention.</p>
     * <p>Configure via {@code wait-strategy-config}:</p>
     * <ul>
     *   <li>{@code phased-backoff-spin-timeout} - Duration to spin before yielding</li>
     *   <li>{@code phased-backoff-yield-timeout} - Duration to yield before fallback</li>
     *   <li>{@code phased-backoff-fallback} - Fallback strategy (cannot be PHASED_BACKOFF)</li>
     * </ul>
     * <p>Best for: Systems requiring adaptive latency with configurable behavior.</p>
     *
     * @see com.lmax.disruptor.PhasedBackoffWaitStrategy
     * @see RingProperties.WaitStrategyConfig
     */
    PHASED_BACKOFF
}
