package com.rudyunguru.trucks.render;

import com.rudyunguru.trucks.render.math.Mat4;

/**
 * CPU-side mesh builder for all procedural geometry in the game.
 *
 * Vertex layout (interleaved by the GPU uploader, 10 floats per vertex):
 *   position xyz, normal xyz, colour rgb, material flag
 *
 * Material flags drive the shader: 0 = matte (paint, concrete, dirt), 1 = metal (chrome, alu),
 * 2 = emissive (lights, windows at night), 3 = glass (transparent-ish, blurry reflections).
 *
 * No texture needs to be shipped: detail comes from per-vertex colour variation, which keeps the
 * build free of third-party art while still looking like painted metal, asphalt and vegetation.
 * A mesh is uploaded once and can then be drawn thousands of times with instancing.
 */
public final class MeshData {

    public static final int FLAG_MATTE = 0;
    public static final int FLAG_METAL = 1;
    public static final int FLAG_EMISSIVE = 2;
    public static final int FLAG_GLASS = 3;

    private float[] positions = new float[3 * 512];
    private float[] normals = new float[3 * 512];
    private float[] colors = new float[3 * 512];
    private float[] flags = new float[512];
    private int[] indices = new int[3 * 512];
    private int vertexCount;
    private int indexCount;

    private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

    public int vertexCount() {
        return vertexCount;
    }

    public int indexCount() {
        return indexCount;
    }

    public int triangleCount() {
        return indexCount / 3;
    }

    public float[] positions() {
        return positions;
    }

    public float[] normals() {
        return normals;
    }

    public float[] colors() {
        return colors;
    }

    public float[] flags() {
        return flags;
    }

    public int[] indices() {
        return indices;
    }

    public float minX() { return minX; }
    public float minY() { return minY; }
    public float minZ() { return minZ; }
    public float maxX() { return maxX; }
    public float maxY() { return maxY; }
    public float maxZ() { return maxZ; }

    private void ensureCapacity(int extraVertices, int extraIndices) {
        int needV = (vertexCount + extraVertices) * 3;
        if (needV > positions.length) {
            int size = Math.max(needV, positions.length * 2);
            positions = grow(positions, size);
            normals = grow(normals, size);
            colors = grow(colors, size);
        }
        int needF = vertexCount + extraVertices;
        if (needF > flags.length) {
            flags = grow(flags, Math.max(needF, flags.length * 2));
        }
        int needI = indexCount + extraIndices;
        if (needI > indices.length) {
            indices = growI(indices, Math.max(needI, indices.length * 2));
        }
    }

