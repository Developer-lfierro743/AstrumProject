package com.novusforge.astrum.game;

import com.novusforge.astrum.core.*;
import com.novusforge.astrum.engine.*;
import com.novusforge.astrum.world.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Project Astrum - Pre-Classic (Cave Game)
 * Main Entry Point with Account System and IIV Verification
 * 
 * Flow:
 * 1. Account System (Login/Register) OR Developer Mode bypass
 * 2. IIV Questionnaire (Identity Intent Verification) OR Developer bypass
 * 3. SafetyGuardian Initialization (11 rules)
 * 4. Launch Game
 * 
 * DEVELOPER MODE:
 * Set AstrumConstants.DEVELOPER_MODE = true to enable:
 * - Skip Account System (auto-login as DevPlayer)
 * - Skip IIV Questionnaire (auto-pass)
 * - Debug logging enabled
 * 
 * PRODUCTION MODE:
 * Set AstrumConstants.DEVELOPER_MODE = false for:
 * - Full Account System GUI required
 * - IIV Questionnaire MUST be completed
 * - Minimal logging
 */
public class Main {
    
    private static final String TITLE = AstrumConstants.GAME_TITLE;
    private static final int TARGET_FPS = AstrumConstants.TARGET_FPS;
    private static final long FRAME_TIME = 1_000_000_000 / TARGET_FPS;
    private static final String IIV_FILE = AstrumConstants.IIV_FILE;

