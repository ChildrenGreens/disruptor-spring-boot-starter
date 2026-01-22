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
