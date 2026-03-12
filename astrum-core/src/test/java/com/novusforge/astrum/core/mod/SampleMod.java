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
import com.novusforge.astrum.api.ecs.Component;
import com.novusforge.astrum.api.ecs.ECSSystem;

/**
 * A sample mod used to verify the modding API.
 */
public class SampleMod implements Mod {

    private final ModMetadata metadata = new ModMetadata("sample_mod", "Sample Mod", "1.0.0");
    public int systemTickCount = 0;

    @Override
    public void onInitialize(ModContext context) {
        // Register a modded component
        context.registerComponent(ModdedComponent.class);
        
        // Register a modded block
        short blockId = context.registerBlock("modded_block");
        System.out.println("[SampleMod] Registered Block ID: " + blockId);

        // Register a modded system
        context.registerSystem(dt -> {
            systemTickCount++;
        });
    }

    @Override
    public ModMetadata getMetadata() {
        return metadata;
    }

    public static class ModdedComponent implements Component {
        public String data = "Modded Data";
    }
}
