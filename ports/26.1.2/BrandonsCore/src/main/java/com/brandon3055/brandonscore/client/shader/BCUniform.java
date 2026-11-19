package com.brandon3055.brandonscore.client.shader;

import codechicken.lib.vec.Matrix4;
import org.joml.Matrix4f;
import java.nio.ByteBuffer;

/** Typed writes into the model shader's std140 staging block. No direct OpenGL state. */
public final class BCUniform {
    private final ByteBuffer buffer;
    private final int offset;
    private final String type;
    BCUniform(ByteBuffer buffer, int offset, String type) {
        this.buffer = buffer; this.offset = offset; this.type = type;
    }
    private void require(String expected) {
        if (!type.equals(expected)) throw new IllegalArgumentException("Expected " + type + ", got " + expected);
    }
    public void glUniform1f(float value) { require("float"); buffer.putFloat(offset, value); }
    public void glUniform1b(boolean value) { glUniform1i(value ? 1 : 0); }
    public void glUniform1i(int value) { require("int"); buffer.putInt(offset, value); }
    public void glUniform2i(int x, int y) { require("i_vec2"); buffer.putInt(offset,x).putInt(offset+4,y); }
    public void glUniform2f(float x, float y) {
        require("vec2"); buffer.putFloat(offset,x).putFloat(offset+4,y);
    }
    public void glUniform3f(float x, float y, float z) {
        require("vec3"); buffer.putFloat(offset,x).putFloat(offset+4,y).putFloat(offset+8,z);
    }
    public void glUniform4f(float x,float y,float z,float w) {
        require("vec4"); buffer.putFloat(offset,x).putFloat(offset+4,y).putFloat(offset+8,z).putFloat(offset+12,w);
    }
    public void glUniformMatrix4f(Matrix4 matrix) { glUniformMatrix4f(matrix.toMatrix4f()); }
    public void glUniformMatrix4f(Matrix4f matrix) {
        require("matrix4x4");
        // JOML's native-memory fast path assumes a direct buffer. Our staging block is on the heap.
        float[] values=matrix.get(new float[16]);
        for(int i=0;i<16;i++)buffer.putFloat(offset+i*4,values[i]);
    }
}
