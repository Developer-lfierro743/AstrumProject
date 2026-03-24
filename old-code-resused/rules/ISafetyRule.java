package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.SafetyContext;
import com.novusforge.astrum.core.SafetyGuardian.SafetyResult;

/**
 * ISafetyRule - Base interface for all SafetyGuardian rules.
 * 
 * Project Astrum has 11 core safety rules:
 * 1. FileIntegrityRule - Checksum/hash verification
 * 2. SexualContentRule - CSAM/adult content blocking
 * 3. MinorContactRule - Minor protection
 * 4. GroomingPatternRule - Grooming detection
 * 5. NativeModRule - Mod loader verification
 * 6. AntiCheatRule - Cheat detection
 * 7. IdentityFraudRule - Identity verification
 * 8. GriefingPatternRule - Griefing detection
 * 9. HarassmentRule - Harassment blocking
 * 10. ChatGuardianRule - Chat filtering
 * 11. IGDRule - Internet Gaming Disorder (wellbeing)
 */
public interface ISafetyRule<T extends SafetyContext> {
    
    /**
     * Rule name (for logging/debugging)
     */
    String name();
    
    /**
     * Evaluate the rule against a context.
     * @param context The safety context to check
     * @return SafetyResult with decision (ALLOW/WARN/BLOCK) and reason
     */
    SafetyResult check(T context);
    
    /**
     * Safe evaluation that handles type mismatches.
     * Returns ALLOW if context type doesn't match rule.
     */
    @SuppressWarnings("unchecked")
    default SafetyResult evaluateUnsafe(SafetyContext context) {
        try {
            return check((T) context);
        } catch (ClassCastException e) {
            return SafetyResult.ALLOW;
        }
    }
    
    /**
     * Check if this rule supports the given context type.
     */
    default boolean supportsContext(SafetyContext context) {
        try {
            check((T) context);
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }
}
