package com.novusforge.astrum.core

import com.novusforge.astrum.core.rules.*
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * SafetyGuardian - Security layer for Project Astrum
 * Kotlin conversion: Sealed interfaces, data classes, object singleton rules
 */
class SafetyGuardian {
    
    companion object {
        private val LOGGER = Logger.getLogger("SafetyGuardian")
    }
    
    /**
     * SafetyResult - Decision with reason
     */
    data class SafetyResult(val decision: Decision, val reason: String) {
        enum class Decision { ALLOW, WARN, BLOCK }
        
        companion object {
            val ALLOW = SafetyResult(Decision.ALLOW, "Clean")
            val WARN = SafetyResult(Decision.WARN, "Suspicious")
            val BLOCK = SafetyResult(Decision.BLOCK, "Forbidden")
        }
    }
    
    /**
     * Safety Contexts
     */
    sealed interface SafetyContext {
        fun identifier(): String
    }
    
    sealed interface DataContext : SafetyContext {
        fun data(): ByteArray
    }
    
    data class ChatContext(val playerId: String, val message: String) : SafetyContext {
        override fun identifier() = "Player:$playerId"
    }
    
    data class ModContext(
        val modId: String, 
        val loader: String, 
        val data: ByteArray,
        val metadata: Map<String, String>
    ) : DataContext {
        override fun identifier() = "Mod:$modId ($loader)"
        override fun data() = data
    }
    
    data class ActionContext(val action: String, val actor: String, val target: String) : SafetyContext {
        override fun identifier() = "Action:$action by $actor on $target"
    }
    
    data class ContentContext(val type: String, val assetName: String, val data: ByteArray) : DataContext {
        override fun identifier() = "Asset:$assetName [$type]"
        override fun data() = data
    }
    
    /**
     * Safety Rule Interface
     */
    interface ISafetyRule<T : SafetyContext> {
        fun name(): String
        fun check(context: T): SafetyResult
        
        @Suppress("UNCHECKED_CAST")
        fun evaluateUnsafe(context: SafetyContext): SafetyResult {
            return try {
                check(context as T)
            } catch (e: ClassCastException) {
                SafetyResult.ALLOW
            }
        }
    }
    
    private val rules = mutableListOf<ISafetyRule<*>>()
    
    init {
        // Initialize all 11 rules
        rules.add(FileIntegrityRule())
        rules.add(SexualContentRule())
        rules.add(MinorContactRule())
        rules.add(GroomingPatternRule())
        rules.add(NativeModRule())
        rules.add(AntiCheatRule())
        rules.add(IdentityFraudRule())
        rules.add(GriefingPatternRule())
        rules.add(HarassmentRule())
        rules.add(ChatGuardianRule())
        rules.add(IGDRule())
        
        println("[SafetyGuardian] Initialized with ${rules.size} rules active")
    }
    
    fun validate(context: SafetyContext): SafetyResult {
        var worstCase = SafetyResult.ALLOW
        
        for (rule in rules) {
            val result = rule.evaluateUnsafe(context)
            if (result.decision.ordinal > worstCase.decision.ordinal) {
                worstCase = result
                logViolation(rule.name(), context, result)
                if (worstCase.decision == SafetyResult.Decision.BLOCK) break
            }
        }
        
        return worstCase
    }
    
    fun validateAsync(context: SafetyContext): CompletableFuture<SafetyResult> {
        return CompletableFuture.completedFuture(validate(context))
    }
    
    private fun logViolation(rule: String, ctx: SafetyContext, res: SafetyResult) {
        if (res.decision == SafetyResult.Decision.BLOCK) {
            LOGGER.severe("[FORT-KNOX] ${res.decision} → $rule triggered by ${ctx.identifier()}")
        }
    }
}
