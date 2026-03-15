package com.novusforge.astrum.security.guardian;

import com.novusforge.astrum.api.Mod;

/**
 * Base interface for SafetyGuardian rules.
 */
public interface SafetyRule {
    /**
     * Checks if the given mod complies with this safety rule.
     */
    boolean check(Mod mod);

    /**
     * Checks if the given asset complies with this safety rule.
     */
    boolean checkAsset(Object asset);
}
