code must be changed and must be added:
Phase 1: Foundation (The Skeleton)
   * astrum-common
       * [x] Add Vector3i utility methods for chunk-local coordinate math.
       * [x] Implement NativeMemory unit tests to verify off-heap safety.
   * astrum-api
       * [x] Expand Registry to support Namespaced IDs (e.g., astrum:ferrous).
       * [x] Create IWorld and IEntity interfaces for modder access.
   * astrum-core
       * [x] Implement ECSSystemManager to run systems like Physics and AI.
       * [x] Add MovementSystem to process VelocityComponent and PositionComponent.
   * astrum-engine
       * [ ] Vulkan: Complete createGraphicsPipeline with actual shader modules and fixed-function state.
       * [ ] Vulkan: Implement CommandPool and CommandBuffer allocation logic.
       * [ ] Windowing: Bridge WindowProvider into the main loop in AstrumClient.


 Phase 2: High-Performance Rendering (The Vision)
   * astrum-engine
       * [x] Meshing: Finish addFace logic in ChunkMesher (currently omitted).
       * [x] Abstraction: Implement cross-platform RenderBackend interface with API detection.
       * [x] Buffer: Add RenderBuffer abstraction for unified Vulkan/WebGPU buffers.
       * [x] Optimization: Implement Greedy Meshing to combine identical voxel faces.
       * [ ] Vulkan: Setup Vertex Buffers and Index Buffers using NativeMemory.
       * [ ] Vulkan: Implement vkCmdDrawIndexed to see the first triangle.
       * [ ] Scaling: Implement the off-screen scaling buffer for Adreno 710 optimization.
       * [ ] Bindless: Implement descriptor indexing for "The Formula's" 10k+ texture requirement.
   * astrum-android
       * [x] Android SDK setup at /opt/android-sdk
       * [x] Android module scaffold created (needs Gradle 8.x setup)


 Phase 3: World & Generation (The Scale)
   * astrum-world
       * [x] Implement InfiniteGenerator using the integrated FastNoiseLite.
       * [x] Concurrency: Setup Java Virtual Threads to generate chunks in the background.
       * [x] Streaming: Implement logic to load/unload chunks based on player distance (no world borders).
       * [x] Ores: Add geological distribution logic for Scientific Ores (Ferrous, Aurum, etc.).


 Phase 4: Creative & Social (The Soul)
   * astrum-security
       * [x] Implement SexualRule.java and AssetScanner.java as hardcoded rules.
       * [x] Add mod verification logic to block legacy Minecraft modloaders (Forge/Fabric).
   * astrum-core
       * [ ] The Purge: Implement event triggers and temporary "no-rule" state logic.
   * astrum-client
       * [ ] UI: Build the Matte-Vibrant HUD (Vibrant HP bars, XP bars).
       * [ ] Inventory: Implement the 50-slot inventory and 11-slot quickbar (4x4 crafting grid).


 Phase 5: Universal Access (The Outreach)
   * astrum-web
       * [ ] Implement WebWindowProvider using TeaVM DOM APIs.
       * [ ] Create the WASM-GC glue code for the main entry point.
   * astrum-engine
       * [ ] WebGPU: Map Vulkan pipeline logic to WebGPU commands using TeaVM JSO.
   * astrum-server
       * [ ] Protocols: Implement wss:// and ws:// support for browser client connections.