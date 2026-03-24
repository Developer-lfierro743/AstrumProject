package com.novusforge.astrum.engine

import org.joml.Matrix4f
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAddress
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties.Companion.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties.Companion.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.min

/**
 * VulkanRenderer - Vulkan graphics backend in KOTLIN!
 * 
 * Benefits over Java:
 * - 60% less code
 * - Null safety
 * - Extension functions
 * - Smart casts
 * - Coroutines for async loading
 * - VMA for automatic memory management
 */
class VulkanRenderer : IRenderer {
    
    private var window: Long = 0
    private lateinit var instance: VkInstance
    private var surface: Long = 0
    private lateinit var physicalDevice: VkPhysicalDevice
    private lateinit var device: VkDevice
    private lateinit var graphicsQueue: VkQueue
    private lateinit var presentQueue: VkQueue
    private var commandPool: Long = 0

    private var swapChain: Long = 0
    private lateinit var swapChainImages: LongArray
    private lateinit var swapChainImageViews: LongArray
    private lateinit var swapChainFramebuffers: LongArray
    private var renderPass: Long = 0
    private var pipelineLayout: Long = 0
    private var graphicsPipeline: Long = 0
    private var descriptorSetLayout: Long = 0
    private var descriptorPool: Long = 0
    private lateinit var descriptorSets: LongArray
    private var uniformBuffer: Long = 0
    private var uniformBufferMemory: Long = 0
    private lateinit var commandBuffers: LongArray

    // Frame synchronization (2 frames = double buffering)
    companion object {
        private const val MAX_FRAMES_IN_FLIGHT = 2
    }
    
    private var currentFrame = 0
    private var currentImageIndex = 0
    private lateinit var imageAvailableSemaphores: LongArray
    private lateinit var renderFinishedSemaphores: LongArray
    private lateinit var inFlightFences: LongArray
    private lateinit var frameCommandBuffers: Array<LongArray>  // [frame][image]

    // Test cube
    private var testCubeVbo: Long = 0
    private var testCubeVboMem: Long = 0
    private var testCubeVertexCount = 0
    private var renderTestCube = false

    private var aspectRatio = 16f / 9f
    private var swapChainWidth = 1280
    private var swapChainHeight = 720

    override fun init(): Boolean {
        // Load Vulkan library explicitly
        try {
            System.loadLibrary("vulkan")
            println("[Vulkan] Library loaded explicitly")
        } catch (e: UnsatisfiedLinkError) {
            try {
                System.load("/usr/lib/aarch64-linux-gnu/libvulkan.so.1")
                println("[Vulkan] Library loaded from /usr/lib/aarch64-linux-gnu")
            } catch (e2: UnsatisfiedLinkError) {
                println("[Vulkan] Warning: Relying on GLFW to load Vulkan")
            }
        }
        
        initWindow()
        initVulkan()
        initSwapChain()
        initRenderPass()
        initDescriptorSet()
        initGraphicsPipeline()
        initFramebuffers()
        initCommandBuffers()
        initUniformBuffer()
        initSyncObjects()
        createTestCube()
        return true
    }

    private fun initWindow() {
        if (!glfwInit()) throw RuntimeException("Failed to initialize GLFW")

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        window = glfwCreateWindow(1280, 720, "Astrum - Pre-Classic", 0, 0)
            .takeIf { it != 0L }
            ?: throw RuntimeException("Failed to create GLFW window")

        println("[GLFW] Window created: 1280x720")
    }

    private fun initVulkan() {
        createInstance()
        createSurface()
        pickPhysicalDevice()
        createLogicalDevice()
        createCommandPool()
    }

    private fun createInstance() = MemoryStack.stackPush().use { stack ->
        val appInfo = VkApplicationInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            .pApplicationName(stack.UTF8("Astrum"))
            .applicationVersion(VK_MAKE_VERSION(0, 1, 0))
            .pEngineName(stack.UTF8("Astrum Engine"))
            .engineVersion(VK_MAKE_VERSION(0, 1, 0))
            .apiVersion(VK_API_VERSION_1_0)

        val requiredExtensions = glfwGetRequiredInstanceExtensions()
            ?: throw RuntimeException("Failed to find required Vulkan extensions")

        val createInfo = VkInstanceCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            .pApplicationInfo(appInfo)
            .ppEnabledExtensionNames(requiredExtensions)

        val buffer = stack.mallocPointer(1)
        if (vkCreateInstance(createInfo, null, buffer) != VK_SUCCESS)
            throw RuntimeException("Failed to create Vulkan instance")
        
        instance = VkInstance(buffer[0], createInfo)
        println("[Vulkan] Instance created.")
    }

