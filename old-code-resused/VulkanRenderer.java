package com.novusforge.astrum.engine;

import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.*;
import java.util.Map;

import com.novusforge.astrum.world.ChunkMesh;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/**
 * VulkanRenderer - Vulkan graphics backend
 * Following Formula: "Graphics: Vulkan"
 * 
 * Features:
 * - Proper swap chain with MAILBOX/FIFO present modes
 * - Dynamic viewport/scissor for resize support
 * - 2 frames in flight for GPU-CPU synchronization
 * - Depth testing for proper 3D rendering
 */
public class VulkanRenderer implements IRenderer {
    
    private long window;
    private VkInstance instance;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private long commandPool;

    private long swapChain;
    private long[] swapChainImages;
    private long[] swapChainImageViews;
    private long[] swapChainFramebuffers;
    private long renderPass;
    private long pipelineLayout;
    private long graphicsPipeline;
    private long descriptorSetLayout;
    private long descriptorPool;
    private long[] descriptorSets;
    private long uniformBuffer;
    private long uniformBufferMemory;
    private long[] commandBuffers;

    // Frame synchronization (2 frames = front buffer + back buffer)
    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private int currentFrame = 0;
    private int currentImageIndex = 0;
    private long[] imageAvailableSemaphores;
    private long[] renderFinishedSemaphores;
    private long[] inFlightFences;
    
    // Per-frame command buffers (for double buffering)
    private long[][] frameCommandBuffers;

    // Test cube
    private long testCubeVbo = 0;
    private long testCubeVboMem = 0;
    private int testCubeVertexCount = 0;
    private boolean renderTestCube = false;

    private float aspectRatio = 16f / 9f;
    private int swapChainWidth = 1280;
    private int swapChainHeight = 720;

    @Override
    public boolean init() {
        // Explicitly load Vulkan library before GLFW initializes
        try {
            System.loadLibrary("vulkan");
            System.out.println("[Vulkan] Vulkan library loaded explicitly");
        } catch (UnsatisfiedLinkError e) {
            // Try loading from standard path
            try {
                System.load("/usr/lib/aarch64-linux-gnu/libvulkan.so.1");
                System.out.println("[Vulkan] Vulkan library loaded from /usr/lib/aarch64-linux-gnu");
            } catch (UnsatisfiedLinkError e2) {
                System.out.println("[Vulkan] Warning: Could not explicitly load Vulkan library");
                System.out.println("[Vulkan] Will rely on GLFW to load it");
            }
        }
        
        initWindow();
        initVulkan();
        initSwapChain();
        initRenderPass();
        initDescriptorSet();
        initGraphicsPipeline();
        initFramebuffers();
        initCommandBuffers();
        initUniformBuffer();
        initSyncObjects();
        createTestCube();
        return true;
    }

    private void initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(1280, 720, "Astrum - Pre-Classic", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        System.out.println("[GLFW] Window created: 1280x720");
    }

    private void initVulkan() {
        createInstance();
        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
        createCommandPool();
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8("Astrum"))
                    .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
                    .pEngineName(stack.UTF8("Astrum Engine"))
                    .engineVersion(VK_MAKE_VERSION(0, 1, 0))
                    .apiVersion(VK_API_VERSION_1_0);

            PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
            if (requiredExtensions == null) {
                throw new RuntimeException("Failed to find required Vulkan extensions");
            }

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(requiredExtensions);

