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
package com.childrengreens.disruptor.core;

import com.childrengreens.disruptor.properties.DisruptorProperties;
import com.childrengreens.disruptor.properties.ShutdownStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DisruptorLifecycleTest {
    @Test
    void delegatesStartAndStopToManager() {
        DisruptorManager manager = mock(DisruptorManager.class);
        DisruptorProperties properties = new DisruptorProperties();
        properties.setShutdownTimeout(Duration.ofSeconds(5));
        properties.setShutdownStrategy(ShutdownStrategy.HALT);

        DisruptorLifecycle lifecycle = new DisruptorLifecycle(manager, properties);
        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        lifecycle.stop();
        assertThat(lifecycle.isRunning()).isFalse();
        verify(manager).start();
        verify(manager).stop(Duration.ofSeconds(5), ShutdownStrategy.HALT);
    }

    @Test
    void alwaysAutoStartsWithMaxPhase() {
        DisruptorLifecycle lifecycle =
                new DisruptorLifecycle(mock(DisruptorManager.class), new DisruptorProperties());
        assertThat(lifecycle.isAutoStartup()).isTrue();
        assertThat(lifecycle.getPhase()).isEqualTo(Integer.MAX_VALUE);
    }
}
