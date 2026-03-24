package com.novusforge.astrum.game

import com.novusforge.astrum.engine.*
import com.novusforge.astrum.world.*
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import kotlin.math.floor

/**
 * Game logic - Pre-Classic stage
 * Following Notch's approach: simple, functional, optimized
 * Kotlin conversion: Using primary constructor, property accessors, and idiomatic math.
 */
class Game(private val window: Long, private val renderer: IRenderer) {
    
    lateinit var player: Player
    lateinit var input: InputManager
    lateinit var world: World
    lateinit var chunkManager: ChunkManager
    
    var isRunning: Boolean = true
        private set
    
    private var lastLeftClick = 0
    private var lastRightClick = 0

    fun init() {
        input = InputManager(window)
        player = Player(0f, 100f, 0f) // Spawn high
        
        world = World()
        chunkManager = world.chunkManager
        
        println("[Game] World seed: ${world.seed}")
        println("[Game] Spawn: (0, 100, 0)")
    }
    
    fun update(deltaTime: Float) {
        // Escape releases mouse
        if (input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            input.releasePointerLock()
        }
        
        // Click locks mouse
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {
            input.requestPointerLock()
        }
        
        // Update player
        val pos = player.position
        world.updatePlayerPosition(pos.x, pos.y, pos.z)
        player.update(deltaTime, input, object : Player.WorldInterface {
            override fun getBlock(x: Int, y: Int, z: Int): Short = world.getBlock(x, y, z)
        })
        
        // Update chunks
        chunkManager.tick()
        
        // Block breaking
        val leftClick = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1)
        if (leftClick == GLFW_PRESS && lastLeftClick == GLFW_RELEASE) {
            breakBlock()
        }
        lastLeftClick = leftClick
        
        // Block placing
        val rightClick = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_2)
        if (rightClick == GLFW_PRESS && lastRightClick == GLFW_RELEASE) {
            placeBlock()
        }
        lastRightClick = rightClick
    }
    
    private fun breakBlock() {
        val forward = Vector3f()
        input.getForwardVector(forward)
        val pos = player.position
        
        for (i in 1..6) {
            val bx = floor((pos.x + forward.x * i).toDouble()).toInt()
            val by = floor((pos.y + forward.y * i).toDouble()).toInt()
            val bz = floor((pos.z + forward.z * i).toDouble()).toInt()
            
            val block = world.getBlock(bx, by, bz)
            if (block != 0.toShort()) {
                world.setBlock(bx, by, bz, 0.toShort())
                println("[Game] Broke block at ($bx, $by, $bz)")
                break
            }
        }
    }
    
    private fun placeBlock() {
        val forward = Vector3f()
        input.getForwardVector(forward)
        val pos = player.position
        
        for (i in 5 downTo 1) {
            val bx = floor((pos.x + forward.x * i).toDouble()).toInt()
            val by = floor((pos.y + forward.y * i).toDouble()).toInt()
            val bz = floor((pos.z + forward.z * i).toDouble()).toInt()
            
            val block = world.getBlock(bx, by, bz)
            if (block != 0.toShort()) {
                val px = floor((pos.x + forward.x * (i + 1)).toDouble()).toInt()
                val py = floor((pos.y + forward.y * (i + 1)).toDouble()).toInt()
                val pz = floor((pos.z + forward.z * (i + 1)).toDouble()).toInt()
                
                world.setBlock(px, py, pz, 1.toShort()) // Dirt block
                println("[Game] Placed block at ($px, $py, $pz)")
                break
            }
        }
    }
    
    fun cleanup() {
        if (::input.isInitialized) input.cleanup()
        if (::player.isInitialized) player.cleanup()
        if (::world.isInitialized) world.dispose()
    }
}
