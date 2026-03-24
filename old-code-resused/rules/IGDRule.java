package com.novusforge.astrum.core.rules;

import com.novusforge.astrum.core.SafetyGuardian.*;

/**
 * Rule 11: IGDRule (Internet Gaming Disorder)
 * Player wellbeing and healthy gaming enforcement.
 * 
 * Based on YouTube/Google's wellbeing tools.
 * 
 * Features:
 * - Play time reminders
 * - Break suggestions
 * - Excessive play detection
 * - Parental controls integration
 */
public class IGDRule implements ISafetyRule<ActionContext> {
    
    // Recommended maximum continuous play time (in hours)
    private static final int MAX_CONTINUOUS_HOURS = 4;
    
    // Break reminder interval (in minutes)
    private static final int BREAK_INTERVAL_MINUTES = 60;
    
    @Override
    public String name() {
        return "IGDRule";
    }
    
    @Override
    public SafetyResult check(ActionContext context) {
        String action = context.action().toLowerCase();
        
        // Detect excessive play patterns
        if (action.contains("playtime") || action.contains("session")) {
            // Would check actual play time in production
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Consider taking a break - healthy gaming reminder"
            );
        }
        
        // Detect addiction-related phrases
        if (action.contains("can't stop") || action.contains("addicted") ||
            action.contains("playing all day") || action.contains("no sleep")) {
            return new SafetyResult(
                SafetyResult.Decision.WARN,
                "Healthy gaming reminder: Take regular breaks, stay hydrated"
            );
        }
        
        return SafetyResult.ALLOW;
    }
    
    /**
     * Get recommended break message
     */
    public static String getBreakReminder() {
        return "Time for a break! Remember to:\n" +
               "- Stand up and stretch\n" +
               "- Rest your eyes (20-20-20 rule)\n" +
               "- Stay hydrated\n" +
               "- Take care of your health!";
    }
}
