package game.client;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL45.glCreateBuffers;
import static org.lwjgl.opengl.GL45.glCreateVertexArrays;
import static org.lwjgl.opengl.GL45.glEnableVertexArrayAttrib;
import static org.lwjgl.opengl.GL45.glNamedBufferData;
import static org.lwjgl.opengl.GL45.glVertexArrayAttribBinding;
import static org.lwjgl.opengl.GL45.glVertexArrayAttribFormat;
import static org.lwjgl.opengl.GL45.glVertexArrayElementBuffer;
import static org.lwjgl.opengl.GL45.glVertexArrayVertexBuffer;
import org.lwjgl.system.MemoryUtil;

public class Mesh {

    private static final List<Mesh> meshes = new ArrayList<>();

    public int vao;
    public int vbo;
    public int ebo;

    public int size;

    public Mesh(float[] vertex_data, int[] indices){
        
        // a vao, or vertex array object, is like the orchistrator or manager of a mesh
        // it has attribute slots where it stores pointers to data. our vbo will be in the 0th attribute slot.
        vao = glCreateVertexArrays();

        int position = glGetAttribLocation(Opengl.vertex_program, "position");
        int color = glGetAttribLocation(Opengl.vertex_program, "color");

        // the vbo, or vertex buffer object, is just a buffer of floats that we want read as points. 
        // we need to describe to the vao about how exacly we plan to use this buffer, including size , stride, etc.
        vbo = glCreateBuffers();
        FloatBuffer vertex_buffer = MemoryUtil.memAllocFloat(vertex_data.length);
        vertex_buffer.put(vertex_data);
        vertex_buffer.flip();
        glNamedBufferData(vbo, vertex_buffer, GL_STATIC_DRAW); 
            //'GL_STATIC_DRAW' has nothing to do with drawing, and is only a hint to opengl about how often the values inside the buffer change
        glVertexArrayVertexBuffer(vao,0, vbo, 0, 6 * Float.BYTES);

        glEnableVertexArrayAttrib(vao, position);
        glVertexArrayAttribFormat(vao, position, 3, GL_FLOAT, false, 0);
        glVertexArrayAttribBinding(vao, position, 0);
        
        glEnableVertexArrayAttrib(vao, color);
        glVertexArrayAttribFormat(vao, color, 3, GL_FLOAT, false, 3 * Float.BYTES);
        glVertexArrayAttribBinding(vao, color, 0);


        
        // the ebo, or element buffer object, is list of indexes into the vbo.
        // it tells the vao what order the points defined in the vbo should be drawn 
        ebo = glCreateBuffers();
        IntBuffer element_buffer = MemoryUtil.memAllocInt(indices.length);
        element_buffer.put(indices);
        element_buffer.flip();
        glNamedBufferData(ebo, element_buffer, GL_STATIC_DRAW);
        glVertexArrayElementBuffer(vao, ebo);

        size = indices.length;

        // free memory (it stays on the gpu)
        MemoryUtil.memFree(vertex_buffer);
        MemoryUtil.memFree(element_buffer);

        meshes.add(this);
    }

    public void draw(Matrix4f modelMatrix) {
        Opengl.setDrawScreenSpace(false);
        Opengl.setModelMatrix(modelMatrix);
        glBindVertexArray(this.vao);
        glDrawElements(GL_TRIANGLES, this.size , GL_UNSIGNED_INT, 0);
    }
    public void draw() {
        Opengl.setDrawScreenSpace(true);
        glBindVertexArray(this.vao);
        glDrawElements(GL_TRIANGLES, this.size , GL_UNSIGNED_INT, 0);
    }
    public void drawLines() {
        Opengl.setDrawScreenSpace(true);
        glBindVertexArray(this.vao);
        glDrawElements(GL_LINES, this.size , GL_UNSIGNED_INT, 0);
    }
    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }

    public static void cleanupAll() {
        for (Mesh mesh : meshes) {
            mesh.cleanup();
        }
        meshes.clear();
    }
}
