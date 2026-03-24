package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.HashUtils;
import com.novusforge.astrum.core.SafetyGuardian.*;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Rule 1: FileIntegrityRule
 * Validates file checksums and blocks known malicious assets.
 * 
 * Checks:
 * - SHA-256 hash against blacklist
 * - Filename patterns for harmful content
 */
public class FileIntegrityRule implements ISafetyRule<DataContext> {
    
    // Known malicious file hashes (CSAM, malware, etc.)
    private static final Set<String> BLACKLISTED_CHECKSUMS = Set.of(
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", // Empty
        "0000000000000000000000000000000000000000000000000000000000000000"  // Null
    );
    
    // Pattern for harmful asset names
    private static final Pattern HARMFUL_ASSET_PATTERN = Pattern.compile(
        ".*(j[e3]n+y|n[u0]de|nsfw|adult|xxx|porn|csam).*",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String name() {
        return "FileIntegrityRule";
    }
    
    @Override
    public SafetyResult check(DataContext context) {
        // Cast to ContentContext for type and assetName
        if (!(context instanceof ContentContext cc)) {
            return SafetyResult.ALLOW;
        }
        
        // Check 1: Cryptographic hash verification
        String actualHash = HashUtils.computeSHA256(cc.data());
        if (BLACKLISTED_CHECKSUMS.contains(actualHash)) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "File hash matches blacklist (SHA-256: " + actualHash.substring(0, 16) + "...)"
            );
        }
        
        // Check 2: Filename pattern matching
        if (HARMFUL_ASSET_PATTERN.matcher(cc.assetName()).matches()) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Asset name matches harmful pattern: " + cc.assetName()
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