    private static float[] grow(float[] src, int size) {
        float[] out = new float[size];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    private static int[] growI(int[] src, int size) {
        int[] out = new int[size];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    /** Adds one vertex and returns its index. */
    public int vertex(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b, int flag) {
        ensureCapacity(1, 0);
        int index = vertexCount++;
        int p = index * 3;
        positions[p] = x;
        positions[p + 1] = y;
        positions[p + 2] = z;
        normals[p] = nx;
        normals[p + 1] = ny;
        normals[p + 2] = nz;
        colors[p] = r;
        colors[p + 1] = g;
        colors[p + 2] = b;
        flags[index] = flag;
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (z < minZ) minZ = z;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        if (z > maxZ) maxZ = z;
        return index;
    }

    public void triangle(int a, int b, int c) {
        ensureCapacity(0, 3);
        indices[indexCount++] = a;
        indices[indexCount++] = b;
        indices[indexCount++] = c;
    }

    public void quad(int a, int b, int c, int d) {
        triangle(a, b, c);
        triangle(a, c, d);
    }

    // ---------------------------------------------------------------- primitive shapes

    /** Axis aligned box centred on (cx,cy,cz) with full sizes (sx,sy,sz). Flat shaded. */
    public MeshData addBox(float cx, float cy, float cz, float sx, float sy, float sz,
                           float r, float g, float b, int flag) {
        return addBoxRotY(cx, cy, cz, sx, sy, sz, 0f, r, g, b, flag);
    }

    /** Box rotated around Y, used for buildings and props that are not axis aligned. */
    public MeshData addBoxRotY(float cx, float cy, float cz, float sx, float sy, float sz, float rotY,
                               float r, float g, float bl, int flag) {
        float hx = sx / 2f, hy = sy / 2f, hz = sz / 2f;
        float c = (float) Math.cos(rotY), s = (float) Math.sin(rotY);
        float[][] corners = {
                {-hx, -hy, -hz}, {hx, -hy, -hz}, {hx, hy, -hz}, {-hx, hy, -hz},
                {-hx, -hy, hz}, {hx, -hy, hz}, {hx, hy, hz}, {-hx, hy, hz},
        };
        // vertex indices of each face, followed by the local face normal
        int[][] faces = {
                {1, 2, 6, 5, 1, 0, 0},
                {0, 4, 7, 3, -1, 0, 0},
                {3, 7, 6, 2, 0, 1, 0},
                {0, 1, 5, 4, 0, -1, 0},
                {4, 5, 6, 7, 0, 0, 1},
                {0, 3, 2, 1, 0, 0, -1},
        };
        for (int[] face : faces) {
            float nx = face[4], ny = face[5], nz = face[6];
            float wnx = nx * c + nz * s;
            float wnz = -nx * s + nz * c;
            int[] v = new int[4];
            for (int i = 0; i < 4; i++) {
                float[] p = corners[face[i]];
                float lx = p[0] * c + p[2] * s;
                float lz = -p[0] * s + p[2] * c;
                v[i] = vertex(cx + lx, cy + p[1], cz + lz, wnx, ny, wnz, r, g, bl, flag);
            }
            quad(v[0], v[1], v[2], v[3]);
        }
        return this;
    }

    /** Cylinder whose axis runs along X (wheels roll forward along Z, so this is the wheel shape). */
    public MeshData addCylinderX(float cx, float cy, float cz, float radius, float length,
                                 int segments, float r, float g, float b, int flag) {
        float half = length / 2f;
        float prevY = radius, prevZ = 0;
        int firstRingStart = -1;
        int prevRingStart = -1;
        for (int i = 0; i <= segments; i++) {
            double a = i * Math.PI * 2 / segments;
            float y = (float) Math.cos(a) * radius;
            float z = (float) Math.sin(a) * radius;
            int ringStart = vertexCount;
            vertex(cx - half, cy + y, cz + z, 0, y / radius, z / radius, r, g, b, flag);
            vertex(cx + half, cy + y, cz + z, 0, y / radius, z / radius, r, g, b, flag);
            // side caps: one centre fan vertex per side per ring step keeps the winding simple
            vertex(cx - half, cy, cz, -1, 0, 0, r * 0.82f, g * 0.82f, b * 0.82f, flag);
            vertex(cx + half, cy, cz, 1, 0, 0, r * 0.70f, g * 0.70f, b * 0.70f, flag);
            if (prevRingStart >= 0) {
                quad(prevRingStart, prevRingStart + 1, ringStart + 1, ringStart);
                triangle(prevRingStart + 2, prevRingStart, ringStart);
                triangle(prevRingStart + 3, ringStart + 1, prevRingStart + 1);
            }
            if (firstRingStart < 0) firstRingStart = ringStart;
            else if (i == segments) {
                // close the ring so the seam has no gap
                quad(ringStart, ringStart + 1, firstRingStart + 1, firstRingStart);
            }
            prevRingStart = ringStart;
            prevY = y;
            prevZ = z;
        }
        return this;
    }

    /** Cylinder whose axis runs along Y (poles, chimneys, silos, tree trunks). */
    public MeshData addCylinderY(float cx, float cy, float cz, float radius, float height,
                                 int segments, float r, float g, float b, int flag) {
        float bottom = cy - height / 2f;
        float top = cy + height / 2f;
        int firstRing = -1;
        int prevRing = -1;
        for (int i = 0; i <= segments; i++) {
            double a = i * Math.PI * 2 / segments;
            float x = (float) Math.cos(a) * radius;
            float z = (float) Math.sin(a) * radius;
            float nx = x / radius, nz = z / radius;
            int ring = vertexCount;
            int v0 = vertex(cx + x, bottom, cz + z, nx, 0, nz, r, g, b, flag);
            int v1 = vertex(cx + x, top, cz + z, nx, 0, nz, r, g, b, flag);
            vertex(cx, top, cz, 0, 1, 0, r * 0.94f, g * 0.94f, b * 0.94f, flag);
            vertex(cx, bottom, cz, 0, -1, 0, r * 0.7f, g * 0.7f, b * 0.7f, flag);
            if (prevRing >= 0) {
                quad(prevRing, prevRing + 1, v1, v0);
                triangle(prevRing + 2, prevRing + 1, v1);
                triangle(prevRing + 3, v0, prevRing);
            }
            if (firstRing < 0) firstRing = ring;
            else if (i == segments) {
                quad(ring, ring + 1, firstRing + 1, firstRing);
                vertexCount -= 2; // the closing ring needs no extra cap vertices
            }
            prevRing = ring;
        }
        return this;
    }

    /** A complete wheel: rubber tyre, metal rim and hub, rolling around the X axis. */
    public MeshData addWheel(float cx, float cy, float cz, float radius, float width,
                             float tireR, float tireG, float tireB) {
        addCylinderX(cx, cy, cz, radius, width, 16, tireR, tireG, tireB, FLAG_MATTE);
        addCylinderX(cx, cy, cz, radius * 0.62f, width * 1.02f, 12, 0.62f, 0.64f, 0.68f, FLAG_METAL);
        addCylinderX(cx, cy, cz, radius * 0.22f, width * 1.06f, 8, 0.45f, 0.47f, 0.50f, FLAG_METAL);
        return this;
    }

    /** Horizontal quad (ground, road, water). Counter-clockwise when seen from above. */
    public MeshData addGroundQuad(float x0, float z0, float x1, float z1, float y,
                                  float r, float g, float b, int flag) {
        int v0 = vertex(x0, y, z0, 0, 1, 0, r, g, b, flag);
        int v1 = vertex(x0, y, z1, 0, 1, 0, r, g, b, flag);
        int v2 = vertex(x1, y, z1, 0, 1, 0, r, g, b, flag);
        int v3 = vertex(x1, y, z0, 0, 1, 0, r, g, b, flag);
        quad(v0, v1, v2, v3);
        return this;
    }

    /**
     * Extrudes a polyline (x,z pairs, world units) into a road surface of the given width, slightly
     * above [y] so it never z-fights with the terrain. Normals point up, which is what a road needs.
     */
    public MeshData addRoadStrip(float[] xz, int count, float width, float y,
                                 float r, float g, float b, int flag) {
        if (count < 2) return this;
        float half = width / 2f;
        for (int i = 0; i < count - 1; i++) {
            float ax = xz[i * 2], az = xz[i * 2 + 1];
            float bx = xz[i * 2 + 2], bz = xz[i * 2 + 3];
            float dx = bx - ax, dz = bz - az;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            if (len < 1e-4f) continue;
            float px = -dz / len, pz = dx / len;
            // a slight per-segment tint variation makes the asphalt read as worn, not flat
            float tint = 0.94f + 0.06f * ((i % 3) == 0 ? 1f : (i % 3) == 1 ? 0.5f : 0f);
            int v0 = vertex(ax + px * half, y, az + pz * half, 0, 1, 0, r * tint, g * tint, b * tint, flag);
            int v1 = vertex(ax - px * half, y, az - pz * half, 0, 1, 0, r * tint, g * tint, b * tint, flag);
            int v2 = vertex(bx - px * half, y, bz - pz * half, 0, 1, 0, r * tint, g * tint, b * tint, flag);
            int v3 = vertex(bx + px * half, y, bz + pz * half, 0, 1, 0, r * tint, g * tint, b * tint, flag);
            quad(v0, v1, v2, v3);
        }
        return this;
    }

    /**
     * Paints a dashed centre line, lane separators or a solid edge line over a road strip.
     * Dashed mode advances along the polyline so markings stay continuous across segments.
     */
    public MeshData addRoadMarkings(float[] xz, int count, float offset, float width, float y,
                                    float dashLength, float gapLength,
                                    float r, float g, float b) {
        if (count < 2) return this;
        float travelled = 0f;
        boolean painting = true;
        for (int i = 0; i < count - 1; i++) {
            float ax = xz[i * 2], az = xz[i * 2 + 1];
            float bx = xz[i * 2 + 2], bz = xz[i * 2 + 3];
            float dx = bx - ax, dz = bz - az;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            if (len < 1e-4f) continue;
            float px = -dz / len, pz = dx / len;
            float midX = (ax + bx) / 2f + px * offset;
            float midZ = (az + bz) / 2f + pz * offset;
            float hx = dx / 2f, hz = dz / 2f;
            float sx = px * width / 2f, sz = pz * width / 2f;
            if (dashLength <= 0f || painting) {
                int v0 = vertex(midX - hx + sx, y, midZ - hz + sz, 0, 1, 0, r, g, b, FLAG_MATTE);
                int v1 = vertex(midX - hx - sx, y, midZ - hz - sz, 0, 1, 0, r, g, b, FLAG_MATTE);
                int v2 = vertex(midX + hx - sx, y, midZ + hz - sz, 0, 1, 0, r, g, b, FLAG_MATTE);
                int v3 = vertex(midX + hx + sx, y, midZ + hz + sz, 0, 1, 0, r, g, b, FLAG_MATTE);
                quad(v0, v1, v2, v3);
            }
            if (dashLength > 0f) {
                travelled += len;
                if (painting && travelled >= dashLength) {
                    painting = false;
                    travelled = 0f;
                } else if (!painting && travelled >= gapLength) {
                    painting = true;
                    travelled = 0f;
                }
            }
        }
        return this;
    }

    /** Bakes a transform into every vertex (prefab placement, mirrored parts, wheels at angles). */
    public MeshData transform(Mat4 matrix) {
        float[] nm = matrix.normalMatrix();
        for (int i = 0; i < vertexCount; i++) {
            int p = i * 3;
            float x = positions[p], y = positions[p + 1], z = positions[p + 2];
            positions[p] = matrix.m[0] * x + matrix.m[4] * y + matrix.m[8] * z + matrix.m[12];
            positions[p + 1] = matrix.m[1] * x + matrix.m[5] * y + matrix.m[9] * z + matrix.m[13];
            positions[p + 2] = matrix.m[2] * x + matrix.m[6] * y + matrix.m[10] * z + matrix.m[14];
            float nx = normals[p], ny = normals[p + 1], nz = normals[p + 2];
            float tx = nm[0] * nx + nm[3] * ny + nm[6] * nz;
            float ty = nm[1] * nx + nm[4] * ny + nm[7] * nz;
            float tz = nm[2] * nx + nm[5] * ny + nm[8] * nz;
            float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (len > 1e-6f) {
                normals[p] = tx / len;
                normals[p + 1] = ty / len;
                normals[p + 2] = tz / len;
            }
        }
        return this;
    }

    /** Merges another mesh into this one (both must already be in the same space). */
    public MeshData merge(MeshData other) {
        int base = vertexCount;
        ensureCapacity(other.vertexCount, other.indexCount);
        for (int i = 0; i < other.vertexCount; i++) {
            vertex(other.positions[i * 3], other.positions[i * 3 + 1], other.positions[i * 3 + 2],
                    other.normals[i * 3], other.normals[i * 3 + 1], other.normals[i * 3 + 2],
                    other.colors[i * 3], other.colors[i * 3 + 1], other.colors[i * 3 + 2],
                    (int) other.flags[i]);
        }
        for (int i = 0; i < other.indexCount; i++) {
            indices[indexCount++] = other.indices[i] + base;
        }
        return this;
    }

    /** Parses a `#RRGGBB` colour into three 0..1 floats. */
    public static float[] rgb(String hex, float[] out) {
        int value = 0xFFFFFF;
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            value = Integer.parseInt(clean, 16);
        } catch (NumberFormatException ignored) {
            // keep the default colour
        }
        out[0] = ((value >> 16) & 0xFF) / 255f;
        out[1] = ((value >> 8) & 0xFF) / 255f;
        out[2] = (value & 0xFF) / 255f;
        return out;
    }
}
