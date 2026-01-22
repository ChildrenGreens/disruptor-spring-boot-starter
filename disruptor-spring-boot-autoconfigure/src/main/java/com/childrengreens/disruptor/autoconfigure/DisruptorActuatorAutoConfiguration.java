package com.childrengreens.disruptor.autoconfigure;

import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.childrengreens.disruptor.metrics.DisruptorEndpoint;
import com.childrengreens.disruptor.metrics.DisruptorMeterBinder;
import com.childrengreens.disruptor.properties.DisruptorProperties;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for actuator endpoint and micrometer metrics.
 */
@AutoConfiguration
@ConditionalOnClass(Endpoint.class)
public class DisruptorActuatorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public DisruptorEndpoint disruptorEndpoint(
            DisruptorManager manager, DisruptorMetrics metrics) {
        return new DisruptorEndpoint(manager, metrics);
    }

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    public DisruptorMeterBinder disruptorMeterBinder(
            DisruptorManager manager, DisruptorMetrics metrics, DisruptorProperties properties) {
        return new DisruptorMeterBinder(manager, metrics, properties);
    }
}
