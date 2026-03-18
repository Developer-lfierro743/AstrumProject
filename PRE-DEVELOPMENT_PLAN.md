# Pre-development Plan for Project Astrum (Astrum)

## Stage: Pre-development
**Goal:** Complete all planning before writing production code  
**Target:** Move to Pre-classic (Cave Game) stage  
**Estimated Timeline:** 2026-2027

---

## 1. Game Design Document (GDD)

### 1.1 Core Concept
- **Name:** Astrum (public) / Project Astrum (internal)
- **Studio:** Novusforge Studios
- **Elevator Pitch:** "A reclaim of the sandbox vision. Independent, Resilient, and Secure."
- **Ambition:** More ambitious than Notch's original Minecraft vision + beat Hytale

### 1.2 Genre Pillars
| Pillar | Description |
|--------|-------------|
| Sandbox | Voxel building, infinite worlds, no world border, no height limit |
| RPG | Tiers (Basic/Advanced/Overpowered), weapons, stats, 4x4 crafting grid |
| Competitive | 100-player events, The Purge, PvP |

### 1.3 Unique Selling Points
- Greenstone (redstone counterpart)
- Infernite (netherite counterpart)
- Sky Dimension (abandoned Notch feature)
- SafetyGuardian (engine-level moderation)
- Built-in modding API (no breaking updates)
- No GaaS, one-time purchase $16.99
- Scientific ore names
- Matte-vibrant art style
- Voice chat in web version

---

## 2. Technical Specification

### 2.1 Tech Stack (LOCKED)
| Component | Choice | Reason |
|-----------|--------|--------|
| Language | Java 21+ | Virtual Threads (Loom), Panama API |
| Graphics | Vulkan + WebGPU | GPU-driven rendering |
| Framework | LWJGL3 | Raw, no abstraction overhead |
| Architecture | ECS | Entity Component System |
| Build | Gradle 9+ | Multiproject |
| IDE | vscode.dev (web) | Accessibility |
| Noise | FastNoiseLite | Perlin + Simplex |

### 2.2 Performance Targets
| Hardware | Performance |
|----------|--------------|
| Intel i3-5005U (IGPU) | Minimum playable |
| School Chromebooks (Celeron) | Web version works |
| Threadripper + RTX | Maximum quality |

### 2.3 Module Architecture
```
astrum-api      - Modding API
astrum-core     - ECS, The Purge logic
astrum-engine   - Render Bridge (Vulkan/WebGPU)
astrum-security - SafetyGuardian
astrum-world    - Procedural generation
astrum-web      - TeaVM/WASM bridge
astrum-game     - Game logic
```

---

## 3. Asset Planning

### 3.1 Art Style
- **Name:** Matte-Vibrant
- **No textures** - procedural/colored
- **Colors:** Bright, vibrant, NOT muted (unlike Mojang)
- **Lighting:** SSAO, reflections, cutting-edge shaders
- **Animations:** Custom from scratch (not stiff like Minecraft)

### 3.2 Block/Item Registry (Planned)
| Block Type | Scientific Name | Tier |
|------------|-----------------|------|
| Coal Ore | Carbonite | Basic |
| Iron Ore | Ferros | Basic |
| Gold Ore | Aurum | Advanced |
| Diamond Ore | Carbon Crystallis | Overpowered |
| Greenstone | - | Advanced (redstone counterpart) |
| Infernite | - | Overpowered (netherite counterpart) |

---

## 4. Feature Backlog

### 4.1 MVP Features (Pre-classic to Classic)
- [ ] 3D movement (WASD + mouse look)
- [ ] Block placement/destruction
- [ ] 32x32x32 chunk system
- [ ] Greedy meshing
- [ ] Frustum culling
- [ ] Basic procedural terrain

### 4.2 Indev Features
- [ ] Inventory (50 slots + 11 quickbar)
- [ ] Crafting system (4x4 grid)
- [ ] Day/night cycle
- [ ] Mobs (hostile/passive)
- [ ] Multiplayer (TCP/UDP/WebRTC)
- [ ] World saving/loading

### 4.3 Alpha/Beta Features
- [ ] Greenstone
- [ ] Infernite
- [ ] The Purge event
- [ ] Sky Dimension
- [ ] Better AI
- [ ] Self-Sustaining Villages
- [ ] SafetyGuardian (full implementation)

### 4.4 Post-1.0 Ideas
- [ ] Marketplace (free, no microtransactions)
- [ ] Skin editor
- [ ] Hardcore mode
- [ ] Vertical slabs

---

## 5. Safety & Moderation

