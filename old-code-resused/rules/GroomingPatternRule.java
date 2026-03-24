package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

import java.util.regex.Pattern;

/**
 * Rule 4: GroomingPatternRule
 * Detects grooming behavior patterns in chat.
 * 
 * Flags:
 * - Excessive compliments/flattery
 * - Isolation attempts
 * - Secret-keeping requests
 * - Gift/offer manipulation
 */
public class GroomingPatternRule implements ISafetyRule<ChatContext> {
    
    private static final Pattern GROOMING_PATTERNS = Pattern.compile(
        "(you're so mature|you're special|they don't understand you|" +
        "keep this secret|don't tell anyone|i have a gift|" +
        "i can help you|trust me|just us|our little secret)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String name() {
        return "GroomingPatternRule";
    }
    
    @Override
    public SafetyResult check(ChatContext context) {
        if (GROOMING_PATTERNS.matcher(context.message()).find()) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Grooming pattern detected - conversation flagged"
            );
        }
        
        // Check for excessive private messaging
        // (Would require message frequency tracking in production)
        
        return SafetyResult.ALLOW;
    }
}
