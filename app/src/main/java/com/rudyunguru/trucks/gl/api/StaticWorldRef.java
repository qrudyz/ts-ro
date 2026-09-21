package com.rudyunguru.trucks.gl.api;

/**
 * Opaque handle for a world that has been uploaded to the GPU. The renderer creates it in
 * ``WorldBuildApi.buildWorld`` and the session keeps it alive until the driving session ends, when
 * ``WorldBuildApi.releaseWorld`` frees the buffers. Bounds are exposed so the simulation can clamp
 * the truck inside the playable area and so the streaming system knows what is loaded.
 */
public final class StaticWorldRef {

    public final long id;
    public final float minX;
    public final float minZ;
    public final float maxX;
    public final float maxZ;
    public final int roadStrips;
    public final int buildings;
    public final int props;
    public final int triangles;
    /** True until the GPU upload has finished (the loading screen polls this). */
    public volatile boolean ready;

    public StaticWorldRef(long id, float minX, float minZ, float maxX, float maxZ,
                          int roadStrips, int buildings, int props, int triangles) {
        this.id = id;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.roadStrips = roadStrips;
        this.buildings = buildings;
        this.props = props;
        this.triangles = triangles;
    }

    public float width() {
        return maxX - minX;
    }

    public float depth() {
        return maxZ - minZ;
    }

    public boolean contains(float x, float z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    @Override
    public String toString() {
        return "StaticWorldRef#" + id + "(" + roadStrips + " strips, " + buildings + " buildings, "
                + props + " props, " + triangles + " tris)";
    }
}
