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
package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.annotation.DisruptorSubscriber;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

/**
 * Bean post-processor that scans {@link com.childrengreens.disruptor.annotation.DisruptorSubscriber}
 * and registers subscriber definitions for later Disruptor wiring.
 */
public class SubscriberBeanPostProcessor implements BeanPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(SubscriberBeanPostProcessor.class);

    private final SubscriberRegistry registry;

    public SubscriberBeanPostProcessor(SubscriberRegistry registry) {
        this.registry = registry;
    }

    /**
     * Scan bean methods and register {@link SubscriberDefinition} instances.
     */
    @Override
    public Object postProcessAfterInitialization(
            @NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        DisruptorSubscriber classAnnotation =
                AnnotationUtils.findAnnotation(targetClass, DisruptorSubscriber.class);
        boolean hasClassAnnotation = classAnnotation != null;
        boolean registered = false;

        Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(targetClass);
        for (Method method : methods) {
            if (method.isSynthetic()
                    || method.isBridge()
                    || Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            DisruptorSubscriber methodAnnotation =
                    AnnotationUtils.findAnnotation(method, DisruptorSubscriber.class);
            if (methodAnnotation == null && !hasClassAnnotation) {
                continue;
            }

            DisruptorSubscriber effective =
                    methodAnnotation != null ? methodAnnotation : classAnnotation;
            if (!isValidSubscriberMethod(method, effective)) {
                log.warn(
                        "Skip @DisruptorSubscriber method {}.{} due to invalid signature.",
                        targetClass.getName(),
                        method.getName());
                continue;
            }
            if (effective.batch() && effective.mode()
                    == com.childrengreens.disruptor.annotation.Concurrency.MODE_WORKER) {
                log.warn(
                        "Skip @DisruptorSubscriber method {}.{}: batch is not supported in worker mode.",
                        targetClass.getName(),
                        method.getName());
                continue;
            }

            Method invocable = AopUtils.selectInvocableMethod(method, targetClass);
            ReflectionUtils.makeAccessible(invocable);
            Class<?> eventType = invocable.getParameterTypes()[0];
            registry.register(
                    new SubscriberDefinition(
                            bean,
                            beanName,
                            invocable,
                            eventType,
                            effective.ring(),
                            effective.mode(),
                            effective.order(),
                            effective.batch(),
                            effective.batchSize(),
                            effective.eventType(),
                            effective.exceptionPolicy()));
            registered = true;
        }

        if (hasClassAnnotation && !registered) {
            registry.register(
                    new SubscriberDefinition(
                            bean,
                            beanName,
                            null,
                            null,
                            classAnnotation.ring(),
                            classAnnotation.mode(),
                            classAnnotation.order(),
                            classAnnotation.batch(),
                            classAnnotation.batchSize(),
                            classAnnotation.eventType(),
                            classAnnotation.exceptionPolicy()));
        }

        return bean;
    }

    /**
     * Validate subscriber method signature: void with exactly one argument.
     */
    private boolean isValidSubscriberMethod(Method method, DisruptorSubscriber subscriber) {
        if (!Void.TYPE.equals(method.getReturnType())) {
            return false;
        }
        if (method.getParameterCount() != 1) {
            return false;
        }
        if (subscriber.batch()) {
            return List.class.isAssignableFrom(method.getParameterTypes()[0]);
        }
        return true;
    }
}
