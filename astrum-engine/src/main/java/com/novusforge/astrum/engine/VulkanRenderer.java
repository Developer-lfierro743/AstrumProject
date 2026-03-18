package com.novusforge.astrum.engine;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class VulkanRenderer implements GPUResourceManager {
    private long window;
    private VkInstance instance;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private long commandPool;
    
    private long swapChain;
    private LongBuffer swapChainImages;
    private LongBuffer swapChainImageViews;
    private LongBuffer swapChainFramebuffers;
    private long renderPass;
    private long pipelineLayout;
    private long graphicsPipeline;
    private LongBuffer commandBuffers;

    public void init() {
        initWindow();
        initVulkan();
        initSwapChain();
        initRenderPass();
        initGraphicsPipeline();
        initFramebuffers();
        initCommandBuffers();
    }

    private void initWindow() {
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        window = glfwCreateWindow(1280, 720, "Astrum - Pre-classic (Cave Game)", 0, 0);
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
                
                ByteBuffer nameBuf = props.deviceName();
                String name = memUTF8(nameBuf);
                int type = props.deviceType();
                System.out.println("[Vulkan] GPU found: " + name);
                
                if (type == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU ||
                    type == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                    physicalDevice = pd;
                    System.out.println("[Vulkan] Selected GPU: " + name);
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
            final int pres = presentFamily;

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
            
            vkGetDeviceQueue(device, pres, 0, queue);
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

            int presentMode = VK_PRESENT_MODE_FIFO_KHR;
            for (int i = 0; i < modeCountVal; i++) {
                if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                    presentMode = modes.get(i);
                    break;
                }
            }

            int width = caps.minImageExtent().width();
            int height = caps.minImageExtent().height();
            if (caps.currentExtent().width() != 0xFFFFFFFF) {
                width = caps.currentExtent().width();
                height = caps.currentExtent().height();
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(caps.minImageCount())
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

            IntBuffer imageCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);
            int imgCount = imageCount.get(0);
            swapChainImages = stack.mallocLong(imgCount);
            vkGetSwapchainImagesKHR(device, swapChain, imageCount, swapChainImages);

            swapChainImageViews = stack.mallocLong(imgCount);
            for (int i = 0; i < imgCount; i++) {
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .image(swapChainImages.get(i))
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
                swapChainImageViews.put(i, viewBuf.get(0));
            }
            
            System.out.println("[Vulkan] Swap chain created: " + imgCount + " images");
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

    private void initGraphicsPipeline() {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer vertCode = stack.UTF8(
                "#version 450\n" +
                "vec2 pos[6] = vec2[](vec2(-1,-1), vec2(1,-1), vec2(-1,1), vec2(-1,1), vec2(1,-1), vec2(1,1));\n" +
                "void main() { gl_Position = vec4(pos[gl_VertexIndex], 0.0, 1.0); }"
            );
            
            ByteBuffer fragCode = stack.UTF8(
                "#version 450\n" +
                "layout(location = 0) out vec4 color;\n" +
                "void main() { color = vec4(0.2, 0.4, 0.7, 1.0); }"
            );

            VkShaderModuleCreateInfo vertInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(vertCode);

            VkShaderModuleCreateInfo fragInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(fragCode);

            LongBuffer vertBuf = stack.mallocLong(1);
            vkCreateShaderModule(device, vertInfo, null, vertBuf);
            long vertModule = vertBuf.get(0);

            LongBuffer fragBuf = stack.mallocLong(1);
            vkCreateShaderModule(device, fragInfo, null, fragBuf);
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

            VkPipelineVertexInputStateCreateInfo vertInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

            VkPipelineInputAssemblyStateCreateInfo assembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0).y(0)
                    .width(1280).height(720)
                    .minDepth(0).maxDepth(1);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .offset(VkOffset2D.calloc(stack).set(0, 0))
                    .extent(VkExtent2D.calloc(stack).set(1280, 720));

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1).pViewports(viewport)
                    .scissorCount(1).pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .lineWidth(1.0f)
                    .cullMode(VK_CULL_MODE_BACK_BIT)
                    .frontFace(VK_FRONT_FACE_CLOCKWISE)
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
                    .pAttachments(colorBlend)
                    .blendConstants(stack.floats(0, 0, 0, 0));

            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

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
        }
    }

    private void initFramebuffers() {
        int count = (int) swapChainImages.remaining();
        swapChainFramebuffers = org.lwjgl.system.MemoryStack.stackPush().mallocLong(count);
        
        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < count; i++) {
                LongBuffer attachment = stack.mallocLong(1);
                attachment.put(0, swapChainImageViews.get(i));

                VkFramebufferCreateInfo info = VkFramebufferCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .renderPass(renderPass)
                        .pAttachments(attachment)
                        .width(1280)
                        .height(720)
                        .layers(1);

                LongBuffer buf = stack.mallocLong(1);
                vkCreateFramebuffer(device, info, null, buf);
                swapChainFramebuffers.put(i, buf.get(0));
            }
        }
        System.out.println("[Vulkan] " + count + " framebuffers created.");
    }

    private void initCommandBuffers() {
        int count = (int) swapChainFramebuffers.remaining();
        commandBuffers = org.lwjgl.system.MemoryStack.stackPush().mallocLong(count);
        
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(count);

            PointerBuffer buf = stack.mallocPointer(count);
            vkAllocateCommandBuffers(device, allocInfo, buf);
            for (int i = 0; i < count; i++) {
                commandBuffers.put(i, buf.get(i));
            }
        }
        
        for (int i = 0; i < count; i++) {
            recordCommandBuffer(commandBuffers.get(i), i);
        }
        System.out.println("[Vulkan] Command buffers created.");
    }

    private void recordCommandBuffer(long cmdBuffer, int imageIndex) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer vkCmd = new VkCommandBuffer(cmdBuffer, device);
            
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            vkBeginCommandBuffer(vkCmd, beginInfo);

            VkClearValue.Buffer clearValue = VkClearValue.calloc(1, stack);
            clearValue.color().float32(stack.floats(0.15f, 0.15f, 0.2f, 1.0f));

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(swapChainFramebuffers.get(imageIndex))
                    .renderArea(VkRect2D.calloc(stack)
                        .offset(VkOffset2D.calloc(stack).set(0, 0))
                        .extent(VkExtent2D.calloc(stack).set(1280, 720)))
                    .pClearValues(clearValue);

            vkCmdBeginRenderPass(vkCmd, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
            vkCmdBindPipeline(vkCmd, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
            vkCmdDraw(vkCmd, 6, 1, 0, 0);
            vkCmdEndRenderPass(vkCmd);

            vkEndCommandBuffer(vkCmd);
        }
    }

    public boolean windowShouldClose() {
        return glfwWindowShouldClose(window);
    }

    public void render() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer imageIndex = stack.mallocInt(1);
            
            vkAcquireNextImageKHR(device, swapChain, Long.MAX_VALUE, 
                VK_NULL_HANDLE, VK_NULL_HANDLE, imageIndex);

            VkCommandBuffer vkCmd = new VkCommandBuffer(commandBuffers.get(imageIndex.get(0)), device);
            
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(vkCmd.address()));

            vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(graphicsQueue);

            IntBuffer idx = stack.mallocInt(1);
            idx.put(0, imageIndex.get(0));
            
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pSwapchains(stack.longs(swapChain))
                    .pImageIndices(idx);

            vkQueuePresentKHR(presentQueue, presentInfo);
            vkQueueWaitIdle(presentQueue);
        }
    }

    public long getWindow() {
        return window;
    }

    @Override
    public void deleteBuffer(long bufferId) {
        if (device != null && bufferId != 0) {
            vkDestroyBuffer(device, bufferId, null);
        }
    }

    @Override
    public void deleteImage(long imageId) {
        if (device != null && imageId != 0) {
            vkDestroyImage(device, imageId, null);
        }
    }

    public void cleanup() {
        if (device != null) {
            vkDeviceWaitIdle(device);
            
            int count = (int) swapChainFramebuffers.remaining();
            for (int i = 0; i < count; i++) {
                vkDestroyFramebuffer(device, swapChainFramebuffers.get(i), null);
            }
            vkDestroyPipeline(device, graphicsPipeline, null);
            vkDestroyPipelineLayout(device, pipelineLayout, null);
            vkDestroyRenderPass(device, renderPass, null);
            
            count = (int) swapChainImageViews.remaining();
            for (int i = 0; i < count; i++) {
                vkDestroyImageView(device, swapChainImageViews.get(i), null);
            }
            vkDestroySwapchainKHR(device, swapChain, null);
            vkDestroyCommandPool(device, commandPool, null);
            vkDestroyDevice(device, null);
        }
        if (instance != null) {
            vkDestroySurfaceKHR(instance, surface, null);
            vkDestroyInstance(instance, null);
        }
        if (window != 0) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();
    }
}
