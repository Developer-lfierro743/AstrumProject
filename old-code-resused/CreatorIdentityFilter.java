package com.novusforge.astrum.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * CreatorIdentityFilter: Deterministic impersonation protection for Project Astrum.
 * Integrated with CommunityIncidentDatabase for automatic bad actor blocking.
 * 
 * Protection Layers:
 * 1. CommunityIncidentDatabase → Auto-extract bad actors (BLOCK)
 * 2. GOOD_CREATOR_NAMES → Protect legitimate creators (BLOCK impersonation)
 * 3. ALIASES → Real name mappings (BLOCK)
 * 4. Leet speak detection (0→o, 1→i, 3→e, 4→a, 5→s, 7→t)
 * 5. Levenshtein distance (≤2 characters = WARN)
 * 6. YouTube whitelist (verified creators can use their names)
 * 
 * Why This Exists:
 * Minecraft has documented problems with impersonation, grooming, cheating, and abuse.
 * This filter prevents bad actors from using known problematic names.
 */
public final class CreatorIdentityFilter {

    public enum FilterSeverity {
        ALLOW,   // Username is permitted
        WARN,    // Username is suspicious (logged for review)
        BLOCK    // Username is blocked
    }

    public record FilterResult(boolean allowed, String reason, FilterSeverity severity) {
        public static final FilterResult ALLOW = new FilterResult(true, "Username permitted", FilterSeverity.ALLOW);
    }

    /**
     * Verified YouTube account (whitelist)
     */
    public record VerifiedAccount(
        String youtubeChannelId,
        String youtubeChannelName,
        String minecraftUsername,
        long verificationDate,
        boolean isPartner
    ) {}

    // ============================================================
    // BAD ACTOR NAMES - Auto-extracted from CommunityIncidentDatabase
    // These creators have documented incidents (grooming, cheating, abuse, etc.)
    // ============================================================
    private static final Set<String> BAD_ACTOR_NAMES = new HashSet<>(
        CommunityIncidentDatabase.COMMUNITY_INCIDENT_DATABASE.stream()
            .map(incident -> normalize(incident.creator()))
            .collect(Collectors.toSet())
    );

    // Additional manually added bad actors
    static {
        BAD_ACTOR_NAMES.addAll(Set.of(
            "dream", "dreamwastaken", "dreamxd",
            "callmecarson", "carson",
            "lionmaker", "jinbop", "bashurverse",
            "wilbursoot", "wilbur", "wilbursoot",
            "skydoesminecraft", "skydoesgaming", "adamdahlberg",
            "georgenotfound", "georgewbh",
            "skeppy", "iskall85", "iskall",
            "punz", "gerg", "marlowww", "dangermario",
            "popularmmos", "popularmmospat",
            "jschlatt", "schlatt", "lforlee", "lforleex",
            "thefifthcolumn", "fifthcolumn"
        ));
    }

    // ============================================================
    // GOOD CREATOR NAMES - Protected from impersonation
    // Event partners, legitimate creators, community figures
    // ============================================================
    private static final Set<String> GOOD_CREATOR_NAMES = Set.of(
        // Event creators / partners
        "sword4000", "mindofneo", "neo",
        "nyxlunarii", "nyx", "sharkilz",
        "pharolen", "bobicraft",
        "minecraftcurios", "ledgy",
        "knappyt", "knapp", "knappy",
        
        // Major creators
        "sapnap", "badboyhalo", "bbh",
        "quackity", "nihachu", "niki",
        "jackmanifold", "jack", "karljacobs", "karl",
        "fundy", "hbomb94", "hbomb",
        "vikkstar123", "vikkstar",
        "bdoubleo100", "bdubs", "impulsesv", "impulse",
        "scar", "scara", "goodtimeswithscar",
        "grian", "mumbo", "mumbojumbo",
        "zombiecleo", "cleo", "rendog", "ren",
        "ethoslab", "etho", "xbcrafted", "xb",
        "docm77", "doc", "jellies",
        "tango", "tangotek", "keralis",
        "joehills", "beef", "beefcraft",
        
        // Additional protected
        "technoblade", "techno", "tommyinnit", "tommy",
        "tubbo", "ranboo", "philza", "ph1lza"
    );

