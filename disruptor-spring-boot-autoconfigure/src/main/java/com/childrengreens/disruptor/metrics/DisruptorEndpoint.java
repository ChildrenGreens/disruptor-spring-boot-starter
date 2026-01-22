package com.childrengreens.disruptor.metrics;

import com.childrengreens.disruptor.core.DisruptorEvent;
import com.childrengreens.disruptor.core.DisruptorManager;
import com.childrengreens.disruptor.core.DisruptorMetrics;
import com.lmax.disruptor.RingBuffer;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Actuator endpoint that exposes basic Disruptor ring stats.
 */
@Endpoint(id = "disruptor")
public class DisruptorEndpoint {
    private final DisruptorManager manager;
    private final DisruptorMetrics metrics;

    public DisruptorEndpoint(DisruptorManager manager, DisruptorMetrics metrics) {
        this.manager = manager;
        this.metrics = metrics;
    }

    /**
     * Expose basic ring buffer metrics.
     */
    @ReadOperation
    public Map<String, Object> disruptor() {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (Map.Entry<String, RingBuffer<DisruptorEvent>> entry :
                manager.getRingBuffers().entrySet()) {
            RingBuffer<DisruptorEvent> ringBuffer = entry.getValue();
            Map<String, Object> ringInfo = new LinkedHashMap<>();
            ringInfo.put("bufferSize", ringBuffer.getBufferSize());
            ringInfo.put("cursor", ringBuffer.getCursor());
            ringInfo.put("remainingCapacity", ringBuffer.remainingCapacity());
            ringInfo.put("backlog", ringBuffer.getBufferSize() - ringBuffer.remainingCapacity());
            ringInfo.put("publishCount", metrics.getPublishCount(entry.getKey()));
            ringInfo.put("consumeCount", metrics.getConsumeCount(entry.getKey()));
            ringInfo.put("avgLatencyMillis", metrics.getAverageLatencyMillis(entry.getKey()));
            ringInfo.put("handlers", handlerCounts(entry.getKey()));
            payload.put(entry.getKey(), ringInfo);
        }
        return payload;
    }

    private Map<String, Long> handlerCounts(String ring) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, java.util.concurrent.atomic.LongAdder> entry :
                metrics.getHandlerCounts().entrySet()) {
            String key = entry.getKey();
            String prefix = ring + "::";
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), entry.getValue().sum());
            }
        }
        return result;
    }
}
