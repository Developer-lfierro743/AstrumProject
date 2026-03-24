package com.novusforge.astrum.engine

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Input Manager - Handles keyboard, mouse, and touch input
 * Pre-Classic implementation
 * Kotlin conversion: Using primary constructor, property accessors, and idiomatic math.
 */
class InputManager(private val window: Long) {
    
    private val keys = BooleanArray(512)
    private val mouseButtons = BooleanArray(8)
    
    var mouseSensitivity = 0.1f
    var yaw = 0.0f
    var pitch = 0.0f
    
    private var pointerLocked = false

    init {
        setupCallbacks()
    }

    private fun setupCallbacks() {
        // Keyboard callback
        glfwSetKeyCallback(window) { _, key, _, action, _ ->
            if (key in keys.indices) {
                keys[key] = (action == GLFW_PRESS || action == GLFW_REPEAT)
            }
        }

        // Mouse button callback
        glfwSetMouseButtonCallback(window) { _, button, action, _ ->
            if (button in mouseButtons.indices) {
                mouseButtons[button] = (action == GLFW_PRESS)
            }
        }

        // Cursor position callback (for mouse look)
        glfwSetCursorPosCallback(window) { _, _, _ ->
            if (pointerLocked) {
                // Mouse look logic handled in update
            }
        }

        // Cursor enter callback
        glfwSetCursorEnterCallback(window) { w, entered ->
            if (entered && pointerLocked) {
                val x = DoubleArray(1)
                val y = DoubleArray(1)
                glfwGetCursorPos(w, x, y)
            }
        }
    }

    fun requestPointerLock() {
        if (!pointerLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
            pointerLocked = true
        }
    }

    fun releasePointerLock() {
        if (pointerLocked) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
            pointerLocked = false
        }
    }

    fun isKeyPressed(key: Int): Boolean = if (key in keys.indices) keys[key] else false

    fun isForwardPressed(): Boolean = keys[GLFW_KEY_W] || keys[GLFW_KEY_UP]

    fun isBackwardPressed(): Boolean = keys[GLFW_KEY_S] || keys[GLFW_KEY_DOWN]

    fun isLeftPressed(): Boolean = keys[GLFW_KEY_A] || keys[GLFW_KEY_LEFT]

    fun isRightPressed(): Boolean = keys[GLFW_KEY_D] || keys[GLFW_KEY_RIGHT]

    fun isJumpPressed(): Boolean = keys[GLFW_KEY_SPACE]

    /**
     * Get view matrix for camera
     */
    fun getViewMatrix(result: Matrix4f) {
        val cosPitch = cos(Math.toRadians(pitch.toDouble())).toFloat()
        val sinPitch = sin(Math.toRadians(pitch.toDouble())).toFloat()
        val cosYaw = cos(Math.toRadians(yaw.toDouble())).toFloat()
        val sinYaw = sin(Math.toRadians(yaw.toDouble())).toFloat()

        val x = cosPitch * cosYaw
        val y = sinPitch
        val z = cosPitch * sinYaw

        result.setLookAt(0f, 0f, 0f, x, y, z, 0f, 1f, 0f)
    }

    /**
     * Get forward direction vector
     */
    fun getForwardVector(result: Vector3f) {
        val cosPitch = cos(Math.toRadians(pitch.toDouble())).toFloat()
        result.x = -cosPitch * sin(Math.toRadians(yaw.toDouble())).toFloat()
        result.y = sin(Math.toRadians(pitch.toDouble())).toFloat()
        result.z = -cosPitch * cos(Math.toRadians(yaw.toDouble())).toFloat()
    }

    /**
     * Get right direction vector
     */
    fun getRightVector(result: Vector3f) {
        result.x = cos(Math.toRadians(yaw.toDouble())).toFloat()
        result.y = 0f
        result.z = -sin(Math.toRadians(yaw.toDouble())).toFloat()
    }

    fun cleanup() {
        releasePointerLock()
    }
}
