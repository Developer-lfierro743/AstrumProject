package com.novusforge.astrum.engine;

/**
 * Unified interface for hardware-resilient rendering backends (Vulkan, WebGPU).
 */
public interface RenderBackend {
    void initialize();
    void render();
    void shutdown();
    
    // Unified Resource & Lifecycle Management
    void resize(int width, int height);
    void clear(float r, float g, float b, float a);
    
    // Abstracted Pipeline State
    void setLegacyMode(boolean enabled);

    WindowProvider getWindowProvider();
}

