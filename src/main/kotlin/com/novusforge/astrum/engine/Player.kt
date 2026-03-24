package com.novusforge.astrum.engine

import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.floor

/**
 * Player - First-person camera and movement
 * Pre-Classic implementation (simple physics)
 * Kotlin conversion: Using primary constructor, property accessors, and idiomatic math.
 */
class Player(x: Float, y: Float, z: Float) {
    var position: Vector3f = Vector3f(x, y, z)
    var speed: Float = 5.0f
    var jumpVelocity: Float = 8.0f
    
    var isOnGround: Boolean = false
        private set
    private var fallVelocity: Float = 0.0f
    private var gravity: Float = -25.0f

    fun update(deltaTime: Float, input: InputManager, world: WorldInterface?) {
        // Get movement direction
        val forward = Vector3f()
        val right = Vector3f()
        input.getForwardVector(forward)
        input.getRightVector(right)
        
        forward.y = 0f
        right.y = 0f
        forward.normalize()
        right.normalize()

        // Calculate movement
        val moveDir = Vector3f(0f, 0f, 0f)

        if (input.isForwardPressed()) moveDir.add(forward)
        if (input.isBackwardPressed()) moveDir.sub(forward)
        if (input.isLeftPressed()) moveDir.sub(right)
        if (input.isRightPressed()) moveDir.add(right)

        if (moveDir.length() > 0) {
            moveDir.normalize().mul(speed * deltaTime)
            position.add(moveDir.x, 0f, moveDir.z)
        }

        // Jump
        if (input.isJumpPressed() && isOnGround) {
            fallVelocity = jumpVelocity
            isOnGround = false
        }

        // Gravity
        fallVelocity += gravity * deltaTime
        position.y += fallVelocity * deltaTime

        // Ground collision (simple - check block below)
        if (world != null) {
            val blockX = floor(position.x.toDouble()).toInt()
            val blockY = floor((position.y - 1.0f).toDouble()).toInt()
            val blockZ = floor(position.z.toDouble()).toInt()
            
            val blockBelow = world.getBlock(blockX, blockY, blockZ)
            val groundLevel = blockY + 1.0f
            
            if (position.y <= groundLevel + 0.5f && position.y > groundLevel - 1.0f && blockBelow != 0.toShort()) {
                position.y = groundLevel + 0.5f
                fallVelocity = 0f
                isOnGround = true
            } else if (position.y < 1.5f) {
                // Bedrock floor at y=0
                position.y = 1.5f
                fallVelocity = 0f
                isOnGround = true
            } else {
                isOnGround = false
            }
        } else {
            // Fallback floor
            if (position.y < 1.5f) {
                position.y = 1.5f
                fallVelocity = 0f
                isOnGround = true
            }
        }
    }

    fun setPosition(x: Float, y: Float, z: Float) {
        position.set(x, y, z)
    }
    
    val chunkCoord: Vector3i get() {
        val cx = floor((position.x / 32.0f).toDouble()).toInt()
        val cz = floor((position.z / 32.0f).toDouble()).toInt()
        return Vector3i(cx, 0, cz)
    }

    fun cleanup() {}

    /**
     * World interface for block collision detection
     */
    interface WorldInterface {
        fun getBlock(x: Int, y: Int, z: Int): Short
    }
}
