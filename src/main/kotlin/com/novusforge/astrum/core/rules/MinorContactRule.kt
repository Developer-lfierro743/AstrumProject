package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 3: MinorContactRule - Minor protection
 */
class MinorContactRule : ISafetyRule<ChatContext> {
    
    private val agePattern = Regex(".*(how old|what age|your age|age\\?).*", RegexOption.IGNORE_CASE)
    private val privatePattern = Regex(".*(add me on|discord|snapchat|private chat).*", RegexOption.IGNORE_CASE)
    private val personalPattern = Regex(".*(where do you live|your address|your school|phone number).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "MinorContactRule"
    
    override fun check(context: ChatContext): SafetyResult {
        val msg = context.message
        
        // Age inquiries
        if (agePattern.containsMatchIn(msg)) {
            return SafetyResult.WARN.copy(reason = "Age inquiry detected")
        }
        
        // Private conversation requests
        if (privatePattern.containsMatchIn(msg)) {
            return SafetyResult.WARN.copy(reason = "Private conversation request")
        }
        
        // Personal info requests
        if (personalPattern.containsMatchIn(msg)) {
            return SafetyResult.BLOCK.copy(reason = "Personal information request blocked")
        }
        
        return SafetyResult.ALLOW
    }
}
