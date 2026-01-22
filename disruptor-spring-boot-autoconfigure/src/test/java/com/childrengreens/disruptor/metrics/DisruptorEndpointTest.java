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
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DisruptorEndpointTest {
    @Test
    void exposesRingBufferMetrics() {
        RingBuffer<DisruptorEvent> ringBuffer =
                RingBuffer.createMultiProducer(new DisruptorEventFactory(), 8);
        ringBuffer.publishEvent(
                (event, sequence, payload) -> event.setPayload(payload),
                "payload");

        DisruptorMetrics metrics = new DisruptorMetrics();
        metrics.recordPublish("default");
        metrics.recordConsume("default", "handlerA");
        metrics.recordLatency("default", 12);

        DisruptorManager manager = mock(DisruptorManager.class);
        when(manager.getRingBuffers()).thenReturn(Map.of("default", ringBuffer));

        DisruptorEndpoint endpoint = new DisruptorEndpoint(manager, metrics);
        Map<String, Object> payload = endpoint.disruptor();

        assertThat(payload).containsKey("default");
        Map<String, Object> ring = (Map<String, Object>) payload.get("default");
        assertThat(ring.get("bufferSize")).isEqualTo(8);
        assertThat(ring.get("publishCount")).isEqualTo(1L);
        assertThat(ring.get("consumeCount")).isEqualTo(1L);
        assertThat(ring.get("handlers")).isInstanceOf(Map.class);
        Map<String, Long> handlers = (Map<String, Long>) ring.get("handlers");
        assertThat(handlers).containsEntry("handlerA", 1L);
    }
}
