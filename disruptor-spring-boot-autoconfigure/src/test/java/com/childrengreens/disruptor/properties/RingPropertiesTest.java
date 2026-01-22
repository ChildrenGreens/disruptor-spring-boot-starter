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
