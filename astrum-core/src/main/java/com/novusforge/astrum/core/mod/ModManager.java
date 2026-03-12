/*
 * Copyright (c) 2026 NovusForge Project Astrum. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.novusforge.astrum.core.mod;

import com.novusforge.astrum.api.Mod;
import com.novusforge.astrum.api.ModContext;
import com.novusforge.astrum.api.ModMetadata;
import com.novusforge.astrum.core.ecs.World;
import com.novusforge.astrum.api.ecs.Component;
import com.novusforge.astrum.api.ecs.ECSSystem;
import com.novusforge.astrum.security.guardian.SafetyGuardian;
import com.novusforge.astrum.security.guardian.AssetScanner;
import com.novusforge.astrum.security.integrity.IntegrityVerifier;
import com.novusforge.astrum.security.audit.SecurityAudit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages mod discovery and lifecycle.
 * Central coordinator for Project Astrum modding.
 */
public class ModManager implements ModContext {

    private final World ecsWorld;
    private final List<Mod> activeMods = new ArrayList<>();
    private final AssetScanner assetScanner;
    
    // Simple block registry for modded blocks
    private final Map<String, Short> modBlocks = new HashMap<>();
    private short nextModBlockId = 1000; // Offset for mod blocks (The Formula Part 5)

    public ModManager(World ecsWorld) {
        this.ecsWorld = ecsWorld;
        this.assetScanner = new AssetScanner(SafetyGuardian.getInstance());
    }

    /**
     * Manually registers a mod (Indev method).
     * @param mod The mod instance.
     */
    public void registerMod(Mod mod) {
        // Default Indev call with placeholder signature for backward compatibility
        registerMod(mod, "OFFICIAL_MOD_SIG");
    }

    /**
     * Registers a mod with full verification (Indev method).
     * @param mod The mod instance.
     * @param signature The mod signature for integrity check.
     */
    public void registerMod(Mod mod, String signature) {
        ModMetadata meta = mod.getMetadata();
        
        // 1. Scan Metadata for Safety
        if (!assetScanner.scanModMetadata(meta)) {
            SecurityAudit.logViolation("INTEGRITY", "MOD_METADATA_SPOOFING", 
                "Rejected mod due to unsafe metadata: " + meta.id());
            return;
        }

        // 2. Verify Cryptographic Integrity
        if (!IntegrityVerifier.verifyModSignature(new byte[0], signature)) {
            SecurityAudit.logViolation("INTEGRITY", "MOD_SIGNATURE_FORGERY", 
                "Mod signature verification failed for: " + meta.id());
            return;
        }

        System.out.println("[ModManager] Verified and Loading Mod: " + meta.name() + " (v" + meta.version() + ")");
        
        mod.onInitialize(this);
        activeMods.add(mod);
    }

    @Override
    public short registerBlock(String name) {
        if (modBlocks.containsKey(name)) {
            return modBlocks.get(name);
        }
        short id = nextModBlockId++;
        modBlocks.put(name, id);
        System.out.println("[ModManager] Registered Block: " + name + " -> ID: " + id);
        return id;
    }

    @Override
    public <T extends Component> void registerComponent(Class<T> type) {
        ecsWorld.registerComponent(type);
        System.out.println("[ModManager] Registered Component: " + type.getSimpleName());
    }

    @Override
    public void registerSystem(ECSSystem system) {
        ecsWorld.registerSystem(system);
        System.out.println("[ModManager] Registered System logic into ECS World.");
    }
}
