package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;

import java.lang.reflect.Method;

/**
 * Immutable description of a subscriber method or handler bean.
 */
public record SubscriberDefinition(Object bean, String beanName, Method method, Class<?> eventClass, String ring,
                                   Concurrency mode, int order, boolean batch, int batchSize, String eventType,
                                   ExceptionPolicy exceptionPolicy) {

    public String getHandlerId() {
        if (method == null) {
            return beanName;
        }
        return beanName + "#" + method.getName();
    }
}
