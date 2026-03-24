package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

/**
 * Rule 7: IdentityFraudRule
 * Prevents identity theft and impersonation.
 * 
 * Works with CreatorIdentityFilter to block:
 * - Known creator names
 * - Staff impersonation
 * - Fake verification claims
 */
public class IdentityFraudRule implements ISafetyRule<ChatContext> {
    
    @Override
    public String name() {
        return "IdentityFraudRule";
    }
    
    @Override
    public SafetyResult check(ChatContext context) {
        String msg = context.message().toLowerCase();
        
        // Detect fake staff claims
        if (msg.contains("i'm a moderator") || msg.contains("i'm an admin") ||
            msg.contains("i work for") || msg.contains("i'm staff")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Fake staff claim detected"
            );
        }
        
        // Detect verification fraud
        if (msg.contains("i'm verified") || msg.contains("blue check") ||
            msg.contains("official account")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Fake verification claim detected"
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
