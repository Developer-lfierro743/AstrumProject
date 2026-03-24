package com.novusforge.astrum.core

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * CreatorIdentityFilter - Impersonation protection
 * Kotlin conversion: Object singleton, data classes, extension functions
 */
object CreatorIdentityFilter {
    
    enum class FilterSeverity { ALLOW, WARN, BLOCK }
    
    data class FilterResult(val allowed: Boolean, val reason: String, val severity: FilterSeverity) {
        companion object {
            val ALLOW = FilterResult(true, "Username permitted", FilterSeverity.ALLOW)
        }
    }
    
    data class VerifiedAccount(
        val youtubeChannelId: String,
        val youtubeChannelName: String,
        val minecraftUsername: String,
        val verificationDate: Long,
        val isPartner: Boolean
    )
    
    // Bad actors from CommunityIncidentDatabase
    private val badActorNames = mutableSetOf(
        "dream", "dreamwastaken", "dreamxd",
        "callmecarson", "carson", "lionmaker",
        "jinbop", "bashurverse", "wilbursoot", "wilbur",
        "skydoesminecraft", "skydoesgaming", "adamdahlberg",
        "georgenotfound", "georgewbh", "skeppy",
        "iskall85", "iskall", "punz", "gerg",
        "marlowww", "dangermario", "popularmmos",
        "popularmmospat", "jschlatt", "schlatt",
        "lforlee", "lforleex", "thefifthcolumn", "fifthcolumn"
    )
    
    // Good creators
    private val goodCreatorNames = setOf(
        "sword4000", "mindofneo", "neo",
        "nyxlunarii", "nyx", "sharkilz",
        "pharolen", "bobicraft",
        "minecraftcurios", "ledgy",
        "knappyt", "knapp", "knappy",
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
        "technoblade", "techno", "tommyinnit", "tommy",
        "tubbo", "ranboo", "philza", "ph1lza"
    )
    
    // Aliases
    private val aliases = mapOf(
        "clay" to "Dream",
        "clayhuff" to "Dream",
        "marcuswilton" to "LionMaker",
        "brandonasher" to "Bashurverse",
        "zakahmed" to "Skeppy",
        "georgedavidson" to "GeorgeNotFound",
        "thomas" to "TommyInnit",
        "toby" to "Tubbo",
        "ran" to "Ranboo",
        "nick" to "Nihachu",
        "niki" to "Nihachu",
        "jack" to "JackManifold",
        "karl" to "KarlJacobs",
        "george" to "GeorgeNotFound",
        "sap" to "Sapnap",
        "bad" to "BadBoyHalo",
        "alex" to "Quackity",
        "wilbur" to "WilburSoot",
        "phil" to "Philza",
        "techno" to "Technoblade",
        "blade" to "Technoblade",
        "dave" to "Technoblade",
        "charles" to "Cr1TiKaL",
        "moist" to "Cr1TiKaL",
        "penguinz0" to "Cr1TiKaL"
    )
    
    // Whitelist
    private val whitelist = ConcurrentHashMap<String, VerifiedAccount>()
    private val normalizationCache = ConcurrentHashMap<String, String>()
    
    private fun normalize(username: String): String {
        return normalizationCache.computeIfAbsent(username) { key ->
            key.lowercase().replace(Regex("\\s+"), "").replace(Regex("[^a-z0-9]"), "")
        }
    }
    
    private fun convertLeetSpeak(text: String): String {
        return text.lowercase()
            .replace("0", "o")
            .replace("1", "i")
            .replace("3", "e")
            .replace("4", "a")
            .replace("5", "s")
            .replace("7", "t")
            .replace("@", "a")
            .replace(Regex("[^a-z0-9]"), "")
    }
    
