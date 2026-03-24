package com.novusforge.astrum.game;

import com.novusforge.astrum.engine.*;
import com.novusforge.astrum.world.*;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Game logic - Pre-Classic stage
 * Following Notch's approach: simple, functional, optimized
 */
public class Game {
    
    private Player player;
    private InputManager input;
    private World world;
    private ChunkManager chunkManager;
    private IRenderer renderer;
    
    private long window;
    private boolean running = true;
    
    private int lastLeftClick = 0;
    private int lastRightClick = 0;

    public Game(long window, IRenderer renderer) {
        this.window = window;
        this.renderer = renderer;
    }

    public void init() {
        input = new InputManager(window);
        player = new Player(0, 100, 0); // Spawn high
        
        world = new World();
        chunkManager = world.getChunkManager();
        
        System.out.println("[Game] World seed: " + world.getSeed());
        System.out.println("[Game] Spawn: (0, 100, 0)");
    }
    
    public void update(float deltaTime) {
        // Escape releases mouse
        if (input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            input.releasePointerLock();
        }
        
        // Click locks mouse
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
            input.requestPointerLock();
        }
        
        // Update player
        Vector3f pos = player.getPosition();
        world.updatePlayerPosition(pos.x, pos.y, pos.z);
        player.update(deltaTime, input, (x, y, z) -> world.getBlock(x, y, z));
        
        // Update chunks
        chunkManager.tick();
        
        // Block breaking
        int leftClick = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1);
        if (leftClick == GLFW_PRESS && lastLeftClick == GLFW_RELEASE) {
            breakBlock();
        }
        lastLeftClick = leftClick;
        
        // Block placing
        int rightClick = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_2);
        if (rightClick == GLFW_PRESS && lastRightClick == GLFW_RELEASE) {
            placeBlock();
        }
        lastRightClick = rightClick;
    }
    
    private void breakBlock() {
        Vector3f forward = new Vector3f();
        input.getForwardVector(forward);
        Vector3f pos = player.getPosition();
        
        for (int i = 1; i <= 6; i++) {
            int bx = (int) Math.floor(pos.x + forward.x * i);
            int by = (int) Math.floor(pos.y + forward.y * i);
            int bz = (int) Math.floor(pos.z + forward.z * i);
            
            short block = world.getBlock(bx, by, bz);
            if (block != 0) {
                world.setBlock(bx, by, bz, (short) 0);
                System.out.println("[Game] Broke block at (" + bx + ", " + by + ", " + bz + ")");
                break;
            }
        }
    }
    
    private void placeBlock() {
        Vector3f forward = new Vector3f();
        input.getForwardVector(forward);
        Vector3f pos = player.getPosition();
        
        for (int i = 5; i >= 1; i--) {
            int bx = (int) Math.floor(pos.x + forward.x * i);
            int by = (int) Math.floor(pos.y + forward.y * i);
            int bz = (int) Math.floor(pos.z + forward.z * i);
            
            short block = world.getBlock(bx, by, bz);
            if (block != 0) {
                int px = (int) Math.floor(pos.x + forward.x * (i + 1));
                int py = (int) Math.floor(pos.y + forward.y * (i + 1));
                int pz = (int) Math.floor(pos.z + forward.z * (i + 1));
                
                world.setBlock(px, py, pz, (short) 1); // Dirt block
                System.out.println("[Game] Placed block at (" + px + ", " + py + ", " + pz + ")");
                break;
            }
        }
    }
    
    public World getWorld() { return world; }
    public Player getPlayer() { return player; }
    public InputManager getInput() { return input; }
    public boolean isRunning() { return running; }
    
    public void cleanup() {
        if (input != null) input.cleanup();
        if (player != null) player.cleanup();
        if (world != null) world.dispose();
    }
}
