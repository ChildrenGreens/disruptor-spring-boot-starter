package com.childrengreens.disruptor.properties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration properties for the Disruptor starter.
 */
@ConfigurationProperties(prefix = "spring.disruptor")
public class DisruptorProperties {
    private boolean enabled = true;
    private Duration shutdownTimeout = Duration.ofSeconds(30);
    private ShutdownStrategy shutdownStrategy = ShutdownStrategy.DRAIN;
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
