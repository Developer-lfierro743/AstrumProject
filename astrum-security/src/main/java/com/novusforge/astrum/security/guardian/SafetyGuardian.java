package com.novusforge.astrum.security.guardian;

import com.novusforge.astrum.api.Mod;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The SafetyGuardian: A hardcoded, multi-layered security system.
 * Designed to protect minors and prevent harmful content at the engine level.
 * According to "The Formula", this is a guardian angel for the whole game.
 */
public class SafetyGuardian {
    private final List<SafetyRule> rules = new ArrayList<>();
    private final AssetScanner assetScanner = new AssetScanner();
    private static final Set<String> BLOCKED_MODLOADERS = Set.of(
        "forge", "neoforge", "fabric", "quilt", "liteloader", "risugami", "modloader"
    );
    
    public SafetyGuardian() {
        rules.add(new SexualRule());
    }

    /**
     * Verifies a mod before loading to ensure it doesn't contain prohibited content.
     */
    public boolean verifyMod(Mod mod) {
        System.out.println("SafetyGuardian verifying mod: " + mod.getModId());
        
        for (SafetyRule rule : rules) {
            if (!rule.check(mod)) {
                System.err.println("SafetyGuardian BLOCKED mod " + mod.getModId() + " due to rule violation.");
                return false;
            }
        }
        
        if (isBlockedModloader(mod)) {
            System.err.println("SafetyGuardian BLOCKED mod from incompatible modloader: " + mod.getModId());
            return false;
        }
        
        return true;
    }
    
    private boolean isBlockedModloader(Mod mod) {
        String modId = mod.getModId().toLowerCase();
        for (String blocked : BLOCKED_MODLOADERS) {
            if (modId.contains(blocked)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans mod assets for prohibited content.
     */
    public boolean scanAssets(Path modPath) {
        System.out.println("SafetyGuardian scanning assets: " + modPath);
        boolean result = assetScanner.scanModAssets(modPath);
        
        if (!result) {
            System.err.println("SafetyGuardian: Asset scan found violations!");
        }
        
        return result;
    }

    public void scanAssets(Object asset) {
        for (SafetyRule rule : rules) {
            if (!rule.checkAsset(asset)) {
                System.err.println("SafetyGuardian BLOCKED asset due to rule violation.");
            }
        }
    }
    
    public AssetScanner getAssetScanner() {
        return assetScanner;
    }
    
    public void addRule(SafetyRule rule) {
        rules.add(rule);
    }
}
