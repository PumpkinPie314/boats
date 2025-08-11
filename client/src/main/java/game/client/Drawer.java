package game.client;

import static org.lwjgl.opengl.GL11.glDepthMask;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.stream.IntStream;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    static Mesh cannonMesh;
    static Mesh cannonBallMesh;
    static Mesh cannonBallShadowMesh;
    static int section_count; // 6
    static int damages_levels = 3;
    public static void generateMeshes() {
        int boat_pixel_length = 128;
        float bls = (float) boat_pixel_length / Opengl.atlasWidth;
        float blt = (float) boat_pixel_length / Opengl.atlasHeight;
        float px = (float) 1 / Opengl.atlasWidth;
        if (Opengl.textureId == -1) {
            System.err.println("tried to generate meshes before loading a texture");
            System.exit(1);
        }
        { // hull of boat
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
                        float ttex = (-(vert[2] + 0.4f) /*-1 to 0 (mirrored)*/ * boat_pixel_length + t_offset) / Opengl.atlasHeight;
                        section_vertex_buffer.put(xpos).put(ypos).put(zpos).put(stex).put(ttex);        
                    }
                    section_vertex_buffer.flip();
                    Mesh mesh = switch (sect.length) {
                        case 3 -> new Mesh(section_vertex_buffer.array(), new int[]{0, 1, 2});
                        case 4 -> new Mesh(section_vertex_buffer.array(), new int[]{0, 1, 3, 0, 2, 3});
                        default -> throw new IllegalArgumentException();
                    };
                    hullMeshes.add(mesh);
                }
            }
        }
        { // mast
            float[] mast_quad = {
                -0.05f, 0.0f, 0f, (4.0f/4)*bls + 8*px , 2*blt, 
                 0.05f, 0.0f, 0f, (5.0f/4)*bls - 8*px , 2*blt,
                -0.05f, 1.0f, 0f, (4.0f/4)*bls + 8*px , blt,
                 0.05f, 1.0f, 0f, (5.0f/4)*bls - 8*px , blt,
                
                -0.5f, 1f,-0.025f, (5.0f/4)*bls + 8*px , 2*blt,
                -0.5f ,1f ,0.025f, (6.0f/4)*bls - 8*px , 2*blt,
                0.5f, 1f,-0.025f, (5.0f/4)*bls + 8*px , blt,
                0.5f, 1f, 0.025f, (6.0f/4)*bls - 8*px , blt,
            };
            mastMesh = new Mesh(mast_quad, new int[]{0, 1, 3, 0, 2, 3,   4, 5, 7, 4, 6, 7});
        }
        { // sail
            int segments = 10;
            float tau = (float) Math.TAU;
            float[] vertex_data = new float[(segments+1)*2 * 5];
            int pointer = 0;
            for (int i = 0; i < segments+1; i++) {
                float t = (float) i /segments; // t ranges from 0 to 1
                //                                  x   y                           z                       s           t
                float[]  left_vert = new float[] {-0.4f, (float)Math.cos(t*tau/2), (float)Math.sin(t*tau/2), (1f/2)*bls, ((t+2f)/2)*blt};
                float[] right_vert = new float[] { 0.4f, (float)Math.cos(t*tau/2), (float)Math.sin(t*tau/2), (2f/2)*bls, ((t+2f)/2)*blt};
                System.arraycopy(left_vert, 0, vertex_data, pointer, 5);
                System.arraycopy(right_vert, 0, vertex_data, pointer+5, 5);
                pointer += 10;
            }
            int[] strip_ebo = IntStream.range(0, (segments+1)*2).toArray();
            sailMesh = new Mesh(vertex_data, strip_ebo);
        }
        { // cannon
            float[] cannon_quad = {
                -0.5f, 0.01f,-0.5f, (3.0f/4)*bls , (4f/2)*blt, 
                 0.5f, 0.01f,-0.5f, (4.0f/4)*bls , (4f/2)*blt,
                -0.5f, 0.01f, 1.5f, (3.0f/4)*bls , (3f/2)*blt,
                 0.5f, 0.01f, 1.5f, (4.0f/4)*bls , (3f/2)*blt,
            };
            cannonMesh = new Mesh(cannon_quad, new int[] {0, 1, 3, 0, 2, 3});
        }
        { // cannonball
            float[] cannonball_quad = {
                -0.5f, -0.5f, 0f, (0.0f/2)*bls , (3f/2)*blt, 
                 0.5f, -0.5f, 0f, (1.0f/2)*bls , (3f/2)*blt,
                -0.5f,  0.5f, 0f, (0.0f/2)*bls , (2f/2)*blt,
                 0.5f,  0.5f, 0f, (1.0f/2)*bls , (2f/2)*blt,
            };
            cannonBallMesh = new Mesh(cannonball_quad, new int[] {0, 1, 3, 0, 2, 3});
        }
        { // cannonball shadow
            float[] cannonball_shadow_quad = {
                -0.5f, 0f, -0.5f, (0.0f/2)*bls , (4f/2)*blt, 
                 0.5f, 0f, -0.5f, (1.0f/2)*bls , (4f/2)*blt,
                -0.5f, 0f,  0.5f, (0.0f/2)*bls , (3f/2)*blt,
                 0.5f, 0f,  0.5f, (1.0f/2)*bls , (3f/2)*blt,
            };
            cannonBallShadowMesh = new Mesh(cannonball_shadow_quad, new int[] {0, 1, 3, 0, 2, 3});
        }
    }
    public static boolean isInsideHurtSphere(Vector3f point, Boat boat, int segment){
        float[][] spheres = {
            // portside                // starboard
            // x,y,z,radius            // x,y,z,radius 
            {-1f/4, 0,  1f/2, 1f/3}, { 1f/4, 0,  1f/2, 1f/3}, //uv  // bow
            {-1f/4, 0,  0f/2, 1f/3}, { 1f/4, 0,  0f/2, 1f/3}, //wx
            {-1f/4, 0, -1f/2, 1f/3}, { 1f/4, 0, -1f/2, 1f/3}, //yz  // stern
        };
        float[] center = {spheres[segment][0], spheres[segment][1], spheres[segment][2]};
        float radius = spheres[segment][3];
        Vector3f hurtSphereCenter = new Vector3f(center)
            .rotate(boat.rotation)
            .add(boat.position);
        Vector3f projectedPoint = new Vector3f(point).mul(1,0,1);
             // the point(cannonball) is projected onto the sea floor before distance check. this means shooting above the boat still registers a hit
        return hurtSphereCenter.distance(projectedPoint) < radius;

    }
    public static void drawBoat(Boat boat) {
        if (hullMeshes.size() == 0) {
            System.err.println("tried draw a boat without generating meshes");
            System.exit(1);
        }
        Matrix4f hullMatrix = new Matrix4f().translationRotate(boat.position, boat.rotation);
        // hull
        IntStream.range(0, section_count)
            .filter(n->boat.sectionHealth[n]>0)
            .map(n->n + (boat.sectionHealth[n]-1)*section_count)
            .forEach(n-> Drawer.hullMeshes.get(n).draw(hullMatrix));
        // mast
        Matrix4f mastMatrix = new Matrix4f(hullMatrix)
            .translate(0, 0 ,0.2f) // position further up towards the front of the boat
            .rotateY(boat.sail_turn_percent * Main.gameState.config.sail_angle_limit * -1);
        mastMesh.draw(mastMatrix);
        // sail
        Vector3f saildir = new Vector3f(0,0,1).rotate(new Quaternionf(boat.rotation).rotateY(boat.sail_turn_percent * Main.gameState.config.sail_angle_limit * -1));
        Vector3f winddir = new Vector3f(Main.gameState.wind);

        sailMesh.drawStrip(new Matrix4f(mastMatrix)
            .translate(0, 1f, 0)//move mesh origin to the top of the circle
            .scale(new Vector3f(1, -boat.mast_down_percent/2, boat.mast_down_percent*(saildir.dot(winddir)+1)/2/2)) // one 2 to normalize, the other 2 cuts in half
            .translate(0, 1, 0) //move to the top of the mast
        );
        // cannon
        glDepthMask(false);
        cannonMesh.draw(new Matrix4f()
            .translate(boat.position)
            .rotate(boat.cannonRotation)
            .scale(new Vector3f(1f/4))
        );
        glDepthMask(true);
        
    }
    public static void drawCannonBall(CannonBall cb) {
        // ball
        glDepthMask(false);
        cannonBallMesh.draw(new Matrix4f()
            .translate(cb.position)
            .mul(new Matrix4f(Opengl.viewMatrix) // copy camera
                .m30(0).m31(0).m32(0) // remove translation
                .invert()
            ).scale(1f/4)
        );
        glDepthMask(true);
        // shadow
        glDepthMask(false);
        cannonBallShadowMesh.draw(new Matrix4f()
            .translate(new Vector3f(cb.position.x, 0, cb.position.z))
            .scale(1f/4 + (1f/4) * cb.position.y)
        );
        glDepthMask(true);
    }
}
