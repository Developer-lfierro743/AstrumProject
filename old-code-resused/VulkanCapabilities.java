package com.novusforge.astrum.engine;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

/**
 * VulkanCapabilities - Check Vulkan GPU capabilities and ICD support
 * 
 * This utility checks:
 * - Vulkan ICD installation
 * - Physical device properties
 * - Supported extensions
 * - Swapchain capabilities
 * - Queue families
 * - Format support
 */
public class VulkanCapabilities {
    
    /**
     * Check and print Vulkan capabilities
     * @return true if Vulkan is properly configured
     */
    public static boolean checkCapabilities() {
        System.out.println("=".repeat(60));
        System.out.println("  VULKAN CAPABILITY CHECK");
        System.out.println("=".repeat(60));
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Step 1: Check GLFW Vulkan support
            System.out.println("\n[1] Checking GLFW Vulkan Support...");
            PointerBuffer extensions = glfwGetRequiredInstanceExtensions();
            if (extensions == null) {
                System.err.println("  ❌ GLFW Vulkan extensions NOT available!");
                System.err.println("  → Check if your GPU driver has Vulkan ICD installed");
                return false;
            }
            System.out.println("  ✓ GLFW Vulkan extensions: " + extensions.capacity() + " extensions");
            
            // Step 2: Create Vulkan instance
            System.out.println("\n[2] Creating Vulkan Instance...");
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8("Astrum Capability Check"))
                .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
                .pEngineName(stack.UTF8("Astrum Engine"))
                .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                .apiVersion(VK_API_VERSION_1_0);
            
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(extensions);
            
            PointerBuffer pInstance = stack.mallocPointer(1);
            int err = vkCreateInstance(createInfo, null, pInstance);
            
            if (err != VK_SUCCESS) {
                System.err.println("  ❌ Failed to create Vulkan instance: " + translateVulkanResult(err));
                System.err.println("  → Possible causes:");
                System.err.println("     - Vulkan ICD not installed");
                System.err.println("     - GPU driver outdated");
                System.err.println("     - Vulkan runtime missing");
                return false;
            }
            
            VkInstance instance = new VkInstance(pInstance.get(0), createInfo);
            System.out.println("  ✓ Vulkan instance created");
            
