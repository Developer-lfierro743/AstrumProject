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
    public static final boolean DEVELOPER_MODE = true;
    
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
    // EXPERIMENTAL FEATURES (BETA/TESTING)
    // Enable these to test new features before release
    // ============================================================

    /**
     * EXPERIMENTAL_MODE: Master switch for all experimental features
     * Set to false to disable all experimental features
     */
    public static final boolean EXPERIMENTAL_MODE = false;

    /**
     * NEW_WORLDGEN: New terrain generation algorithm
     * Features:
     * - Improved cave systems
     * - Better mountain generation
     * - New biome blending
     * Status: ALPHA - May have bugs
     */
    public static final boolean ENABLE_NEW_WORLDGEN = EXPERIMENTAL_MODE;

    /**
     * NEW_LIGHTING_ENGINE: Advanced lighting system
     * Features:
     * - Dynamic shadows
     * - Ambient occlusion
     * - Colored lighting
     * Status: BETA - Mostly stable
     */
    public static final boolean ENABLE_NEW_LIGHTING = EXPERIMENTAL_MODE;

    /**
     * NEW_WATER_PHYSICS: Improved water simulation
     * Features:
     * - Flowing water
     * - Water pressure
     * - Underwater currents
     * Status: ALPHA - Experimental
     */
    public static final boolean ENABLE_NEW_WATER_PHYSICS = EXPERIMENTAL_MODE;

    /**
     * NEW_CRAFTING_SYSTEM: Enhanced crafting interface
     * Features:
     * - Recipe book
     * - Auto-crafting
     * - Crafting presets
     * Status: BETA - Ready for testing
     */
    public static final boolean ENABLE_NEW_CRAFTING = EXPERIMENTAL_MODE;

    /**
     * NEW_MOB_AI: Improved mob artificial intelligence
     * Features:
     * - Better pathfinding
     * - Mob memory
     * - Group behaviors
     * Status: ALPHA - Under development
     */
    public static final boolean ENABLE_NEW_MOB_AI = EXPERIMENTAL_MODE;

    /**
     * NEW_CHUNK_LOADING: Async chunk loading system
     * Features:
     * - Faster world loading
     * - Reduced stuttering
     * - Better memory management
     * Status: BETA - Performance testing
     */
    public static final boolean ENABLE_NEW_CHUNK_LOADING = EXPERIMENTAL_MODE;

    /**
     * NEW_PARTICLE_SYSTEM: Enhanced particle effects
     * Features:
     * - More particle types
     * - Better performance
     * - Custom particle shapes
     * Status: ALPHA - Visual testing
     */
    public static final boolean ENABLE_NEW_PARTICLES = EXPERIMENTAL_MODE;

    /**
     * NEW_SOUND_ENGINE: 3D positional audio
     * Features:
     * - Distance attenuation
     * - Environmental reverb
     * - Dynamic music
     * Status: BETA - Audio testing
     */
    public static final boolean ENABLE_NEW_SOUND = EXPERIMENTAL_MODE;

    /**
     * NEW_UI_SYSTEM: Modernized user interface
     * Features:
     * - Scalable UI
     * - Better animations
     * - Customizable HUD
     * Status: ALPHA - UI/UX testing
     */
    public static final boolean ENABLE_NEW_UI = EXPERIMENTAL_MODE;

    /**
     * NEW_SAVE_FORMAT: Improved world save format
     * Features:
     * - Faster saves
     * - Compression
     * - Incremental backups
     * Status: ALPHA - Data integrity testing
     */
    public static final boolean ENABLE_NEW_SAVE_FORMAT = EXPERIMENTAL_MODE;

    /**
     * NEW_NETWORK_PROTOCOL: Next-gen networking
     * Features:
     * - Lower latency
     * - Better compression
     * - Prediction algorithms
     * Status: ALPHA - Network testing
     */
    public static final boolean ENABLE_NEW_NETWORK = EXPERIMENTAL_MODE;

    /**
     * NEW_ANIMATION_SYSTEM: Smooth animation framework
     * Features:
     * - Interpolated animations
     * - Animation blending
     * - Procedural animations
     * Status: BETA - Animation testing
     */
    public static final boolean ENABLE_NEW_ANIMATIONS = EXPERIMENTAL_MODE;

    /**
     * NEW_WEATHER_SYSTEM: Dynamic weather
     * Features:
     * - Storm systems
     * - Seasonal changes
     * - Weather effects on gameplay
     * Status: ALPHA - Weather testing
     */
    public static final boolean ENABLE_NEW_WEATHER = EXPERIMENTAL_MODE;

    /**
     * NEW_ECONOMY_SYSTEM: Player trading and economy
     * Features:
     * - Player shops
     * - Currency system
     * - Trade verification
     * Status: ALPHA - Economy balancing
     */
    public static final boolean ENABLE_NEW_ECONOMY = EXPERIMENTAL_MODE;

    /**
     * NEW_QUEST_SYSTEM: Quest and achievement framework
     * Features:
     * - Dynamic quests
     * - Achievement tracking
     * - Reward distribution
     * Status: BETA - Content testing
     */
    public static final boolean ENABLE_NEW_QUESTS = EXPERIMENTAL_MODE;

    /**
     * NEW_PVP_COMBAT: Enhanced PvP combat system
     * Features:
     * - Combo system
     * - Weapon balancing
     * - Arena modes
     * Status: ALPHA - Combat testing
     */
    public static final boolean ENABLE_NEW_PVP = EXPERIMENTAL_MODE;

    /**
     * NEW_BUILDING_TOOLS: Advanced building assistance
     * Features:
     * - Blueprint system
     * - Copy/paste structures
     * - Building templates
     * Status: BETA - Builder testing
     */
    public static final boolean ENABLE_NEW_BUILDING_TOOLS = EXPERIMENTAL_MODE;

    /**
     * NEW_MOD_API: Next-generation modding support
     * Features:
     * - Hot-reload mods
     * - Mod dependencies
     * - Mod marketplace
     * Status: ALPHA - Developer testing
     */
    public static final boolean ENABLE_NEW_MOD_API = EXPERIMENTAL_MODE;

    /**
     * NEW_CROSSPLAY: Cross-platform multiplayer
     * Features:
     * - PC + Mobile compatibility
     * - Unified accounts
     * - Cross-platform friends
     * Status: ALPHA - Compatibility testing
     */
    public static final boolean ENABLE_CROSSPLAY = EXPERIMENTAL_MODE;

    /**
     * NEW_VOICE_CHAT: Integrated voice communication
     * Features:
     * - Proximity chat
     * - Team channels
     * - Noise suppression
     * Status: BETA - Audio quality testing
     */
    public static final boolean ENABLE_VOICE_CHAT = EXPERIMENTAL_MODE;

    /**
     * NEW_SCREENSHOTS: Enhanced screenshot system
     * Features:
     * - 4K support
     * - Photo mode
     * - Filters and effects
     * Status: BETA - Visual testing
     */
    public static final boolean ENABLE_NEW_SCREENSHOTS = EXPERIMENTAL_MODE;

    /**
     * NEW_REPLAY_SYSTEM: Game replay recording
     * Features:
     * - Record gameplay
     * - Replay editing
     * - Share replays
     * Status: ALPHA - Recording testing
     */
    public static final boolean ENABLE_REPLAY_SYSTEM = EXPERIMENTAL_MODE;

    /**
     * NEW_STAT_TRACKING: Advanced statistics tracking
     * Features:
     * - Play time tracking
     * - Achievement stats
     * - Performance metrics
     * Status: BETA - Data collection
     */
    public static final boolean ENABLE_STAT_TRACKING = EXPERIMENTAL_MODE;

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
        if (EXPERIMENTAL_MODE) {
            System.out.println("  *** EXPERIMENTAL FEATURES ENABLED ***");
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

    /**
     * Print experimental features status
     */
    public static void printExperimentalStatus() {
        if (!EXPERIMENTAL_MODE) {
            return;
        }
        
        System.out.println("=".repeat(50));
        System.out.println("  EXPERIMENTAL FEATURES STATUS");
        System.out.println("=".repeat(50));
        
        int enabledCount = 0;
        
        if (ENABLE_NEW_WORLDGEN) { System.out.println("  [✓] New World Generation (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_LIGHTING) { System.out.println("  [✓] New Lighting Engine (BETA)"); enabledCount++; }
        if (ENABLE_NEW_WATER_PHYSICS) { System.out.println("  [✓] New Water Physics (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_CRAFTING) { System.out.println("  [✓] New Crafting System (BETA)"); enabledCount++; }
        if (ENABLE_NEW_MOB_AI) { System.out.println("  [✓] New Mob AI (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_CHUNK_LOADING) { System.out.println("  [✓] New Chunk Loading (BETA)"); enabledCount++; }
        if (ENABLE_NEW_PARTICLES) { System.out.println("  [✓] New Particle System (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_SOUND) { System.out.println("  [✓] New Sound Engine (BETA)"); enabledCount++; }
        if (ENABLE_NEW_UI) { System.out.println("  [✓] New UI System (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_SAVE_FORMAT) { System.out.println("  [✓] New Save Format (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_NETWORK) { System.out.println("  [✓] New Network Protocol (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_ANIMATIONS) { System.out.println("  [✓] New Animation System (BETA)"); enabledCount++; }
        if (ENABLE_NEW_WEATHER) { System.out.println("  [✓] New Weather System (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_ECONOMY) { System.out.println("  [✓] New Economy System (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_QUESTS) { System.out.println("  [✓] New Quest System (BETA)"); enabledCount++; }
        if (ENABLE_NEW_PVP) { System.out.println("  [✓] New PvP Combat (ALPHA)"); enabledCount++; }
        if (ENABLE_NEW_BUILDING_TOOLS) { System.out.println("  [✓] New Building Tools (BETA)"); enabledCount++; }
        if (ENABLE_NEW_MOD_API) { System.out.println("  [✓] New Mod API (ALPHA)"); enabledCount++; }
        if (ENABLE_CROSSPLAY) { System.out.println("  [✓] Cross-Platform Play (ALPHA)"); enabledCount++; }
        if (ENABLE_VOICE_CHAT) { System.out.println("  [✓] Voice Chat (BETA)"); enabledCount++; }
        if (ENABLE_NEW_SCREENSHOTS) { System.out.println("  [✓] Enhanced Screenshots (BETA)"); enabledCount++; }
        if (ENABLE_REPLAY_SYSTEM) { System.out.println("  [✓] Replay System (ALPHA)"); enabledCount++; }
        if (ENABLE_STAT_TRACKING) { System.out.println("  [✓] Statistics Tracking (BETA)"); enabledCount++; }
        
        System.out.println("-".repeat(50));
        System.out.println("  Total Experimental Features: " + enabledCount + " enabled");
        System.out.println("=".repeat(50));
        System.out.println();
        
        if (enabledCount > 0) {
            System.out.println("⚠️  WARNING: Experimental features may be unstable!");
            System.out.println("⚠️  Report bugs at: https://github.com/Novusforge/Astrum/issues");
            System.out.println();
        }
    }

    /**
     * Check if a specific experimental feature is enabled
     * @param featureName Name of the feature (e.g., "ENABLE_NEW_WORLDGEN")
     * @return true if feature is enabled, false otherwise
     */
    public static boolean isExperimentalFeatureEnabled(String featureName) {
        if (!EXPERIMENTAL_MODE) {
            return false;
        }
        
        return switch (featureName.toUpperCase()) {
            case "ENABLE_NEW_WORLDGEN" -> ENABLE_NEW_WORLDGEN;
            case "ENABLE_NEW_LIGHTING" -> ENABLE_NEW_LIGHTING;
            case "ENABLE_NEW_WATER_PHYSICS" -> ENABLE_NEW_WATER_PHYSICS;
            case "ENABLE_NEW_CRAFTING" -> ENABLE_NEW_CRAFTING;
            case "ENABLE_NEW_MOB_AI" -> ENABLE_NEW_MOB_AI;
            case "ENABLE_NEW_CHUNK_LOADING" -> ENABLE_NEW_CHUNK_LOADING;
            case "ENABLE_NEW_PARTICLES" -> ENABLE_NEW_PARTICLES;
            case "ENABLE_NEW_SOUND" -> ENABLE_NEW_SOUND;
            case "ENABLE_NEW_UI" -> ENABLE_NEW_UI;
            case "ENABLE_NEW_SAVE_FORMAT" -> ENABLE_NEW_SAVE_FORMAT;
            case "ENABLE_NEW_NETWORK" -> ENABLE_NEW_NETWORK;
            case "ENABLE_NEW_ANIMATIONS" -> ENABLE_NEW_ANIMATIONS;
            case "ENABLE_NEW_WEATHER" -> ENABLE_NEW_WEATHER;
            case "ENABLE_NEW_ECONOMY" -> ENABLE_NEW_ECONOMY;
            case "ENABLE_NEW_QUESTS" -> ENABLE_NEW_QUESTS;
            case "ENABLE_NEW_PVP" -> ENABLE_NEW_PVP;
            case "ENABLE_NEW_BUILDING_TOOLS" -> ENABLE_NEW_BUILDING_TOOLS;
            case "ENABLE_NEW_MOD_API" -> ENABLE_NEW_MOD_API;
            case "ENABLE_CROSSPLAY" -> ENABLE_CROSSPLAY;
            case "ENABLE_VOICE_CHAT" -> ENABLE_VOICE_CHAT;
            case "ENABLE_NEW_SCREENSHOTS" -> ENABLE_NEW_SCREENSHOTS;
            case "ENABLE_REPLAY_SYSTEM" -> ENABLE_REPLAY_SYSTEM;
            case "ENABLE_STAT_TRACKING" -> ENABLE_STAT_TRACKING;
            default -> false;
        };
    }

    /**
     * Get count of enabled experimental features
     */
    public static int getEnabledExperimentalFeatureCount() {
        if (!EXPERIMENTAL_MODE) {
            return 0;
        }
        
        int count = 0;
        if (ENABLE_NEW_WORLDGEN) count++;
        if (ENABLE_NEW_LIGHTING) count++;
        if (ENABLE_NEW_WATER_PHYSICS) count++;
        if (ENABLE_NEW_CRAFTING) count++;
        if (ENABLE_NEW_MOB_AI) count++;
        if (ENABLE_NEW_CHUNK_LOADING) count++;
        if (ENABLE_NEW_PARTICLES) count++;
        if (ENABLE_NEW_SOUND) count++;
        if (ENABLE_NEW_UI) count++;
        if (ENABLE_NEW_SAVE_FORMAT) count++;
        if (ENABLE_NEW_NETWORK) count++;
        if (ENABLE_NEW_ANIMATIONS) count++;
        if (ENABLE_NEW_WEATHER) count++;
        if (ENABLE_NEW_ECONOMY) count++;
        if (ENABLE_NEW_QUESTS) count++;
        if (ENABLE_NEW_PVP) count++;
        if (ENABLE_NEW_BUILDING_TOOLS) count++;
        if (ENABLE_NEW_MOD_API) count++;
        if (ENABLE_CROSSPLAY) count++;
        if (ENABLE_VOICE_CHAT) count++;
        if (ENABLE_NEW_SCREENSHOTS) count++;
        if (ENABLE_REPLAY_SYSTEM) count++;
        if (ENABLE_STAT_TRACKING) count++;
        return count;
    }

    // Private constructor - utility class
    private AstrumConstants() {}
}
