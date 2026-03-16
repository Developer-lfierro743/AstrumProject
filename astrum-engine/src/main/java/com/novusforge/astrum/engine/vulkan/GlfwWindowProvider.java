package com.novusforge.astrum.engine.vulkan;

import com.novusforge.astrum.engine.WindowProvider;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Native window provider using GLFW.
 */
public class GlfwWindowProvider implements WindowProvider {
    private long window;

    @Override
    public void createWindow(int width, int height, String title) {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    @Override
    public void swapBuffers() {
        // Vulkan handles swapping via swapchain, but GLFW might need this for OpenGL fallbacks
    }

    @Override
    public void shutdown() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    @Override
    public long getHandle() {
        return window;
    }
}
