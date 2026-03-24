package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

/**
 * Rule 8: GriefingPatternRule
 * Detects griefing and destructive behavior patterns.
 * 
 * Flags:
 * - Mass block destruction
 * - Lava/water placement near builds
 * - TNT/explosive abuse
 * - The Fifth Column-style attacks
 */
public class GriefingPatternRule implements ISafetyRule<ActionContext> {
    
    @Override
    public String name() {
        return "GriefingPatternRule";
    }
    
    @Override
    public SafetyResult check(ActionContext context) {
        String action = context.action().toLowerCase();
        String target = context.target().toLowerCase();
        
        // Detect mass destruction patterns
        if (action.contains("mass") && (action.contains("break") || action.contains("destroy"))) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Mass destruction attempt blocked"
            );
        }
        
        // Detect griefing tool references
        if (action.contains("copenheimer") || action.contains("worldedit") ||
            action.contains("nuker") || action.contains("destroyer")) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Griefing tool detected: " + action
            );
        }
        
        // Detect targeted destruction of player builds
        if (target.contains("base") || target.contains("house") || 
            target.contains("build") || target.contains("spawn")) {
            if (action.contains("destroy") || action.contains("blow") || 
                action.contains("burn") || action.contains("flood")) {
                return new SafetyResult(
                    SafetyResult.Decision.WARN,
                    "Potential griefing of " + target
                );
            }
        }
        
        return SafetyResult.ALLOW;
    }
}
