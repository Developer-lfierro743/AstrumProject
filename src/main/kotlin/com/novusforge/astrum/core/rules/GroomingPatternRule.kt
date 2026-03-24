package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 4: GroomingPatternRule - Grooming detection
 */
class GroomingPatternRule : ISafetyRule<ChatContext> {
    
    private val groomingPattern = Regex(
        "(you're so mature|you're special|they don't understand you|" +
        "keep this secret|don't tell anyone|i have a gift|" +
        "i can help you|trust me|just us|our little secret)",
        RegexOption.IGNORE_CASE
    )
    
    override fun name() = "GroomingPatternRule"
    
    override fun check(context: ChatContext): SafetyResult {
        if (groomingPattern.containsMatchIn(context.message)) {
            return SafetyResult.BLOCK.copy(reason = "Grooming pattern detected")
        }
        
        return SafetyResult.ALLOW
    }
}