    private fun levenshtein(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length
        
        val costs = IntArray(s2.length + 1)
        
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1]) {
                            newValue = minOf(minOf(newValue, lastValue), costs[j]) + 1
                        }
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0) costs[s2.length] = lastValue
        }
        
        return costs[s2.length]
    }
    
    fun check(username: String): FilterResult {
        if (username.isNullOrBlank()) {
            return FilterResult(false, "Username cannot be empty", FilterSeverity.BLOCK)
        }
        
        if (username.length < 3) {
            return FilterResult(false, "Username must be at least 3 characters", FilterSeverity.BLOCK)
        }
        
        if (username.length > 20) {
            return FilterResult(false, "Username must be at most 20 characters", FilterSeverity.BLOCK)
        }
        
        val normalized = normalize(username)
        val leetConverted = convertLeetSpeak(normalized)
        
        // Check whitelist
        if (isWhitelisted(username)) {
            return FilterResult(true, "Verified YouTube account", FilterSeverity.ALLOW)
        }
        
        // Exact match
        if (normalized in badActorNames) {
            return FilterResult(false, "Bad actor identity blocked: $username", FilterSeverity.BLOCK)
        }
        
        if (normalized in goodCreatorNames) {
            return FilterResult(false, "Creator impersonation blocked: $username", FilterSeverity.BLOCK)
        }
        
        if (normalized in aliases.keys) {
            return FilterResult(false, "Identity blocked (Alias of ${aliases[normalized]})", FilterSeverity.BLOCK)
        }
        
        // Leet speak
        if (leetConverted in badActorNames || leetConverted in goodCreatorNames || leetConverted in aliases.keys) {
            return FilterResult(false, "Obfuscated identity detected (leet speak)", FilterSeverity.BLOCK)
        }
        
        // Substring match
        for (protectedName in badActorNames) {
            if (normalized.contains(protectedName) && protectedName.length >= 4) {
                return FilterResult(true, "Username contains bad actor name: $protectedName", FilterSeverity.WARN)
            }
        }
        
        for (protectedName in goodCreatorNames) {
            if (normalized.contains(protectedName) && protectedName.length >= 4) {
                return FilterResult(true, "Username contains protected creator name: $protectedName", FilterSeverity.WARN)
            }
        }
        
        // Levenshtein distance
        if (username.length <= 15) {
            for (protectedName in badActorNames) {
                if (levenshtein(normalized, protectedName) <= 2) {
                    return FilterResult(true, "Username too similar to bad actor: $protectedName", FilterSeverity.WARN)
                }
            }
            
            for (protectedName in goodCreatorNames) {
                if (levenshtein(normalized, protectedName) <= 2) {
                    return FilterResult(true, "Username too similar to protected creator: $protectedName", FilterSeverity.WARN)
                }
            }
        }
        
        return FilterResult.ALLOW
    }
    
    fun isBadActor(username: String): Boolean {
        val normalized = normalize(username)
        val leet = convertLeetSpeak(normalized)
        return normalized in badActorNames || leet in badActorNames || normalized in aliases.keys
    }
    
    fun isCreatorImpersonation(username: String): Boolean {
        val normalized = normalize(username)
        val leet = convertLeetSpeak(normalized)
        return normalized in goodCreatorNames || leet in goodCreatorNames
    }
    
    fun isWhitelisted(username: String): Boolean {
        val normalized = normalize(username)
        return whitelist.values.any { normalize(it.minecraftUsername) == normalized }
    }
    
    fun isYouTubeVerified(channelId: String): Boolean = whitelist.containsKey(channelId)
    
    fun addVerifiedAccount(channelId: String, channelName: String, minecraftUsername: String, isPartner: Boolean): Boolean {
        if (channelId.isNullOrBlank()) return false
        
        whitelist[channelId] = VerifiedAccount(channelId, channelName, minecraftUsername, System.currentTimeMillis(), isPartner)
        normalizationCache.clear()
        println("[CreatorIdentityFilter] Verified: $channelName")
        return true
    }
    
    fun removeVerifiedAccount(channelId: String): Boolean {
        val removed = whitelist.remove(channelId)
        if (removed != null) {
            normalizationCache.clear()
            println("[CreatorIdentityFilter] Removed: ${removed.youtubeChannelName}")
            return true
        }
        return false
    }
    
    fun getVerifiedAccount(channelId: String): VerifiedAccount? = whitelist[channelId]
    
    fun getAllVerifiedAccounts(): Map<String, VerifiedAccount> = whitelist.toMap()
    
    fun getVerifiedAccountCount(): Int = whitelist.size
    
    fun getAllProtectedNames(): Set<String> = badActorNames + goodCreatorNames + aliases.keys
    
    fun getIncidentDatabaseSize(): Int = badActorNames.size
    
    fun clearCache() {
        normalizationCache.clear()
    }
}
