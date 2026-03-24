package com.novusforge.astrum.game

import com.novusforge.astrum.core.*
import com.novusforge.astrum.engine.*
import com.novusforge.astrum.world.World
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*
import javax.swing.SwingUtilities

/**
 * Project Astrum - Pre-Classic (Cave Game)
 * Main Entry Point in KOTLIN! 🚀
 * 
 * Flow:
 * 1. Account System (Login/Register) OR Developer Mode
 * 2. IIV Questionnaire OR Developer bypass
 * 3. SafetyGuardian (11 rules)
 * 4. Launch Game
 */
const val TITLE = "Astrum - Pre-Classic (Cave Game)"
const val TARGET_FPS = 60
val FRAME_TIME = 1_000_000_000L / TARGET_FPS
val IIV_FILE = "${System.getProperty("user.home")}/iiv_result.dat"

fun main() {
    // Print banner
    AstrumConstants.printBanner()
    
    // Developer warning
    if (AstrumConstants.DEVELOPER_MODE) {
        AstrumConstants.printDevWarning()
    }

    // Step 1: Account System
    checkAccountSession()

    // Step 2: IIV Verification
    checkIdentityVerification()

    // Step 3: SafetyGuardian
    val guardian = SafetyGuardian()
    val startupContext = SafetyGuardian.ActionContext("startup", "system", "engine")
    if (guardian.validate(startupContext).decision == SafetyGuardian.SafetyResult.Decision.BLOCK) {
        System.err.println("CRITICAL: SafetyGuardian blocked startup!")
        return
    }

    println("  Player         : ${SessionManager.getUsername()}")
    println("  SafetyGuardian : ONLINE (11 rules)")
    println("  IIV Framework  : READY")
    println("=".repeat(50))
    println()

    // Step 4: Launch Game - Use VulkanRenderer
    val renderer = VulkanRenderer()
    var game: Game? = null

    try {
        if (!renderer.init()) {
            System.err.println("[ERROR] Renderer failed!")
            System.exit(1)
        }

        game = Game(renderer.window, renderer)
        game.init()

        World.setBufferDeleter(renderer::deleteBuffer)

        println()
        println("[Render] ${renderer.rendererName} initialized!")
        println("[Game] Astrum loaded!")
        println("[Controls] WASD=Move Space=Jump Mouse=Look")
        println("[Controls] L-Click=Break R-Click=Place")
        println()

        // Game Loop
        var lastTime = System.nanoTime()
        var frameCount = 0
        var fpsTimer = System.currentTimeMillis()

        val projectionMatrix = Matrix4f()
        val viewMatrix = Matrix4f()

        while (!renderer.windowShouldClose() && game.isRunning()) {
            glfwPollEvents()

            val currentTime = System.nanoTime()
            var deltaTime = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime
            deltaTime = minOf(deltaTime, 0.1f)

            game.update(deltaTime)

            projectionMatrix.setPerspective(Math.toRadians(70.0).toFloat(), renderer.aspectRatio, 0.1f, 1000.0f, true)
            game.getInput().getViewMatrix(viewMatrix)
            val pos = game.getPlayer().position
            viewMatrix.translate(-pos.x, -pos.y, -pos.z)

            val visibleMeshes = game.getWorld().getVisibleMeshes(pos.x, pos.y, pos.z, null)
            renderer.render(viewMatrix, projectionMatrix, visibleMeshes)

            // FPS counter
            frameCount++
            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                val chunks = game.getWorld().loadedChunkCount
                println("[FPS] $frameCount | [Chunks] $chunks | [Pos] ${pos.x}, ${pos.y}, ${pos.z}")
                frameCount = 0
                fpsTimer = System.currentTimeMillis()
            }
            
            // Frame timing
            val elapsed = System.nanoTime() - currentTime
            if (elapsed < FRAME_TIME) {
                Thread.sleep((FRAME_TIME - elapsed) / 1_000_000)
            }
        }
        
        println("[Shutdown] Closing Astrum...")
        
    } finally {
        game?.cleanup()
        renderer.cleanup()
        println("[Shutdown] Done!")
    }
}

/**
 * Step 1: Account System
 */
fun checkAccountSession() {
    if (SessionManager.isLoggedIn()) {
        println("Account: Logged in as ${SessionManager.getUsername()}")
        return
    }
    
    if (AstrumConstants.SKIP_ACCOUNT_SYSTEM) {
        println("Account: DEVELOPER MODE - Skipping GUI")
        SessionManager.saveSession(AstrumConstants.DEV_USERNAME, AstrumConstants.DEV_AVATAR_ID)
        println("Account: Auto-logged in as ${AstrumConstants.DEV_USERNAME}")
        return
    }
    
    try {
        println("Account: Launching Account System...")
        SwingUtilities.invokeAndWait { AccountSystem() }
        
        if (!SessionManager.isLoggedIn()) {
            System.err.println("Account: Login incomplete. Exiting.")
            System.exit(1)
        }
        println("Account: Welcome ${SessionManager.getUsername()}!")
    } catch (e: Exception) {
        System.err.println("Account: Error - ${e.message}")
        System.exit(1)
    }
}

/**
 * Step 2: IIV Questionnaire
 */
fun checkIdentityVerification() {
    val file = java.io.File(IIV_FILE)
    
    if (file.exists()) {
        try {
            java.io.ObjectInputStream(java.io.FileInputStream(file)).use { ois ->
                val result = ois.readObject().toString()
                if (result.contains("BLOCK")) {
                    System.err.println("CRITICAL: IIV denied entry!")
                    System.exit(0)
                }
                println("IIV: Previous verification found. Proceeding.")
                return
            }
        } catch (e: Exception) {
            println("IIV: Previous verification corrupted. Re-running...")
            file.delete()
        }
    }
    
    if (AstrumConstants.SKIP_IIV_QUESTIONNAIRE) {
        println("IIV: DEVELOPER MODE - Skipping Questionnaire")
        println("IIV: Auto-passed: ${AstrumConstants.DEV_IIV_DECISION}")
        file.writeText(AstrumConstants.DEV_IIV_DECISION)
        return
    }
    
    try {
        println("IIV: Launching Identity Intent Verification...")
        println("IIV: Complete all 12 questions to proceed.")
        SwingUtilities.invokeAndWait { IIVQuestionnaire() }
        Thread.sleep(500)
        
        if (!file.exists()) {
            System.err.println("IIV: ERROR - Verification not completed!")
            System.exit(1)
        }
        
        java.io.ObjectInputStream(java.io.FileInputStream(file)).use { ois ->
            val result = ois.readObject().toString()
            when {
                result.contains("BLOCK") -> {
                    System.err.println("CRITICAL: IIV denied entry!")
                    System.exit(0)
                }
                result.contains("WARN") -> println("IIV: Verification with warnings. You'll be monitored.")
                else -> println("IIV: Verification PASSED. Welcome!")
            }
        }
    } catch (e: Exception) {
        System.err.println("IIV: Error - ${e.message}")
        System.exit(1)
    }
}
