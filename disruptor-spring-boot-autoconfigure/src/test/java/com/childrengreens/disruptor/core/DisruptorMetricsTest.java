package com.childrengreens.disruptor.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisruptorMetricsTest {
    @Test
    void tracksPublishConsumeAndLatency() {
        DisruptorMetrics metrics = new DisruptorMetrics();
        metrics.recordPublish("alpha");
        metrics.recordPublish("alpha");
        metrics.recordConsume("alpha", "handlerA");
        metrics.recordConsume("alpha", "handlerA");
        metrics.recordLatency("alpha", 10);
        metrics.recordLatency("alpha", 30);

        assertThat(metrics.getPublishCount("alpha")).isEqualTo(2);
        assertThat(metrics.getConsumeCount("alpha")).isEqualTo(2);
        assertThat(metrics.getAverageLatencyMillis("alpha")).isEqualTo(20.0);
        assertThat(metrics.getHandlerCounts()).containsKey("alpha::handlerA");
        assertThat(metrics.getHandlerKeys()).contains("alpha::handlerA");
    }

    @Test
    void returnsZeroWhenNoMetricsPresent() {
        DisruptorMetrics metrics = new DisruptorMetrics();
        assertThat(metrics.getPublishCount("missing")).isZero();
        assertThat(metrics.getConsumeCount("missing")).isZero();
        assertThat(metrics.getAverageLatencyMillis("missing")).isZero();
    }
}
