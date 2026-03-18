package com.novusforge.astrum.engine;

import org.joml.Vector3f;
import org.joml.Vector3i;
import static org.lwjgl.glfw.GLFW.*;

public class Player {
    private Vector3f position;
    private Vector3f velocity;
    private float speed = 5.0f;
    private float sprintSpeed = 8.0f;
    private float jumpVelocity = 8.0f;
    
    private boolean onGround = false;
    private boolean isSprinting = false;
    
    private float gravity = -25.0f;
    private float fallVelocity = 0.0f;
    
    private InputManager input;
    
    public Player(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f(0, 0, 0);
    }
    
    public void update(float deltaTime, InputManager input, WorldInterface world) {
        this.input = input;
        
        float currentSpeed = isSprinting ? sprintSpeed : speed;
        
        Vector3f forward = new Vector3f();
        Vector3f right = new Vector3f();
        input.getForwardVector(forward);
        input.getRightVector(right);
        forward.y = 0;
        right.y = 0;
        forward.normalize();
        right.normalize();
        
        Vector3f moveDir = new Vector3f(0, 0, 0);
        
        if (input.isForwardPressed()) {
            moveDir.add(forward);
        }
        if (input.isBackwardPressed()) {
            moveDir.sub(forward);
        }
        if (input.isLeftPressed()) {
            moveDir.sub(right);
        }
        if (input.isRightPressed()) {
            moveDir.add(right);
        }
        
        if (moveDir.length() > 0) {
            moveDir.normalize().mul(currentSpeed * deltaTime);
            position.add(moveDir.x, 0, moveDir.z);
        }
        
        if (input.isJumpPressed() && onGround) {
            fallVelocity = jumpVelocity;
            onGround = false;
        }
        
        isSprinting = input.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || 
                      input.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
        
        fallVelocity += gravity * deltaTime;
        position.y += fallVelocity * deltaTime;
        
        if (world != null) {
            int blockY = (int) Math.floor(position.y);
            if (position.y < blockY + 1.5f && blockY >= 0) {
                position.y = blockY + 1.5f;
                fallVelocity = 0;
                onGround = true;
            }
        }
        
        if (position.y < 1.5f) {
            position.y = 1.5f;
            fallVelocity = 0;
            onGround = true;
        }
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    public Vector3i getChunkCoord() {
        int cx = (int) Math.floor(position.x / 32.0f);
        int cz = (int) Math.floor(position.z / 32.0f);
        return new Vector3i(cx, 0, cz);
    }
    
    public boolean isOnGround() {
        return onGround;
    }
    
    public boolean isSprinting() {
        return isSprinting;
    }
    
    public void cleanup() {
    }
    
    public interface WorldInterface {
        short getBlock(int x, int y, int z);
    }
}
