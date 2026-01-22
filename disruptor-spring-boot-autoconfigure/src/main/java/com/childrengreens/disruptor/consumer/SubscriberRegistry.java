package com.childrengreens.disruptor.consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry holding all discovered subscriber definitions.
 */
public class SubscriberRegistry {
    private final List<SubscriberDefinition> definitions = new ArrayList<>();

    public synchronized void register(SubscriberDefinition definition) {
        definitions.add(definition);
    }

    public synchronized List<SubscriberDefinition> getDefinitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions));
    }
}
