package com.novusforge.astrum.core;

/**
 * AstrumConstants - Central configuration constants for Project Astrum.
 * 
 * "A reclaim of the sandbox vision. Independent, Resilient, and Secure."
 */
public final class AstrumConstants {
    
    // ============================================================
    // DEVELOPER MODE FLAGS
    // Set these to false for production release
    // ============================================================
    
    /**
     * DEVELOPER_MODE: Enable development shortcuts
     * - Skip Account System GUI (auto-login as DevPlayer)
     * - Skip IIV Questionnaire (auto-pass verification)
     * - Enable debug logging
     */
    public static final boolean DEVELOPER_MODE = false;
    
    /**
     * SKIP_ACCOUNT_SYSTEM: Bypass login/register GUI
     * Only active when DEVELOPER_MODE = true
     */
    public static final boolean SKIP_ACCOUNT_SYSTEM = DEVELOPER_MODE;
    
    /**
     * SKIP_IIV_QUESTIONNAIRE: Bypass identity verification
     * Only active when DEVELOPER_MODE = true
     */
    public static final boolean SKIP_IIV_QUESTIONNAIRE = DEVELOPER_MODE;
    
    /**
     * DEBUG_LOGGING: Enable verbose console output
     */
    public static final boolean DEBUG_LOGGING = DEVELOPER_MODE;
    
    /**
     * VULKAN_VALIDATION: Enable Vulkan validation layers
     * WARNING: Performance impact - dev only!
     */
    public static final boolean VULKAN_VALIDATION = DEVELOPER_MODE;
    
    // ============================================================
    // DEVELOPER DEFAULTS (when SKIP_* is enabled)
    // ============================================================
    
    /**
     * DEV_USERNAME: Default username for developer mode
     */
    public static final String DEV_USERNAME = "DevPlayer";
    
    /**
     * DEV_AVATAR_ID: Default avatar for developer mode
     */
    public static final int DEV_AVATAR_ID = 0;
    
    /**
     * DEV_IIV_DECISION: Default IIV result for developer mode
     * Options: "ALLOW", "WARN", "BLOCK"
     */
    public static final String DEV_IIV_DECISION = "ALLOW";
    
    // ============================================================
    // PRODUCTION SETTINGS (do not change)
    // ============================================================
    
    /**
     * GAME_TITLE: Official game title
     */
    public static final String GAME_TITLE = "Astrum - Pre-Classic (Cave Game)";
    
    /**
     * GAME_VERSION: Current version string
     */
    public static final String GAME_VERSION = "0.0.1";
    
    /**
     * STUDIO_NAME: Development studio
     */
    public static final String STUDIO_NAME = "Novusforge Studios";
    
    /**
     * MIN_USERNAME_LENGTH: Minimum username characters
     */
    public static final int MIN_USERNAME_LENGTH = 3;
    
    /**
     * MAX_USERNAME_LENGTH: Maximum username characters
     */
    public static final int MAX_USERNAME_LENGTH = 20;
    
    /**
     * IIV_PASSING_SCORE: Maximum score to pass IIV
     */
    public static final int IIV_PASSING_SCORE = 5;
    
    /**
     * IIV_WARN_SCORE: Maximum score for warning (still allowed)
     */
    public static final int IIV_WARN_SCORE = 12;
    
    /**
     * SESSION_FILE: Session data file location
     */
    public static final String SESSION_FILE = System.getProperty("user.home") + "/astrum_session.dat";
    
    /**
     * IIV_FILE: IIV result file location
     */
    public static final String IIV_FILE = System.getProperty("user.home") + "/iiv_result.dat";
    
    // ============================================================
    // SAFETY GUARDIAN CONSTANTS
    // ============================================================
    
    /**
     * SAFETY_GUARDIAN_ENABLED: Always true - never disable!
     */
    public static final boolean SAFETY_GUARDIAN_ENABLED = true;
    
    /**
     * SAFETY_RULE_COUNT: Number of active safety rules
     */
    public static final int SAFETY_RULE_COUNT = 11;
    
    /**
     * AUTO_BLOCK_THRESHOLD: Violations before auto-ban
     */
    public static final int AUTO_BLOCK_THRESHOLD = 5;
    
    // ============================================================
    // RENDERER CONSTANTS
    // ============================================================
    
    /**
     * DEFAULT_WIDTH: Default window width
     */
    public static final int DEFAULT_WIDTH = 1280;
    
    /**
     * DEFAULT_HEIGHT: Default window height
     */
    public static final int DEFAULT_HEIGHT = 720;
    
    /**
     * TARGET_FPS: Target frames per second
     */
    public static final int TARGET_FPS = 60;
    
    /**
     * MAX_FRAMES_IN_FLIGHT: Vulkan frames in flight
     */
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    
    /**
     * RENDER_DISTANCE: Chunk render distance
     */
    public static final int RENDER_DISTANCE = 3;
    
    // ============================================================
    // UTILITY METHODS
    // ============================================================
    
    /**
     * Check if running in developer mode
     */
    public static boolean isDeveloperMode() {
        return DEVELOPER_MODE;
    }
    
    /**
     * Check if debug logging is enabled
     */
    public static boolean isDebugLogging() {
        return DEBUG_LOGGING;
    }
    
    /**
     * Print startup banner
     */
    public static void printBanner() {
        System.out.println("=".repeat(50));
        System.out.println("  " + GAME_TITLE);
        System.out.println("  " + STUDIO_NAME);
        System.out.println("  Version: " + GAME_VERSION);
        if (DEVELOPER_MODE) {
            System.out.println("  *** DEVELOPER MODE ACTIVE ***");
        }
        System.out.println("=".repeat(50));
        System.out.println();
    }
    
    /**
     * Print developer mode warning
     */
    public static void printDevWarning() {
        if (DEVELOPER_MODE) {
            System.out.println("=".repeat(50));
            System.out.println("  DEVELOPER MODE WARNING");
            System.out.println("  - Account System: SKIPPED");
            System.out.println("  - IIV Questionnaire: SKIPPED");
            System.out.println("  - Debug Logging: ENABLED");
            System.out.println("  DO NOT USE IN PRODUCTION!");
            System.out.println("=".repeat(50));
            System.out.println();
        }
    }
    
    // Private constructor - utility class
    private AstrumConstants() {}
}
