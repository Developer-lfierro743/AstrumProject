package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 5: NativeModRule - Mod loader verification
 */
class NativeModRule : ISafetyRule<ModContext> {
    
    private val blockedLoaders = listOf("forge", "fabric", "quilt", "neoforge", "rift", "liteloader")
    
    override fun name() = "NativeModRule"
    
    override fun check(context: ModContext): SafetyResult {
        val loader = context.loader.lowercase()
        
        // Block Minecraft mod loaders
        for (blocked in blockedLoaders) {
            if (loader.contains(blocked)) {
                return SafetyResult.BLOCK.copy(reason = "Unauthorized mod loader: ${context.loader}")
            }
        }
        
        // Only allow native Astrum mods
        if (loader != "astrum-native" && loader != "astrum") {
            return SafetyResult.WARN.copy(reason = "Unknown mod loader: ${context.loader}")
        }
        
        return SafetyResult.ALLOW
    }
}
