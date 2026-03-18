package com.novusforge.astrum.game;

import com.novusforge.astrum.engine.*;
import com.novusforge.astrum.world.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

public class Game {
    private Player player;
    private InputManager input;
    private World world;
    private ChunkManager chunkManager;
    
    private long window;
    private boolean running = true;
    
    private int lastLeftClickState = 0;
    private int lastRightClickState = 0;
    
    public Game(long window) {
        this.window = window;
    }
    
    public void init() {
        input = new InputManager(window);
        player = new Player(0, 20, 0);
        world = new World();
        chunkManager = world.getChunkManager();
        
        System.out.println("[Game] World initialized with seed: " + world.getSeed());
        System.out.println("[Game] Spawn point: (0, 20, 0)");
    }
    
    public void update(float deltaTime) {
        if (input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            input.releasePointerLock();
        }
        
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
            input.requestPointerLock();
        }
        
        Vector3f pos = player.getPosition();
        world.updatePlayerPosition(pos.x, pos.y, pos.z);
        
        player.update(deltaTime, input, (x, y, z) -> world.getBlock(x, y, z));
        
        int playerChunkX = (int) Math.floor(pos.x / Chunk.SIZE);
        int playerChunkZ = (int) Math.floor(pos.z / Chunk.SIZE);
        chunkManager.tick();
        
        int leftClick = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1);
        if (leftClick == GLFW_PRESS && lastLeftClickState == GLFW_RELEASE) {
            breakBlock();
        }
        lastLeftClickState = leftClick;
        
        int rightClick = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_2);
        if (rightClick == GLFW_PRESS && lastRightClickState == GLFW_RELEASE) {
            placeBlock();
        }
        lastRightClickState = rightClick;
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
                System.out.println("[Game] Block broken: (" + bx + ", " + by + ", " + bz + ")");
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
                
                world.setBlock(px, py, pz, (short) 1);
                System.out.println("[Game] Block placed: (" + px + ", " + py + ", " + pz + ")");
                break;
            }
        }
    }
    
    public World getWorld() {
        return world;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public InputManager getInput() {
        return input;
    }
    
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void stop() {
        running = false;
    }
    
    public void cleanup() {
        if (input != null) input.cleanup();
        if (player != null) player.cleanup();
        if (world != null) world.dispose();
        System.out.println("[Game] Cleanup complete.");
    }
    
    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
    }
}
