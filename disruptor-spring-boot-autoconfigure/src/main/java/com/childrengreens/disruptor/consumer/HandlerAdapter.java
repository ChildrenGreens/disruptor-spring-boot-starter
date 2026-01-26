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

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;
import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.WorkHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

/**
 * Adapter that converts subscriber definitions to Disruptor handlers.
 */
public class HandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(HandlerAdapter.class);
    private final DisruptorMetrics metrics;

    public HandlerAdapter(DisruptorMetrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Adapt handler-mode subscribers by ring.
     */
    public Map<String, List<EventHandler<DisruptorEvent>>> adaptEventHandlers(
            SubscriberRegistry registry) {
        return adapt(registry, Concurrency.MODE_HANDLER);
    }

    /**
     * Adapt worker-mode subscribers by ring.
     */
    public Map<String, List<WorkHandler<DisruptorEvent>>> adaptWorkHandlers(
            SubscriberRegistry registry) {
        Map<String, List<SubscriberDefinition>> grouped =
                groupByRing(registry, Concurrency.MODE_WORKER);
        Map<String, List<WorkHandler<DisruptorEvent>>> handlers = new LinkedHashMap<>();
        for (Map.Entry<String, List<SubscriberDefinition>> entry : grouped.entrySet()) {
            List<WorkHandler<DisruptorEvent>> adapted = new ArrayList<>();
            for (SubscriberDefinition definition : entry.getValue()) {
                WorkHandler<DisruptorEvent> handler = adaptWorkHandler(definition);
                if (handler != null) {
                    adapted.add(handler);
                }
            }
            if (!adapted.isEmpty()) {
                handlers.put(entry.getKey(), adapted);
            }
        }
        return handlers;
    }

    /**
     * Adapt subscribers for the given concurrency mode.
     */
    private Map<String, List<EventHandler<DisruptorEvent>>> adapt(
            SubscriberRegistry registry, Concurrency mode) {
        Map<String, List<SubscriberDefinition>> grouped = groupByRing(registry, mode);
        Map<String, List<EventHandler<DisruptorEvent>>> handlers = new LinkedHashMap<>();
        for (Map.Entry<String, List<SubscriberDefinition>> entry : grouped.entrySet()) {
            List<EventHandler<DisruptorEvent>> adapted = new ArrayList<>();
            for (SubscriberDefinition definition : entry.getValue()) {
                EventHandler<DisruptorEvent> handler = adaptEventHandler(definition);
                if (handler != null) {
                    adapted.add(handler);
                }
            }
            if (!adapted.isEmpty()) {
                handlers.put(entry.getKey(), adapted);
            }
        }
        return handlers;
    }

    /**
     * Group definitions by ring name.
     */
    private Map<String, List<SubscriberDefinition>> groupByRing(
            SubscriberRegistry registry, Concurrency mode) {
        return registry.getDefinitions().stream()
                .filter(def -> def.mode() == mode)
                .sorted(Comparator.comparingInt(SubscriberDefinition::order))
                .collect(Collectors.groupingBy(SubscriberDefinition::ring));
    }

    /**
     * Adapt a single definition to an {@link EventHandler} when possible.
     */
    public EventHandler<DisruptorEvent> adaptEventHandler(SubscriberDefinition definition) {
        if (definition.method() == null) {
            if (definition.bean() instanceof EventHandler) {
                @SuppressWarnings("unchecked")
                EventHandler<DisruptorEvent> handler =
                        (EventHandler<DisruptorEvent>) definition.bean();
                return new DelegatingEventHandler(definition, handler, metrics);
            }
            log.warn(
                    "Bean {} does not implement EventHandler, skip ring={}",
                    definition.beanName(),
                    definition.ring());
            return null;
        }
        return new MethodEventHandler(definition, metrics);
    }

    /**
     * Adapt a single definition to a {@link WorkHandler} when possible.
     */
    public WorkHandler<DisruptorEvent> adaptWorkHandler(SubscriberDefinition definition) {
        if (definition.method() == null) {
            if (definition.bean() instanceof WorkHandler) {
                @SuppressWarnings("unchecked")
                WorkHandler<DisruptorEvent> handler =
                        (WorkHandler<DisruptorEvent>) definition.bean();
                return new DelegatingWorkHandler(definition, handler, metrics);
            }
            log.warn(
                    "Bean {} does not implement WorkHandler, skip ring={}",
                    definition.beanName(),
                    definition.ring());
            return null;
        }
        if (definition.batch()) {
            log.warn(
                    "Batch mode is not supported for worker handlers. Skip bean={}",
                    definition.beanName());
            return null;
        }
        return new MethodWorkHandler(definition, metrics);
    }

    /**
     * Method-based handler adapter for handler mode.
     */
    private abstract static class BaseHandler {
        protected final SubscriberDefinition definition;
        protected final DisruptorMetrics metrics;

        private BaseHandler(SubscriberDefinition definition, DisruptorMetrics metrics) {
            this.definition = definition;
            this.metrics = metrics;
        }

        protected boolean matchesEventType(DisruptorEvent event) {
            String expected = definition.eventType();
            if (expected == null || expected.isEmpty()) {
                return true;
            }
            return expected.equals(event.getEventType());
        }

        protected void handleException(Throwable ex, Consumer<Throwable> logAction) {
            ExceptionPolicy policy = definition.exceptionPolicy();
            if (policy == ExceptionPolicy.LOG_AND_CONTINUE) {
                logAction.accept(ex);
                return;
            }
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }

        protected void recordMetrics(DisruptorEvent event) {
            if (metrics == null) {
                return;
            }
            metrics.recordConsume(definition.ring(), definition.getHandlerId());
            long createdAt = event.getCreatedAt();
            if (createdAt > 0) {
                metrics.recordLatency(
                        definition.ring(), Math.max(0, System.currentTimeMillis() - createdAt));
            }
        }
    }

    private static final class MethodEventHandler extends BaseHandler
            implements EventHandler<DisruptorEvent> {
        private final Object target;
        private final Method method;
        private final List<Object> batchBuffer = new ArrayList<>();

        private MethodEventHandler(SubscriberDefinition definition, DisruptorMetrics metrics) {
            super(definition, metrics);
            this.target = definition.bean();
            this.method = definition.method();
        }

        @Override
        public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
            if (!matchesEventType(event)) {
                return;
            }
            boolean flushing = false;
            try {
                if (definition.batch()) {
                    batchBuffer.add(event.getPayload());
                    if (shouldFlush(endOfBatch)) {
                        flushing = true;
                        ReflectionUtils.invokeMethod(method, target, new ArrayList<>(batchBuffer));
                        batchBuffer.clear();
                    }
                } else {
                    ReflectionUtils.invokeMethod(method, target, event.getPayload());
                }
                recordMetrics(event);
            } catch (Throwable ex) {
                if (definition.batch() && flushing) {
                    batchBuffer.clear();
                }
                handleException(
                        ex,
                        throwable -> log.warn(
                                "Subscriber invocation failed (bean={} method={}).",
                                definition.beanName(),
                                method.getName(),
                                throwable));
            }
        }

        private boolean shouldFlush(boolean endOfBatch) {
            if (!definition.batch()) {
                return false;
            }
            int batchSize = definition.batchSize();
            if (batchSize > 0 && batchBuffer.size() >= batchSize) {
                return true;
            }
            return endOfBatch;
        }
    }

    /**
     * Method-based handler adapter for worker mode.
     */
    private static final class MethodWorkHandler extends BaseHandler
            implements WorkHandler<DisruptorEvent> {
        private final Object target;
        private final Method method;

        private MethodWorkHandler(SubscriberDefinition definition, DisruptorMetrics metrics) {
            super(definition, metrics);
            this.target = definition.bean();
            this.method = definition.method();
        }

        @Override
        public void onEvent(DisruptorEvent event) {
            if (!matchesEventType(event)) {
                if (event != null) {
                    event.clear();
                }
                return;
            }
            try {
                ReflectionUtils.invokeMethod(method, target, event.getPayload());
                recordMetrics(event);
            } catch (Throwable ex) {
                handleException(
                        ex,
                        throwable -> log.warn(
                                "Subscriber invocation failed (bean={} method={}).",
                                definition.beanName(),
                                method.getName(),
                                throwable));
            } finally {
                if (event != null) {
                    event.clear();
                }
            }
        }
    }

    private static final class DelegatingEventHandler extends BaseHandler
            implements EventHandler<DisruptorEvent> {
        private final EventHandler<DisruptorEvent> delegate;

        private DelegatingEventHandler(
                SubscriberDefinition definition,
                EventHandler<DisruptorEvent> delegate,
                DisruptorMetrics metrics) {
            super(definition, metrics);
            this.delegate = delegate;
        }

        @Override
        public void onEvent(DisruptorEvent event, long sequence, boolean endOfBatch) {
            if (!matchesEventType(event)) {
                return;
            }
            try {
                delegate.onEvent(event, sequence, endOfBatch);
                recordMetrics(event);
            } catch (Throwable ex) {
                handleException(
                        ex,
                        throwable -> log.warn(
                                "Subscriber invocation failed (bean={}).",
                                definition.beanName(),
                                throwable));
            }
        }

    }

    private static final class DelegatingWorkHandler extends BaseHandler
            implements WorkHandler<DisruptorEvent> {
        private final WorkHandler<DisruptorEvent> delegate;

        private DelegatingWorkHandler(
                SubscriberDefinition definition,
                WorkHandler<DisruptorEvent> delegate,
                DisruptorMetrics metrics) {
            super(definition, metrics);
            this.delegate = delegate;
        }

        @Override
        public void onEvent(DisruptorEvent event) {
            if (!matchesEventType(event)) {
                if (event != null) {
                    event.clear();
                }
                return;
            }
            try {
                delegate.onEvent(event);
                recordMetrics(event);
            } catch (Throwable ex) {
                handleException(
                        ex,
                        throwable -> log.warn(
                                "Subscriber invocation failed (bean={}).",
                                definition.beanName(),
                                throwable));
            } finally {
                if (event != null) {
                    event.clear();
                }
            }
        }
    }
}