            // Step 3: Enumerate physical devices (GPUs)
            System.out.println("\n[3] Enumerating Physical Devices...");
            IntBuffer deviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);
            
            int count = deviceCount.get(0);
            if (count == 0) {
                System.err.println("  ❌ NO Vulkan devices found!");
                System.err.println("  → Check Vulkan ICD installation:");
                System.err.println("     export VK_ICD_FILENAMES=/path/to/vulkan_icd.json");
                System.err.println("     vulkaninfo --summary");
                vkDestroyInstance(instance, null);
                return false;
            }
            
            System.out.println("  ✓ Found " + count + " Vulkan device(s)");
            
            // Step 4: Check each device
            PointerBuffer devices = stack.mallocPointer(count);
            vkEnumeratePhysicalDevices(instance, deviceCount, devices);
            
            for (int i = 0; i < count; i++) {
                System.out.println("\n[4] Checking Device " + i + "...");
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(devices.get(i), instance);
                
                VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(physicalDevice, props);
                
                String name = props.deviceNameString();
                int type = props.deviceType();
                int apiVersion = props.apiVersion();
                
                System.out.println("  Device Name: " + name);
                System.out.println("  Device Type: " + getDeviceTypeName(type));
                System.out.println("  Vulkan API:  " + VK_API_VERSION_MAJOR(apiVersion) + "." + 
                                   VK_API_VERSION_MINOR(apiVersion) + "." + 
                                   VK_API_VERSION_PATCH(apiVersion));
                
                // Check if suitable for rendering
                if (type != VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU && 
                    type != VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                    System.out.println("  ⚠️  Warning: Device type may not support full rendering");
                } else {
                    System.out.println("  ✓ Suitable for rendering");
                }
                
                // Check extensions
                System.out.println("\n[5] Checking Device Extensions...");
                IntBuffer extensionCount = stack.mallocInt(1);
                vkEnumerateDeviceExtensionProperties(physicalDevice, (String)null, extensionCount, null);
                
                int extCount = extensionCount.get(0);
                VkExtensionProperties.Buffer extProps = VkExtensionProperties.calloc(extCount, stack);
                vkEnumerateDeviceExtensionProperties(physicalDevice, (String)null, extensionCount, extProps);
                
                boolean hasSwapchain = false;
                boolean hasSurface = false;
                
                for (int j = 0; j < extCount; j++) {
                    String extName = extProps.get(j).extensionNameString();
                    if (extName.equals(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) hasSwapchain = true;
                    if (extName.equals(VK_KHR_SURFACE_EXTENSION_NAME)) hasSurface = true;
                }
                
                System.out.println("  Total Extensions: " + extCount);
                System.out.println("  " + (hasSwapchain ? "✓" : "❌") + " VK_KHR_swapchain");
                System.out.println("  " + (hasSurface ? "✓" : "❌") + " VK_KHR_surface");
                
                if (!hasSwapchain) {
                    System.err.println("  ❌ Device missing swapchain support!");
                    vkDestroyInstance(instance, null);
                    return false;
                }
            }
            
            // Step 5: Check surface capabilities (if window created)
            System.out.println("\n[6] Surface Capabilities...");
            long window = glfwCreateWindow(1280, 720, "Test", 0, 0);
            if (window != 0) {
                java.nio.LongBuffer pSurface = stack.mallocLong(1);
                if (glfwCreateWindowSurface(instance, window, null, pSurface) == VK_SUCCESS) {
                    long surface = pSurface.get(0);
                    
                    VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.calloc(stack);
                    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(
                        new VkPhysicalDevice(devices.get(0), instance), surface, caps);
                    
                    System.out.println("  Min Image Count: " + caps.minImageCount());
                    System.out.println("  Max Image Count: " + caps.maxImageCount());
                    System.out.println("  Current Extent: " + caps.currentExtent().width() + "x" + 
                                       caps.currentExtent().height());
                    System.out.println("  Supported Transforms: 0x" + 
                                       Integer.toHexString(caps.supportedTransforms()));
                    
                    // Check present modes
                    IntBuffer modeCount = stack.mallocInt(1);
                    vkGetPhysicalDeviceSurfacePresentModesKHR(
                        new VkPhysicalDevice(devices.get(0), instance), surface, modeCount, null);
                    
                    int modeCountVal = modeCount.get(0);
                    IntBuffer modes = stack.mallocInt(modeCountVal);
                    vkGetPhysicalDeviceSurfacePresentModesKHR(
                        new VkPhysicalDevice(devices.get(0), instance), surface, modeCount, modes);
                    
                    System.out.println("  Available Present Modes: " + modeCountVal);
                    for (int i = 0; i < modeCountVal; i++) {
                        int mode = modes.get(i);
                        String modeName = getPresentModeName(mode);
                        System.out.println("    - " + modeName);
                    }
                    
                    vkDestroySurfaceKHR(instance, surface, null);
                } else {
                    System.out.println("  ⚠️  Could not create surface");
                }
                glfwDestroyWindow(window);
            } else {
                System.out.println("  ⚠️  Could not create test window");
            }
            
            // Cleanup
            vkDestroyInstance(instance, null);
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("  ✓ VULKAN CAPABILITY CHECK PASSED");
            System.out.println("=".repeat(60));
            return true;
            
        } catch (Exception e) {
            System.err.println("\n❌ Vulkan capability check failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static String getDeviceTypeName(int type) {
        switch (type) {
            case VK_PHYSICAL_DEVICE_TYPE_OTHER: return "Other";
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU: return "Integrated GPU";
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU: return "Discrete GPU";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU: return "Virtual GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU: return "CPU";
            default: return "Unknown (" + type + ")";
        }
    }
    
    private static String getPresentModeName(int mode) {
        switch (mode) {
            case VK_PRESENT_MODE_IMMEDIATE_KHR: return "IMMEDIATE (no vsync, may tear)";
            case VK_PRESENT_MODE_MAILBOX_KHR: return "MAILBOX (triple buffering, recommended)";
            case VK_PRESENT_MODE_FIFO_KHR: return "FIFO (vsync, always supported)";
            case VK_PRESENT_MODE_FIFO_RELAXED_KHR: return "FIFO_RELAXED (relaxed vsync)";
            default: return "UNKNOWN (" + mode + ")";
        }
    }
    
    private static String translateVulkanResult(int result) {
        switch (result) {
            case VK_SUCCESS: return "SUCCESS";
            case VK_NOT_READY: return "NOT_READY";
            case VK_TIMEOUT: return "TIMEOUT";
            case VK_EVENT_SET: return "EVENT_SET";
            case VK_EVENT_RESET: return "EVENT_RESET";
            case VK_INCOMPLETE: return "INCOMPLETE";
            case VK_ERROR_OUT_OF_HOST_MEMORY: return "OUT_OF_HOST_MEMORY";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY: return "OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_INITIALIZATION_FAILED: return "INITIALIZATION_FAILED";
            case VK_ERROR_DEVICE_LOST: return "DEVICE_LOST";
            case VK_ERROR_MEMORY_MAP_FAILED: return "MEMORY_MAP_FAILED";
            case VK_ERROR_LAYER_NOT_PRESENT: return "LAYER_NOT_PRESENT";
            case VK_ERROR_EXTENSION_NOT_PRESENT: return "EXTENSION_NOT_PRESENT";
            case VK_ERROR_FEATURE_NOT_PRESENT: return "FEATURE_NOT_PRESENT";
            case VK_ERROR_INCOMPATIBLE_DRIVER: return "INCOMPATIBLE_DRIVER";
            default: return "UNKNOWN (" + result + ")";
        }
    }
    
    /**
     * Print ICD installation instructions
     */
    public static void printICDInstructions() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  VULKAN ICD INSTALLATION GUIDE");
        System.out.println("=".repeat(60));
        System.out.println("\nFor Turnip/Mesa (Adreno GPUs):");
        System.out.println("  export VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/freedreno_icd.aarch64.json");
        System.out.println("  vulkaninfo --summary");
        System.out.println("\nFor NVIDIA:");
        System.out.println("  sudo apt install nvidia-driver-525");
        System.out.println("  vulkaninfo --summary");
        System.out.println("\nFor AMD:");
        System.out.println("  sudo apt install mesa-vulkan-drivers");
        System.out.println("  vulkaninfo --summary");
        System.out.println("\nFor Intel:");
        System.out.println("  sudo apt install intel-vulkan-driver");
        System.out.println("  vulkaninfo --summary");
        System.out.println("\nGeneral troubleshooting:");
        System.out.println("  1. Check ICD files: ls /usr/share/vulkan/icd.d/");
        System.out.println("  2. Verify Vulkan: vulkaninfo --summary");
        System.out.println("  3. Check GPU: lspci | grep -i vga");
        System.out.println("  4. Update drivers: sudo apt update && sudo apt upgrade");
        System.out.println("=".repeat(60));
    }
}
