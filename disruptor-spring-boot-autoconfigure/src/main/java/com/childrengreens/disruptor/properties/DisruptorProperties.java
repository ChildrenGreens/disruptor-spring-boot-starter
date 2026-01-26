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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root configuration properties for the Disruptor Spring Boot Starter.
 *
 * <p>These properties control the global behavior of the Disruptor infrastructure,
 * including enabling/disabling the starter, shutdown behavior, and per-ring
 * configurations.</p>
 *
 * <p>Example configuration in application.yml:</p>
 * <pre>{@code
 * spring:
 *   disruptor:
 *     enabled: true
 *     shutdown-timeout: 30s
 *     shutdown-strategy: DRAIN
 *     rings:
 *       default:
 *         buffer-size: 65536
 *         producer-type: MULTI
 *         wait-strategy: BLOCKING
 *       audit:
 *         buffer-size: 16384
 *         producer-type: SINGLE
 *         wait-strategy: YIELDING
 * }</pre>
 *
 * @see RingProperties
 * @see ShutdownStrategy
 */
@ConfigurationProperties(prefix = "spring.disruptor")
public class DisruptorProperties {

    /**
     * Whether to enable the Disruptor starter.
     * <p>When set to {@code false}, no Disruptor beans will be created
     * and the starter is effectively disabled.</p>
     * <p>Default: true</p>
     */
    private boolean enabled = true;

    /**
     * Timeout duration for graceful shutdown.
     * <p>When the application context is closed, the Disruptor will attempt
     * to process remaining events within this timeout period before forcing
     * a halt.</p>
     * <p>Default: 30s</p>
     *
     * @see ShutdownStrategy
     */
    private Duration shutdownTimeout = Duration.ofSeconds(30);

    /**
     * Strategy to use when shutting down Disruptor rings.
     * <ul>
     *   <li>{@link ShutdownStrategy#DRAIN} - Attempt to process all remaining
     *       events in the RingBuffer before stopping. Recommended for most
     *       use cases to prevent data loss.</li>
     *   <li>{@link ShutdownStrategy#HALT} - Stop immediately without processing
     *       remaining events. Use when fast shutdown is more important than
     *       processing all events.</li>
     * </ul>
     * <p>Default: DRAIN</p>
     *
     * @see ShutdownStrategy
     */
    private ShutdownStrategy shutdownStrategy = ShutdownStrategy.DRAIN;

    /**
     * Configuration for individual Disruptor rings, keyed by ring name.
     * <p>Each entry defines a separate Disruptor instance with its own
     * RingBuffer, wait strategy, and exception handling configuration.</p>
     * <p>If no rings are configured, a "default" ring will be created
     * automatically with default settings.</p>
     * <p>Ring names are used when publishing events via
     * {@code DisruptorTemplate.publish(ringName, event)} and when
     * subscribing via {@code @DisruptorSubscriber(ring = "ringName")}.</p>
     *
     * @see RingProperties
     */
    private Map<String, RingProperties> rings = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
    }

    public ShutdownStrategy getShutdownStrategy() {
        return shutdownStrategy;
    }

    public void setShutdownStrategy(ShutdownStrategy shutdownStrategy) {
        this.shutdownStrategy = shutdownStrategy;
    }

    public Map<String, RingProperties> getRings() {
        return rings;
    }

    public void setRings(Map<String, RingProperties> rings) {
        this.rings = rings;
    }
}