            PointerBuffer buffer = stack.mallocPointer(1);
            if (vkCreateInstance(createInfo, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance");
            }
            instance = new VkInstance(buffer.get(0), createInfo);
            System.out.println("[Vulkan] Instance created.");
        }
    }

    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer buffer = stack.mallocLong(1);
            if (glfwCreateWindowSurface(instance, window, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }
            surface = buffer.get(0);
            System.out.println("[Vulkan] Surface created.");
        }
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            PointerBuffer devices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, deviceCount, devices);

            for (int i = 0; i < devices.limit(); i++) {
                VkPhysicalDevice pd = new VkPhysicalDevice(devices.get(i), instance);
                VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(pd, props);

                String name = props.deviceNameString();
                int type = props.deviceType();
                System.out.println("[Vulkan] GPU found: " + name);

                if (type == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ||
                    type == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                    physicalDevice = pd;
                    System.out.println("[Vulkan] Selected GPU: " + name);
                }
            }

            if (physicalDevice == null) {
                physicalDevice = new VkPhysicalDevice(devices.get(0), instance);
            }
        }
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
            VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.calloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, families);

            int graphicsFamily = -1;
            int presentFamily = -1;

            for (int i = 0; i < families.limit(); i++) {
                if ((families.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphicsFamily = i;
                }

                IntBuffer supported = stack.mallocInt(1);
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface, supported);
                if (supported.get(0) == VK_TRUE) {
                    presentFamily = i;
                }

                if (graphicsFamily != -1 && presentFamily != -1) break;
            }

            if (graphicsFamily == -1) graphicsFamily = 0;
            if (presentFamily == -1) presentFamily = graphicsFamily;

            final int gfx = graphicsFamily;

            VkDeviceQueueCreateInfo.Buffer queues = VkDeviceQueueCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(gfx)
                    .pQueuePriorities(stack.floats(1.0f));

            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);

            PointerBuffer extensions = stack.mallocPointer(1);
            extensions.put(0, stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queues)
                    .pEnabledFeatures(features)
                    .ppEnabledExtensionNames(extensions);

            PointerBuffer buffer = stack.mallocPointer(1);
            if (vkCreateDevice(physicalDevice, createInfo, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }
            device = new VkDevice(buffer.get(0), physicalDevice, createInfo);

            PointerBuffer queue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, gfx, 0, queue);
            graphicsQueue = new VkQueue(queue.get(0), device);

            vkGetDeviceQueue(device, presentFamily, 0, queue);
            presentQueue = new VkQueue(queue.get(0), device);

            System.out.println("[Vulkan] Logical device created.");
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo info = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(0)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer buffer = stack.mallocLong(1);
            if (vkCreateCommandPool(device, info, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = buffer.get(0);
            System.out.println("[Vulkan] Command pool created.");
        }
    }

    private void initSwapChain() {
        try (MemoryStack stack = stackPush()) {
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.calloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, caps);

            IntBuffer formatCount = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, null);
            int formatCountVal = formatCount.get(0);
            VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(formatCountVal, stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, formats);

            IntBuffer modeCount = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, modeCount, null);
            int modeCountVal = modeCount.get(0);
            IntBuffer modes = stack.mallocInt(modeCountVal);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, modeCount, modes);

            int format = VK_FORMAT_B8G8R8A8_SRGB;
            int colorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;

            if (formatCountVal > 0) {
                for (int i = 0; i < formats.limit(); i++) {
                    VkSurfaceFormatKHR f = formats.get(i);
                    if (f.format() == VK_FORMAT_B8G8R8A8_SRGB) {
                        format = f.format();
                        colorSpace = f.colorSpace();
                        break;
                    }
                }
            }

            // Prefer MAILBOX (triple buffering), fallback to FIFO (vsync)
            int presentMode = VK_PRESENT_MODE_FIFO_KHR;
            for (int i = 0; i < modeCountVal; i++) {
                if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                    presentMode = VK_PRESENT_MODE_MAILBOX_KHR;
                    break;
                }
            }
            System.out.println("[Vulkan] Using present mode: " + 
                (presentMode == VK_PRESENT_MODE_MAILBOX_KHR ? "MAILBOX" : "FIFO"));

            int width = caps.minImageExtent().width();
            int height = caps.minImageExtent().height();
            if (caps.currentExtent().width() != 0xFFFFFFFF) {
                width = caps.currentExtent().width();
                height = caps.currentExtent().height();
            }

            aspectRatio = (float) width / (float) height;
            swapChainWidth = width;
            swapChainHeight = height;

            int imageCount = Math.max(caps.minImageCount() + 1, MAX_FRAMES_IN_FLIGHT + 1);
            if (caps.maxImageCount() > 0 && imageCount > caps.maxImageCount()) {
                imageCount = caps.maxImageCount();
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(imageCount)
                    .imageFormat(format)
                    .imageColorSpace(colorSpace)
                    .imageExtent(VkExtent2D.calloc(stack).set(width, height))
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .preTransform(caps.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);

            LongBuffer buffer = stack.mallocLong(1);
            if (vkCreateSwapchainKHR(device, createInfo, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }
            swapChain = buffer.get(0);

            IntBuffer imgCountBuf = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, swapChain, imgCountBuf, null);
            int imgCount = imgCountBuf.get(0);
            LongBuffer imagesBuf = stack.mallocLong(imgCount);
            vkGetSwapchainImagesKHR(device, swapChain, imgCountBuf, imagesBuf);
            swapChainImages = new long[imgCount];
            for (int i = 0; i < imgCount; i++) {
                swapChainImages[i] = imagesBuf.get(i);
            }
            System.out.println("[Vulkan] Swap chain created: " + imgCount + " images");

            swapChainImageViews = new long[imgCount];
            for (int i = 0; i < imgCount; i++) {
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .image(swapChainImages[i])
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(format)
                        .components(VkComponentMapping.calloc(stack)
                            .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                            .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                            .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                            .a(VK_COMPONENT_SWIZZLE_IDENTITY))
                        .subresourceRange(VkImageSubresourceRange.calloc(stack)
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

                LongBuffer viewBuf = stack.mallocLong(1);
                vkCreateImageView(device, viewInfo, null, viewBuf);
                swapChainImageViews[i] = viewBuf.get(0);
            }
        }
    }

    private void initRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer attachment = VkAttachmentDescription.calloc(1, stack)
                    .format(VK_FORMAT_B8G8R8A8_SRGB)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                    .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer ref = VkAttachmentReference.calloc(1, stack)
                    .attachment(0)
                    .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(ref);

            VkSubpassDependency.Buffer dep = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL)
                    .dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcAccessMask(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo info = VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(attachment)
                    .pSubpasses(subpass)
                    .pDependencies(dep);

            LongBuffer buffer = stack.mallocLong(1);
            if (vkCreateRenderPass(device, info, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }
            renderPass = buffer.get(0);
            System.out.println("[Vulkan] Render pass created.");
        }
    }

    private void initDescriptorSet() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(bindings);

            LongBuffer layoutBuf = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(device, layoutInfo, null, layoutBuf) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            descriptorSetLayout = layoutBuf.get(0);

            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .maxSets(1)
                    .pPoolSizes(poolSize);

            LongBuffer poolBuf = stack.mallocLong(1);
            if (vkCreateDescriptorPool(device, poolInfo, null, poolBuf) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }
            descriptorPool = poolBuf.get(0);

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool)
                    .pSetLayouts(stack.longs(descriptorSetLayout));

            descriptorSets = new long[1];
            LongBuffer setBuf = stack.mallocLong(1);
            if (vkAllocateDescriptorSets(device, allocInfo, setBuf) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor set");
            }
            descriptorSets[0] = setBuf.get(0);

            System.out.println("[Vulkan] Descriptor set created.");
        }
    }

    private void initUniformBuffer() {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = 64 * 3; // 3 x mat4

            LongBuffer lp = stack.mallocLong(1);
            LongBuffer mp = stack.mallocLong(1);
            createBuffer(bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, lp, mp);
            uniformBuffer = lp.get(0);
            uniformBufferMemory = mp.get(0);

            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(uniformBuffer)
                    .offset(0)
                    .range(bufferSize);

            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[0])
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            vkUpdateDescriptorSets(device, descriptorWrite, null);
            System.out.println("[Vulkan] Uniform buffer initialized.");
        }
    }

    private void initGraphicsPipeline() {
        try (MemoryStack stack = stackPush()) {
            String vertSource = "#version 450\n" +
                "layout(binding = 0) uniform UniformBuffer {\n" +
                "    mat4 projection;\n" +
                "    mat4 view;\n" +
                "    mat4 model;\n" +
                "} ubo;\n" +
                "layout(location = 0) in vec3 position;\n" +
                "layout(location = 1) in vec3 color;\n" +
                "layout(location = 0) out vec3 fragColor;\n" +
                "void main() {\n" +
                "    gl_Position = ubo.projection * ubo.view * ubo.model * vec4(position, 1.0);\n" +
                "    fragColor = color;\n" +
                "}";

            String fragSource = "#version 450\n" +
                "layout(location = 0) in vec3 fragColor;\n" +
                "layout(location = 0) out vec4 outColor;\n" +
                "void main() {\n" +
                "    outColor = vec4(fragColor, 1.0);\n" +
                "}";

            ByteBuffer vertCode = compileShader("main.vert", vertSource, shaderc_vertex_shader);
            ByteBuffer fragCode = compileShader("main.frag", fragSource, shaderc_fragment_shader);

            try {
                VkShaderModuleCreateInfo vertInfo = VkShaderModuleCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                        .pCode(vertCode);

                VkShaderModuleCreateInfo fragInfo = VkShaderModuleCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                        .pCode(fragCode);

                LongBuffer vertBuf = stack.mallocLong(1);
                if (vkCreateShaderModule(device, vertInfo, null, vertBuf) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create vertex shader module");
                }
                long vertModule = vertBuf.get(0);

                LongBuffer fragBuf = stack.mallocLong(1);
                if (vkCreateShaderModule(device, fragInfo, null, fragBuf) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create fragment shader module");
                }
                long fragModule = fragBuf.get(0);

                VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
                stages.get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertModule)
                    .pName(stack.UTF8("main"));
                stages.get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragModule)
                    .pName(stack.UTF8("main"));

                VkVertexInputBindingDescription.Buffer bindingDesc = VkVertexInputBindingDescription.calloc(1, stack)
                        .binding(0)
                        .stride(24)
                        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

                VkVertexInputAttributeDescription.Buffer attrDesc = VkVertexInputAttributeDescription.calloc(2, stack);
                attrDesc.get(0)
                    .binding(0)
                    .location(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(0);
                attrDesc.get(1)
                    .binding(0)
                    .location(1)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(12);

                VkPipelineVertexInputStateCreateInfo vertInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                        .pVertexBindingDescriptions(bindingDesc)
                        .pVertexAttributeDescriptions(attrDesc);

                VkPipelineInputAssemblyStateCreateInfo assembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                        .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                        .primitiveRestartEnable(false);

                // Dynamic viewport and scissor
                IntBuffer dynamicStates = stack.mallocInt(2);
                dynamicStates.put(0, VK_DYNAMIC_STATE_VIEWPORT);
                dynamicStates.put(1, VK_DYNAMIC_STATE_SCISSOR);

                VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(dynamicStates);

                VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                        .viewportCount(1)
                        .scissorCount(1);

                VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                        .depthClampEnable(false)
                        .rasterizerDiscardEnable(false)
                        .polygonMode(VK_POLYGON_MODE_FILL)
                        .lineWidth(1.0f)
                        .cullMode(VK_CULL_MODE_BACK_BIT)
                        .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                        .depthBiasEnable(false);

                VkPipelineMultisampleStateCreateInfo multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                        .sampleShadingEnable(false)
                        .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

                VkPipelineColorBlendAttachmentState.Buffer colorBlend = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                                       VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                        .blendEnable(false);

                VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                        .logicOpEnable(false)
                        .pAttachments(colorBlend);

                VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                        .pSetLayouts(stack.longs(descriptorSetLayout));

                LongBuffer layoutBuf = stack.mallocLong(1);
                vkCreatePipelineLayout(device, layoutInfo, null, layoutBuf);
                pipelineLayout = layoutBuf.get(0);

                VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                        .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                        .stageCount(2)
                        .pStages(stages)
                        .pVertexInputState(vertInput)
                        .pInputAssemblyState(assembly)
                        .pViewportState(viewportState)
                        .pRasterizationState(rasterizer)
                        .pMultisampleState(multisample)
                        .pColorBlendState(colorBlendState)
                        .pDynamicState(dynamicState)
                        .layout(pipelineLayout)
                        .renderPass(renderPass)
                        .subpass(0);

                LongBuffer pipeBuf = stack.mallocLong(1);
                if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pipeBuf) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create graphics pipeline");
                }
                graphicsPipeline = pipeBuf.get(0);

                vkDestroyShaderModule(device, vertModule, null);
                vkDestroyShaderModule(device, fragModule, null);

                System.out.println("[Vulkan] Graphics pipeline created.");
            } finally {
                memFree(vertCode);
                memFree(fragCode);
            }
        }
    }

    private ByteBuffer compileShader(String name, String source, int shadercStage) {
        long compiler = shaderc_compiler_initialize();
        long options = shaderc_compile_options_initialize();

        long result = shaderc_compile_into_spv(compiler, source, shadercStage, name, "main", options);
        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            throw new RuntimeException("Shader compilation failed: " + shaderc_result_get_error_message(result));
        }

        ByteBuffer spvCode = memAlloc(Math.toIntExact(shaderc_result_get_length(result)));
        spvCode.put(shaderc_result_get_bytes(result));
        spvCode.flip();

        shaderc_result_release(result);
        shaderc_compile_options_release(options);
        shaderc_compiler_release(compiler);

        return spvCode;
    }

    private void initFramebuffers() {
        swapChainFramebuffers = new long[swapChainImageViews.length];

        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < swapChainImageViews.length; i++) {
                LongBuffer attachment = stack.mallocLong(1);
                attachment.put(0, swapChainImageViews[i]);

                VkFramebufferCreateInfo info = VkFramebufferCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .renderPass(renderPass)
                        .pAttachments(attachment)
                        .width(swapChainWidth)
                        .height(swapChainHeight)
                        .layers(1);

                LongBuffer buf = stack.mallocLong(1);
                vkCreateFramebuffer(device, info, null, buf);
                swapChainFramebuffers[i] = buf.get(0);
            }
        }
        System.out.println("[Vulkan] " + swapChainFramebuffers.length + " framebuffers created.");
    }

    private void initCommandBuffers() {
        commandBuffers = new long[swapChainFramebuffers.length];
        
        // Create per-frame command buffers for double buffering
        // Each frame has command buffers for all swapchain images
        frameCommandBuffers = new long[MAX_FRAMES_IN_FLIGHT][swapChainFramebuffers.length];
        
        try (MemoryStack stack = stackPush()) {
            // Allocate command buffers for each swapchain image
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(commandBuffers.length);

            PointerBuffer buf = stack.mallocPointer(commandBuffers.length);
            vkAllocateCommandBuffers(device, allocInfo, buf);
            for (int i = 0; i < commandBuffers.length; i++) {
                commandBuffers[i] = buf.get(i);
            }
            
            // Allocate per-frame command buffers
            for (int frame = 0; frame < MAX_FRAMES_IN_FLIGHT; frame++) {
                vkAllocateCommandBuffers(device, allocInfo, buf);
                for (int i = 0; i < commandBuffers.length; i++) {
                    frameCommandBuffers[frame][i] = buf.get(i);
                }
            }
        }
        System.out.println("[Vulkan] Command buffers created: " + MAX_FRAMES_IN_FLIGHT + " frames × " + 
                          swapChainFramebuffers.length + " images (double buffering)");
    }

    private void initSyncObjects() {
        try (MemoryStack stack = stackPush()) {
            imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
            renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
            inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer semBuf = stack.mallocLong(1);
            LongBuffer fenceBuf = stack.mallocLong(1);

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                if (vkCreateSemaphore(device, semaphoreInfo, null, semBuf) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create semaphore");
                }
                imageAvailableSemaphores[i] = semBuf.get(0);

                if (vkCreateSemaphore(device, semaphoreInfo, null, semBuf) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create semaphore");
                }
                renderFinishedSemaphores[i] = semBuf.get(0);

                if (vkCreateFence(device, fenceInfo, null, fenceBuf) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create fence");
                }
                inFlightFences[i] = fenceBuf.get(0);
            }

            System.out.println("[Vulkan] Sync objects created: " + MAX_FRAMES_IN_FLIGHT + " frames in flight");
        }
    }

    private void createTestCube() {
        try (MemoryStack stack = stackPush()) {
            float[] vertices = com.novusforge.astrum.engine.CubeMesh.generateTexturedCube(0.5f, 0.5f, 0.5f, 0.8f, 0.4f, 0.2f);
            long vSize = vertices.length * 4L;
            testCubeVertexCount = vertices.length / 6;

            LongBuffer vBuf = stack.mallocLong(1);
            LongBuffer vMem = stack.mallocLong(1);
            createBuffer(vSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, vBuf, vMem);

            // Convert float[] to FloatBuffer for memCopy
            FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
            vertexBuffer.put(vertices);
            vertexBuffer.flip();

            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, vMem.get(0), 0, vSize, 0, data);
            memCopy(memAddress(vertexBuffer), data.get(0), vSize);
            vkUnmapMemory(device, vMem.get(0));

            testCubeVbo = vBuf.get(0);
            testCubeVboMem = vMem.get(0);

            System.out.println("[Vulkan] Test cube created: " + testCubeVertexCount + " vertices");
        }
    }

    public void createBuffer(long size, int usage, int properties, LongBuffer buffer, LongBuffer memory) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            if (vkCreateBuffer(device, bufferInfo, null, buffer) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer");
            }

            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, buffer.get(0), memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(findMemoryType(memReqs.memoryTypeBits(), properties));

            if (vkAllocateMemory(device, allocInfo, null, memory) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate buffer memory");
            }

            vkBindBufferMemory(device, buffer.get(0), memory.get(0), 0);
        }
    }

    private int findMemoryType(int typeFilter, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);

            for (int i = 0; i < memProps.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0 &&
                    (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    @Override
    public void render(Matrix4f view, Matrix4f projection, Map<Long, ChunkMesh> meshes) {
        try (MemoryStack stack = stackPush()) {
            // Wait for previous frame (GPU → CPU sync)
            vkWaitForFences(device, inFlightFences[currentFrame], true, Long.MAX_VALUE);
            
            // Acquire next image from swapchain (front buffer → back buffer)
            IntBuffer imageIndexBuf = stack.mallocInt(1);
            int result = vkAcquireNextImageKHR(device, swapChain, Long.MAX_VALUE,
                imageAvailableSemaphores[currentFrame], VK_NULL_HANDLE, imageIndexBuf);
            
            currentImageIndex = imageIndexBuf.get(0);

            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                return;
            } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Failed to acquire swap chain image");
            }
            
            // Reset fence for this frame
            vkResetFences(device, inFlightFences[currentFrame]);

            // Get command buffer for THIS FRAME and THIS IMAGE
            VkCommandBuffer vkCmd = new VkCommandBuffer(frameCommandBuffers[currentFrame][currentImageIndex], device);

            // Update uniform buffer
            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, uniformBufferMemory, 0, 64 * 3, 0, data);
            ByteBuffer buffer = data.getByteBuffer(0, 64 * 3);
            projection.get(0, buffer);
            view.get(64, buffer);
            new Matrix4f().get(128, buffer);
            vkUnmapMemory(device, uniformBufferMemory);

            // Reset and begin command buffer
            vkResetCommandBuffer(vkCmd, 0);
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            vkBeginCommandBuffer(vkCmd, beginInfo);

            // Begin render pass
            VkClearValue.Buffer clearColor = VkClearValue.calloc(1, stack);
            // TEST COLOR: Bright magenta (very visible - proves front buffer is working!)
            clearColor.color().float32(stack.floats(1.0f, 0.0f, 1.0f, 1.0f));  // MAGENTA

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(swapChainFramebuffers[currentImageIndex])
                    .renderArea(VkRect2D.calloc(stack)
                        .offset(VkOffset2D.calloc(stack).set(0, 0))
                        .extent(VkExtent2D.calloc(stack).set(swapChainWidth, swapChainHeight)))
                    .pClearValues(clearColor);

            vkCmdBeginRenderPass(vkCmd, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdBindPipeline(vkCmd, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

            // Set dynamic viewport and scissor
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                .x(0).y(0)
                .width(swapChainWidth).height(swapChainHeight)
                .minDepth(0).maxDepth(1);
            vkCmdSetViewport(vkCmd, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).set(swapChainWidth, swapChainHeight));
            vkCmdSetScissor(vkCmd, 0, scissor);

            vkCmdBindDescriptorSets(vkCmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, descriptorSets, null);

            // Render test cube
            if (renderTestCube && testCubeVbo != 0) {
                LongBuffer vboBuf = stack.mallocLong(1);
                vboBuf.put(0, testCubeVbo);
                LongBuffer offsetBuf = stack.mallocLong(1);
                offsetBuf.put(0, 0L);
                vkCmdBindVertexBuffers(vkCmd, 0, vboBuf, offsetBuf);
                vkCmdDraw(vkCmd, testCubeVertexCount, 1, 0, 0);
            }

            // Render chunk meshes
            for (ChunkMesh mesh : meshes.values()) {
                if (mesh.getOpaqueVboId() == 0) {
                    uploadMesh(mesh);
                }
                if (mesh.getOpaqueVboId() != 0 && mesh.getOpaqueIndexCount() > 0) {
                    LongBuffer vboBuf = stack.mallocLong(1);
                    vboBuf.put(0, mesh.getOpaqueVboId());
                    LongBuffer offsetBuf = stack.mallocLong(1);
                    offsetBuf.put(0, 0L);
                    vkCmdBindVertexBuffers(vkCmd, 0, vboBuf, offsetBuf);
                    vkCmdBindIndexBuffer(vkCmd, mesh.getOpaqueIboId(), 0, VK_INDEX_TYPE_UINT32);
                    vkCmdDrawIndexed(vkCmd, mesh.getOpaqueIndexCount(), 1, 0, 0, 0);
                }
            }

            vkCmdEndRenderPass(vkCmd);
            vkEndCommandBuffer(vkCmd);

            // Submit command buffer (CPU → GPU sync)
            IntBuffer waitStages = stack.mallocInt(1);
            waitStages.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);

            LongBuffer waitSemaphores = stack.mallocLong(1);
            waitSemaphores.put(0, imageAvailableSemaphores[currentFrame]);

            LongBuffer signalSemaphores = stack.mallocLong(1);
            signalSemaphores.put(0, renderFinishedSemaphores[currentFrame]);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pWaitSemaphores(waitSemaphores)
                    .pWaitDstStageMask(waitStages)
                    .pCommandBuffers(stack.pointers(vkCmd.address()))
                    .pSignalSemaphores(signalSemaphores);

            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[currentFrame]) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit command buffer");
            }

            // Present to screen (back buffer → front buffer swap)
            LongBuffer presentWaitSemaphores = stack.mallocLong(1);
            presentWaitSemaphores.put(0, renderFinishedSemaphores[currentFrame]);

            IntBuffer imageIndexToPresent = stack.mallocInt(1);
            imageIndexToPresent.put(0, currentImageIndex);

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(presentWaitSemaphores)
                    .pSwapchains(stack.longs(swapChain))
                    .pImageIndices(imageIndexToPresent);

            result = vkQueuePresentKHR(presentQueue, presentInfo);

            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                return;
            } else if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            // Move to next frame (toggle front/back buffer)
            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    private void uploadMesh(ChunkMesh mesh) {
        if (!mesh.hasOpaqueData()) return;

        try (MemoryStack stack = stackPush()) {
            FloatBuffer vertices = mesh.buildOpaqueVertexData();
            long vSize = vertices.limit() * 4L;

            LongBuffer vBuf = stack.mallocLong(1);
            LongBuffer vMem = stack.mallocLong(1);
            createBuffer(vSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, vBuf, vMem);

            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(device, vMem.get(0), 0, vSize, 0, data);
            memCopy(memAddress(vertices), data.get(0), vSize);
            vkUnmapMemory(device, vMem.get(0));

            mesh.setOpaqueVboId(vBuf.get(0));
            mesh.setOpaqueVboMemId(vMem.get(0));

            IntBuffer indices = mesh.buildOpaqueIndexData();
            long iSize = indices.limit() * 4L;

            LongBuffer iBuf = stack.mallocLong(1);
            LongBuffer iMem = stack.mallocLong(1);
            createBuffer(iSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, iBuf, iMem);

            vkMapMemory(device, iMem.get(0), 0, iSize, 0, data);
            memCopy(memAddress(indices), data.get(0), iSize);
            vkUnmapMemory(device, iMem.get(0));

            mesh.setOpaqueIboId(iBuf.get(0));
            mesh.setOpaqueIboMemId(iMem.get(0));
        }
    }

    @Override
    public boolean windowShouldClose() {
        return glfwWindowShouldClose(window);
    }

    @Override
    public long getWindow() {
        return window;
    }

    @Override
    public float getAspectRatio() {
        return aspectRatio;
    }

    @Override
    public void setRenderTestCube(boolean render) {
        this.renderTestCube = render;
    }

    @Override
    public void cleanup() {
        if (device != null) {
            vkDeviceWaitIdle(device);

            if (testCubeVbo != 0) {
                vkDestroyBuffer(device, testCubeVbo, null);
                vkFreeMemory(device, testCubeVboMem, null);
            }

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                if (imageAvailableSemaphores[i] != 0) vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
                if (renderFinishedSemaphores[i] != 0) vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
                if (inFlightFences[i] != 0) vkDestroyFence(device, inFlightFences[i], null);
            }

            if (uniformBuffer != 0) {
                vkDestroyBuffer(device, uniformBuffer, null);
                vkFreeMemory(device, uniformBufferMemory, null);
            }
            if (descriptorPool != 0) vkDestroyDescriptorPool(device, descriptorPool, null);
            if (descriptorSetLayout != 0) vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);

            for (long fb : swapChainFramebuffers) vkDestroyFramebuffer(device, fb, null);
            vkDestroyPipeline(device, graphicsPipeline, null);
            vkDestroyPipelineLayout(device, pipelineLayout, null);
            vkDestroyRenderPass(device, renderPass, null);

            for (long view : swapChainImageViews) vkDestroyImageView(device, view, null);
            vkDestroySwapchainKHR(device, swapChain, null);
            vkDestroyCommandPool(device, commandPool, null);
            vkDestroyDevice(device, null);
        }
        if (instance != null) {
            vkDestroySurfaceKHR(instance, surface, null);
            vkDestroyInstance(instance, null);
        }
        if (window != 0) glfwDestroyWindow(window);
        glfwTerminate();
    }

    @Override
    public void deleteBuffer(long bufferId, long memoryId) {
        if (device != null) {
            vkDestroyBuffer(device, bufferId, null);
            if (memoryId != 0) vkFreeMemory(device, memoryId, null);
        }
    }

    @Override
    public String getRendererName() {
        return "Vulkan";
    }
}