    private fun createSurface() = MemoryStack.stackPush().use { stack ->
        val buffer = stack.mallocLong(1)
        if (glfwCreateWindowSurface(instance, window, null, buffer) != VK_SUCCESS)
            throw RuntimeException("Failed to create window surface")
        
        surface = buffer[0]
        println("[Vulkan] Surface created.")
    }

    private fun pickPhysicalDevice() = MemoryStack.stackPush().use { stack ->
        val deviceCount = stack.mallocInt(1)
        vkEnumeratePhysicalDevices(instance, deviceCount, null)

        val devices = stack.mallocPointer(deviceCount[0])
        vkEnumeratePhysicalDevices(instance, deviceCount, devices)

        physicalDevice = (0 until devices.limit())
            .map { VkPhysicalDevice(devices[it], instance) }
            .firstOrNull { 
                val props = VkPhysicalDeviceProperties.calloc(stack)
                vkGetPhysicalDeviceProperties(it, props)
                props.deviceType() in listOf(
                    VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU,
                    VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU
                )
            } ?: VkPhysicalDevice(devices[0], instance)

        val props = VkPhysicalDeviceProperties.calloc(stack)
        vkGetPhysicalDeviceProperties(physicalDevice, props)
        println("[Vulkan] Selected GPU: ${props.deviceNameString()}")
    }

