package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.annotation.DisruptorEventType;
import com.lmax.disruptor.RingBuffer;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.Order;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisruptorTemplateTest {
    @Test
    void publishesConvertedEventWithResolvedType() {
        RingBuffer<DisruptorEvent> ringBuffer =
                RingBuffer.createMultiProducer(new DisruptorEventFactory(), 8);
        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.getRingBuffer("default")).thenReturn(ringBuffer);

        DisruptorTemplate template = new DisruptorTemplate(
                manager,
                List.of(new AnnotatedEventConverter(), new DefaultEventConverter()),
                new DisruptorMetrics());

        template.publish(null, "payload");

        DisruptorEvent stored = ringBuffer.get(ringBuffer.getCursor());
        assertThat(stored.getPayload()).isInstanceOf(AnnotatedPayload.class);
        assertThat(stored.getEventType()).isEqualTo("custom");
        assertThat(stored.getCreatedAt()).isPositive();
    }

    @Test
    void usesProvidedRingName() {
        RingBuffer<DisruptorEvent> ringBuffer =
                RingBuffer.createMultiProducer(new DisruptorEventFactory(), 8);
        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.getRingBuffer("alpha")).thenReturn(ringBuffer);

        DisruptorTemplate template = new DisruptorTemplate(
                manager,
                List.of(new DefaultEventConverter()),
                null);

        template.publish("alpha", 1);
        DisruptorEvent stored = ringBuffer.get(ringBuffer.getCursor());
        assertThat(stored.getPayload()).isEqualTo(1);
        assertThat(stored.getEventType()).isEqualTo(Integer.class.getName());
    }

    @Test
    void throwsWhenNotRunning() {
        DisruptorTemplate template = new DisruptorTemplate(
                mock(DisruptorManager.class),
                List.of(new DefaultEventConverter()),
                null);
        assertThatThrownBy(() -> template.publish("default", "event"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Disruptor is not running");
    }

    @Test
    void throwsWhenRingMissing() {
        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.isRunning()).thenReturn(true);
        when(manager.getRingBuffer("default")).thenReturn(null);
        DisruptorTemplate template = new DisruptorTemplate(
                manager,
                List.of(new DefaultEventConverter()),
                null);
        assertThatThrownBy(() -> template.publish("", "event"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ring not found");
    }

    @DisruptorEventType("custom")
    static class AnnotatedPayload {
        private final String value;

        AnnotatedPayload(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    @Order(0)
    static class AnnotatedEventConverter implements EventConverter<AnnotatedPayload> {
        @Override
        public AnnotatedPayload convert(Object source) {
            return new AnnotatedPayload(String.valueOf(source));
        }

        @Override
        public boolean supports(Object source) {
            return source instanceof String;
        }
    }
}
