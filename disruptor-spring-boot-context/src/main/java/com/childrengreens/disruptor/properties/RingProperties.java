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

import com.lmax.disruptor.dsl.ProducerType;

import java.time.Duration;

/**
 * Configuration properties for a single Disruptor ring.
 *
 * <p>Each ring represents an independent Disruptor instance with its own
 * RingBuffer, wait strategy, and exception handling configuration.</p>
 *
 * <p>Example configuration in application.yml:</p>
 * <pre>{@code
 * spring:
 *   disruptor:
 *     rings:
 *       order:
 *         buffer-size: 65536
 *         producer-type: MULTI
 *         wait-strategy: BLOCKING
 *         exception-handler: LOG_AND_CONTINUE
 * }</pre>
 *
 * @see DisruptorProperties
 * @see WaitStrategyType
 * @see ExceptionHandlerType
 */
public class RingProperties {

    /**
     * Size of the RingBuffer. Must be a power of 2 (e.g., 1024, 2048, 4096).
     * <p>Larger buffer sizes reduce the risk of blocking producers when
     * consumers are slow, but consume more memory.</p>
     * <p>Default: 1024</p>
     */
    private int bufferSize = 1024;

    /**
     * Producer type for the RingBuffer.
     * <ul>
     *   <li>{@code SINGLE} - Optimized for single-threaded producers. Provides
     *       better performance when only one thread publishes events.</li>
     *   <li>{@code MULTI} - Safe for multi-threaded producers. Use this when
     *       multiple threads may publish events concurrently.</li>
     * </ul>
     * <p>Default: MULTI</p>
     */
    private ProducerType producerType = ProducerType.MULTI;

    /**
     * Wait strategy used by consumers when no events are available.
     * <p>The choice of wait strategy affects CPU usage and latency:</p>
     * <ul>
     *   <li>{@code BLOCKING} - Lowest CPU usage, uses locks. Suitable when
     *       low latency is not critical.</li>
     *   <li>{@code SLEEPING} - Balanced CPU usage and latency. Uses a
     *       progressive backoff (spin → yield → sleep).</li>
     *   <li>{@code YIELDING} - Low latency with moderate CPU usage. Spins
     *       then yields to other threads.</li>
     *   <li>{@code BUSY_SPIN} - Lowest latency but highest CPU usage.
     *       Best for low-latency systems with dedicated CPU cores.</li>
     *   <li>{@code PHASED_BACKOFF} - Adaptive strategy that progressively
     *       backs off from spinning to blocking.</li>
     * </ul>
     * <p>Default: BLOCKING</p>
     *
     * @see WaitStrategyType
     * @see WaitStrategyConfig
     */
    private WaitStrategyType waitStrategy = WaitStrategyType.BLOCKING;

    /**
     * Exception handler type for this ring.
     * <ul>
     *   <li>{@code LOG_AND_CONTINUE} - Logs the exception and continues
     *       processing subsequent events.</li>
     *   <li>{@code LOG_AND_HALT} - Logs the exception and stops the
     *       Disruptor, preventing further event processing.</li>
     * </ul>
     * <p>Default: LOG_AND_CONTINUE</p>
     *
     * @see ExceptionHandlerType
     */
    private ExceptionHandlerType exceptionHandler = ExceptionHandlerType.LOG_AND_CONTINUE;

    /**
     * Advanced configuration for wait strategies that require additional parameters.
     * <p>Only applicable for certain wait strategies like TIMEOUT_BLOCKING,
     * LITE_TIMEOUT_BLOCKING, and PHASED_BACKOFF.</p>
     *
     * @see WaitStrategyConfig
     */
    private WaitStrategyConfig waitStrategyConfig = new WaitStrategyConfig();

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public ProducerType getProducerType() {
        return producerType;
    }

    public void setProducerType(ProducerType producerType) {
        this.producerType = producerType;
    }

    public WaitStrategyType getWaitStrategy() {
        return waitStrategy;
    }

