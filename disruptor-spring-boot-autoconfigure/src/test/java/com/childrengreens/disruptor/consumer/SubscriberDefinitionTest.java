package com.childrengreens.disruptor.consumer;

import com.childrengreens.disruptor.annotation.Concurrency;
import com.childrengreens.disruptor.annotation.ExceptionPolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberDefinitionTest {
    @Test
    void handlerIdFallsBackToBeanNameForHandlerBeans() {
        SubscriberDefinition definition = new SubscriberDefinition(
                new Object(),
                "beanName",
                null,
                Object.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.DELEGATE);
        assertThat(definition.getHandlerId()).isEqualTo("beanName");
    }

    @Test
    void handlerIdIncludesMethodNameWhenPresent() throws Exception {
        Method method = SubscriberDefinitionTest.class.getDeclaredMethod("sample", String.class);
        SubscriberDefinition definition = new SubscriberDefinition(
                this,
                "beanName",
                method,
                String.class,
                "ring",
                Concurrency.MODE_HANDLER,
                0,
                false,
                0,
                "",
                ExceptionPolicy.DELEGATE);
        assertThat(definition.getHandlerId()).isEqualTo("beanName#sample");
    }

    private void sample(String payload) {
    }
}
