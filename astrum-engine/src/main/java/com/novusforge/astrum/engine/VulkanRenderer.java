package com.novusforge.astrum.engine;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.system.MemoryStack.*;

public class VulkanRenderer implements GPUResourceManager {
    private long window;
    private VkInstance instance;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private long commandPool;

    private final AtomicLong nextResourceId = new AtomicLong(1);
    private final Map<Long, ResourceInfo> resources = new ConcurrentHashMap<>();
    private volatile boolean disposed = false;

    private static class ResourceInfo {
        final long deviceHandle;
        final int type;
        ResourceInfo(long deviceHandle, int type) {
            this.deviceHandle = deviceHandle;
            this.type = type;
        }
    }

    public static final int BUFFER = 1;
    public static final int IMAGE = 2;

    public void init() {
        initWindow();
        initVulkan();
    }

    public boolean windowShouldClose() {
        return glfwWindowShouldClose(window);
    }

    public long getWindow() {
        return window;
    }

    private void initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        window = glfwCreateWindow(1280, 720, "Project Astrum", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }
    }

    private void initVulkan() {
        createInstance();
        pickPhysicalDevice();
        createLogicalDevice();
        createCommandPool();
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8Safe("Project Astrum"))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8Safe("Astrum Engine"))
                    .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK_API_VERSION_1_0);

            PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
            if (requiredExtensions == null) {
                throw new RuntimeException("Failed to find required Vulkan extensions");
            }

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(requiredExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, pInstance) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance");
            }
            instance = new VkInstance(pInstance.get(0), createInfo);
            System.out.println("Vulkan Instance created successfully.");
        }
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);
            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }
            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, devices);

            for (int i = 0; i < devices.limit(); i++) {
                VkPhysicalDevice pd = new VkPhysicalDevice(devices.get(i), instance);
                VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(pd, props);
                if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ||
                    props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                    physicalDevice = pd;
                    System.out.println("Selected GPU: " + props.deviceName());
                    props.free();
                    break;
                }
                props.free();
            }
            if (physicalDevice == null) {
                physicalDevice = new VkPhysicalDevice(devices.get(0), instance);
            }
        }
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            VkDeviceQueueCreateInfo.Buffer queueCreateInfo = VkDeviceQueueCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(0)
                    .pQueuePriorities(stack.floats(1.0f));

            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfo)
                    .pEnabledFeatures(features);

            PointerBuffer pDevice = stack.mallocPointer(1);
            if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, 0, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(0)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            LongBuffer pCommandPool = stack.mallocLong(1);
            if (vkCreateCommandPool(device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = pCommandPool.get(0);
        }
    }

    public long createBuffer(long size, int usage) {
        if (device == null || disposed) return 0;
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
                return 0;
            }
            long buffer = pBuffer.get(0);
            resources.put(buffer, new ResourceInfo(device.hashCode(), BUFFER));
            return buffer;
        }
    }

    @Override
    public void deleteBuffer(long bufferId) {
        if (device == null || disposed || bufferId == 0) return;
        ResourceInfo info = resources.remove(bufferId);
        if (info != null && info.type == BUFFER) {
            vkDestroyBuffer(device, bufferId, null);
        }
    }

    @Override
    public void deleteImage(long imageId) {
        if (device == null || disposed || imageId == 0) return;
        ResourceInfo info = resources.remove(imageId);
        if (info != null && info.type == IMAGE) {
            vkDestroyImage(device, imageId, null);
        }
    }

    public void cleanup() {
        disposed = true;
        for (ResourceInfo info : resources.values()) {
        }
        resources.clear();

        if (commandPool != 0 && device != null) {
            vkDestroyCommandPool(device, commandPool, null);
        }
        if (device != null) {
            vkDestroyDevice(device, null);
        }
        if (instance != null) {
            vkDestroyInstance(instance, null);
        }
        if (window != 0) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();
    }

    public VkDevice getDevice() { return device; }
    public VkQueue getGraphicsQueue() { return graphicsQueue; }
}