    public void setWaitStrategy(WaitStrategyType waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    public ExceptionHandlerType getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandlerType exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public WaitStrategyConfig getWaitStrategyConfig() {
        return waitStrategyConfig;
    }

    public void setWaitStrategyConfig(WaitStrategyConfig waitStrategyConfig) {
        this.waitStrategyConfig = waitStrategyConfig;
    }

    /**
     * Advanced configuration parameters for wait strategies.
     *
     * <p>These settings only apply to wait strategies that support
     * configurable timeouts or fallback behaviors.</p>
     *
     * <p>Example configuration:</p>
     * <pre>{@code
     * spring:
     *   disruptor:
     *     rings:
     *       order:
     *         wait-strategy: PHASED_BACKOFF
     *         wait-strategy-config:
     *           phased-backoff-spin-timeout: 1us
     *           phased-backoff-yield-timeout: 1000us
     *           phased-backoff-fallback: BLOCKING
     * }</pre>
     */
    public static class WaitStrategyConfig {

        /**
         * Timeout for {@link WaitStrategyType#TIMEOUT_BLOCKING} strategy.
         * <p>The consumer will wait up to this duration before timing out
         * and checking for new events again.</p>
         * <p>Default: 1ms</p>
         */
        private Duration timeoutBlockingTimeout = Duration.ofMillis(1);

        /**
         * Timeout for {@link WaitStrategyType#LITE_TIMEOUT_BLOCKING} strategy.
         * <p>Similar to timeoutBlockingTimeout but for the lightweight
         * variant of timeout blocking strategy.</p>
         * <p>Default: 1ms</p>
         */
        private Duration liteTimeoutBlockingTimeout = Duration.ofMillis(1);

        /**
         * Spin timeout for {@link WaitStrategyType#PHASED_BACKOFF} strategy.
         * <p>Duration to spin before transitioning to the yield phase.
         * Spinning provides the lowest latency but consumes CPU.</p>
         * <p>Default: 1000ns (1 microsecond)</p>
         */
        private Duration phasedBackoffSpinTimeout = Duration.ofNanos(1000);

        /**
         * Yield timeout for {@link WaitStrategyType#PHASED_BACKOFF} strategy.
         * <p>Duration to yield before falling back to the fallback strategy.
         * Yielding allows other threads to run while still maintaining
         * relatively low latency.</p>
         * <p>Default: 1000000ns (1 millisecond)</p>
         */
        private Duration phasedBackoffYieldTimeout = Duration.ofNanos(1_000_000);

        /**
         * Fallback wait strategy for {@link WaitStrategyType#PHASED_BACKOFF}.
         * <p>After the spin and yield phases, this strategy is used for
         * waiting. Cannot be set to PHASED_BACKOFF (will default to YIELDING).</p>
         * <p>Default: YIELDING</p>
         */
        private WaitStrategyType phasedBackoffFallback = WaitStrategyType.YIELDING;

        public Duration getTimeoutBlockingTimeout() {
            return timeoutBlockingTimeout;
        }

        public void setTimeoutBlockingTimeout(Duration timeoutBlockingTimeout) {
            this.timeoutBlockingTimeout = timeoutBlockingTimeout;
        }

        public Duration getLiteTimeoutBlockingTimeout() {
            return liteTimeoutBlockingTimeout;
        }

        public void setLiteTimeoutBlockingTimeout(Duration liteTimeoutBlockingTimeout) {
            this.liteTimeoutBlockingTimeout = liteTimeoutBlockingTimeout;
        }

        public Duration getPhasedBackoffSpinTimeout() {
            return phasedBackoffSpinTimeout;
        }

        public void setPhasedBackoffSpinTimeout(Duration phasedBackoffSpinTimeout) {
            this.phasedBackoffSpinTimeout = phasedBackoffSpinTimeout;
        }

        public Duration getPhasedBackoffYieldTimeout() {
            return phasedBackoffYieldTimeout;
        }

        public void setPhasedBackoffYieldTimeout(Duration phasedBackoffYieldTimeout) {
            this.phasedBackoffYieldTimeout = phasedBackoffYieldTimeout;
        }

        public WaitStrategyType getPhasedBackoffFallback() {
            return phasedBackoffFallback;
        }

        public void setPhasedBackoffFallback(WaitStrategyType phasedBackoffFallback) {
            this.phasedBackoffFallback = phasedBackoffFallback;
        }
    }
}
