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
package com.childrengreens.disruptor.autoconfigure;

import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.childrengreens.disruptor.metrics.DisruptorEndpoint;
import com.childrengreens.disruptor.metrics.DisruptorMeterBinder;

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
            DisruptorManager manager, DisruptorMetrics metrics) {
        return new DisruptorMeterBinder(manager, metrics);
    }
}
