package game.client;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import game.common.Boat;

public class Drawer {
    static final ArrayList<Mesh> hullMeshes = new ArrayList<>(18); // kind of like an arena
        /* the meshes are stored in the array like this:
            u1 v1 w1 x1 y1 z1
            u2 v2 w1 x2 y2 z2
            u3 v3 w3 x3 y3 z3
         */
    static Mesh mastMesh;
    static Mesh sailMesh;
    static int section_count; // 6
    static int damages_levels = 3;
    public static void generateMeshes() {
        int boat_pixel_length = 128;
        if (Opengl.textureId == -1) {
            System.err.println("tried to generate meshes before loading a texture");
            System.exit(1);
        }
        {// hull of boat
            float[][][] sections_positions = {
                { { 0.0f, 0.0f, 0.6f }, { -0.25f, 0.0f, 0.2f }, { 0.0f, 0.0f, 0.2f } }, // u
                { { 0.0f, 0.0f, 0.6f }, { 0.0f, 0.0f, 0.2f }, { 0.25f, 0.0f, 0.2f } },  // v
                { { -0.25f, 0.0f, 0.2f }, { 0.0f, 0.0f, 0.2f }, { -0.25f, 0.0f, -0.2f }, { 0.0f, 0.0f, -0.2f } }, // w
                { { 0.0f, 0.0f, 0.2f }, { 0.25f, 0.0f, 0.2f }, { 0.0f, 0.0f, -0.2f }, { 0.25f, 0.0f, -0.2f } },   // x
                { { -0.25f, 0.0f, -0.2f }, { 0.0f, 0.0f, -0.2f }, { -0.125f, 0.0f, -0.4f }, { 0.0f, 0.0f, -0.4f } }, // y
                { { 0.0f, 0.0f, -0.2f }, { 0.25f, 0.0f, -0.2f }, { 0.0f, 0.0f, -0.4f }, { 0.125f, 0.0f, -0.4f } },  // z
            };
            section_count = sections_positions.length;
            for (int dmg = 0; dmg < damages_levels; dmg++) {
                int s_offset = (int) (boat_pixel_length * (1 + 0.5 * dmg));
                int t_offset = boat_pixel_length * 1;
                for (float[][] sect : sections_positions) {
                    float[] vertex_data = new float[sect.length * 5];
                    FloatBuffer section_vertex_buffer = FloatBuffer.wrap(vertex_data);
                    for (float[] vert: sect){
                        float xpos = vert[0]; // -0.25 to 0.25
                        float ypos = vert[1]; // 0
                        float zpos = vert[2]; // -0.4 to 0.6
                        float stex = ((-vert[0] + 0.25f) /*0 to 0.50 (mirrored)*/ * boat_pixel_length + s_offset) / Opengl.atlasWidth;
                        float ttex = (-(vert[2] + 0.4f) /*-1 to 0 */ * boat_pixel_length + t_offset) / Opengl.atlasHeight;
                        section_vertex_buffer.put(xpos).put(ypos).put(zpos).put(stex).put(ttex);        
                    }
                    section_vertex_buffer.flip();
                    Mesh mesh = switch (sect.length) {
                        case 3 -> Mesh.newTriangle(section_vertex_buffer.array());
                        case 4 -> Mesh.newQuad(section_vertex_buffer.array());
                        default -> throw new IllegalArgumentException();
                    };
                    hullMeshes.add(mesh);
                }
            }
        }
        float bw = (float) boat_pixel_length / Opengl.atlasWidth;
        float bh = (float) boat_pixel_length / Opengl.atlasHeight;
        float px = (float) 1 / Opengl.atlasWidth;
        { // mast
            float[] mast_quad = {
                -0.1f, 0.0f, 0.2f, (4.0f/4)*bw + 8*px , 2*bh,
                 0.1f, 0.0f, 0.2f, (5.0f/4)*bw - 8*px , 2*bh,
                -0.1f, 2.0f, 0.2f, (4.0f/4)*bw + 8*px , bh,
                 0.1f, 2.0f, 0.2f, (5.0f/4)*bw - 8*px , bh,
            };
            mastMesh = Mesh.newQuad(mast_quad);
        }
        { // sail
            int segments = 2;
            int pairs = segments + 2;
            

        }
        

    }
    public static void drawhBoat(Boat boat) {
        if (hullMeshes.size() == 0) {
            System.err.println("tried draw a boat without generating meshes");
            System.exit(1);
        }
        Matrix4f modelMatrix = new Matrix4f().translationRotate(boat.position, boat.rotation);
        // hull
        IntStream.range(0, section_count)
            .map(n->n + (boat.sectionHealth[n]-1)*section_count)
            .forEach(n-> Drawer.hullMeshes.get(n).draw(modelMatrix));
        mastMesh.draw(modelMatrix);
    }
}
