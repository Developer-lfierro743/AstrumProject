# Project Astrum: Multi-threaded Voxel Engine Plan

## Objective
Implement the high-performance lifecycle for chunks as defined in **@Formulas/keyconcepts.txt**. This includes asynchronous world generation, background mesh generation (Greedy Meshing), and safe GPU synchronization using Java 21's `ExecutorService`.

## Key Files & Context
- **astrum.world.Chunk**: 32x32x32 storage.
- **astrum.world.World**: Needs to manage async tasks.
- **astrum.engine.VulkanRenderer**: Will receive the finalized meshes.

## Implementation Steps

### 1. The Async Task Manager (`astrum-world`)
- Implement a `ChunkWorkerPool` using `Executors.newFixedThreadPool`.
- Define `WorldGenTask` and `MeshGenTask`.
- Ensure **No Single-Threaded Bottleneck** by offloading all math to background threads.

### 2. Chunk State Management
- Track chunk states: `EMPTY` -> `GENERATING` -> `DIRTY` -> `MESHING` -> `READY`.
- Implement `isDirty()` and `markDirty()` to handle block changes.

### 3. Mesh Generation (Greedy Meshing)
- Implement the "Opaque vs Transparent" mesh separation from **Part 2 of finalForReach**.
- Use 1D array traversal to generate the minimum number of quads.
- Implement the **Fresnel Effect** logic in the mesh data (normals/tangents).

### 4. GPU Synchronization
- Implement the "synchronized mesh generation" to avoid race conditions.
- Ensure `dispose()` is called on old meshes to prevent the **VRAM memory leak** mentioned in the Formulas.

### 5. Infinite Generation
- Implement the camera-based "move and unload" logic.
- Dispose of meshes properly when chunks are too far from the camera.

## Verification & Testing
- **Profiling:** Use Java JFR to ensure the Main Thread is not blocked by worldgen.
- **Stress Test:** Rapidly fly through the world to verify mesh cleanup and VRAM stability.
