package com.novusforge.astrum.game;

import com.novusforge.astrum.core.ECS;
import com.novusforge.astrum.core.SafetyGuardian;
import com.novusforge.astrum.engine.VulkanRenderer;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Main Entry Point for Project Astrum.
 * Orchestrates the ECS, SafetyGuardian, and Vulkan Engine.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Project Astrum by Novusforge (Java 22 + Vulkan)...");

        // 1. Initialize SafetyGuardian
        SafetyGuardian guardian = new SafetyGuardian();
        if (!guardian.validateAction("startup")) {
            System.err.println("SafetyGuardian blocked startup.");
            return;
        }

        // 2. Initialize ECS
        ECS ecs = new ECS();

        // 3. Initialize Vulkan Renderer
        VulkanRenderer renderer = new VulkanRenderer();
        try {
            renderer.init();
            
            // Main Loop
            while (!renderer.windowShouldClose()) {
                glfwPollEvents();
                // Render logic here
                break; 
            }
        } finally {
            renderer.cleanup();
        }
    }
}
