package com.novusforge.astrum.game;

import com.novusforge.astrum.engine.VulkanRenderer;
import com.novusforge.astrum.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Project Astrum - Main Entry Point
 * A Vulkan-based voxel sandbox game by Novusforge Studios
 */
public class Main {
    
    private static final String TITLE = "Astrum - Pre-classic (Cave Game)";
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1_000_000_000 / TARGET_FPS;

    public static void main(String[] args) {
        // Print Astrum Banner
        System.out.println("=".repeat(50));
        System.out.println("  ASTRUM - Pre-classic (Cave Game)");
        System.out.println("  Project Astrum v0.0.1");
        System.out.println("  By Novusforge Studios");
        System.out.println("=".repeat(50));
        System.out.println();

        // Initialize Vulkan Renderer
        VulkanRenderer renderer = new VulkanRenderer();
        Game game = null;
        
        try {
            renderer.init();
            game = new Game(renderer.getWindow(), renderer);
            game.init();
            
            // Set up buffer deletion callback
            World.setBufferDeleter(renderer::deleteBuffer);
            
            System.out.println();
            System.out.println("[Render] Vulkan renderer initialized!");
            System.out.println("[Game] Astrum Pre-classic loaded!");
            System.out.println("[Controls] WASD to move, Space to jump");
            System.out.println("[Controls] Mouse to look, Click to lock");
            System.out.println("[Controls] Left click: Break block, Right click: Place block");
            System.out.println();

            // Game Loop
            long lastTime = System.nanoTime();
            int frameCount = 0;
            long fpsTimer = System.currentTimeMillis();
            
            Matrix4f projectionMatrix = new Matrix4f();
            Matrix4f viewMatrix = new Matrix4f();
            
            while (!renderer.windowShouldClose() && game.isRunning()) {
                glfwPollEvents();
                
                long currentTime = System.nanoTime();
                float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
                lastTime = currentTime;
                
                // Cap delta time to avoid physics explosions
                deltaTime = Math.min(deltaTime, 0.1f);
                
                // Update game logic
                game.update(deltaTime);
                
                // Update matrices
                projectionMatrix.setPerspective(
                    (float) Math.toRadians(70.0f), 
                    renderer.getAspectRatio(), 
                    0.1f, 
                    1000.0f, 
                    true
                );
                game.getInput().getViewMatrix(viewMatrix);
                Vector3f pos = game.getPlayer().getPosition();
                viewMatrix.translate(-pos.x, -pos.y, -pos.z);
                
                // Get visible chunk meshes
                java.util.Map<Long, com.novusforge.astrum.world.ChunkMesh> visibleMeshes =
                    game.getWorld().getVisibleMeshes(pos.x, pos.y, pos.z, null);
                
                // Render
                renderer.render(viewMatrix, projectionMatrix, visibleMeshes);
                
                // FPS counter
                frameCount++;
                if (System.currentTimeMillis() - fpsTimer >= 1000) {
                    int chunks = game.getWorld().getLoadedChunkCount();
                    System.out.printf("[FPS] %d | [Chunks] %d | [Pos] %.1f, %.1f, %.1f%n",
                        frameCount, chunks, pos.x, pos.y, pos.z);
                    frameCount = 0;
                    fpsTimer = System.currentTimeMillis();
                }
                
                // Frame timing
                long elapsed = System.nanoTime() - currentTime;
                if (elapsed < FRAME_TIME) {
                    try {
                        Thread.sleep((FRAME_TIME - elapsed) / 1_000_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            System.out.println("[Shutdown] Closing Astrum...");
            
        } finally {
            if (game != null) {
                game.cleanup();
            }
            renderer.cleanup();
            System.out.println("[Shutdown] Astrum closed successfully!");
        }
    }
}
