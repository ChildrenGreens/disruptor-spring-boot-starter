package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriberRegistryTest {
    @Test
    void registersAndReturnsImmutableDefinitions() {
        SubscriberRegistry registry = new SubscriberRegistry();
        registry.register(new SubscriberDefinition(
                new Object(),
                "bean",
                null,
                Object.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.DELEGATE));

        assertThat(registry.getDefinitions()).hasSize(1);
        assertThatThrownBy(() -> registry.getDefinitions().add(new SubscriberDefinition(
                new Object(),
                "another",
                null,
                Object.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.DELEGATE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
