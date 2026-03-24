package com.novusforge.astrum.core

import java.io.*
import java.nio.file.Files
import java.nio.file.Paths

/**
 * SessionManager - Manages user login sessions
 * Kotlin conversion: Uses data class, object singleton, scope functions
 */
object SessionManager {
    
    private val SESSION_FILE = "${System.getProperty("user.home")}/astrum_session.dat"
    private var currentSession: SessionData? = null
    
    init {
        loadSession()
    }
    
    private data class SessionData(val username: String, val avatarId: Int) : Serializable
    
    fun getUsername(): String = currentSession?.username ?: "Guest"
    
    fun getAvatarId(): Int = currentSession?.avatarId ?: 0
    
    fun isLoggedIn(): Boolean = currentSession != null
    
    fun saveSession(username: String, avatarId: Int) {
        currentSession = SessionData(username, avatarId)
        try {
            ObjectOutputStream(FileOutputStream(SESSION_FILE)).use { oos ->
                oos.writeObject(currentSession)
            }
        } catch (e: IOException) {
            System.err.println("[Session] Failed to save session: ${e.message}")
        }
    }
    
    fun loadSession() {
        val path = Paths.get(SESSION_FILE)
        if (Files.exists(path)) {
            try {
                ObjectInputStream(FileInputStream(SESSION_FILE)).use { ois ->
                    currentSession = ois.readObject() as? SessionData
                }
            } catch (e: Exception) {
                System.err.println("[Session] Failed to load session: ${e.message}")
                clearSession()
            }
        }
    }
    
    fun clearSession() {
        currentSession = null
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE))
        } catch (e: IOException) {
            System.err.println("[Session] Failed to delete session file: ${e.message}")
        }
    }
}
