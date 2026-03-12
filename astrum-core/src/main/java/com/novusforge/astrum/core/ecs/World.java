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

package com.novusforge.astrum.core.ecs;

import com.novusforge.astrum.api.ecs.Component;
import com.novusforge.astrum.api.ecs.ECSSystem;
import java.util.*;

/**
 * The World manager orchestrates entities, components, and systems.
 * Uses BitSets for high-performance entity filtering (Formula Part 1).
 */
public class World {
    private static final int MAX_ENTITIES = 100_000;
    private int entityCount = 0;
    
    private final Map<Class<? extends Component>, Integer> componentIds = new HashMap<>();
    private final Map<Integer, Component[]> componentStores = new HashMap<>();
    private final BitSet[] entityMasks = new BitSet[MAX_ENTITIES];
    private final List<ECSSystem> registeredSystems = new ArrayList<>();

    public World() {
        for (int i = 0; i < MAX_ENTITIES; i++) {
            entityMasks[i] = new BitSet();
        }
    }

    /**
     * Ticks the world's simulation.
     * @param deltaTime The time since the last tick (in seconds).
     */
    public void tick(float deltaTime) {
        // Execute all registered systems in order
        for (ECSSystem system : registeredSystems) {
            system.update(deltaTime);
        }
    }

    /**
     * Registers a new system logic into the world.
     * @param system The system to run every tick.
     */
    public void registerSystem(ECSSystem system) {
        registeredSystems.add(system);
    }

    public int createEntity() {
        return entityCount++;
    }

    public <T extends Component> void registerComponent(Class<T> type) {
        int id = componentIds.size();
        componentIds.put(type, id);
        componentStores.put(id, new Component[MAX_ENTITIES]);
    }

    public <T extends Component> void addComponent(int entityId, T component) {
        int compId = componentIds.get(component.getClass());
        componentStores.get(compId)[entityId] = component;
        entityMasks[entityId].set(compId);
    }

    public <T extends Component> T getComponent(int entityId, Class<T> type) {
        int compId = componentIds.get(type);
        return type.cast(componentStores.get(compId)[entityId]);
    }

    public List<Integer> query(BitSet requiredComponents) {
        List<Integer> results = new ArrayList<>();
        int requiredCount = requiredComponents.cardinality();
        
        for (int i = 0; i < entityCount; i++) {
            BitSet entityMask = entityMasks[i];
            
            // Fast check: bitwise AND without cloning
            // A contains B if (A & B) == B
            boolean matches = true;
            for (int bit = requiredComponents.nextSetBit(0); bit >= 0; bit = requiredComponents.nextSetBit(bit + 1)) {
                if (!entityMask.get(bit)) {
                    matches = false;
                    break;
                }
            }
            
            if (matches) {
                results.add(i);
            }
        }
        return results;
    }
    
    @SafeVarargs
    public final BitSet createMask(Class<? extends Component>... components) {
        BitSet mask = new BitSet();
        for (Class<? extends Component> c : components) {
            mask.set(componentIds.get(c));
        }
        return mask;
    }
}
