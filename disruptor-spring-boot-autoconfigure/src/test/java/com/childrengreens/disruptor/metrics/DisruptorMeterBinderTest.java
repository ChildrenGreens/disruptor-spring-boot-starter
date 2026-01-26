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
package com.childrengreens.disruptor.metrics;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.core.DisruptorEventFactory;
import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.lmax.disruptor.RingBuffer;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisruptorMeterBinderTest {
    @Test
    void registersMetersForDefaultRing() {
        RingBuffer<DisruptorEvent> ringBuffer =
                RingBuffer.createMultiProducer(new DisruptorEventFactory(), 8);

        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.getRingBuffer("default")).thenReturn(ringBuffer);
        when(manager.getResolvedRingNames()).thenReturn(List.of("default"));

        DisruptorMetrics metrics = new DisruptorMetrics();
        metrics.recordPublish("default");
        metrics.recordConsume("default", "handlerA");

        DisruptorMeterBinder binder = new DisruptorMeterBinder(manager, metrics);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        binder.bindTo(registry);

        Gauge backlog = registry.get("disruptor.ringbuffer.backlog")
                .tag("ring", "default")
                .gauge();
        assertThat(backlog.value()).isGreaterThanOrEqualTo(0);

        FunctionCounter publishCount = registry.get("disruptor.publish.count")
                .tag("ring", "default")
                .functionCounter();
        assertThat(publishCount.count()).isEqualTo(1.0);
    }

    @Test
    void registersMetersWhenRingBufferMissing() {
        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.getRingBuffer("missing")).thenReturn(null);
        when(manager.getResolvedRingNames()).thenReturn(List.of("missing"));

        DisruptorMetrics metrics = new DisruptorMetrics();
        DisruptorMeterBinder binder = new DisruptorMeterBinder(manager, metrics);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        binder.bindTo(registry);

        Gauge backlog = registry.get("disruptor.ringbuffer.backlog")
                .tag("ring", "missing")
                .gauge();
        assertThat(backlog.value()).isEqualTo(0.0);
    }
}
