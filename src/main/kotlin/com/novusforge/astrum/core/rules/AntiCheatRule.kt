package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 6: AntiCheatRule - Cheat detection
 */
class AntiCheatRule : ISafetyRule<ActionContext> {
    
    private val cheatPattern = Regex(".*(killaura|flyhack|xray|reach|speedhack|norecoil).*", RegexOption.IGNORE_CASE)
    private val exploitPattern = Regex(".*(exploit|dupe|glitch|hack).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "AntiCheatRule"
    
    override fun check(context: ActionContext): SafetyResult {
        val action = context.action.lowercase()
        
        // Detect cheats
        if (cheatPattern.containsMatchIn(action)) {
            return SafetyResult.BLOCK.copy(reason = "Cheat module: $action")
        }
        
        // Detect exploits
        if (exploitPattern.containsMatchIn(action)) {
            return SafetyResult.BLOCK.copy(reason = "Exploit attempt: $action")
        }
        
        return SafetyResult.ALLOW
    }
}
