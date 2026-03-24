package com.novusforge.astrum.engine;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Player - First-person camera and movement
 * Pre-Classic implementation (simple physics)
 */
public class Player {
    private Vector3f position;
    private float speed = 5.0f;
    private float jumpVelocity = 8.0f;
    
    private boolean onGround = false;
    private float fallVelocity = 0.0f;
    private float gravity = -25.0f;

    public Player(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
    }

    public void update(float deltaTime, InputManager input, WorldInterface world) {
        // Get movement direction
        Vector3f forward = new Vector3f();
        Vector3f right = new Vector3f();
        input.getForwardVector(forward);
        input.getRightVector(right);
        
        forward.y = 0;
        right.y = 0;
        forward.normalize();
        right.normalize();

        // Calculate movement
        Vector3f moveDir = new Vector3f(0, 0, 0);

        if (input.isForwardPressed()) moveDir.add(forward);
        if (input.isBackwardPressed()) moveDir.sub(forward);
        if (input.isLeftPressed()) moveDir.sub(right);
        if (input.isRightPressed()) moveDir.add(right);

        if (moveDir.length() > 0) {
            moveDir.normalize().mul(speed * deltaTime);
            position.add(moveDir.x, 0, moveDir.z);
        }

        // Jump
        if (input.isJumpPressed() && onGround) {
            fallVelocity = jumpVelocity;
            onGround = false;
        }

        // Gravity
        fallVelocity += gravity * deltaTime;
        position.y += fallVelocity * deltaTime;

        // Ground collision (simple - check block below)
        if (world != null) {
            int blockX = (int) Math.floor(position.x);
            int blockY = (int) Math.floor(position.y - 1.0f);
            int blockZ = (int) Math.floor(position.z);
            
            short blockBelow = world.getBlock(blockX, blockY, blockZ);
            float groundLevel = blockY + 1.0f;
            
            if (position.y <= groundLevel + 0.5f && position.y > groundLevel - 1.0f && blockBelow != 0) {
                position.y = groundLevel + 0.5f;
                fallVelocity = 0;
                onGround = true;
            } else if (position.y < 1.5f) {
                // Bedrock floor at y=0
                position.y = 1.5f;
                fallVelocity = 0;
                onGround = true;
            } else {
                onGround = false;
            }
        } else {
            // Fallback floor
            if (position.y < 1.5f) {
                position.y = 1.5f;
                fallVelocity = 0;
                onGround = true;
            }
        }
    }

    public Vector3f getPosition() { return position; }
    public void setPosition(float x, float y, float z) { position.set(x, y, z); }
    
    public Vector3i getChunkCoord() {
        int cx = (int) Math.floor(position.x / 32.0f);
        int cz = (int) Math.floor(position.z / 32.0f);
        return new Vector3i(cx, 0, cz);
    }

    public boolean isOnGround() { return onGround; }
    public void cleanup() {}

    /**
     * World interface for block collision detection
     */
    public interface WorldInterface {
        short getBlock(int x, int y, int z);
    }
}
