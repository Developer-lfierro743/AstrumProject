package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 7: IdentityFraudRule - Identity verification
 */
class IdentityFraudRule : ISafetyRule<ChatContext> {
    
    private val staffPattern = Regex(".*(i'm a moderator|i'm an admin|i work for).*", RegexOption.IGNORE_CASE)
    private val verifyPattern = Regex(".*(i'm verified|blue check|official account).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "IdentityFraudRule"
    
    override fun check(context: ChatContext): SafetyResult {
        val msg = context.message
        
        // Fake staff claims
        if (staffPattern.containsMatchIn(msg)) {
            return SafetyResult.WARN.copy(reason = "Fake staff claim")
        }
        
        // Fake verification claims
        if (verifyPattern.containsMatchIn(msg)) {
            return SafetyResult.WARN.copy(reason = "Fake verification claim")
        }
        
        return SafetyResult.ALLOW
    }
}
