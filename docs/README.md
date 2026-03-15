# Project Astrum (The Formula)

**"More ambitious than the current Minecraft. More independent than Hytale."**

Project Astrum is a next-generation sandbox engine and game built from the ground up to reclaim the original ambitious vision of the voxel genre. Designed in 2026, it addresses the technical debt of "spaghetti OOP code" and the corporate stagnation of the "Games as a Service" (GaaS) model.

## 🌌 The Vision
Project Astrum is built on **The Formula**—a technical and conceptual blueprint designed to deliver unlimited freedom, cutting-edge performance, and a safe, ethical environment for the next generation of players.

### Core Pillars:
*   **Performance First**: Built with an **Entity Component System (ECS)** and **Vulkan** to utilize modern multi-core CPUs (Threadrippers) and RTX GPUs.
*   **Unlimited Scale**: No world borders. No building height limits. Infinite possibilities.
*   **The Purge**: Integrated support for massive 100-player social experiments and simulations, inspired by the 2024-2025 "Social Experiment" meta.
*   **SafetyGuardian**: A hardcoded, multi-layered security system (`SafetyGuardian.java`) designed to protect minors and prevent harmful content at the engine level.
*   **Universal Access**: Play anywhere. Native desktop performance meets **Instant Web Play** via WASM-GC and WebGPU.

---

## 🛠 Tech Stack (2026 Baseline)
*   **Language**: Java 21+ (utilizing modern Virtual Threads and Paname API).
*   **Build System**: Gradle 9.3.1 (Multiproject architecture).
*   **Graphics API**: Vulkan (via RAW LWJGL3).
*   **Architecture**: High-performance ECS (verified for 100k+ entities at 60FPS).
*   **World Gen**: Perlin + Simplex noise using a localized, zero-overhead **FastNoiseLite** implementation.
*   **Web Support**: TeaVM 0.13.0 (WASM-GC / WebGPU targets).

---

## ✨ Key Features (The Formula)
- **Non-Scientific Ores**: Replaced with real scientific names (e.g., *Ferrous*).
- **Greenstone**: The high-performance counterpart to Redstone.
- **Vibrant Aesthetics**: Matte vibrant colors designed to outshine muted palettes.
- **Tiered Progression**: Circular gameplay loops (Basic → Advanced → Overpowered).
- **Baked-in Modding API**: An API designed for **universal unification**—no breaking updates, ever.
- **SafetyGuardian**: Engine-level protection against CSAM, grooming, and harmful mods.

---

## 🚀 Development Status: INDEV
We are currently in the **Pre-development / Foundation** stage.

### Current Progress:
- [x] **Gradle Multiproject Infrastructure**: All modules initialized.
- [x] **ECS Foundation**: Core world manager and bitmask filtering implemented.
- [x] **High-Performance Data Structures**: 1D byte array chunk storage (32x32x32) optimized for cache locality.
- [x] **Threading Architecture**: Specialized pools for `WorldGen` and high-priority `MeshGen`.
- [x] **Vulkan Bootstrap**: Core graphics context initialized.
- [ ] **SafetyGuardian**: Hardcoded rule implementation (Phase 4).
- [ ] **World Generation**: FastNoiseLite integration and infinite terrain.

---

## 📦 Building from Source
Ensure you have **OpenJDK 21** and **Gradle 9.3.1** installed.

```bash
# Clone the repository
git clone https://github.com/Developer-lfierro743/AstrumProject.git

# Build all modules
gradle build

# Run ECS Performance Tests
gradle :astrum-core:test --info
```

---

## 🛡 Ethics & Privacy
*   **No Microtransactions**: The Marketplace is free and community-driven.
*   **Privacy First**: No telemetry or censorship in single-player worlds.
*   **One-time Purchase**: $16.99 USD (Indev stage is free for a limited time).

---

## 👥 Influencers & Community
Project Astrum is designed to support the "100 Players" simulation trend, with built-in stress-testing infrastructure for creators like:
*   @Sword4000
*   @MindOfNeo
*   @Bobicraft
*   @nyxlunarii
*   ...and the broader Minecraft/Sandbox community.

---

**Novusforge Studios**  
*Building the Future of the Sandbox.*
