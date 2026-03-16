package com.novusforge.astrum.engine;

/**
 * Universal interface for window management.
 * Bridges GLFW (Native) and DOM Canvas (Web).
 */
public interface WindowProvider {
    void createWindow(int width, int height, String title);
    boolean shouldClose();
    void pollEvents();
    void swapBuffers();
    void shutdown();
    long getHandle();
}