    // ============================================================
    // ALIASES - Real name → Creator name mappings
    // ============================================================
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("clay", "Dream"),
        Map.entry("clayhuff", "Dream"),
        Map.entry("marcuswilton", "LionMaker"),
        Map.entry("brandonasher", "Bashurverse"),
        Map.entry("zakahmed", "Skeppy"),
        Map.entry("georgedavidson", "GeorgeNotFound"),
        Map.entry("thomas", "TommyInnit"),
        Map.entry("toby", "Tubbo"),
        Map.entry("ran", "Ranboo"),
        Map.entry("nick", "Nihachu"),
        Map.entry("niki", "Nihachu"),
        Map.entry("jack", "JackManifold"),
        Map.entry("karl", "KarlJacobs"),
        Map.entry("george", "GeorgeNotFound"),
        Map.entry("sap", "Sapnap"),
        Map.entry("bad", "BadBoyHalo"),
        Map.entry("alex", "Quackity"),
        Map.entry("wilbur", "WilburSoot"),
        Map.entry("phil", "Philza"),
        Map.entry("techno", "Technoblade"),
        Map.entry("blade", "Technoblade"),
        Map.entry("dave", "Technoblade"),
        Map.entry("charles", "Cr1TiKaL"),
        Map.entry("moist", "Cr1TiKaL"),
        Map.entry("penguinz0", "Cr1TiKaL")
    );

    // ============================================================
    // WHITELIST - Verified YouTube accounts (can use their names)
    // ============================================================
    private static final Map<String, VerifiedAccount> WHITELIST = new ConcurrentHashMap<>();
    private static final Map<String, String> NORMALIZATION_CACHE = new ConcurrentHashMap<>();

    private CreatorIdentityFilter() {}

    /**
     * Main filter method - checks username against all protection layers.
     * 
     * @param username The username to check
     * @return FilterResult with decision, reason, and severity
     */
    public static FilterResult check(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new FilterResult(false, "Username cannot be empty", FilterSeverity.BLOCK);
        }
        
        if (username.length() < 3) {
            return new FilterResult(false, "Username must be at least 3 characters", FilterSeverity.BLOCK);
        }
        
        if (username.length() > 20) {
            return new FilterResult(false, "Username must be at most 20 characters", FilterSeverity.BLOCK);
        }
        
        // Layer 0: Check whitelist (verified YouTubers)
        if (isWhitelisted(username)) {
            return new FilterResult(true, "Verified YouTube account", FilterSeverity.ALLOW);
        }
        
        // Layer 1: Exact Match (Normalized)
        String normalized = normalize(username);
        
        // Check against CommunityIncidentDatabase bad actors
        if (BAD_ACTOR_NAMES.contains(normalized)) {
            String incident = findIncidentForCreator(normalized);
            return new FilterResult(
                false,
                "Blocked: " + username + " (Incident: " + incident + ")",
                FilterSeverity.BLOCK
            );
        }
        
        if (GOOD_CREATOR_NAMES.contains(normalized)) {
            return new FilterResult(false, "Creator impersonation blocked: " + username, FilterSeverity.BLOCK);
        }
        
        if (ALIASES.containsKey(normalized)) {
            return new FilterResult(
                false,
                "Identity blocked (Alias of " + ALIASES.get(normalized) + ")",
                FilterSeverity.BLOCK
            );
        }

        // Layer 2: Leet Speak Detection
        String leetConverted = convertLeetSpeak(normalized);
        if (BAD_ACTOR_NAMES.contains(leetConverted)) {
            return new FilterResult(false, "Obfuscated bad actor identity (leet speak)", FilterSeverity.BLOCK);
        }
        if (GOOD_CREATOR_NAMES.contains(leetConverted)) {
            return new FilterResult(false, "Obfuscated creator impersonation (leet speak)", FilterSeverity.BLOCK);
        }
        if (ALIASES.containsKey(leetConverted)) {
            return new FilterResult(false, "Obfuscated alias detected (leet speak)", FilterSeverity.BLOCK);
        }

        // Layer 3: Substring Match (WARN only)
        for (String protectedName : BAD_ACTOR_NAMES) {
            if (normalized.contains(protectedName) && protectedName.length() >= 4) {
                return new FilterResult(
                    true,
                    "Username contains bad actor name: " + protectedName,
                    FilterSeverity.WARN
                );
            }
        }
        for (String protectedName : GOOD_CREATOR_NAMES) {
            if (normalized.contains(protectedName) && protectedName.length() >= 4) {
                return new FilterResult(
                    true,
                    "Username contains protected creator name: " + protectedName,
                    FilterSeverity.WARN
                );
            }
        }

        // Layer 4: Levenshtein Distance (≤2 = WARN for short usernames)
        if (username.length() <= 15) {
            for (String protectedName : BAD_ACTOR_NAMES) {
                if (levenshtein(normalized, protectedName) <= 2) {
                    return new FilterResult(
                        true,
                        "Username too similar to bad actor: " + protectedName,
                        FilterSeverity.WARN
                    );
                }
            }
            for (String protectedName : GOOD_CREATOR_NAMES) {
                if (levenshtein(normalized, protectedName) <= 2) {
                    return new FilterResult(
                        true,
                        "Username too similar to protected creator: " + protectedName,
                        FilterSeverity.WARN
                    );
                }
            }
        }

        return FilterResult.ALLOW;
    }

    /**
     * Find incident description for a creator from database
     */
    private static String findIncidentForCreator(String normalized) {
        for (CommunityIncidentDatabase.IncidentRecord incident : 
             CommunityIncidentDatabase.COMMUNITY_INCIDENT_DATABASE) {
            if (normalize(incident.creator()).equals(normalized)) {
                return incident.category() + " (" + incident.year() + ")";
            }
        }
        return "Documented incident";
    }

    /**
     * Check if username is a bad actor from CommunityIncidentDatabase
     */
    public static boolean isBadActor(String username) {
        String normalized = normalize(username);
        String leet = convertLeetSpeak(normalized);
        return BAD_ACTOR_NAMES.contains(normalized) || 
               BAD_ACTOR_NAMES.contains(leet) || 
               ALIASES.containsKey(normalized);
    }

    /**
     * Check if username attempts creator impersonation
     */
    public static boolean isCreatorImpersonation(String username) {
        String normalized = normalize(username);
        String leet = convertLeetSpeak(normalized);
        return GOOD_CREATOR_NAMES.contains(normalized) || 
               GOOD_CREATOR_NAMES.contains(leet) ||
               levenshtein(normalized, GOOD_CREATOR_NAMES) <= 2;
    }

    /**
     * Check if username is whitelisted (verified YouTube)
     */
    public static boolean isWhitelisted(String username) {
        String normalized = normalize(username);
        for (VerifiedAccount account : WHITELIST.values()) {
            if (normalize(account.minecraftUsername()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if YouTube channel is verified
     */
    public static boolean isYouTubeVerified(String youtubeChannelId) {
        return WHITELIST.containsKey(youtubeChannelId);
    }

    /**
     * Add verified YouTube account to whitelist
     */
    public static boolean addVerifiedAccount(String channelId, String channelName, 
                                             String minecraftUsername, boolean isPartner) {
        if (channelId == null || channelId.isEmpty()) return false;
        
        WHITELIST.put(channelId, new VerifiedAccount(
            channelId, channelName, minecraftUsername,
            System.currentTimeMillis(), isPartner
        ));
        NORMALIZATION_CACHE.clear();
        System.out.println("[CreatorIdentityFilter] Verified: " + channelName);
        return true;
    }

    /**
     * Remove verified account
     */
    public static boolean removeVerifiedAccount(String channelId) {
        VerifiedAccount removed = WHITELIST.remove(channelId);
        if (removed != null) {
            NORMALIZATION_CACHE.clear();
            System.out.println("[CreatorIdentityFilter] Removed: " + removed.youtubeChannelName());
            return true;
        }
        return false;
    }

    public static VerifiedAccount getVerifiedAccount(String channelId) {
        return WHITELIST.get(channelId);
    }

    public static Map<String, VerifiedAccount> getAllVerifiedAccounts() {
        return Collections.unmodifiableMap(WHITELIST);
    }

    public static int getVerifiedAccountCount() {
        return WHITELIST.size();
    }

    /**
     * Normalize: lowercase, remove spaces/special chars
     */
    private static String normalize(String username) {
        return NORMALIZATION_CACHE.computeIfAbsent(username, key -> 
            key.toLowerCase().replaceAll("\\s+", "").replaceAll("[^a-z0-9]", "")
        );
    }

    /**
     * Convert leet speak: 0→o, 1→i, 3→e, 4→a, 5→s, 7→t, @→a
     */
    private static String convertLeetSpeak(String text) {
        return text.replaceAll("0", "o")
                   .replaceAll("1", "i")
                   .replaceAll("3", "e")
                   .replaceAll("4", "a")
                   .replaceAll("5", "s")
                   .replaceAll("7", "t")
                   .replaceAll("@", "a")
                   .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Levenshtein distance (edit distance)
     */
    private static int levenshtein(String s1, String s2) {
        if (s1.equals(s2)) return 0;
        if (s1.isEmpty()) return s2.length();
        if (s2.isEmpty()) return s1.length();
        
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    private static int levenshtein(String s, Set<String> strings) {
        int min = Integer.MAX_VALUE;
        for (String str : strings) {
            int dist = levenshtein(s, str);
            if (dist < min) {
                min = dist;
                if (min == 0) break;
            }
        }
        return min;
    }

    /**
     * Get all protected names (debug/admin)
     */
    public static Set<String> getAllProtectedNames() {
        Set<String> all = new HashSet<>();
        all.addAll(BAD_ACTOR_NAMES);
        all.addAll(GOOD_CREATOR_NAMES);
        all.addAll(ALIASES.keySet());
        return Collections.unmodifiableSet(all);
    }

    /**
     * Get incident database size (for stats)
     */
    public static int getIncidentDatabaseSize() {
        return CommunityIncidentDatabase.COMMUNITY_INCIDENT_DATABASE.size();
    }

    /**
     * Get all incidents for a creator
     */
    public static List<CommunityIncidentDatabase.IncidentRecord> getIncidentsForCreator(String creator) {
        String normalized = normalize(creator);
        return CommunityIncidentDatabase.COMMUNITY_INCIDENT_DATABASE.stream()
            .filter(incident -> normalize(incident.creator()).equals(normalized))
            .toList();
    }

    /**
     * Export all bad actor names (admin)
     */
    public static Set<String> getBadActorNames() {
        return Collections.unmodifiableSet(BAD_ACTOR_NAMES);
    }

    /**
     * Clear cache (testing)
     */
    public static void clearCache() {
        NORMALIZATION_CACHE.clear();
    }
}