### 5.1 SafetyGuardian Implementation
- Hardcoded rules (NOT AI/ML)
- Age verification layers
- CSAM prevention
- Grooming detection
- Blocks: Jenny Mod, old MC mods, unauthorized assets

### 5.2 SafetyRules Structure
```java
SexualContentRule  - Anti-CSAM/adult content
ModSafetyRule      - Native mods only (no Forge/Fabric/Neoforge)
ChatSafetyRule     - Anti-grooming, profanity filter (optional)
AssetRule          - JSON verification, image blocking
```

---

## 6. Multiplayer Protocol Stack

### 6.1 Supported Protocols
| Protocol | Use Case |
|----------|----------|
| WebSocket (WS) | Web clients |
| WebSocket Secure (WSS) | Secure web |
| TCP/IP | Desktop servers |
| UDP | Fast gameplay |
| WebRTC | Universal LAN (anywhere) |

### 6.2 Server Software
- Custom server software (not PaperMC/Purpur)
- Event modes (Hunger Games, Battle Royale, Purge, Civilization)
- Stress test with YouTuber collaborations

---

## 7. Beta Testing Network

### 7.1 Target YouTubers
| Creator | Language | Role |
|---------|----------|------|
| @MindofNeo | EN | Event content |
| @Sword4000 | EN+ES | Purge expert |
| @Nyxlunarii | EN | Event content |
| @Pharolen | EN | Event content |
| @bobicraft | ES | Spanish audience |
| @MinecraftCurios | EN | Roleplay/skits |

### 7.2 Community Building
- Discord server (Lunar Events partnership)
- 100-player survival simulations as marketing
- English + Spanish bilingual community

---

## 8. Legal & Business

### 8.1 IP Independence
- 100% original code (clean-room design)
- Original assets only
- Custom auth schema (NOT Azure AD)
- Legal shield against DMCA

### 8.2 Monetization
- **Price:** $16.99 USD
- **Free during:** Pre-classic → Classic → Indev (alpha)
- **Paid starting:** Indev stage
- No microtransactions
- Free marketplace

---

## 9. Pre-development Checklist

### Completed ✅
- [x] Project structure (Gradle multiproject)
- [x] Basic Main.java entry point
- [x] ECS skeleton
- [x] SafetyGuardian skeleton
- [x] Chunk class (32x32x32, short[] blocks)
- [x] GreedyMesher concept
- [x] FrustumCuller concept
- [x] FastNoiseLite integration
- [x] README.md
- [x] Formulas documentation
- [x] **Vulkan Renderer** (full pipeline with swap chain)
- [x] **LWJGL 3.4.1** upgrade
- [x] **InputManager** (keyboard, mouse, touch controls)
- [x] **Player class** (first-person movement, jumping, gravity)
- [x] **Game class** (integrates player, world, input)
- [x] **World/ChunkManager** (infinite terrain, procedural generation)

### In Progress 🔄
- [ ] Chunk mesh rendering to GPU (Vulkan buffers)
- [ ] First-person camera matrix integration
- [ ] Block raycasting (placement/destruction)

### Not Started ⬜
- [ ] Complete spec.md
- [ ] Block registry design
- [ ] Art style/color palette definition
- [ ] Game Design Document (formal)
- [ ] Animation system design
- [ ] Inventory UI design
- [ ] Crafting system design
- [ ] Mob AI system design
- [ ] Server software design
- [ ] Web version architecture
- [ ] SafetyGuardian full implementation
- [ ] YouTuber outreach plan
- [ ] Community guidelines

---

## 10. Exit Criteria (Pre-development → Pre-classic)

To move to Pre-classic stage, you must have:
1. ✅ Runnable "Cave Game" demo (move, look, place blocks)
2. ✅ Basic chunk rendering
3. ✅ Simple terrain generation
4. ✅ Working Vulkan/WebGPU window
5. ✅ Complete Game Design Document
6. ✅ Defined block/item registry
7. ✅ Art style guide finalized

---

## 11. Milestone Timeline

| Year | Quarter | Goal |
|------|---------|------|
| 2025 | Q4 | Finish Pre-development |
| 2026 | Q1 | Pre-classic (Cave Game) playable |
| 2026 | Q2 | Classic (multiplayer, crafting) |
| 2026 | Q3 | Indev (PAID early access) |
| 2026 | Q4 | Infdev (multiplayer stress tests) |
| 2027 | Q1 | Alpha (all features) |
| 2027 | Q2 | Beta (polish, bug fixes) |
| 2027 | Q3 | QA phase |
| 2027 | Q4 | **v1.0 RELEASE** |

---

*"Novusforge Studios: Building the Future of the Sandbox."*
