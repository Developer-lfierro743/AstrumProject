package com.novusforge.astrum.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stub: Hash utilities for password hashing
 * Will be expanded later
 */
public final class HashUtils {
    
    private HashUtils() {}
    
    /**
     * Compute SHA-256 hash of data
     */
    public static String computeSHA256(byte[] data) {
        if (data == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
