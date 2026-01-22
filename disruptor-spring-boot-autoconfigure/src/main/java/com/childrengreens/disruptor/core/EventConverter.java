package com.childrengreens.disruptor.core;

public interface EventConverter<T> {
    /**
     * Convert a user payload to a Disruptor event type.
     */
    T convert(Object source);

    /**
     * Whether this converter supports the given payload.
     */
    default boolean supports(Object source) {
        return true;
    }
}
