package com.novusforge.astrum.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SessionManager {
    private static final String SESSION_FILE = System.getProperty("user.home") + "/astrum_session.dat";
    private static SessionData currentSession;

    static {
        loadSession();
    }

    private record SessionData(String username, int avatarId) implements Serializable {}

    public static String getUsername() {
        return currentSession != null ? currentSession.username : "Guest";
    }

    public static int getAvatarId() {
        return currentSession != null ? currentSession.avatarId : 0;
    }

    public static boolean isLoggedIn() {
        return currentSession != null;
    }

    public static void saveSession(String username, int avatarId) {
        currentSession = new SessionData(username, avatarId);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SESSION_FILE))) {
            oos.writeObject(currentSession);
        } catch (IOException e) {
            System.err.println("[Session] Failed to save session: " + e.getMessage());
        }
    }

    public static void loadSession() {
        Path path = Paths.get(SESSION_FILE);
        if (Files.exists(path)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SESSION_FILE))) {
                currentSession = (SessionData) ois.readObject();
            } catch (Exception e) {
                System.err.println("[Session] Failed to load session: " + e.getMessage());
                clearSession();
            }
        }
    }

    public static void clearSession() {
        currentSession = null;
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
        } catch (IOException e) {
            System.err.println("[Session] Failed to delete session file: " + e.getMessage());
        }
    }
}
