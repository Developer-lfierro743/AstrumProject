package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 2: SexualContentRule - CSAM/adult content blocking (ZERO TOLERANCE)
 */
class SexualContentRule : ISafetyRule<ContentContext> {
    
    private val sexualPattern = Regex(".*(nsfw|nude|naked|sex|porn|xxx|adult|explicit|18\\+|21\\+).*", RegexOption.IGNORE_CASE)
    
    override fun name() = "SexualContentRule"
    
    override fun check(context: ContentContext): SafetyResult {
        val type = context.type.lowercase()
        val name = context.assetName.lowercase()
        
        // Block explicit content types
        if (type.contains("adult") || type.contains("xxx") || type.contains("nsfw")) {
            return SafetyResult.BLOCK.copy(reason = "Content type blocked: $type")
        }
        
        // Block explicit names
        if (sexualPattern.matches(name)) {
            return SafetyResult.BLOCK.copy(reason = "Sexual content in asset: ${context.assetName}")
        }
        
        // Block known adult mods
        if (name.contains("jenny") || name.contains("pregnant") || name.contains("breast")) {
            return SafetyResult.BLOCK.copy(reason = "Known adult mod: ${context.assetName}")
        }
        
        return SafetyResult.ALLOW
    }
}
