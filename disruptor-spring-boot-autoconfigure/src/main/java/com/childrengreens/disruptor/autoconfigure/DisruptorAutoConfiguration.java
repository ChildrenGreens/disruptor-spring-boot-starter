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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;

/**
 * Auto-configuration for core Disruptor beans.
 */
@AutoConfiguration
@EnableConfigurationProperties(DisruptorProperties.class)
@ConditionalOnProperty(prefix = "spring.disruptor", name = "enabled", matchIfMissing = true)
public class DisruptorAutoConfiguration {
    @Bean
    public DisruptorMetrics disruptorMetrics() {
        return new DisruptorMetrics();
    }

    @Bean
    public HandlerAdapter handlerAdapter(DisruptorMetrics metrics) {
        return new HandlerAdapter(metrics);
    }

    @Bean
    public ExceptionHandlerSupport exceptionHandlerSupport() {
        return new ExceptionHandlerSupport();
    }

    @Bean
    public WorkerPoolSupport workerPoolSupport() {
        return new WorkerPoolSupport();
    }

    @Bean
    public DisruptorManager disruptorManager(
            DisruptorProperties properties,
            SubscriberRegistry registry,
            HandlerAdapter handlerAdapter,
            ExceptionHandlerSupport exceptionHandlerSupport,
            WorkerPoolSupport workerPoolSupport) {
        return new DisruptorManager(
                properties, registry, handlerAdapter, exceptionHandlerSupport, workerPoolSupport);
    }

    @Bean
    public EventConverter<Object> defaultEventConverter() {
        return new DefaultEventConverter();
    }

    @Bean
    public DisruptorTemplate disruptorTemplate(
            DisruptorManager manager,
            @NonNull List<EventConverter<?>> converters,
            DisruptorMetrics metrics) {
        return new DisruptorTemplate(manager, converters, metrics);
    }

    @Bean
    public DisruptorLifecycle disruptorLifecycle(
            DisruptorManager manager, DisruptorProperties properties) {
        return new DisruptorLifecycle(manager, properties);
    }

    @Bean
    public SubscriberRegistry subscriberRegistry() {
        return new SubscriberRegistry();
    }

    @Bean
    public SubscriberBeanPostProcessor subscriberBeanPostProcessor(SubscriberRegistry registry) {
        return new SubscriberBeanPostProcessor(registry);
    }
}
