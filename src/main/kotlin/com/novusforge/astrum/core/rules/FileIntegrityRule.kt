package com.novusforge.astrum.core.rules

import com.novusforge.astrum.core.HashUtils
import com.novusforge.astrum.core.SafetyGuardian.*

/**
 * Rule 1: FileIntegrityRule - Checksum verification
 */
class FileIntegrityRule : ISafetyRule<DataContext> {
    
    companion object {
        private val BLACKLISTED_CHECKSUMS = setOf(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "0000000000000000000000000000000000000000000000000000000000000000"
        )
        
        private val HARMFUL_ASSET_PATTERN = Regex(".*(j[e3]n+y|n[u0]de|nsfw|adult|xxx|porn|csam).*", RegexOption.IGNORE_CASE)
    }
    
    override fun name() = "FileIntegrityRule"
    
    override fun check(context: DataContext): SafetyResult {
        // Check hash
        val hash = HashUtils.computeSHA256(context.data())
        if (hash in BLACKLISTED_CHECKSUMS) {
            return SafetyResult.BLOCK.copy(reason = "File hash matches blacklist")
        }
        
        // Check filename
        if (context is ContentContext && HARMFUL_ASSET_PATTERN.matches(context.assetName)) {
            return SafetyResult.BLOCK.copy(reason = "Harmful asset name: ${context.assetName}")
        }
        
        return SafetyResult.ALLOW
    }
}
