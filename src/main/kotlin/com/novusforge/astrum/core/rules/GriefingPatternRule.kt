package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 8: GriefingPatternRule - Griefing detection
 */
class GriefingPatternRule : ISafetyRule<ActionContext> {
    
    private val massDestructionPattern = Regex(".*(mass|all).*(break|destroy).*", RegexOption.IGNORE_CASE)
    private val griefToolPattern = Regex(".*(copenheimer|worldedit|nuker|destroyer).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "GriefingPatternRule"
    
    override fun check(context: ActionContext): SafetyResult {
        val action = context.action.lowercase()
        val target = context.target.lowercase()
        
        // Mass destruction
        if (massDestructionPattern.containsMatchIn(action)) {
            return SafetyResult.BLOCK.copy(reason = "Mass destruction attempt")
        }
        
        // Griefing tools
        if (griefToolPattern.containsMatchIn(action)) {
            return SafetyResult.BLOCK.copy(reason = "Griefing tool: $action")
        }
        
        // Targeted destruction
        if ((target.contains("base") || target.contains("house") || target.contains("build")) &&
            (action.contains("destroy") || action.contains("blow") || action.contains("burn") || action.contains("flood"))) {
            return SafetyResult.WARN.copy(reason = "Potential griefing of $target")
        }
        
        return SafetyResult.ALLOW
    }
}
