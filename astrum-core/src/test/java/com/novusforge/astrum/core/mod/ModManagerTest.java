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

import com.novusforge.astrum.core.ecs.World;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ModManagerTest {

    @Test
    public void testModRegistration() {
        World world = new World();
        ModManager modManager = new ModManager(world);
        
        SampleMod mod = new SampleMod();
        modManager.registerMod(mod);
        
        // Create an entity and try adding the modded component
        int entityId = world.createEntity();
        SampleMod.ModdedComponent modComp = new SampleMod.ModdedComponent();
        world.addComponent(entityId, modComp);
        
        SampleMod.ModdedComponent retrieved = world.getComponent(entityId, SampleMod.ModdedComponent.class);
        assertNotNull(retrieved);
        assertEquals("Modded Data", retrieved.data);

        // Verify System registration and execution
        assertEquals(0, mod.systemTickCount);
        world.tick(1.0f / 60.0f);
        assertEquals(1, mod.systemTickCount);
        world.tick(1.0f / 60.0f);
        assertEquals(2, mod.systemTickCount);
    }
}
