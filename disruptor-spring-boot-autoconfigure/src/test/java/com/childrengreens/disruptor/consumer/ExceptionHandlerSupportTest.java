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

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.properties.ExceptionHandlerType;
import com.lmax.disruptor.ExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionHandlerSupportTest {
    @Test
    void logAndContinueDoesNotThrow() {
        ExceptionHandlerSupport support = new ExceptionHandlerSupport();
        ExceptionHandler<DisruptorEvent> handler =
                support.create(ExceptionHandlerType.LOG_AND_CONTINUE, "ring");
        handler.handleEventException(new RuntimeException("boom"), 1L, null);
        handler.handleOnStartException(new RuntimeException("boom"));
        handler.handleOnShutdownException(new RuntimeException("boom"));
    }

    @Test
    void logAndHaltThrowsOnEventAndStart() {
        ExceptionHandlerSupport support = new ExceptionHandlerSupport();
        ExceptionHandler<DisruptorEvent> handler =
                support.create(ExceptionHandlerType.LOG_AND_HALT, "ring");
        assertThatThrownBy(() -> handler.handleEventException(new RuntimeException("boom"), 1L, null))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> handler.handleOnStartException(new RuntimeException("boom")))
                .isInstanceOf(RuntimeException.class);
    }
}
