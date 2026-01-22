package com.childrengreens.disruptor.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultEventConverterTest {
    @Test
    void returnsPayloadAsIs() {
        DefaultEventConverter converter = new DefaultEventConverter();
        Object payload = new Object();
        assertThat(converter.convert(payload)).isSameAs(payload);
    }
}
