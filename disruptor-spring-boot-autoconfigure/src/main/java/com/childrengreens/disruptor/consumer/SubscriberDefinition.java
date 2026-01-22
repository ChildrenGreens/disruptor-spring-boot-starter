package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;

import java.lang.reflect.Method;

/**
 * Immutable description of a subscriber method or handler bean.
 */
public class SubscriberDefinition {
    private final Object bean;
    private final String beanName;
    private final Method method;
    private final Class<?> eventClass;
    private final String ring;
    private final Concurrency mode;
    private final int order;
    private final boolean batch;
    private final int batchSize;
    private final String eventType;
    private final ExceptionPolicy exceptionPolicy;

    public SubscriberDefinition(
            Object bean,
            String beanName,
            Method method,
            Class<?> eventClass,
            String ring,
            Concurrency mode,
            int order,
            boolean batch,
            int batchSize,
            String eventTypeName,
            ExceptionPolicy exceptionPolicy) {
        this.bean = bean;
        this.beanName = beanName;
        this.method = method;
        this.eventClass = eventClass;
        this.ring = ring;
        this.mode = mode;
        this.order = order;
        this.batch = batch;
        this.batchSize = batchSize;
        this.eventType = eventTypeName;
        this.exceptionPolicy = exceptionPolicy;
    }

    public Object getBean() {
        return bean;
    }

    public String getBeanName() {
        return beanName;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getEventClass() {
        return eventClass;
    }

    public String getRing() {
        return ring;
    }

    public Concurrency getMode() {
        return mode;
    }

    public int getOrder() {
        return order;
    }

    public boolean isBatch() {
        return batch;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public String getEventType() {
        return eventType;
    }

    public ExceptionPolicy getExceptionPolicy() {
        return exceptionPolicy;
    }

    public String getHandlerId() {
        if (method == null) {
            return beanName;
        }
        return beanName + "#" + method.getName();
    }
}
