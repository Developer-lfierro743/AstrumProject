package com.novusforge.astrum.api;

/**
 * Base interface for all Astrum mods.
 * Unified modding API designed for long-term stability according to "The Formula".
 */
public interface Mod {
    /**
     * Called during the initialization phase of the mod.
     */
    void onInitialize();
    
    /**
     * Gets the unique mod identifier.
     */
    String getModId();
    
    /**
     * Gets the display name of the mod.
     */
    default String getName() {
        return getModId();
    }
    
    /**
     * Gets the mod description.
     */
    default String getDescription() {
        return "";
    }
    
    /**
     * Gets the mod version.
     */
    default String getVersion() {
        return "1.0.0";
    }
    
    /**
     * Gets the author of the mod.
     */
    default String getAuthor() {
        return "Unknown";
    }
}
