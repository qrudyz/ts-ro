package com.rudyunguru.trucks.gl;

import android.opengl.GLES30;
import com.rudyunguru.trucks.render.MeshData;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * A static mesh stored on the GPU.
 */
public final class StaticMesh {

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int count;

    public StaticMesh(MeshData data) {
        this.count = data.indexCount();

        int[] ids = new int[3];
        GLES30.glGenVertexArrays(1, ids, 0);
        GLES30.glGenBuffers(2, ids, 1);
        vao = ids[0];
        vbo = ids[1];
        ebo = ids[2];

        GLES30.glBindVertexArray(vao);

        // Interleave data: pos(3), norm(3), col(3), flag(1) = 10 floats per vertex
        float[] interleaved = new float[data.vertexCount() * 10];
        for (int i = 0; i < data.vertexCount(); i++) {
            int p = i * 10;
            interleaved[p] = data.positions()[i * 3];
            interleaved[p + 1] = data.positions()[i * 3 + 1];
            interleaved[p + 2] = data.positions()[i * 3 + 2];
            interleaved[p + 3] = data.normals()[i * 3];
            interleaved[p + 4] = data.normals()[i * 3 + 1];
            interleaved[p + 5] = data.normals()[i * 3 + 2];
            interleaved[p + 6] = data.colors()[i * 3];
            interleaved[p + 7] = data.colors()[i * 3 + 1];
            interleaved[p + 8] = data.colors()[i * 3 + 2];
            interleaved[p + 9] = data.flags()[i];
        }

        FloatBuffer vb = GLUtil.floatBuffer(interleaved);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, interleaved.length * 4, vb, GLES30.GL_STATIC_DRAW);

        IntBuffer ib = GLUtil.intBuffer(data.indices());
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, data.indexCount() * 4, ib, GLES30.GL_STATIC_DRAW);

        int stride = 10 * 4;
        GLES30.glEnableVertexAttribArray(0); // pos
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0);
        GLES30.glEnableVertexAttribArray(1); // norm
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4);
        GLES30.glEnableVertexAttribArray(2); // col
        GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, stride, 6 * 4);
        GLES30.glEnableVertexAttribArray(3); // flag
        GLES30.glVertexAttribPointer(3, 1, GLES30.GL_FLOAT, false, stride, 9 * 4);

        GLES30.glBindVertexArray(0);
    }

    public void draw() {
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, count, GLES30.GL_UNSIGNED_INT, 0);
        GLES30.glBindVertexArray(0);
    }

    public void release() {
        GLES30.glDeleteBuffers(2, new int[]{vbo, ebo}, 0);
        GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
    }
}
