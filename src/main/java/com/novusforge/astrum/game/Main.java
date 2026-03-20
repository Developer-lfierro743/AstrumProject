package com.novusforge.astrum.game;

import com.novusforge.astrum.core.ECS;
import com.novusforge.astrum.core.SafetyGuardian;
import com.novusforge.astrum.engine.VulkanRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class Main {
    private static final String TITLE = "Astrum - Pre-classic (Cave Game)";
    private static final int TARGET_FPS = 60;
    private static final long FRAME_TIME = 1_000_000_000 / TARGET_FPS;
    
    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("  ASTRUM - Pre-classic (Cave Game)");
        System.out.println("  Project Astrum v0.1.0");
        System.out.println("  By Novusforge Studios");
        System.out.println("=".repeat(50));
        System.out.println();

        SafetyGuardian guardian = new SafetyGuardian();
        SafetyGuardian.ActionContext startupContext = new SafetyGuardian.ActionContext("startup", "system", "engine");
        if (guardian.validate(startupContext) == SafetyGuardian.SafetyResult.BLOCK) {
            System.err.println("SafetyGuardian blocked startup.");
            return;
        }
        System.out.println("[Init] SafetyGuardian: OK");

        ECS ecs = new ECS();
        System.out.println("[Init] ECS: OK");
        System.out.println();

        VulkanRenderer renderer = new VulkanRenderer();
        Game game = null;
        
        try {
            renderer.init();
            game = new Game(renderer.getWindow(), ecs, guardian);
            game.init();
            
            // Set up buffer deletion callback
            com.novusforge.astrum.world.World.setBufferDeleter(renderer::deleteBuffer);
            
            System.out.println();
            System.out.println("[Render] Vulkan renderer initialized!");
            System.out.println("[Game] Astrum Pre-classic loaded!");
            System.out.println("[Controls] Click to lock mouse, WASD to move, Space to jump");
            System.out.println("[Controls] Left click: Break block, Right click: Place block");
            System.out.println();
            
            long lastTime = System.nanoTime();
            int frameCount = 0;
            long fpsTimer = System.currentTimeMillis();
            int tickCount = 0;
            
            Matrix4f projectionMatrix = new Matrix4f();
            Matrix4f viewMatrix = new Matrix4f();
            
            while (!renderer.windowShouldClose() && game.isRunning()) {
                glfwPollEvents();
                
                long currentTime = System.nanoTime();
                float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
                lastTime = currentTime;
                
                deltaTime = Math.min(deltaTime, 0.1f);
                
                game.update(deltaTime);
                
                // Update matrices
                projectionMatrix.setPerspective((float) Math.toRadians(70.0f), renderer.getAspectRatio(), 0.1f, 1000.0f, true);
                game.getInput().getViewMatrix(viewMatrix);
                viewMatrix.translate(-game.getPlayer().getPosition().x, -game.getPlayer().getPosition().y, -game.getPlayer().getPosition().z);

                // Render visible chunks
                java.util.Map<Long, com.novusforge.astrum.world.ChunkMesh> visibleMeshes = 
                    game.getWorld().getVisibleMeshes(
                        game.getPlayer().getPosition().x, 
                        game.getPlayer().getPosition().y, 
                        game.getPlayer().getPosition().z, 
                        null // TODO: Implement frustum planes
                    );
                
                renderer.render(viewMatrix, projectionMatrix, visibleMeshes);
                
                frameCount++;
                tickCount++;
                
                if (System.currentTimeMillis() - fpsTimer >= 1000) {
                    Vector3f pos = game.getPlayer().getPosition();
                    int chunks = game.getWorld().getLoadedChunkCount();
                    System.out.printf("[FPS] %d | [Chunks] %d | [Pos] %.1f, %.1f, %.1f%n", 
                        frameCount, chunks, pos.x, pos.y, pos.z);
                    frameCount = 0;
                    fpsTimer = System.currentTimeMillis();
                }
                
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
