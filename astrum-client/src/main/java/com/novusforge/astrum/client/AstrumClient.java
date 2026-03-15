package com.novusforge.astrum.client;

import com.novusforge.astrum.core.world.World;
import com.novusforge.astrum.engine.RenderBackend;
import com.novusforge.astrum.engine.vulkan.VulkanContext;
import com.novusforge.astrum.security.guardian.SafetyGuardian;

/**
 * Main entry point for the Astrum game client.
 */
public class AstrumClient {
    private final World world;
    private final RenderBackend engine;
    private final SafetyGuardian guardian;

    public AstrumClient() {
        this.world = new World();
        this.engine = new VulkanContext();
        this.guardian = new SafetyGuardian();
    }

    public void start() {
        System.out.println("Starting Project Astrum...");
        
        // Initialize systems
        engine.initialize();
        
        // Game loop
        System.out.println("Astrum is running. Press Ctrl+C to stop.");
        
        // For demonstration, render one frame and shutdown
        engine.render();
        
        // engine.shutdown();
    }

    public static void main(String[] args) {
        new AstrumClient().start();
    }
}
