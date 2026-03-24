package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

import java.util.regex.Pattern;

/**
 * Rule 9: HarassmentRule
 * Blocks harassment, bullying, and hate speech.
 * 
 * Flags:
 * - Direct threats
 * - Repeated negative behavior
 * - Hate speech patterns
 * - Doxxing attempts
 */
public class HarassmentRule implements ISafetyRule<ChatContext> {
    
    private static final Pattern THREAT_PATTERN = Pattern.compile(
        "(i'll kill you|kill yourself|die|death threat|dox|doxxing|" +
        "i know where|your address|your ip)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HATE_PATTERN = Pattern.compile(
        "(racist|slur|nazi|hitler|kkk|white power|black power|" +
        "faggot|tranny|retard|cunt)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String name() {
        return "HarassmentRule";
    }
    
    @Override
    public SafetyResult check(ChatContext context) {
        String msg = context.message();
        
        // Check for threats
        if (THREAT_PATTERN.matcher(msg).find()) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Threat or doxxing attempt blocked"
            );
        }
        
        // Check for hate speech
        if (HATE_PATTERN.matcher(msg).find()) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Hate speech blocked"
            );
        }
        
        // Check for repeated harassment (would need message history in production)
        if (msg.contains("stupid") || msg.contains("idiot") || 
            msg.contains("trash") || msg.contains("garbage")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Potentially harassing language"
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
