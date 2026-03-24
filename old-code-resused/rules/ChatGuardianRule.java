package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

import java.util.regex.Pattern;

/**
 * Rule 10: ChatGuardianRule
 * General chat filtering and ethics enforcement.
 * 
 * Filters:
 * - Profanity (optional by server)
 * - Spam detection
 * - Link/URL blocking
 * - Suspicious phrases
 */
public class ChatGuardianRule implements ISafetyRule<ChatContext> {
    
    private static final Pattern SPAM_PATTERN = Pattern.compile(
        "(.)\\1{4,}|(http|https|www|\\.com|\\.net|\\.org)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SUSPICIOUS_PHRASES = Pattern.compile(
        "(free nitro|click here|claim your|verify your|password|credit card)",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String name() {
        return "ChatGuardianRule";
    }
    
    @Override
    public SafetyResult check(ChatContext context) {
        String msg = context.message();
        
        // Check for spam/links
        if (SPAM_PATTERN.matcher(msg).find()) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Spam or link detected"
            );
        }
        
        // Check for phishing/scam attempts
        if (SUSPICIOUS_PHRASES.matcher(msg).find()) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Potential scam/phishing blocked"
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
