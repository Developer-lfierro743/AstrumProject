package com.novusforge.astrum.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified registry for blocks, items, and entities.
 * Designed for universal unification and stability.
 */
public class Registry<T> {
    private final Map<String, T> entries = new HashMap<>();

    public void register(String id, T entry) {
        if (entries.containsKey(id)) {
            throw new IllegalArgumentException("Registry entry already exists: " + id);
        }
        entries.put(id, entry);
    }

    public T get(String id) {
        return entries.get(id);
    }

    public Map<String, T> getAll() {
        return entries;
    }
}
