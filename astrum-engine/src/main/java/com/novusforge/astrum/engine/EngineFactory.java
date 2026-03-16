package com.novusforge.astrum.engine;

import com.novusforge.astrum.engine.vulkan.VulkanContext;
import com.novusforge.astrum.engine.webgpu.WebGPUContext;

/**
 * Universal Render Bridge: Automatically selects the best backend for the current platform.
 */
public class EngineFactory {

    public static RenderBackend createBackend() {
        String os = System.getProperty("os.name").toLowerCase();
        
        // Detect if running in a TeaVM environment (Browser)
        // Usually checked via a specific TeaVM property or class presence
        boolean isWeb = System.getProperty("teavm.project") != null || System.getProperty("java.vendor").contains("TeaVM");

        if (isWeb) {
            System.out.println("Platform: Web detected. Using WebGPU Backend.");
            return new WebGPUContext();
        } else {
            System.out.println("Platform: Native (" + os + ") detected. Using Vulkan Backend.");
            return new VulkanContext();
        }
    }
}
