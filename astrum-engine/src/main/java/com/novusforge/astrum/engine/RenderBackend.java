package com.novusforge.astrum.engine;

/**
 * Unified interface for different rendering backends (Vulkan, WebGPU).
 */
public interface RenderBackend {
    void initialize();
    void render();
    void shutdown();
}
