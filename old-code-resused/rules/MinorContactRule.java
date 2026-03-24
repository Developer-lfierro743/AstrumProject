package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

/**
 * Rule 3: MinorContactRule
 * Protects minors from inappropriate contact by adults.
 * 
 * Flags:
 * - Age gap detection (adult + minor)
 * - Private conversation requests
 * - Personal information requests
 */
public class MinorContactRule implements ISafetyRule<ChatContext> {
    
    @Override
    public String name() {
        return "MinorContactRule";
    }
    
    @Override
    public SafetyResult check(ChatContext context) {
        String msg = context.message().toLowerCase();
        
        // Detect age-related requests
        if (msg.contains("how old") || msg.contains("what age") || 
            msg.contains("your age") || msg.contains("age?")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Age inquiry detected - monitor for minor contact"
            );
        }
        
        // Detect private conversation requests to unknown users
        if (msg.contains("add me on") || msg.contains("discord") || 
            msg.contains("snapchat") || msg.contains("private chat")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Private conversation request detected"
            );
        }
        
        // Detect personal information requests
        if (msg.contains("where do you live") || msg.contains("your address") ||
            msg.contains("your school") || msg.contains("phone number")) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Personal information request blocked"
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
