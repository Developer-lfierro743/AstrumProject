package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

/**
 * Rule 5: NativeModRule
 * Verifies mod loaders and blocks unauthorized mod platforms.
 * 
 * Blocks:
 * - Forge, Fabric, Quilt (Minecraft mod loaders)
 * - Unauthorized native code injection
 */
public class NativeModRule implements ISafetyRule<ModContext> {
    
    private static final String[] BLOCKED_LOADERS = {
        "forge", "fabric", "quilt", "neoforge", "rift", "liteloader"
    };
    
    @Override
    public String name() {
        return "NativeModRule";
    }
    
    @Override
    public SafetyResult check(ModContext context) {
        String loader = context.loader().toLowerCase();
        
        // Block Minecraft mod loaders
        for (String blocked : BLOCKED_LOADERS) {
            if (loader.contains(blocked)) {
                return new SafetyResult(
                    SafetyResult.Decision.BLOCK,
                    "Unauthorized mod loader blocked: " + context.loader()
                );
            }
        }
        
        // Only allow native Astrum mods
        if (!loader.equals("astrum-native") && !loader.equals("astrum")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Unknown mod loader: " + context.loader()
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