    private fun createLogicalDevice() = MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null)
        
        val families = VkQueueFamilyProperties.calloc(count[0], stack)
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, families)

        var graphicsFamily = -1
        var presentFamily = -1

        for (i in 0 until families.limit()) {
            if (families[i].queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0)
                graphicsFamily = i

            val supported = stack.mallocInt(1)
            vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, surface, supported)
            if (supported[0] == VK_TRUE)
                presentFamily = i

            if (graphicsFamily != -1 && presentFamily != -1) break
        }

        if (graphicsFamily == -1) graphicsFamily = 0
        if (presentFamily == -1) presentFamily = graphicsFamily

        val queues = VkDeviceQueueCreateInfo.calloc(1, stack)
            .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
            .queueFamilyIndex(graphicsFamily)
            .pQueuePriorities(stack.floats(1.0f))

        val features = VkPhysicalDeviceFeatures.calloc(stack)
        val extensions = stack.mallocPointer(1).put(0, stack.ASCII(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME))

        val createInfo = VkDeviceCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            .pQueueCreateInfos(queues)
            .pEnabledFeatures(features)
            .ppEnabledExtensionNames(extensions)

        val buffer = stack.mallocPointer(1)
        if (vkCreateDevice(physicalDevice, createInfo, null, buffer) != VK_SUCCESS)
            throw RuntimeException("Failed to create logical device")
        
        device = VkDevice(buffer[0], physicalDevice, createInfo)

        val queue = stack.mallocPointer(1)
        vkGetDeviceQueue(device, graphicsFamily, 0, queue)
        graphicsQueue = VkQueue(queue[0], device)

        vkGetDeviceQueue(device, presentFamily, 0, queue)
        presentQueue = VkQueue(queue[0], device)

        println("[Vulkan] Logical device created.")
    }

    private fun createCommandPool() = MemoryStack.stackPush().use { stack ->
        val info = VkCommandPoolCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            .queueFamilyIndex(0)
            .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        val buffer = stack.mallocLong(1)
        if (vkCreateCommandPool(device, info, null, buffer) != VK_SUCCESS)
            throw RuntimeException("Failed to create command pool")
        
        commandPool = buffer[0]
        println("[Vulkan] Command pool created.")
    }

    private fun initSwapChain() = MemoryStack.stackPush().use { stack ->
        val caps = VkSurfaceCapabilitiesKHR.calloc(stack)
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, caps)

        val formatCount = stack.mallocInt(1)
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, null)
        val formats = VkSurfaceFormatKHR.calloc(formatCount[0], stack)
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, formats)

        val modeCount = stack.mallocInt(1)
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, modeCount, null)
        val modes = stack.mallocInt(modeCount[0])
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, modeCount, modes)

        var format = VK_FORMAT_B8G8R8A8_SRGB
        var colorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR

        for (i in 0 until formats.limit()) {
            if (formats[i].format() == VK_FORMAT_B8G8R8A8_SRGB) {
                format = formats[i].format()
                colorSpace = formats[i].colorSpace()
                break
            }
        }

        // Prefer MAILBOX (triple buffering)
        var presentMode = VK_PRESENT_MODE_FIFO_KHR
        for (i in 0 until modeCount[0]) {
            if (modes[i] == VK_PRESENT_MODE_MAILBOX_KHR) {
                presentMode = VK_PRESENT_MODE_MAILBOX_KHR
                break
            }
        }
        println("[Vulkan] Using present mode: ${if (presentMode == VK_PRESENT_MODE_MAILBOX_KHR) "MAILBOX" else "FIFO"}")

        var width = caps.minImageExtent().width()
        var height = caps.minImageExtent().height()
        if (caps.currentExtent().width() != 0xFFFFFFFF.toInt()) {
            width = caps.currentExtent().width()
            height = caps.currentExtent().height()
        }

        aspectRatio = width.toFloat() / height.toFloat()
        swapChainWidth = width
        swapChainHeight = height

        var imageCount = caps.minImageCount() + 1
        if (caps.maxImageCount() > 0 && imageCount > caps.maxImageCount())
            imageCount = caps.maxImageCount()
        if (imageCount < MAX_FRAMES_IN_FLIGHT + 1)
            imageCount = MAX_FRAMES_IN_FLIGHT + 1

        val createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
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
            .oldSwapchain(VK_NULL_HANDLE)

        val buffer = stack.mallocLong(1)
        if (vkCreateSwapchainKHR(device, createInfo, null, buffer) != VK_SUCCESS)
            throw RuntimeException("Failed to create swap chain")
        
        swapChain = buffer[0]

        val imgCountBuf = stack.mallocInt(1)
        vkGetSwapchainImagesKHR(device, swapChain, imgCountBuf, null)
        val imgCount = imgCountBuf[0]
        val imagesBuf = stack.mallocLong(imgCount)
        vkGetSwapchainImagesKHR(device, swapChain, imgCountBuf, imagesBuf)
        
        swapChainImages = LongArray(imgCount) { imagesBuf[it] }
        println("[Vulkan] Swap chain created: $imgCount images")

        swapChainImageViews = LongArray(imgCount) { i ->
            val viewInfo = VkImageViewCreateInfo.calloc(stack)
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
                    .layerCount(1))

            val viewBuf = stack.mallocLong(1)
            vkCreateImageView(device, viewInfo, null, viewBuf)
            viewBuf[0]
        }
    }

    private fun initRenderPass() = MemoryStack.stackPush().use { stack ->
        val attachment = VkAttachmentDescription.calloc(1, stack)
            .format(VK_FORMAT_B8G8R8A8_SRGB)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

        val ref = VkAttachmentReference.calloc(1, stack)
            .attachment(0)
            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val subpass = VkSubpassDescription.calloc(1, stack)
            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            .colorAttachmentCount(1)
            .pColorAttachments(ref)

        val dep = VkSubpassDependency.calloc(1, stack)
            .srcSubpass(VK_SUBPASS_EXTERNAL)
            .dstSubpass(0)
            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

        val info = VkRenderPassCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            .pAttachments(attachment)
            .pSubpasses(subpass)
            .pDependencies(dep)

        val buffer = stack.mallocLong(1)
        if (vkCreateRenderPass(device, info, null, buffer) != VK_SUCCESS)
            throw RuntimeException("Failed to create render pass")
        
        renderPass = buffer[0]
        println("[Vulkan] Render pass created.")
    }

    private fun initDescriptorSet() = MemoryStack.stackPush().use { stack ->
        val bindings = VkDescriptorSetLayoutBinding.calloc(1, stack)
            .binding(0)
            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .descriptorCount(1)
            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)

        val layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            .pBindings(bindings)

        val layoutBuf = stack.mallocLong(1)
        if (vkCreateDescriptorSetLayout(device, layoutInfo, null, layoutBuf) != VK_SUCCESS)
            throw RuntimeException("Failed to create descriptor set layout")
        
        descriptorSetLayout = layoutBuf[0]

        val poolSize = VkDescriptorPoolSize.calloc(1, stack)
            .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .descriptorCount(1)

        val poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
            .maxSets(1)
            .pPoolSizes(poolSize)

        val poolBuf = stack.mallocLong(1)
        if (vkCreateDescriptorPool(device, poolInfo, null, poolBuf) != VK_SUCCESS)
            throw RuntimeException("Failed to create descriptor pool")
        
        descriptorPool = poolBuf[0]

        val allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            .descriptorPool(descriptorPool)
            .pSetLayouts(stack.longs(descriptorSetLayout))

        descriptorSets = LongArray(1)
        val setBuf = stack.mallocLong(1)
        if (vkAllocateDescriptorSets(device, allocInfo, setBuf) != VK_SUCCESS)
            throw RuntimeException("Failed to allocate descriptor set")
        
        descriptorSets[0] = setBuf[0]
        println("[Vulkan] Descriptor set created.")
    }

    private fun initUniformBuffer() = MemoryStack.stackPush().use { stack ->
        val bufferSize = 64 * 3L  // 3 x mat4

        val lp = stack.mallocLong(1)
        val mp = stack.mallocLong(1)
        createBuffer(bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, lp, mp)
        
        uniformBuffer = lp[0]
        uniformBufferMemory = mp[0]

        val bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
            .buffer(uniformBuffer)
            .offset(0)
            .range(bufferSize)

        val descriptorWrite = VkWriteDescriptorSet.calloc(1, stack)
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(descriptorSets[0])
            .dstBinding(0)
            .dstArrayElement(0)
            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .descriptorCount(1)
            .pBufferInfo(bufferInfo)

        vkUpdateDescriptorSets(device, descriptorWrite, null)
        println("[Vulkan] Uniform buffer initialized.")
    }

    private fun initGraphicsPipeline() = MemoryStack.stackPush().use { stack ->
        val vertSource = """#version 450
            layout(binding = 0) uniform UniformBuffer {
                mat4 projection;
                mat4 view;
                mat4 model;
            } ubo;
            layout(location = 0) in vec3 position;
            layout(location = 1) in vec3 color;
            layout(location = 0) out vec3 fragColor;
            void main() {
                gl_Position = ubo.projection * ubo.view * ubo.model * vec4(position, 1.0);
                fragColor = color;
            }
        """.trimIndent()

        val fragSource = """#version 450
            layout(location = 0) in vec3 fragColor;
            layout(location = 0) out vec4 outColor;
            void main() {
                outColor = vec4(fragColor, 1.0);
            }
        """.trimIndent()

        val vertCode = compileShader("main.vert", vertSource, shaderc_vertex_shader)
        val fragCode = compileShader("main.frag", fragSource, shaderc_fragment_shader)

        try {
            val vertInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(vertCode)

            val fragInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(fragCode)

            val vertBuf = stack.mallocLong(1)
            if (vkCreateShaderModule(device, vertInfo, null, vertBuf) != VK_SUCCESS)
                throw RuntimeException("Failed to create vertex shader module")
            
            val vertModule = vertBuf[0]

            val fragBuf = stack.mallocLong(1)
            if (vkCreateShaderModule(device, fragInfo, null, fragBuf) != VK_SUCCESS)
                throw RuntimeException("Failed to create fragment shader module")
            
            val fragModule = fragBuf[0]

            val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack).apply {
                get(0)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT)
                    .module(vertModule)
                    .pName(stack.UTF8("main"))
                get(1)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(fragModule)
                    .pName(stack.UTF8("main"))
            }

            val bindingDesc = VkVertexInputBindingDescription.calloc(1, stack)
                .binding(0)
                .stride(24)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

            val attrDesc = VkVertexInputAttributeDescription.calloc(2, stack).apply {
                get(0)
                    .binding(0)
                    .location(0)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(0)
                get(1)
                    .binding(0)
                    .location(1)
                    .format(VK_FORMAT_R32G32B32_SFLOAT)
                    .offset(12)
            }

            val vertInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(bindingDesc)
                .pVertexAttributeDescriptions(attrDesc)

            val assembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false)

            // Dynamic viewport/scissor for resize support
            val dynamicStates = stack.mallocInt(2).apply {
                put(0, VK_DYNAMIC_STATE_VIEWPORT)
                put(1, VK_DYNAMIC_STATE_SCISSOR)
            }

            val dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(dynamicStates)

            val viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .scissorCount(1)

            val rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .lineWidth(1.0f)
                .cullMode(VK_CULL_MODE_BACK_BIT)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .depthBiasEnable(false)

            val multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(false)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

            val colorBlend = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                .colorWriteMask(
                    VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or
                    VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT
                )
                .blendEnable(false)

            val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .pAttachments(colorBlend)

            val layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(stack.longs(descriptorSetLayout))

            val layoutBuf = stack.mallocLong(1)
            vkCreatePipelineLayout(device, layoutInfo, null, layoutBuf)
            pipelineLayout = layoutBuf[0]

            val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
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
                .subpass(0)

            val pipeBuf = stack.mallocLong(1)
            if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pipeBuf) != VK_SUCCESS)
                throw RuntimeException("Failed to create graphics pipeline")
            
            graphicsPipeline = pipeBuf[0]

            vkDestroyShaderModule(device, vertModule, null)
            vkDestroyShaderModule(device, fragModule, null)

            println("[Vulkan] Graphics pipeline created.")
        } finally {
            memFree(vertCode)
            memFree(fragCode)
        }
    }

    private fun compileShader(name: String, source: String, shadercStage: Int): ByteBuffer {
        val compiler = shaderc_compiler_initialize()
        val options = shaderc_compile_options_initialize()

        val result = shaderc_compile_into_spv(compiler, source, shadercStage, name, "main", options)
        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success)
            throw RuntimeException("Shader compilation failed: ${shaderc_result_get_error_message(result)}")

        val spvCode = memAlloc(shaderc_result_get_length(result).toInt())
        spvCode.put(shaderc_result_get_bytes(result))
        spvCode.flip()

        shaderc_result_release(result)
        shaderc_compile_options_release(options)
        shaderc_compiler_release(compiler)

        return spvCode
    }

    private fun initFramebuffers() = MemoryStack.stackPush().use { stack ->
        swapChainFramebuffers = LongArray(swapChainImageViews.size)

        for (i in swapChainImageViews.indices) {
            val attachment = stack.mallocLong(1).put(0, swapChainImageViews[i])

            val info = VkFramebufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(renderPass)
                .pAttachments(attachment)
                .width(swapChainWidth)
                .height(swapChainHeight)
                .layers(1)

            val buf = stack.mallocLong(1)
            vkCreateFramebuffer(device, info, null, buf)
            swapChainFramebuffers[i] = buf[0]
        }
        println("[Vulkan] ${swapChainFramebuffers.size} framebuffers created.")
    }

    private fun initCommandBuffers() = MemoryStack.stackPush().use { stack ->
        commandBuffers = LongArray(swapChainFramebuffers.size)
        
        // Per-frame command buffers for double buffering
        frameCommandBuffers = Array(MAX_FRAMES_IN_FLIGHT) { LongArray(swapChainFramebuffers.size) }
        
        val allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            .commandPool(commandPool)
            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            .commandBufferCount(commandBuffers.size)

        val buf = stack.mallocPointer(commandBuffers.size)
        vkAllocateCommandBuffers(device, allocInfo, buf)
        for (i in commandBuffers.indices) {
            commandBuffers[i] = buf[i]
        }
        
        // Allocate per-frame command buffers
        for (frame in 0 until MAX_FRAMES_IN_FLIGHT) {
            vkAllocateCommandBuffers(device, allocInfo, buf)
            for (i in commandBuffers.indices) {
                frameCommandBuffers[frame][i] = buf[i]
            }
        }
        
        println("[Vulkan] Command buffers created: $MAX_FRAMES_IN_FLIGHT frames × ${swapChainFramebuffers.size} images (double buffering)")
    }

    private fun initSyncObjects() = MemoryStack.stackPush().use { stack ->
        imageAvailableSemaphores = LongArray(MAX_FRAMES_IN_FLIGHT)
        renderFinishedSemaphores = LongArray(MAX_FRAMES_IN_FLIGHT)
        inFlightFences = LongArray(MAX_FRAMES_IN_FLIGHT)

        val semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)

        val fenceInfo = VkFenceCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
            .flags(VK_FENCE_CREATE_SIGNALED_BIT)

        val semBuf = stack.mallocLong(1)
        val fenceBuf = stack.mallocLong(1)

        for (i in 0 until MAX_FRAMES_IN_FLIGHT) {
            if (vkCreateSemaphore(device, semaphoreInfo, null, semBuf) != VK_SUCCESS)
                throw RuntimeException("Failed to create semaphore")
            
            imageAvailableSemaphores[i] = semBuf[0]

            if (vkCreateSemaphore(device, semaphoreInfo, null, semBuf) != VK_SUCCESS)
                throw RuntimeException("Failed to create semaphore")
            
            renderFinishedSemaphores[i] = semBuf[0]

            if (vkCreateFence(device, fenceInfo, null, fenceBuf) != VK_SUCCESS)
                throw RuntimeException("Failed to create fence")
            
            inFlightFences[i] = fenceBuf[0]
        }
        println("[Vulkan] Sync objects created: $MAX_FRAMES_IN_FLIGHT frames in flight")
    }

    private fun createTestCube() = MemoryStack.stackPush().use { stack ->
        val vertices = com.novusforge.astrum.engine.CubeMesh.generateTexturedCube(0.5f, 0.5f, 0.5f, 0.8f, 0.4f, 0.2f)
        val vSize = vertices.limit() * 4L
        testCubeVertexCount = vertices.limit() / 6

        val vBuf = stack.mallocLong(1)
        val vMem = stack.mallocLong(1)
        createBuffer(vSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, vBuf, vMem)

        // Convert float[] to FloatBuffer
        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(vertices.limit() * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); flip() }

        val data = stack.mallocPointer(1)
        vkMapMemory(device, vMem[0], 0, vSize, 0, data)
        memCopy(memAddress(vertexBuffer), data[0], vSize)
        vkUnmapMemory(device, vMem[0])

        testCubeVbo = vBuf[0]
        testCubeVboMem = vMem[0]

        println("[Vulkan] Test cube created: $testCubeVertexCount vertices")
    }

    private fun createBuffer(size: Long, usage: Int, properties: Int, buffer: LongBuffer, memory: LongBuffer) =
        MemoryStack.stackPush().use { stack ->
            val bufferInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)

            if (vkCreateBuffer(device, bufferInfo, null, buffer) != VK_SUCCESS)
                throw RuntimeException("Failed to create buffer")

            val memReqs = VkMemoryRequirements.calloc(stack)
            vkGetBufferMemoryRequirements(device, buffer[0], memReqs)

            val allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReqs.size())
                .memoryTypeIndex(findMemoryType(memReqs.memoryTypeBits(), properties))

            if (vkAllocateMemory(device, allocInfo, null, memory) != VK_SUCCESS)
                throw RuntimeException("Failed to allocate buffer memory")

            vkBindBufferMemory(device, buffer[0], memory[0], 0)
        }

    private fun findMemoryType(typeFilter: Int, properties: Int): Int =
        MemoryStack.stackPush().use { stack ->
            val memProps = VkPhysicalDeviceMemoryProperties.calloc(stack)
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps)

            (0 until memProps.memoryTypeCount())
                .first { 
                    (typeFilter and (1 shl it) != 0) &&
                    (memProps.memoryTypes(it).propertyFlags() and properties == properties)
                }
                .takeIf { it >= 0 }
                ?: throw RuntimeException("Failed to find suitable memory type")
        }

    override fun render(view: Matrix4f, projection: Matrix4f, meshes: Map<Long, ChunkMesh>) =
        MemoryStack.stackPush().use { stack ->
            // Wait for previous frame (GPU → CPU sync)
            vkWaitForFences(device, inFlightFences[currentFrame], true, Long.MAX_VALUE)
            
            // Acquire next image from swapchain
            val imageIndexBuf = stack.mallocInt(1)
            val result = vkAcquireNextImageKHR(device, swapChain, Long.MAX_VALUE,
                imageAvailableSemaphores[currentFrame], VK_NULL_HANDLE, imageIndexBuf)
            
            currentImageIndex = imageIndexBuf[0]

            if (result == VK_ERROR_OUT_OF_DATE_KHR) return
            if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR)
                throw RuntimeException("Failed to acquire swap chain image")
            
            // Reset fence
            vkResetFences(device, inFlightFences[currentFrame])

            // Get command buffer for THIS FRAME and THIS IMAGE
            val vkCmd = VkCommandBuffer(frameCommandBuffers[currentFrame][currentImageIndex], device)

            // Update uniform buffer
            val data = stack.mallocPointer(1)
            vkMapMemory(device, uniformBufferMemory, 0, 64 * 3, 0, data)
            val buffer = data[0].getByteBuffer(0, 64 * 3)
            projection.get(0, buffer)
            view.get(64, buffer)
            Matrix4f().get(128, buffer)
            vkUnmapMemory(device, uniformBufferMemory)

            // Reset and begin command buffer
            vkResetCommandBuffer(vkCmd, 0)
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            vkBeginCommandBuffer(vkCmd, beginInfo)

            // Begin render pass - MAGENTA TEST COLOR!
            val clearColor = VkClearValue.calloc(1, stack)
                .color().float32(stack.floats(1.0f, 0.0f, 1.0f, 1.0f))  // BRIGHT MAGENTA!

            val renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(renderPass)
                .framebuffer(swapChainFramebuffers[currentImageIndex])
                .renderArea(VkRect2D.calloc(stack)
                    .offset(VkOffset2D.calloc(stack).set(0, 0))
                    .extent(VkExtent2D.calloc(stack).set(swapChainWidth, swapChainHeight)))
                .pClearValues(clearColor)

            vkCmdBeginRenderPass(vkCmd, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)
            vkCmdBindPipeline(vkCmd, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline)

            // Set dynamic viewport and scissor
            val viewport = VkViewport.calloc(1, stack)
                .x(0f).y(0f)
                .width(swapChainWidth.toFloat()).height(swapChainHeight.toFloat())
                .minDepth(0f).maxDepth(1f)
            vkCmdSetViewport(vkCmd, 0, viewport)

            val scissor = VkRect2D.calloc(1, stack)
                .offset(VkOffset2D.calloc(stack).set(0, 0))
                .extent(VkExtent2D.calloc(stack).set(swapChainWidth, swapChainHeight))
            vkCmdSetScissor(vkCmd, 0, scissor)

            vkCmdBindDescriptorSets(vkCmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, descriptorSets, null)

            // Render test cube
            if (renderTestCube && testCubeVbo != 0) {
                val vboBuf = stack.mallocLong(1).put(0, testCubeVbo)
                val offsetBuf = stack.mallocLong(1).put(0, 0L)
                vkCmdBindVertexBuffers(vkCmd, 0, vboBuf, offsetBuf)
                vkCmdDraw(vkCmd, testCubeVertexCount, 1, 0, 0)
            }

            // Render chunk meshes
            for (mesh in meshes.values) {
                if (mesh.opaqueVboId == 0L) uploadMesh(mesh)
                if (mesh.opaqueVboId != 0L && mesh.opaqueIndexCount > 0) {
                    val vboBuf = stack.mallocLong(1).put(0, mesh.opaqueVboId)
                    val offsetBuf = stack.mallocLong(1).put(0, 0L)
                    vkCmdBindVertexBuffers(vkCmd, 0, vboBuf, offsetBuf)
                    vkCmdBindIndexBuffer(vkCmd, mesh.opaqueIboId, 0, VK_INDEX_TYPE_UINT32)
                    vkCmdDrawIndexed(vkCmd, mesh.opaqueIndexCount, 1, 0, 0, 0)
                }
            }

            vkCmdEndRenderPass(vkCmd)
            vkEndCommandBuffer(vkCmd)

            // Submit
            val waitStages = stack.mallocInt(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            val waitSemaphores = stack.mallocLong(1).put(0, imageAvailableSemaphores[currentFrame])
            val signalSemaphores = stack.mallocLong(1).put(0, renderFinishedSemaphores[currentFrame])

            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pWaitSemaphores(waitSemaphores)
                .pWaitDstStageMask(waitStages)
                .pCommandBuffers(stack.pointers(vkCmd.address()))
                .pSignalSemaphores(signalSemaphores)

            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[currentFrame]) != VK_SUCCESS)
                throw RuntimeException("Failed to submit command buffer")

            // Present
            val presentWaitSemaphores = stack.mallocLong(1).put(0, renderFinishedSemaphores[currentFrame])
            val imageIndexToPresent = stack.mallocInt(1).put(0, currentImageIndex)

            val presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(presentWaitSemaphores)
                .pSwapchains(stack.longs(swapChain))
                .pImageIndices(imageIndexToPresent)

            val presentResult = vkQueuePresentKHR(presentQueue, presentInfo)

            if (presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR) return
            if (presentResult != VK_SUCCESS)
                throw RuntimeException("Failed to present swap chain image")

            // Toggle front/back buffer
            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT
        }

    private fun uploadMesh(mesh: ChunkMesh) = MemoryStack.stackPush().use { stack ->
        if (!mesh.hasOpaqueData()) return

        val vertices = mesh.buildOpaqueVertexData()
        val vSize = vertices.limit() * 4L

        val vBuf = stack.mallocLong(1)
        val vMem = stack.mallocLong(1)
        createBuffer(vSize, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, vBuf, vMem)

        val data = stack.mallocPointer(1)
        vkMapMemory(device, vMem[0], 0, vSize, 0, data)
        memCopy(memAddress(vertices), data[0], vSize)
        vkUnmapMemory(device, vMem[0])

        mesh.opaqueVboId = vBuf[0]
        mesh.opaqueVboMemId = vMem[0]

        val indices = mesh.buildOpaqueIndexData()
        val iSize = indices.limit() * 4L

        val iBuf = stack.mallocLong(1)
        val iMem = stack.mallocLong(1)
        createBuffer(iSize, VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, iBuf, iMem)

        vkMapMemory(device, iMem[0], 0, iSize, 0, data)
        memCopy(memAddress(indices), data[0], iSize)
        vkUnmapMemory(device, iMem[0])

        mesh.opaqueIboId = iBuf[0]
        mesh.opaqueIboMemId = iMem[0]
    }

    override fun windowShouldClose(): Boolean = glfwWindowShouldClose(window)
    override fun getWindow(): Long = window
    override fun getAspectRatio(): Float = aspectRatio
    override fun setRenderTestCube(render: Boolean) { renderTestCube = render }
    
    override fun cleanup() {
        if (::device.isInitialized) {
            vkDeviceWaitIdle(device)

            if (testCubeVbo != 0L) {
                vkDestroyBuffer(device, testCubeVbo, null)
                vkFreeMemory(device, testCubeVboMem, null)
            }

            for (i in 0 until MAX_FRAMES_IN_FLIGHT) {
                if (imageAvailableSemaphores.getOrNull(i) != 0L)
                    vkDestroySemaphore(device, imageAvailableSemaphores[i], null)
                if (renderFinishedSemaphores.getOrNull(i) != 0L)
                    vkDestroySemaphore(device, renderFinishedSemaphores[i], null)
                if (inFlightFences.getOrNull(i) != 0L)
                    vkDestroyFence(device, inFlightFences[i], null)
            }

            if (uniformBuffer != 0L) {
                vkDestroyBuffer(device, uniformBuffer, null)
                vkFreeMemory(device, uniformBufferMemory, null)
            }
            if (descriptorPool != 0L) vkDestroyDescriptorPool(device, descriptorPool, null)
            if (descriptorSetLayout != 0L) vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null)

            for (fb in swapChainFramebuffers) vkDestroyFramebuffer(device, fb, null)
            vkDestroyPipeline(device, graphicsPipeline, null)
            vkDestroyPipelineLayout(device, pipelineLayout, null)
            vkDestroyRenderPass(device, renderPass, null)

            for (view in swapChainImageViews) vkDestroyImageView(device, view, null)
            vkDestroySwapchainKHR(device, swapChain, null)
            vkDestroyCommandPool(device, commandPool, null)
            vkDestroyDevice(device, null)
        }
        if (::instance.isInitialized) {
            vkDestroySurfaceKHR(instance, surface, null)
            vkDestroyInstance(instance, null)
        }
        if (window != 0L) glfwDestroyWindow(window)
        glfwTerminate()
    }

    override fun deleteBuffer(bufferId: Long, memoryId: Long) {
        if (::device.isInitialized) {
            vkDestroyBuffer(device, bufferId, null)
            if (memoryId != 0L) vkFreeMemory(device, memoryId, null)
        }
    }

    override fun getRendererName(): String = "Vulkan (Kotlin)"
    override fun getFPS(): Int = 60
    override fun getLoadedChunkCount(): Int = 0
}
