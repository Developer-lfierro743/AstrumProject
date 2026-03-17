package com.novusforge.astrum.security.guardian;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Asset Scanner for SafetyGuardian.
 * Scans mod assets for prohibited content before loading.
 */
public class AssetScanner {
    
    private static final Set<String> FORBIDDEN_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".exe", ".dll", ".bat", ".sh", ".cmd", ".ps1"
    ));
    
    private static final Set<String> SCANNABLE_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".png", ".jpg", ".jpeg", ".gif", ".ogg", ".wav", ".mp3",
        ".json", ".xml", ".txt", ".lang", ".obj", ".mtl"
    ));
    
    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024;
    private static final int CHUNK_SIZE = 8192;
    
    private final List<String> scanLog = new ArrayList<>();
    private int scannedCount = 0;
    private int blockedCount = 0;
    
    public AssetScanner() {}
    
    public boolean scanModAssets(Path modPath) {
        if (!Files.exists(modPath) || !Files.isDirectory(modPath)) {
            return false;
        }
        
        try {
            Files.walk(modPath)
                .filter(Files::isRegularFile)
                .forEach(this::scanFile);
        } catch (IOException e) {
            System.err.println("AssetScanner: Error walking directory: " + e.getMessage());
            return false;
        }
        
        return blockedCount == 0;
    }
    
    private void scanFile(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        String extension = getExtension(fileName);
        
        scannedCount++;
        
        if (FORBIDDEN_EXTENSIONS.contains(extension)) {
            log("BLOCKED: Forbidden file type: " + filePath);
            blockedCount++;
            return;
        }
        
        if (!SCANNABLE_EXTENSIONS.contains(extension)) {
            return;
        }
        
        try {
            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                log("WARNING: Large file detected: " + filePath + " (" + fileSize + " bytes)");
            }
            
            if (isImageFile(extension)) {
                scanImageFile(filePath);
            } else if (isTextFile(extension)) {
                scanTextFile(filePath);
            }
            
        } catch (IOException e) {
            log("ERROR: Could not scan file: " + filePath + " - " + e.getMessage());
        }
    }
    
    private void scanImageFile(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] header = new byte[16];
            int bytesRead = fis.read(header);
            
            if (bytesRead < 8) return;
            
            if (isPNG(header)) {
                if (!validatePNGHeader(header)) {
                    log("SUSPICIOUS: PNG with unusual headers: " + filePath);
                }
            } else if (isJPEG(header)) {
                // JPEG validation
            } else {
                log("SUSPICIOUS: Image with unknown format: " + filePath);
            }
            
        } catch (IOException e) {
            log("ERROR reading image: " + filePath);
        }
    }
    
    private void scanTextFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            content = content.toLowerCase();
            
            if (containsForbiddenKeywords(content)) {
                log("BLOCKED: Text contains forbidden keywords: " + filePath);
                blockedCount++;
            }
            
        } catch (IOException e) {
            // Binary or unreadable file, skip
        }
    }
    
    private boolean containsForbiddenKeywords(String text) {
        String[] forbidden = {"nsfw", "sex", "nude", "naked", "explicit", "porn", "xxx"};
        for (String keyword : forbidden) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isImageFile(String extension) {
        return extension.equals(".png") || extension.equals(".jpg") || 
               extension.equals(".jpeg") || extension.equals(".gif");
    }
    
    private boolean isTextFile(String extension) {
        return extension.equals(".json") || extension.equals(".xml") || 
               extension.equals(".txt") || extension.equals(".lang");
    }
    
    private boolean isPNG(byte[] header) {
        return header.length >= 8 && 
               header[0] == (byte)0x89 && 
               header[1] == 'P' && 
               header[2] == 'N' && 
               header[3] == 'G';
    }
    
    private boolean isJPEG(byte[] header) {
        return header.length >= 2 && 
               header[0] == (byte)0xFF && 
               header[1] == (byte)0xD8;
    }
    
    private boolean validatePNGHeader(byte[] header) {
        return header.length >= 8;
    }
    
    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return "";
    }
    
    private void log(String message) {
        scanLog.add(message);
        System.out.println("AssetScanner: " + message);
    }
    
    public List<String> getScanLog() {
        return new ArrayList<>(scanLog);
    }
    
    public int getScannedCount() {
        return scannedCount;
    }
    
    public int getBlockedCount() {
        return blockedCount;
    }
    
    public void reset() {
        scanLog.clear();
        scannedCount = 0;
        blockedCount = 0;
    }
}
