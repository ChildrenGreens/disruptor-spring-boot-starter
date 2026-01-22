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
import com.childrengreens.disruptor.annotation.DisruptorSubscriber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberBeanPostProcessorTest {
    @Test
    void registersMethodSubscriber() {
        SubscriberRegistry registry = new SubscriberRegistry();
        SubscriberBeanPostProcessor processor = new SubscriberBeanPostProcessor(registry);

        ValidSubscriber bean = new ValidSubscriber();
        processor.postProcessAfterInitialization(bean, "validSubscriber");

        assertThat(registry.getDefinitions()).hasSize(1);
        SubscriberDefinition definition = registry.getDefinitions().get(0);
        assertThat(definition.beanName()).isEqualTo("validSubscriber");
        assertThat(definition.ring()).isEqualTo("ringA");
        assertThat(definition.mode()).isEqualTo(Concurrency.MODE_HANDLER);
        assertThat(definition.method()).isNotNull();
    }

    @Test
    void registersClassLevelSubscriberMethods() {
        SubscriberRegistry registry = new SubscriberRegistry();
        SubscriberBeanPostProcessor processor = new SubscriberBeanPostProcessor(registry);

        ClassLevelSubscriber bean = new ClassLevelSubscriber();
        processor.postProcessAfterInitialization(bean, "classLevel");

        assertThat(registry.getDefinitions()).isNotEmpty();
        assertThat(registry.getDefinitions())
                .anyMatch(definition -> "classRing".equals(definition.ring())
                        && "handle".equals(definition.method().getName()));
    }

    @Test
    void skipsInvalidMethodSignature() {
        SubscriberRegistry registry = new SubscriberRegistry();
        SubscriberBeanPostProcessor processor = new SubscriberBeanPostProcessor(registry);

        InvalidSubscriber bean = new InvalidSubscriber();
        processor.postProcessAfterInitialization(bean, "invalid");

        assertThat(registry.getDefinitions()).isEmpty();
    }

    @Test
    void skipsBatchWorkerSubscriber() {
        SubscriberRegistry registry = new SubscriberRegistry();
        SubscriberBeanPostProcessor processor = new SubscriberBeanPostProcessor(registry);

        BatchWorkerSubscriber bean = new BatchWorkerSubscriber();
        processor.postProcessAfterInitialization(bean, "batchWorker");

        assertThat(registry.getDefinitions()).isEmpty();
    }


    static class ValidSubscriber {
        @DisruptorSubscriber(ring = "ringA", mode = Concurrency.MODE_HANDLER)
        public void handle(String payload) {
        }
    }

    @DisruptorSubscriber(ring = "classRing", mode = Concurrency.MODE_HANDLER)
    static class ClassLevelSubscriber {
        public void handle(String payload) {
        }
    }

    static class InvalidSubscriber {
        @DisruptorSubscriber
        public String invalidReturn(String payload) {
            return payload;
        }
    }

    static class BatchWorkerSubscriber {
        @DisruptorSubscriber(batch = true, mode = Concurrency.MODE_WORKER)
        public void handle(List<String> payloads) {
        }
    }

}
