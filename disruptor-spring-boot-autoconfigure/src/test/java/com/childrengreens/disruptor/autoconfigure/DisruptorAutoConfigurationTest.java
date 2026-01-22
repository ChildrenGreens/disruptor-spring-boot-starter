package com.childrengreens.disruptor.autoconfigure;

import com.childrengreens.disruptor.consumer.ExceptionHandlerSupport;
import com.childrengreens.disruptor.consumer.HandlerAdapter;
import com.childrengreens.disruptor.consumer.SubscriberBeanPostProcessor;
import com.childrengreens.disruptor.consumer.SubscriberRegistry;
import com.childrengreens.disruptor.consumer.WorkerPoolSupport;
import com.childrengreens.disruptor.core.DefaultEventConverter;
import com.childrengreens.disruptor.core.DisruptorLifecycle;
import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.childrengreens.disruptor.core.DisruptorTemplate;
import com.childrengreens.disruptor.core.EventConverter;
import com.childrengreens.disruptor.properties.DisruptorProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DisruptorAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DisruptorAutoConfiguration.class));

    @Test
    void autoConfigurationCreatesCoreBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DisruptorProperties.class);
            assertThat(context).hasSingleBean(DisruptorMetrics.class);
            assertThat(context).hasSingleBean(HandlerAdapter.class);
            assertThat(context).hasSingleBean(ExceptionHandlerSupport.class);
            assertThat(context).hasSingleBean(WorkerPoolSupport.class);
            assertThat(context).hasSingleBean(DisruptorManager.class);
            assertThat(context).hasSingleBean(EventConverter.class);
            assertThat(context).getBean(EventConverter.class)
                    .isInstanceOf(DefaultEventConverter.class);
            assertThat(context).hasSingleBean(DisruptorTemplate.class);
            assertThat(context).hasSingleBean(DisruptorLifecycle.class);
            assertThat(context).hasSingleBean(SubscriberRegistry.class);
            assertThat(context).hasSingleBean(SubscriberBeanPostProcessor.class);
        });
    }

    @Test
    void autoConfigurationBacksOffWhenDisabled() {
        contextRunner.withPropertyValues("spring.disruptor.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DisruptorMetrics.class);
                    assertThat(context).doesNotHaveBean(DisruptorManager.class);
                    assertThat(context).doesNotHaveBean(DisruptorTemplate.class);
                });
    }
}
