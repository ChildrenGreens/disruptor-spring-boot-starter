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

    public static class FailingSubscriber {
        public void handle(String payload) {
            throw new IllegalStateException("boom");
        }
    }
}
