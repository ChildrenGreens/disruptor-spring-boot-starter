package com.childrengreens.disruptor.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DisruptorPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(DisruptorPropertiesConfig.class));

    @Test
    void bindsSimpleProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.disruptor.enabled=false",
                        "spring.disruptor.shutdown-timeout=5s",
                        "spring.disruptor.shutdown-strategy=HALT")
                .run(context -> {
                    DisruptorProperties properties = context.getBean(DisruptorProperties.class);
                    assertThat(properties.isEnabled()).isFalse();
                    assertThat(properties.getShutdownTimeout().toSeconds()).isEqualTo(5);
                    assertThat(properties.getShutdownStrategy()).isEqualTo(ShutdownStrategy.HALT);
                });
    }

    @Test
    void bindsRingProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.disruptor.rings.fast.buffer-size=2048",
                        "spring.disruptor.rings.fast.producer-type=SINGLE",
                        "spring.disruptor.rings.fast.wait-strategy=BUSY_SPIN",
                        "spring.disruptor.rings.fast.exception-handler=LOG_AND_HALT")
                .run(context -> {
                    DisruptorProperties properties = context.getBean(DisruptorProperties.class);
                    RingProperties ring = properties.getRings().get("fast");
                    assertThat(ring).isNotNull();
                    assertThat(ring.getBufferSize()).isEqualTo(2048);
                    assertThat(ring.getProducerType()).isEqualTo(ProducerType.SINGLE);
                    assertThat(ring.getWaitStrategy()).isEqualTo(WaitStrategyType.BUSY_SPIN);
                    assertThat(ring.getExceptionHandler()).isEqualTo(ExceptionHandlerType.LOG_AND_HALT);
                });
    }

    @Configuration
    @EnableConfigurationProperties(DisruptorProperties.class)
    static class DisruptorPropertiesConfig {
    }
}
