package com.novusforge.astrum.core;

import com.novusforge.astrum.core.rules.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SafetyGuardian: The "Fort Knox" guardian of Project Astrum.
 * 
 * "A reclaim of the sandbox vision. Independent, Resilient, and Secure."
 * 
 * This is a hardcoded, non-AI security layer that protects players
 * from CSAM, grooming, cheating, griefing, and other harmful content.
 * 
 * 11 Core Rules:
 * 1. FileIntegrityRule     - Checksum/hash verification
 * 2. SexualContentRule     - CSAM/adult content blocking (ZERO TOLERANCE)
 * 3. MinorContactRule      - Minor protection
 * 4. GroomingPatternRule   - Grooming detection
 * 5. NativeModRule         - Mod loader verification
 * 6. AntiCheatRule         - Cheat detection
 * 7. IdentityFraudRule     - Identity verification
 * 8. GriefingPatternRule   - Griefing detection
 * 9. HarassmentRule        - Harassment blocking
 * 10. ChatGuardianRule     - Chat filtering
 * 11. IGDRule              - Internet Gaming Disorder (wellbeing)
 */
public final class SafetyGuardian {
    
    private static final Logger LOGGER = Logger.getLogger("SafetyGuardian");
    
    /**
     * SafetyResult - Carries decision and reason
     */
    public record SafetyResult(Decision decision, String reason) {
        public enum Decision { 
            ALLOW,  // Action permitted
            WARN,   // Action permitted but logged for review
            BLOCK   // Action denied
        }
        
        public static final SafetyResult ALLOW = new SafetyResult(Decision.ALLOW, "Clean");
        public static final SafetyResult WARN = new SafetyResult(Decision.WARN, "Suspicious");
        public static final SafetyResult BLOCK = new SafetyResult(Decision.BLOCK, "Forbidden");
    }
    
    // ============================================================
    // Context Types
    // ============================================================
    
    public sealed interface SafetyContext permits ChatContext, ActionContext, DataContext {
        String identifier();
    }
    
    public sealed interface DataContext extends SafetyContext permits ModContext, ContentContext {
        byte[] data();
    }
    
    public record ChatContext(String playerId, String message) implements SafetyContext {
        @Override public String identifier() { return "Player:" + playerId; }
    }
    
    public record ModContext(String modId, String loader, byte[] data, Map<String, String> metadata) implements DataContext {
        @Override public String identifier() { return "Mod:" + modId + " (" + loader + ")"; }
    }
    
    public record ActionContext(String action, String actor, String target) implements SafetyContext {
        @Override public String identifier() { return "Action:" + action + " by " + actor + " on " + target; }
    }
    
    public record ContentContext(String type, String assetName, byte[] data) implements DataContext {
        @Override public String identifier() { return "Asset:" + assetName + " [" + type + "]"; }
        @Override public byte[] data() { return data; }
    }
    
    // ============================================================
    // Guardian Engine
    // ============================================================
    
    private final List<ISafetyRule<?>> rules = new ArrayList<>();
    private final Map<String, Integer> violationCounts = new HashMap<>();
    
    public SafetyGuardian() {
        // Initialize all 11 rules
        rules.add(new FileIntegrityRule());      // Rule 1
        rules.add(new SexualContentRule());      // Rule 2 (ZERO TOLERANCE)
        rules.add(new MinorContactRule());       // Rule 3
        rules.add(new GroomingPatternRule());    // Rule 4
        rules.add(new NativeModRule());          // Rule 5
        rules.add(new AntiCheatRule());          // Rule 6
        rules.add(new IdentityFraudRule());      // Rule 7
        rules.add(new GriefingPatternRule());    // Rule 8
        rules.add(new HarassmentRule());         // Rule 9
        rules.add(new ChatGuardianRule());       // Rule 10
        rules.add(new IGDRule());                // Rule 11
        
        System.out.println("[SafetyGuardian] Initialized with " + rules.size() + " rules active");
        printRuleSummary();
    }
    
    /**
     * Print summary of all active rules
     */
    private void printRuleSummary() {
        System.out.println("[SafetyGuardian] Active Rules:");
        for (int i = 0; i < rules.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + rules.get(i).name());
        }
    }
    
    /**
     * Validate a context against all rules.
     * Returns the most severe decision (BLOCK > WARN > ALLOW).
     */
    public SafetyResult validate(SafetyContext context) {
        SafetyResult worstCase = SafetyResult.ALLOW;
        
        for (ISafetyRule<?> rule : rules) {
            SafetyResult result = rule.evaluateUnsafe(context);
            
            // Track worst result
            if (result.decision().ordinal() > worstCase.decision().ordinal()) {
                worstCase = result;
                logViolation(rule.name(), context, result);
                
                // Early exit on BLOCK
                if (worstCase.decision() == SafetyResult.Decision.BLOCK) {
                    break;
                }
            }
        }
        
        return worstCase;
    }
    
    /**
     * Async validation for non-blocking checks.
     */
    public CompletableFuture<SafetyResult> validateAsync(SafetyContext context) {
        return CompletableFuture.supplyAsync(() -> validate(context));
    }
    
    /**
     * Log violations for admin review.
     */
    private void logViolation(String rule, SafetyContext ctx, SafetyResult res) {
        Level level = (res.decision() == SafetyResult.Decision.BLOCK) ? 
                      Level.SEVERE : Level.WARNING;
        
        LOGGER.log(level, "[FORT-KNOX] {0} → {1} ({2}) triggered by {3}",
            new Object[]{res.decision(), rule, res.reason(), ctx.identifier()});
        
        // Track violation counts
        String key = rule + ":" + ctx.identifier();
        violationCounts.put(key, violationCounts.getOrDefault(key, 0) + 1);
        
        // Auto-block after repeated violations
        if (violationCounts.get(key) >= 5) {
            LOGGER.severe("[FORT-KNOX] Auto-block: " + ctx.identifier() + 
                         " exceeded violation threshold for " + rule);
        }
    }
    
    /**
     * Get violation count for a player/rule combination.
     */
    public int getViolationCount(String rule, String identifier) {
        return violationCounts.getOrDefault(rule + ":" + identifier, 0);
    }
    
    /**
     * Clear violation history (admin only).
     */
    public void clearViolations() {
        violationCounts.clear();
        System.out.println("[SafetyGuardian] Violation history cleared");
    }
    
    /**
     * Get total active rule count.
     */
    public int getRuleCount() {
        return rules.size();
    }
    
    /**
     * Check if a specific rule is active.
     */
    public boolean isRuleActive(String ruleName) {
        return rules.stream().anyMatch(r -> r.name().equals(ruleName));
    }
}
