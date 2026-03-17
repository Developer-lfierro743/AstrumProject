package com.novusforge.astrum.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified registry for blocks, items, and entities.
 * Supports Namespaced IDs (e.g., astrum:ferrous, minecraft:stone).
 */
public class Registry<T> {
    private final Map<String, T> entries = new HashMap<>();
    private String namespace = "astrum";

    public Registry() {}

    public Registry(String namespace) {
        this.namespace = namespace;
    }

    public void register(String id, T entry) {
        String fullId = resolveNamespace(id);
        if (entries.containsKey(fullId)) {
            throw new IllegalArgumentException("Registry entry already exists: " + fullId);
        }
        entries.put(fullId, entry);
    }

    public T get(String id) {
        String fullId = resolveNamespace(id);
        return entries.get(fullId);
    }

    public T getOrDefault(String id, T defaultValue) {
        String fullId = resolveNamespace(id);
        return entries.getOrDefault(fullId, defaultValue);
    }

    public Map<String, T> getAll() {
        return entries;
    }

    public boolean contains(String id) {
        return entries.containsKey(resolveNamespace(id));
    }

    private String resolveNamespace(String id) {
        if (id.contains(":")) {
            return id;
        }
        return namespace + ":" + id;
    }

    public String getNamespace() {
        return namespace;
    }
}
