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
package com.childrengreens.disruptor.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RingPropertiesTest {
    @Test
    void defaultsAreApplied() {
        RingProperties properties = new RingProperties();
        assertThat(properties.getBufferSize()).isEqualTo(1024);
        assertThat(properties.getProducerType()).isEqualTo(ProducerType.MULTI);
        assertThat(properties.getWaitStrategy()).isEqualTo(WaitStrategyType.BLOCKING);
        assertThat(properties.getExceptionHandler()).isEqualTo(ExceptionHandlerType.LOG_AND_CONTINUE);
    }
}
