package com.novusforge.astrum.core;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SafetyGuardian: The engine-level guardian of Project Astrum.
 * Enforces hardcoded safety rules for content, mods, and communication.
 * 
 * Mandate: "Independent, Resilient, and Secure."
 */
public class SafetyGuardian {
    private static final Logger LOGGER = Logger.getLogger(SafetyGuardian.class.getName());

    public enum SafetyResult {
        ALLOW, WARN, BLOCK
    }

    public enum SafetyCategory {
        CONTENT_SAFETY,
        MOD_SAFETY,
        CHAT_SAFETY
    }

    /**
     * Typed context for safety validation.
     */
    public sealed interface SafetyContext permits ChatContext, ModContext, ActionContext {
        SafetyCategory category();
        String summary();
    }

    public record ChatContext(String message, String playerId) implements SafetyContext {
        @Override public SafetyCategory category() { return SafetyCategory.CHAT_SAFETY; }
        @Override public String summary() { return "Chat[Player:" + playerId + "]: " + message; }
    }

    public record ModContext(String modId, String source, Map<String, String> metadata) implements SafetyContext {
        @Override public SafetyCategory category() { return SafetyCategory.MOD_SAFETY; }
        @Override public String summary() { return "Mod[" + modId + "] from " + source; }
    }

    public record ActionContext(String actionType, String actorId, String target) implements SafetyContext {
        @Override public SafetyCategory category() { return SafetyCategory.CONTENT_SAFETY; }
        @Override public String summary() { return "Action[" + actionType + "] by " + actorId + " on " + target; }
    }

    /**
     * Specialized rule interface for typed validation.
     */
    public interface SafetyRule<T extends SafetyContext> {
        String name();
        Class<T> supportedContext();
        SafetyResult validate(T context);
    }

    private final List<SafetyRule<? extends SafetyContext>> rules = new ArrayList<>();

    public SafetyGuardian() {
        registerDefaultRules();
    }

    private void registerDefaultRules() {
        // Hardcoded rules from the Formula
        addRule(new SexualContentRule());
        addRule(new NativeModRule());
        addRule(new ChatSafetyRule());
    }

    public <T extends SafetyContext> void addRule(SafetyRule<T> rule) {
        rules.add(rule);
    }

    /**
     * Lightweight real-time validation.
     * Aggregates results: BLOCK > WARN > ALLOW.
     */
    public SafetyResult validate(SafetyContext context) {
        SafetyResult finalResult = SafetyResult.ALLOW;

        for (SafetyRule<? extends SafetyContext> rule : rules) {
            SafetyResult result = tryValidate(rule, context);
            
            // Priority: BLOCK > WARN > ALLOW
            if (result.ordinal() > finalResult.ordinal()) {
                finalResult = result;
                
                // Log violations immediately
                if (finalResult != SafetyResult.ALLOW) {
                    logViolation(rule.name(), context, finalResult);
                }
                
                // Optimization: Fail fast on block
                if (finalResult == SafetyResult.BLOCK) {
                    break;
                }
            }
        }

        return finalResult;
    }

    /**
     * Async validation for heavy analysis (e.g., deep chat scanning).
     */
    public CompletableFuture<SafetyResult> validateAsync(SafetyContext context) {
        return CompletableFuture.supplyAsync(() -> validate(context));
    }

    @SuppressWarnings("unchecked")
    private <T extends SafetyContext> SafetyResult tryValidate(SafetyRule<T> rule, SafetyContext context) {
        if (rule.supportedContext().isInstance(context)) {
            return rule.validate((T) context);
        }
        return SafetyResult.ALLOW;
    }

    private void logViolation(String ruleName, SafetyContext context, SafetyResult result) {
        Level level = (result == SafetyResult.BLOCK) ? Level.SEVERE : Level.WARNING;
        LOGGER.log(level, "[SafetyGuardian] {0} triggered by {1}: {2}", 
            new Object[]{result, ruleName, context.summary()});
    }

    // --- Core Rule Implementations ---

    private static class SexualContentRule implements SafetyRule<ActionContext> {
        @Override public String name() { return "SexualContent-Blocker"; }
        @Override public Class<ActionContext> supportedContext() { return ActionContext.class; }
        @Override public SafetyResult validate(ActionContext context) {
            String target = context.target().toLowerCase();
            // Blocking known unauthorized assets/mods mentioned in Formula
            if (target.contains("jenny") || target.contains("adult_content")) {
                return SafetyResult.BLOCK;
            }
            return SafetyResult.ALLOW;
        }
    }

    private static class NativeModRule implements SafetyRule<ModContext> {
        @Override public String name() { return "Native-Mod-Verifier"; }
        @Override public Class<ModContext> supportedContext() { return ModContext.class; }
        @Override public SafetyResult validate(ModContext context) {
            String source = context.source().toLowerCase();
            // Formula: stop game from loading different modloaders from Minecraft
            if (source.contains("forge") || source.contains("fabric") || source.contains("neoforge")) {
                return SafetyResult.BLOCK;
            }
            return SafetyResult.ALLOW;
        }
    }

    private static class ChatSafetyRule implements SafetyRule<ChatContext> {
        @Override public String name() { return "Anti-Grooming-Monitor"; }
        @Override public Class<ChatContext> supportedContext() { return ChatContext.class; }
        @Override public SafetyResult validate(ChatContext context) {
            String message = context.message().toLowerCase();
            // Basic hardcoded rule implementation for grooming/abuse detection
            if (message.contains("bad-pattern") || message.contains("sus-phrase")) {
                return SafetyResult.WARN;
            }
            return SafetyResult.ALLOW;
        }
    }
}
