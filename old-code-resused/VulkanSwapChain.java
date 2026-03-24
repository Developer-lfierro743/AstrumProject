package com.astrum.core.renderer.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class VulkanSwapChain {
    private long swapChain;
    private long[] images;
    private long[] imageViews; // Needed for Framebuffers
    private VkExtent2D extent;
    private int swapChainImageFormat;

    public void create(VkDevice device, VkPhysicalDevice physicalDevice, long surface, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            // 1. Check Capabilities
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, caps);

            // 2. Set Format and Extent
            swapChainImageFormat = VK_FORMAT_B8G8R8A8_SRGB;
            extent = VkExtent2D.create().set(width, height);

            // 3. Create SwapChain
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(surface)
                .minImageCount(3)
                .imageFormat(swapChainImageFormat)
                .imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .imageExtent(extent)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(caps.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(VK_PRESENT_MODE_MAILBOX_KHR)
                .clipped(true);

            LongBuffer pSwapChain = stack.mallocLong(1);
            if (vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create SwapChain!");
            }
            swapChain = pSwapChain.get(0);

            // 4. Retrieve Images
            IntBuffer imageCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, swapChain, imageCount, null);
            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));
            vkGetSwapchainImagesKHR(device, swapChain, imageCount, pSwapchainImages);

            images = new long[imageCount.get(0)];
            imageViews = new long[imageCount.get(0)];

            for (int i = 0; i < images.length; i++) {
                images[i] = pSwapchainImages.get(i);
                
                // --- CRITICAL ADDITION: Create Image Views ---
                VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(images[i])
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(swapChainImageFormat)
                    .subresourceRange(it -> it
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0)
                        .levelCount(1)
                        .baseArrayLayer(0)
                        .layerCount(1));

                LongBuffer pView = stack.mallocLong(1);
                if (vkCreateImageView(device, viewInfo, null, pView) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create Image View!");
                }
                imageViews[i] = pView.get(0);
            }
        }
    }

    public void cleanup(VkDevice device) {
        for (long imageView : imageViews) {
            vkDestroyImageView(device, imageView, null);
        }
        vkDestroySwapchainKHR(device, swapChain, null);
    }
    
    // Getters for the Renderer
    public long[] getImageViews() { return imageViews; }
    public VkExtent2D getExtent() { return extent; }
}
