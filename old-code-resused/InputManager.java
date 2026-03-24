package com.novusforge.astrum.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.*;

/**
 * Input Manager - Handles keyboard, mouse, and touch input
 * Pre-Classic implementation
 */
public class InputManager {
    private long window;
    
    private boolean[] keys = new boolean[512];
    private boolean[] mouseButtons = new boolean[8];
    
    private float mouseSensitivity = 0.1f;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    
    private boolean pointerLocked = false;

    public InputManager(long window) {
        this.window = window;
        setupCallbacks();
    }

    private void setupCallbacks() {
        // Keyboard callback
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key >= 0 && key < keys.length) {
                keys[key] = (action == GLFW_PRESS || action == GLFW_REPEAT);
            }
        });

        // Mouse button callback
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button >= 0 && button < mouseButtons.length) {
                mouseButtons[button] = (action == GLFW_PRESS);
            }
        });

        // Cursor position callback (for mouse look)
        glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            if (pointerLocked) {
                // Mouse look logic handled in update
            }
        });

        // Cursor enter callback
        glfwSetCursorEnterCallback(window, (w, entered) -> {
            if (entered && pointerLocked) {
                double[] x = new double[1];
                double[] y = new double[1];
                glfwGetCursorPos(w, x, y);
            }
        });
    }

    public void requestPointerLock() {
        if (!pointerLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            pointerLocked = true;
        }
    }

    public void releasePointerLock() {
        if (pointerLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            pointerLocked = false;
        }
    }

    public boolean isKeyPressed(int key) {
        return keys[key];
    }

    public boolean isForwardPressed() {
        return keys[GLFW_KEY_W] || keys[GLFW_KEY_UP];
    }

    public boolean isBackwardPressed() {
        return keys[GLFW_KEY_S] || keys[GLFW_KEY_DOWN];
    }

    public boolean isLeftPressed() {
        return keys[GLFW_KEY_A] || keys[GLFW_KEY_LEFT];
    }

    public boolean isRightPressed() {
        return keys[GLFW_KEY_D] || keys[GLFW_KEY_RIGHT];
    }

    public boolean isJumpPressed() {
        return keys[GLFW_KEY_SPACE];
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    /**
     * Get view matrix for camera
     */
    public void getViewMatrix(Matrix4f result) {
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        float sinPitch = (float) Math.sin(Math.toRadians(pitch));
        float cosYaw = (float) Math.cos(Math.toRadians(yaw));
        float sinYaw = (float) Math.sin(Math.toRadians(yaw));

        float x = cosPitch * cosYaw;
        float y = sinPitch;
        float z = cosPitch * sinYaw;

        result.setLookAt(0, 0, 0, x, y, z, 0, 1, 0);
    }

    /**
     * Get forward direction vector
     */
    public void getForwardVector(Vector3f result) {
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        result.x = -cosPitch * (float) Math.sin(Math.toRadians(yaw));
        result.y = (float) Math.sin(Math.toRadians(pitch));
        result.z = -cosPitch * (float) Math.cos(Math.toRadians(yaw));
    }

    /**
     * Get right direction vector
     */
    public void getRightVector(Vector3f result) {
        result.x = (float) Math.cos(Math.toRadians(yaw));
        result.y = 0;
        result.z = -(float) Math.sin(Math.toRadians(yaw));
    }

    public void cleanup() {
        releasePointerLock();
    }
}
