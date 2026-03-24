package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 9: HarassmentRule - Harassment blocking
 */
class HarassmentRule : ISafetyRule<ChatContext> {
    
    private val threatPattern = Regex(".*(i'll kill you|kill yourself|die|death threat|dox|doxxing|i know where).*", RegexOption.IGNORE_CASE)
    private val hatePattern = Regex(".*(racist|slur|nazi|hitler|kkk|white power|black power|faggot|tranny|retard|cunt).*", RegexOption.IGNORE_CASE)
    private val harassmentPattern = Regex(".*(stupid|idiot|trash|garbage).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "HarassmentRule"
    
    override fun check(context: ChatContext): SafetyResult {
        val msg = context.message
        
        // Threats
        if (threatPattern.containsMatchIn(msg)) {
            return SafetyResult.BLOCK.copy(reason = "Threat or doxxing blocked")
        }
        
        // Hate speech
        if (hatePattern.containsMatchIn(msg)) {
            return SafetyResult.BLOCK.copy(reason = "Hate speech blocked")
        }
        
        // Harassment
        if (harassmentPattern.containsMatchIn(msg)) {
            return SafetyResult.WARN.copy(reason = "Potentially harassing language")
        }
        
        return SafetyResult.ALLOW
    }
}
