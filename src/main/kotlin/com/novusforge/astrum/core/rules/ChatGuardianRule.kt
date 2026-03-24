package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 10: ChatGuardianRule - Chat filtering
 */
class ChatGuardianRule : ISafetyRule<ChatContext> {
    
    private val spamPattern = Regex("(.)\\1{4,}|(http|https|www|\\.com|\\.net|\\.org)", RegexOption.IGNORE_CASE)
    private val scamPattern = Regex(".*(free nitro|click here|claim your|verify your|password|credit card).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "ChatGuardianRule"
    
    override fun check(context: ChatContext): SafetyResult {
        val msg = context.message
        
        // Spam/links
        if (spamPattern.containsMatchIn(msg)) {
            return SafetyResult.WARN.copy(reason = "Spam or link detected")
        }
        
        // Scams
        if (scamPattern.containsMatchIn(msg)) {
            return SafetyResult.BLOCK.copy(reason = "Potential scam/phishing")
        }
        
        return SafetyResult.ALLOW
    }
}
