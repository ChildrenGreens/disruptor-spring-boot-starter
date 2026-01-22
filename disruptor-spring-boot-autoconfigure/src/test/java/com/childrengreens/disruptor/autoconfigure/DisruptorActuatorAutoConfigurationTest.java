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
import com.childrengreens.disruptor.properties.DisruptorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DisruptorActuatorAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DisruptorActuatorAutoConfiguration.class))
            .withBean(DisruptorManager.class, () -> mock(DisruptorManager.class))
            .withBean(DisruptorMetrics.class, DisruptorMetrics::new)
            .withBean(DisruptorProperties.class, DisruptorProperties::new);

    @Test
    void autoConfigurationCreatesEndpointAndMeterBinder() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DisruptorEndpoint.class);
            assertThat(context).hasSingleBean(DisruptorMeterBinder.class);
        });
    }

    @Test
    void autoConfigurationBacksOffWhenEndpointAlreadyDefined() {
        contextRunner.withBean(DisruptorEndpoint.class, () -> mock(DisruptorEndpoint.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DisruptorEndpoint.class);
                    assertThat(context.getBean(DisruptorEndpoint.class))
                            .isInstanceOf(DisruptorEndpoint.class);
                });
    }
}
