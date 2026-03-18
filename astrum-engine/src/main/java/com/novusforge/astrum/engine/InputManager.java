package com.novusforge.astrum.engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

public class InputManager {
    private long window;
    
    private boolean[] keys = new boolean[512];
    private boolean[] mouseButtons = new boolean[8];
    
    private float mouseSensitivity = 0.1f;
    private float yaw = 0.0f;
    private float pitch = 0.0f;
    
    private float touchSensitivity = 0.3f;
    private float lastTouchX = 0;
    private float lastTouchY = 0;
    private boolean isTouching = false;
    
    private boolean pointerLock = false;
    
    public InputManager(long window) {
        this.window = window;
        setupCallbacks();
    }
    
    private void setupCallbacks() {
        glfwSetKeyCallback(window, new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key >= 0 && key < keys.length) {
                    keys[key] = action == GLFW_PRESS || action == GLFW_REPEAT;
                }
            }
        });
        
        glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button >= 0 && button < mouseButtons.length) {
                    mouseButtons[button] = action == GLFW_PRESS;
                }
            }
        });
        
        glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                if (pointerLock) {
                    float dx = (float) (xpos - lastTouchX) * mouseSensitivity;
                    float dy = (float) (ypos - lastTouchY) * mouseSensitivity;
                    
                    yaw += dx;
                    pitch -= dy;
                    pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
                    
                    lastTouchX = (float) xpos;
                    lastTouchY = (float) ypos;
                }
            }
        });
        
        glfwSetCursorEnterCallback(window, (w, entered) -> {
            if (entered) {
                double[] x = new double[1];
                double[] y = new double[1];
                glfwGetCursorPos(w, x, y);
                lastTouchX = (float) x[0];
                lastTouchY = (float) y[0];
            }
        });
    }
    
    public void requestPointerLock() {
        if (!pointerLock) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            double[] x = new double[1];
            double[] y = new double[1];
            glfwGetCursorPos(window, x, y);
            lastTouchX = (float) x[0];
            lastTouchY = (float) y[0];
            pointerLock = true;
        }
    }
    
    public void releasePointerLock() {
        if (pointerLock) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            pointerLock = false;
        }
    }
    
    public void updateTouch(float x, float y) {
        if (!pointerLock) {
            float dx = (x - lastTouchX) * touchSensitivity;
            float dy = (y - lastTouchY) * touchSensitivity;
            
            yaw += dx;
            pitch -= dy;
            pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
            
            lastTouchX = x;
            lastTouchY = y;
        }
    }
    
    public void beginTouch(float x, float y) {
        isTouching = true;
        lastTouchX = x;
        lastTouchY = y;
    }
    
    public void endTouch() {
        isTouching = false;
    }
    
    public boolean isKeyPressed(int key) {
        return keys[key];
    }
    
    public boolean isMouseButtonPressed(int button) {
        return mouseButtons[button];
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
    
    public boolean isLeftMouseButtonPressed() {
        return mouseButtons[GLFW_MOUSE_BUTTON_LEFT];
    }
    
    public boolean isRightMouseButtonPressed() {
        return mouseButtons[GLFW_MOUSE_BUTTON_RIGHT];
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public void getViewMatrix(Matrix4f result) {
        float sinPitch = (float) Math.sin(Math.toRadians(pitch));
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        float sinYaw = (float) Math.sin(Math.toRadians(yaw));
        float cosYaw = (float) Math.cos(Math.toRadians(yaw));
        
        float x = cosPitch * cosYaw;
        float y = sinPitch;
        float z = cosPitch * sinYaw;
        
        result.setLookAt(0, 0, 0, x, y, z, 0, 1, 0);
    }
    
    public void getForwardVector(Vector3f result) {
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        result.x = -cosPitch * (float) Math.sin(Math.toRadians(yaw));
        result.y = (float) Math.sin(Math.toRadians(pitch));
        result.z = -cosPitch * (float) Math.cos(Math.toRadians(yaw));
    }
    
    public void getRightVector(Vector3f result) {
        result.x = (float) Math.cos(Math.toRadians(yaw));
        result.y = 0;
        result.z = -(float) Math.sin(Math.toRadians(yaw));
    }
    
    public void cleanup() {
        releasePointerLock();
    }
}
