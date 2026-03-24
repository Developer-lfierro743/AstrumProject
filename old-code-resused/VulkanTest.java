package com.novusforge.astrum.engine;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.vulkan.*;
import org.lwjgl.system.*;
import java.nio.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.system.MemoryStack.*;

/**
 * Minimal Vulkan test - proves GLFW + Vulkan work together
 */
public class VulkanTest {
    private long window;
    private VkInstance instance;
    private long surface;

    public void run() {
        System.out.println("=== Vulkan GLFW Test ===");
        initWindow();
        initVulkan();
        mainLoop();
        cleanup();
        System.out.println("=== Test Complete ===");
    }

    private void initWindow() {
        System.out.println("[1] Initializing GLFW...");
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        System.out.println("[2] Creating window (GLFW_NO_API for Vulkan)...");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API); // Crucial: No OpenGL context
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        
        window = glfwCreateWindow(800, 600, "Vulkan LWJGL Test", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        System.out.println("✓ Window created: 800x600");
    }

    private void initVulkan() {
        System.out.println("[3] Creating Vulkan instance...");
        createInstance();
        
        System.out.println("[4] Creating Vulkan surface...");
        createSurface();
        
        System.out.println("✓ Vulkan initialized successfully!");
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            // Check for GLFW required extensions
            PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
            
            if (requiredExtensions == null) {
                throw new RuntimeException("Failed to find required Vulkan extensions");
            }
            
            System.out.println("   Found " + requiredExtensions.capacity() + " required extensions");
            
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("Astrum Test"))
                .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(stack.UTF8("Test Engine"))
                .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK_API_VERSION_1_0);
            
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(requiredExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            int err = vkCreateInstance(createInfo, null, pInstance);
            
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance: " + err);
            }
            
            instance = new VkInstance(pInstance.get(0), createInfo);
            System.out.println("✓ Vulkan instance created");
        }
    }

    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            int err = glfwCreateWindowSurface(instance, window, null, pSurface);
            
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface: " + err);
            }
            
            surface = pSurface.get(0);
            System.out.println("✓ Vulkan surface created");
        }
    }

    private void mainLoop() {
        System.out.println("[5] Running main loop (press ESC to exit)...");
        
        // Set ESC key handler
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }

    private void cleanup() {
        System.out.println("[6] Cleaning up...");
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new VulkanTest().run();
    }
}