    public static void main(String[] args) {
        // Print banner
        AstrumConstants.printBanner();
        
        // Print developer warning if enabled
        if (AstrumConstants.DEVELOPER_MODE) {
            AstrumConstants.printDevWarning();
        }
        
        // Print experimental features status
        AstrumConstants.printExperimentalStatus();
        System.out.println();

        // Step 1: Account System Check or Launch
        checkAccountSession();

        // Step 2: IIV Check or Launch
        checkIdentityVerification();

        // Step 3: SafetyGuardian Init
        SafetyGuardian guardian = new SafetyGuardian();
        SafetyGuardian.ActionContext startupContext = new SafetyGuardian.ActionContext("startup", "system", "engine");
        if (guardian.validate(startupContext).decision() == SafetyGuardian.SafetyResult.Decision.BLOCK) {
            System.err.println("CRITICAL: SafetyGuardian blocked startup. Engine halted.");
            return;
        }
        System.out.println("  Player         : " + SessionManager.getUsername());
        System.out.println("  SafetyGuardian : ONLINE (" + AstrumConstants.SAFETY_RULE_COUNT + " rules active)");
        System.out.println("  IIV Framework  : READY");
        System.out.println("=".repeat(50));
        System.out.println();

        // Step 4: Launch Game
        IRenderer renderer = new OpenGLRenderer();  // Use OpenGL (simpler!)
        Game game = null;
        
        try {
            // Initialize renderer
            if (!renderer.init()) {
                System.err.println("[ERROR] Renderer initialization failed!");
                System.exit(1);
            }
            
            // Create game
            game = new Game(renderer.getWindow(), renderer);
            game.init();
            
            // Set buffer deleter callback
            World.setBufferDeleter(renderer::deleteBuffer);
            
            System.out.println();
            System.out.println("[Render] " + renderer.getRendererName() + " initialized!");
            System.out.println("[Game] Astrum Pre-Classic loaded!");
            System.out.println("[Controls] WASD = Move, Space = Jump");
            System.out.println("[Controls] Mouse = Look, Click = Lock");
            System.out.println("[Controls] L-Click = Break, R-Click = Place");
            System.out.println();

            // Game Loop
            long lastTime = System.nanoTime();
            int frameCount = 0;
            long fpsTimer = System.currentTimeMillis();
            
            Matrix4f projectionMatrix = new Matrix4f();
            Matrix4f viewMatrix = new Matrix4f();
            
            while (!renderer.windowShouldClose() && game.isRunning()) {
                glfwPollEvents();
                
                long currentTime = System.nanoTime();
                float deltaTime = (currentTime - lastTime) / 1_000_000_000f;
                lastTime = currentTime;
                
                // Cap delta time
                deltaTime = Math.min(deltaTime, 0.1f);
                
                // Update
                game.update(deltaTime);
                
                // Update matrices
                projectionMatrix.setPerspective(
                    (float) Math.toRadians(70.0f), 
                    renderer.getAspectRatio(), 
                    0.1f, 
                    1000.0f, 
                    true
                );
                game.getInput().getViewMatrix(viewMatrix);
                Vector3f pos = game.getPlayer().getPosition();
                viewMatrix.translate(-pos.x, -pos.y, -pos.z);
                
                // Get visible meshes
                java.util.Map<Long, ChunkMesh> visibleMeshes =
                    game.getWorld().getVisibleMeshes(pos.x, pos.y, pos.z, null);
                
                // Render
                renderer.render(viewMatrix, projectionMatrix, visibleMeshes);
                
                // FPS counter
                frameCount++;
                if (System.currentTimeMillis() - fpsTimer >= 1000) {
                    int chunks = game.getWorld().getLoadedChunkCount();
                    System.out.printf("[FPS] %d | [Chunks] %d | [Pos] %.1f, %.1f, %.1f%n",
                        frameCount, chunks, pos.x, pos.y, pos.z);
                    frameCount = 0;
                    fpsTimer = System.currentTimeMillis();
                }
                
                // Frame timing
                long elapsed = System.nanoTime() - currentTime;
                if (elapsed < FRAME_TIME) {
                    try {
                        Thread.sleep((FRAME_TIME - elapsed) / 1_000_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            System.out.println("[Shutdown] Closing Astrum...");
            
        } finally {
            if (game != null) game.cleanup();
            if (renderer != null) renderer.cleanup();
            System.out.println("[Shutdown] Done!");
        }
    }

    /**
     * Step 1: Account System - Login/Register or Developer Mode bypass
     */
    private static void checkAccountSession() {
        // Check if session already exists
        if (SessionManager.isLoggedIn()) {
            System.out.println("Account: Session found. Logged in as " + SessionManager.getUsername());
            return;
        }

        // DEVELOPER MODE: Auto-login
        if (AstrumConstants.SKIP_ACCOUNT_SYSTEM) {
            System.out.println("Account: DEVELOPER MODE - Skipping Account System GUI");
            SessionManager.saveSession(AstrumConstants.DEV_USERNAME, AstrumConstants.DEV_AVATAR_ID);
            System.out.println("Account: Auto-logged in as " + AstrumConstants.DEV_USERNAME);
            return;
        }

        // PRODUCTION MODE: Launch Account System GUI
        try {
            System.out.println("Account: Launching Account System...");
            SwingUtilities.invokeAndWait(() -> {
                new AccountSystem();
            });

            if (!SessionManager.isLoggedIn()) {
                System.err.println("Account: Login/Registration incomplete. Exiting.");
                System.exit(1);
            }
            System.out.println("Account: Login successful. Welcome " + SessionManager.getUsername());
        } catch (java.awt.AWTError e) {
            System.err.println("Account: X11 connection failed. Using developer account...");
            SessionManager.saveSession(AstrumConstants.DEV_USERNAME, AstrumConstants.DEV_AVATAR_ID);
            System.out.println("Account: Logged in as " + AstrumConstants.DEV_USERNAME);
        } catch (Exception e) {
            System.err.println("Account: Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Step 2: IIV Questionnaire - Identity Intent Verification or Developer bypass
     */
    private static void checkIdentityVerification() {
        File file = new File(IIV_FILE);

        // Check if already verified
        if (file.exists()) {
            try {
                // Try reading as plain text first (dev bypass)
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                if (content.contains("ALLOW")) {
                    System.out.println("IIV: Verification found. Proceeding.");
                    return;
                } else if (content.contains("BLOCK")) {
                    System.err.println("CRITICAL: Entry denied by Identity Intent Verification.");
                    System.exit(0);
                } else if (content.contains("WARN")) {
                    System.out.println("IIV: Previous verification found (warned). Proceeding.");
                    return;
                }
                
                // Try deserializing as IIVResult object
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    Object result = ois.readObject();
                    String decision = result.toString();

                    if (decision.contains("BLOCK")) {
                        System.err.println("CRITICAL: Entry denied by Identity Intent Verification.");
                        System.exit(0);
                    }
                    System.out.println("IIV: Previous verification found. Proceeding.");
                    return;
                }
            } catch (Exception e) {
                System.out.println("IIV: Previous verification corrupted. Re-running...");
                file.delete();
            }
        }

        // DEVELOPER MODE: Auto-pass IIV
        if (AstrumConstants.SKIP_IIV_QUESTIONNAIRE) {
            System.out.println("IIV: DEVELOPER MODE - Skipping Questionnaire");
            System.out.println("IIV: Auto-passed with decision: " + AstrumConstants.DEV_IIV_DECISION);
            // Create bypass file
            try {
                java.nio.file.Files.write(file.toPath(), AstrumConstants.DEV_IIV_DECISION.getBytes());
            } catch (Exception e) {
                System.err.println("IIV: Warning - Could not create bypass file");
            }
            return;
        }

        // PRODUCTION MODE: Launch IIV Questionnaire GUI - MUST complete
        try {
            System.out.println("IIV: Launching Identity Intent Verification...");
            System.out.println("IIV: Please complete all 12 questions.");
            System.out.println("IIV: You MUST complete the questionnaire to proceed.");
            
            // Create and show questionnaire
            SwingUtilities.invokeAndWait(() -> {
                IIVQuestionnaire questionnaire = new IIVQuestionnaire();
                questionnaire.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            });

            // Wait a moment for file to be saved
            Thread.sleep(500);

            // Verify result was saved - MUST exist
            if (!file.exists()) {
                System.err.println("IIV: ERROR - Verification file not created!");
                System.err.println("IIV: The questionnaire was not completed.");
                System.err.println("IIV: You MUST complete all 12 questions to proceed.");
                System.exit(1);
            }

            // Read and verify result
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Object result = ois.readObject();
                String decision = result.toString();
                
                if (decision.contains("BLOCK")) {
                    System.err.println("CRITICAL: Entry denied by Identity Intent Verification.");
                    System.err.println("IIV: Your responses indicate behavior patterns incompatible with Astrum community standards.");
                    System.exit(0);
                } else if (decision.contains("WARN")) {
                    System.out.println("IIV: Verification completed with warnings. You will be monitored.");
                } else {
                    System.out.println("IIV: Verification PASSED. Welcome to Astrum!");
                }
            }
            
        } catch (java.awt.AWTError e) {
            System.err.println("IIV: X11 connection failed. Cannot run verification.");
            System.err.println("IIV: Using developer bypass...");
            SessionManager.saveSession(AstrumConstants.DEV_USERNAME, AstrumConstants.DEV_AVATAR_ID);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("IIV: Interrupted. Exiting.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("IIV: Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
