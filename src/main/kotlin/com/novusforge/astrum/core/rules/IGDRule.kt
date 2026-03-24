package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 11: IGDRule - Internet Gaming Disorder (wellbeing)
 */
class IGDRule : ISafetyRule<ActionContext> {
    
    companion object {
        const val MAX_CONTINUOUS_HOURS = 4
        const val BREAK_INTERVAL_MINUTES = 60
        
        fun getBreakReminder(): String = """
            Time for a break! Remember to:
            - Stand up and stretch
            - Rest your eyes (20-20-20 rule)
            - Stay hydrated
            - Take care of your health!
        """.trimIndent()
    }
    
    private val addictionPattern = Regex(".*(can't stop|addicted|playing all day).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "IGDRule"
    
    override fun check(context: ActionContext): SafetyResult {
        val action = context.action.lowercase()
        
        // Addiction patterns
        if (addictionPattern.containsMatchIn(action)) {
            return SafetyResult.WARN.copy(reason = getBreakReminder())
        }
        
        return SafetyResult.ALLOW
    }
}
