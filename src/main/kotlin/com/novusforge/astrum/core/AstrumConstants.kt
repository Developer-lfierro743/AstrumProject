package com.novusforge.astrum.core

/**
 * AstrumConstants - Central configuration constants for Project Astrum.
 * Kotlin conversion: Using an object singleton for global configuration.
 * 
 * "A reclaim of the sandbox vision. Independent, Resilient, and Secure."
 */
object AstrumConstants {
    
    // ============================================================
    // DEVELOPER MODE FLAGS
    // Set these to false for production release
    // ============================================================
    
    /**
     * DEVELOPER_MODE: Enable development shortcuts
     */
    const val DEVELOPER_MODE = true
    
    /**
     * SKIP_ACCOUNT_SYSTEM: Bypass login/register GUI
     */
    const val SKIP_ACCOUNT_SYSTEM = DEVELOPER_MODE
    
    /**
     * SKIP_IIV_QUESTIONNAIRE: Bypass identity verification
     */
    const val SKIP_IIV_QUESTIONNAIRE = DEVELOPER_MODE
    
    /**
     * DEBUG_LOGGING: Enable verbose console output
     */
    const val DEBUG_LOGGING = DEVELOPER_MODE
    
    /**
     * VULKAN_VALIDATION: Enable Vulkan validation layers
     */
    const val VULKAN_VALIDATION = DEVELOPER_MODE
    
    // ============================================================
    // DEVELOPER DEFAULTS (when SKIP_* is enabled)
    // ============================================================
    
    const val DEV_USERNAME = "DevPlayer"
    const val DEV_AVATAR_ID = 0
    const val DEV_IIV_DECISION = "ALLOW"
    
    // ============================================================
    // PRODUCTION SETTINGS (do not change)
    // ============================================================
    
    const val GAME_TITLE = "Astrum - Pre-Classic (Cave Game)"
    const val GAME_VERSION = "0.0.1"
    const val STUDIO_NAME = "Novusforge Studios"
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_USERNAME_LENGTH = 20
    const val IIV_PASSING_SCORE = 5
    const val IIV_WARN_SCORE = 12
    
    val SESSION_FILE = "${System.getProperty("user.home")}/astrum_session.dat"
    val IIV_FILE = "${System.getProperty("user.home")}/iiv_result.dat"
    
    // ============================================================
    // SAFETY GUARDIAN CONSTANTS
    // ============================================================
    
    const val SAFETY_GUARDIAN_ENABLED = true
    const val SAFETY_RULE_COUNT = 11
    const val AUTO_BLOCK_THRESHOLD = 5
    
    // ============================================================
    // RENDERER CONSTANTS
    // ============================================================
    
    const val DEFAULT_WIDTH = 1280
    const val DEFAULT_HEIGHT = 720
    const val TARGET_FPS = 60
    const val MAX_FRAMES_IN_FLIGHT = 2
    const val RENDER_DISTANCE = 3
    
    // ============================================================
    // UTILITY METHODS
    // ============================================================
    
    /**
     * Check if running in developer mode
     */
    @JvmStatic
    fun isDeveloperMode(): Boolean = DEVELOPER_MODE
    
    /**
     * Check if debug logging is enabled
     */
    @JvmStatic
    fun isDebugLogging(): Boolean = DEBUG_LOGGING
    
    /**
     * Print startup banner
     */
    @JvmStatic
    fun printBanner() {
        println("=".repeat(50))
        println("  $GAME_TITLE")
        println("  $STUDIO_NAME")
        println("  Version: $GAME_VERSION")
        if (DEVELOPER_MODE) {
            println("  *** DEVELOPER MODE ACTIVE ***")
        }
        println("=".repeat(50))
        println()
    }
    
    /**
     * Print developer mode warning
     */
    @JvmStatic
    fun printDevWarning() {
        if (DEVELOPER_MODE) {
            println("=".repeat(50))
            println("  DEVELOPER MODE WARNING")
            println("  - Account System: SKIPPED")
            System.out.println("  - IIV Questionnaire: SKIPPED")
            System.out.println("  - Debug Logging: ENABLED")
            System.out.println("  DO NOT USE IN PRODUCTION!")
            System.out.println("=".repeat(50))
            System.out.println()
        }
    }
}
