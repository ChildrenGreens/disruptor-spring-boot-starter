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
