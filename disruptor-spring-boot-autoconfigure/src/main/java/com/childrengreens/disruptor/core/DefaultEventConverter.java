package com.childrengreens.disruptor.core;

/**
 * Fallback converter that returns the payload as-is.
 */
public class DefaultEventConverter implements EventConverter<Object> {
    @Override
    public Object convert(Object source) {
        return source;
    }
}
