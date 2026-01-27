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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory metrics recorder for Disruptor usage.
 */
public class DisruptorMetrics {
    private final Map<String, LongAdder> publishCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> consumeCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> latencyTotalMillis = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> latencyCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> handlerCounts = new ConcurrentHashMap<>();

    public void recordPublish(String ring) {
        publishCounts.computeIfAbsent(ring, key -> new LongAdder()).increment();
    }

    public void recordConsume(String ring, String handlerId) {
        consumeCounts.computeIfAbsent(ring, key -> new LongAdder()).increment();
        handlerCounts.computeIfAbsent(handlerKey(ring, handlerId), key -> new LongAdder()).increment();
    }

    public void recordLatency(String ring, long latencyMillis) {
        latencyTotalMillis.computeIfAbsent(ring, key -> new LongAdder()).add(latencyMillis);
        latencyCount.computeIfAbsent(ring, key -> new LongAdder()).increment();
    }

    public long getPublishCount(String ring) {
        return sum(publishCounts.get(ring));
    }

    public long getConsumeCount(String ring) {
        return sum(consumeCounts.get(ring));
    }

    public double getAverageLatencyMillis(String ring) {
        long count = sum(latencyCount.get(ring));
        if (count == 0) {
            return 0.0;
        }
        long total = sum(latencyTotalMillis.get(ring));
        return total / (double) count;
    }

    public Map<String, LongAdder> getHandlerCounts() {
        return Collections.unmodifiableMap(handlerCounts);
    }

    public Set<String> getHandlerKeys() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(handlerCounts.keySet()));
    }

    private long sum(LongAdder adder) {
        return adder == null ? 0 : adder.sum();
    }

    private String handlerKey(String ring, String handlerId) {
        return ring + "::" + handlerId;
    }
}
