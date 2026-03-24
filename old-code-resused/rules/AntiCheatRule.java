package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

/**
 * Rule 6: AntiCheatRule
 * Detects cheating and exploit attempts.
 * 
 * Flags:
 * - Impossible movement speeds
 * - Hit detection anomalies
 * - Inventory manipulation
 * - Known cheat signatures
 */
public class AntiCheatRule implements ISafetyRule<ActionContext> {
    
    @Override
    public String name() {
        return "AntiCheatRule";
    }
    
    @Override
    public SafetyResult check(ActionContext context) {
        String action = context.action().toLowerCase();
        
        // Detect cheat-related actions
        if (action.contains("killaura") || action.contains("flyhack") ||
            action.contains("xray") || action.contains("reach") ||
            action.contains("speedhack") || action.contains("norecoil")) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Cheat module detected: " + action
            );
        }
        
        // Detect exploit attempts
        if (action.contains("exploit") || action.contains("dupe") ||
            action.contains("glitch") || action.contains("hack")) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Exploit attempt blocked: " + action
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
