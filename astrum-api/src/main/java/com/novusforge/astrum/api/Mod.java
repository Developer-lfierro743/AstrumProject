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
     * Gets the metadata of the mod.
     */
    String getModId();
}
