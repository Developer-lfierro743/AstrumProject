package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

import java.util.regex.Pattern;

/**
 * Rule 2: SexualContentRule
 * Blocks CSAM, adult content, and sexually explicit material.
 * 
 * This is the PRIMARY protection rule - zero tolerance.
 */
public class SexualContentRule implements ISafetyRule<ContentContext> {
    
    private static final Pattern SEXUAL_PATTERN = Pattern.compile(
        ".*(nsfw|nude|naked|sex|porn|xxx|adult|explicit|18\\+|21\\+).*",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public String name() {
        return "SexualContentRule";
    }
    
    @Override
    public SafetyResult check(ContentContext context) {
        String type = context.type().toLowerCase();
        String name = context.assetName().toLowerCase();
        
        // Block sexually explicit content types
        if (type.contains("adult") || type.contains("xxx") || type.contains("nsfw")) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Content type blocked: " + type
            );
        }
        
        // Block sexually explicit asset names
        if (SEXUAL_PATTERN.matcher(name).matches()) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Sexual content detected in asset: " + context.assetName()
            );
        }
        
        // Block known problematic mods (Jenny Mod, etc.)
        if (name.toLowerCase().contains("jenny") || 
            name.toLowerCase().contains("pregnant") ||
            name.toLowerCase().contains("breast")) {
            return new SafetyResult(
                SafetyResult.Decision.BLOCK,
                "Known adult mod detected: " + context.assetName()
            );
        }
        
        return SafetyResult.ALLOW;
    }
}
