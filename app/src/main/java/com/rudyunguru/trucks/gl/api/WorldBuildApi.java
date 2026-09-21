package com.rudyunguru.trucks.gl.api;

import com.rudyunguru.trucks.core.model.WorldGeometry;

/**
 * Turns generated world data into batched GPU geometry.
 *
 * The world generator (Kotlin, ``com.rudyunguru.trucks.world``) works with pure data; the renderer
 * (Java, ``com.rudyunguru.trucks.render``) owns meshes, materials and buffer uploads. Batching rules
 * the implementation must follow:
 *  - one static vertex/index buffer per material family (asphalt, markings, concrete, facade,
 *    vegetation, metal, glass, emissive), so a whole city costs a handful of draw calls;
 *  - buildings and props are instanced where the mesh is repeated (trees, lamps, signs, fences);
 *  - roads are extruded ribbons (with kerbs, shoulders and lane markings baked in the markings pass);
 *  - texture variants are selected procedurally from the vertex material id + a small atlas built at
 *    runtime, so no external texture files are required (an artist can still drop in PNGs later).
 */
public interface WorldBuildApi {

    /** Uploads a generated world. Returns the handle immediately; sets ``ready`` when finished. */
    StaticWorldRef buildWorld(WorldGeometry geometry);

    /** Releases the buffers of a world built by ``buildWorld``. */
    void releaseWorld(StaticWorldRef world);
}
