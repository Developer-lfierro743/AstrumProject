package com.novusforge.astrum.security.guardian;

import com.novusforge.astrum.api.Mod;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Safety rule that blocks sexual content, CSAM, and prohibited mods.
 * Hardcoded as per "The Formula" - SafetyGuardian.
 */
public class SexualRule implements SafetyRule {
    
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList(
        "nude", "naked", "nsfw", "sex", "sexual", "naked", "underwear",
        "bikini", "lingerie", " Explicit", "cum", "orgasm", "dick", "cock",
        "pussy", "vagina", "boob", "tit", "ass", "butt", "nipple",
        "xxx", "porn", "hentai", "erotic", "strip", "fetish"
    ));
    
    private static final Set<String> FORBIDDEN_MOD_IDS = new HashSet<>(Arrays.asList(
        "jennymod", "jenny_mod", "sexmod", "nudemod", "eroticraft",
        "playert", "bedrockbreaker", "titanim"
    ));
    
    private static final String CSAM_SIGNATURE = "CSAM_DETECTED";
    
    public SexualRule() {}
    
    @Override
    public boolean check(Mod mod) {
        if (mod == null) return true;
        
        String modId = mod.getModId().toLowerCase();
        String modName = mod.getName().toLowerCase();
        String modDescription = mod.getDescription().toLowerCase();
        
        for (String forbiddenId : FORBIDDEN_MOD_IDS) {
            if (modId.contains(forbiddenId)) {
                System.err.println("SafetyGuardian: BLOCKED mod with forbidden ID: " + mod.getModId());
                return false;
            }
        }
        
        if (containsForbiddenContent(modId) || containsForbiddenContent(modName) || containsForbiddenContent(modDescription)) {
            System.err.println("SafetyGuardian: BLOCKED mod for sexual content: " + mod.getModId());
            return false;
        }
        
        return true;
    }
    
    @Override
    public boolean checkAsset(Object asset) {
        if (asset == null) return true;
        
        String assetString = asset.toString().toLowerCase();
        
        if (containsForbiddenContent(assetString)) {
            System.err.println("SafetyGuardian: BLOCKED asset for prohibited content");
            return false;
        }
        
        return true;
    }
    
    private boolean containsForbiddenContent(String text) {
        if (text == null) return false;
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isCSAMDetected(byte[] data) {
        if (data == null) return false;
        String signature = new String(data);
        return signature.contains(CSAM_SIGNATURE);
    }
}
