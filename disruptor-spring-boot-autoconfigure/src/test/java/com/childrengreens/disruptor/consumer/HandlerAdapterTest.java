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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandlerAdapterTest {
    @Test
    void adaptsMethodHandlerAndRecordsMetrics() throws Exception {
        DisruptorMetrics metrics = new DisruptorMetrics();
        HandlerAdapter adapter = new HandlerAdapter(metrics);
        TestSubscriber subscriber = new TestSubscriber();
        Method method = TestSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);

        EventHandler<DisruptorEvent> handler = adapter.adaptEventHandler(definition);
        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("hello");
        event.setEventType("type");
        event.setCreatedAt(System.currentTimeMillis());
        handler.onEvent(event, 0L, true);

        assertThat(subscriber.payloads).containsExactly("hello");
        assertThat(metrics.getConsumeCount("ring")).isEqualTo(1);
    }

    @Test
    void skipsMismatchedEventType() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        TestSubscriber subscriber = new TestSubscriber();
        Method method = TestSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "expected",
                ExceptionPolicy.LOG_AND_CONTINUE);

        EventHandler<DisruptorEvent> handler = adapter.adaptEventHandler(definition);
        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("hello");
        event.setEventType("other");
        handler.onEvent(event, 0L, true);

        assertThat(subscriber.payloads).isEmpty();
    }

    @Test
    void supportsBatchDelivery() throws Exception {
        DisruptorMetrics metrics = new DisruptorMetrics();
        HandlerAdapter adapter = new HandlerAdapter(metrics);
        TestSubscriber subscriber = new TestSubscriber();
        Method method = TestSubscriber.class.getDeclaredMethod("handleBatch", List.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                List.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                true,
                2,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);

        EventHandler<DisruptorEvent> handler = adapter.adaptEventHandler(definition);

        DisruptorEvent first = new DisruptorEvent();
        first.setPayload("a");
        handler.onEvent(first, 0L, false);
        DisruptorEvent second = new DisruptorEvent();
        second.setPayload("b");
        handler.onEvent(second, 1L, false);

        assertThat(subscriber.batches).hasSize(1);
        assertThat(subscriber.batches.get(0)).containsExactly("a", "b");
        assertThat(metrics.getConsumeCount("ring")).isEqualTo(2);
    }

    @Test
    void logsAndContinuesOnExceptionWhenPolicyAllows() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(null);
        FailingSubscriber subscriber = new FailingSubscriber();
        Method method = FailingSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);

        EventHandler<DisruptorEvent> handler = adapter.adaptEventHandler(definition);
        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("boom");
        handler.onEvent(event, 0L, true);
    }

    @Test
    void throwsOnExceptionWhenPolicyIsThrow() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(null);
        FailingSubscriber subscriber = new FailingSubscriber();
        Method method = FailingSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.THROW);

        EventHandler<DisruptorEvent> handler = adapter.adaptEventHandler(definition);
        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("boom");
        assertThatThrownBy(() -> handler.onEvent(event, 0L, true))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void adaptsWorkHandlerAndRespectsBatchConstraint() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        TestSubscriber subscriber = new TestSubscriber();
        Method method = TestSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                true,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);

        assertThat(adapter.adaptWorkHandler(definition)).isNull();
    }

    @Test
    void adaptsDelegatingHandlers() {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        EventHandler<DisruptorEvent> handler = (event, sequence, endOfBatch) -> {
        };
        SubscriberDefinition definition = new SubscriberDefinition(
                handler,
                "handlerBean",
                null,
                DisruptorEvent.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);
        EventHandler<DisruptorEvent> adapted = adapter.adaptEventHandler(definition);
        assertThat(adapted).isNotNull();

        WorkHandler<DisruptorEvent> workHandler = event -> {
        };
        SubscriberDefinition workerDefinition = new SubscriberDefinition(
                workHandler,
                "workHandlerBean",
                null,
                DisruptorEvent.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);
        WorkHandler<DisruptorEvent> adaptedWorker = adapter.adaptWorkHandler(workerDefinition);
        assertThat(adaptedWorker).isNotNull();
    }

    @Test
    void adaptsWorkHandlerMethodsAndRecordsMetrics() throws Exception {
        DisruptorMetrics metrics = new DisruptorMetrics();
        HandlerAdapter adapter = new HandlerAdapter(metrics);
        TestSubscriber subscriber = new TestSubscriber();
        Method method = TestSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                false,
                0,
                "expected",
                ExceptionPolicy.LOG_AND_CONTINUE);

        WorkHandler<DisruptorEvent> handler = adapter.adaptWorkHandler(definition);
        DisruptorEvent mismatch = new DisruptorEvent();
        mismatch.setPayload("skip");
        mismatch.setEventType("other");
        handler.onEvent(mismatch);

        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("ok");
        event.setEventType("expected");
        event.setCreatedAt(System.currentTimeMillis());
        handler.onEvent(event);

        assertThat(subscriber.payloads).containsExactly("ok");
        assertThat(metrics.getConsumeCount("ring")).isEqualTo(1);
    }

    @Test
    void delegatingHandlersRespectExceptionPolicies() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        EventHandler<DisruptorEvent> failingHandler = (event, sequence, endOfBatch) -> {
            throw new IllegalStateException("boom");
        };
        SubscriberDefinition definition = new SubscriberDefinition(
                failingHandler,
                "handlerBean",
                null,
                DisruptorEvent.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);
        EventHandler<DisruptorEvent> adapted = adapter.adaptEventHandler(definition);
        adapted.onEvent(new DisruptorEvent(), 0L, true);

        WorkHandler<DisruptorEvent> workHandler = event -> {
            throw new IllegalStateException("boom");
        };
        SubscriberDefinition workerDefinition = new SubscriberDefinition(
                workHandler,
                "workHandlerBean",
                null,
                DisruptorEvent.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.THROW);
        WorkHandler<DisruptorEvent> adaptedWorker = adapter.adaptWorkHandler(workerDefinition);
        assertThatThrownBy(() -> adaptedWorker.onEvent(new DisruptorEvent()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void returnsNullWhenBeanDoesNotImplementHandlerInterfaces() {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        Object bean = new Object();
        SubscriberDefinition definition = new SubscriberDefinition(
                bean,
                "bean",
                null,
                Object.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);
        assertThat(adapter.adaptEventHandler(definition)).isNull();

        SubscriberDefinition workerDefinition = new SubscriberDefinition(
                bean,
                "bean",
                null,
                Object.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);
        assertThat(adapter.adaptWorkHandler(workerDefinition)).isNull();
    }

    @Test
    void delegatingWorkHandlerSkipsMismatchedEventTypeAndClearsEvent() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        AtomicInteger invoked = new AtomicInteger();
        WorkHandler<DisruptorEvent> workHandler = event -> invoked.incrementAndGet();
        SubscriberDefinition definition = new SubscriberDefinition(
                workHandler,
                "workHandlerBean",
                null,
                DisruptorEvent.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                false,
                0,
                "expected",
                ExceptionPolicy.LOG_AND_CONTINUE);
        WorkHandler<DisruptorEvent> adapted = adapter.adaptWorkHandler(definition);

        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("payload");
        event.setEventType("other");
        adapted.onEvent(event);

        assertThat(invoked.get()).isEqualTo(0);
        assertThat(event.getPayload()).isNull();
        assertThat(event.getEventType()).isNull();
    }

    @Test
    void delegatingEventHandlerThrowsWhenPolicyIsThrow() {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        EventHandler<DisruptorEvent> failingHandler = (event, sequence, endOfBatch) -> {
            throw new IllegalStateException("boom");
        };
        SubscriberDefinition definition = new SubscriberDefinition(
                failingHandler,
                "handlerBean",
                null,
                DisruptorEvent.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.THROW);

        EventHandler<DisruptorEvent> adapted = adapter.adaptEventHandler(definition);
        assertThatThrownBy(() -> adapted.onEvent(new DisruptorEvent(), 0L, true))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void workHandlerLogsAndContinuesWhenPolicyAllows() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        FailingSubscriber subscriber = new FailingSubscriber();
        Method method = FailingSubscriber.class.getDeclaredMethod("handle", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                String.class,
                "ring",
                Concurrency.MODE_WORKER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);
        WorkHandler<DisruptorEvent> handler = adapter.adaptWorkHandler(definition);
        DisruptorEvent event = new DisruptorEvent();
        event.setPayload("boom");
        handler.onEvent(event);
    }

    @Test
    void batchClearsOnFlushException() throws Exception {
        HandlerAdapter adapter = new HandlerAdapter(new DisruptorMetrics());
        FlakyBatchSubscriber subscriber = new FlakyBatchSubscriber();
        Method method = FlakyBatchSubscriber.class.getDeclaredMethod("handleBatch", List.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                subscriber,
                "subscriber",
                method,
                List.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                true,
                2,
                "",
                ExceptionPolicy.LOG_AND_CONTINUE);

        EventHandler<DisruptorEvent> handler = adapter.adaptEventHandler(definition);
        handler.onEvent(eventWithPayload("a"), 0L, false);
        handler.onEvent(eventWithPayload("b"), 1L, false);
        handler.onEvent(eventWithPayload("c"), 2L, false);
        handler.onEvent(eventWithPayload("d"), 3L, false);

        assertThat(subscriber.batches).hasSize(1);
        assertThat(subscriber.batches.get(0)).containsExactly("c", "d");
    }

    private DisruptorEvent eventWithPayload(String payload) {
        DisruptorEvent event = new DisruptorEvent();
        event.setPayload(payload);
        return event;
    }

    public static class TestSubscriber {
        private final List<String> payloads = new ArrayList<>();
        private final List<List<String>> batches = new ArrayList<>();

        public void handle(String payload) {
            payloads.add(payload);
        }

        public void handleBatch(List<String> payloads) {
            batches.add(payloads);
        }
    }

    public static class FlakyBatchSubscriber {
        private final List<List<String>> batches = new ArrayList<>();
        private boolean fail = true;

        public void handleBatch(List<String> payloads) {
            if (fail) {
                fail = false;
                throw new IllegalStateException("boom");
            }
            batches.add(payloads);
        }
    }

    public static class FailingSubscriber {
        public void handle(String payload) {
            throw new IllegalStateException("boom");
        }
    }
}
