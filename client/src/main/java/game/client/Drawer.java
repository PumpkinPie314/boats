package game.client;

import java.util.Arrays;

import org.joml.Matrix4f;

import game.common.Boat;

public class Drawer {
    public static void drawhHull(Boat boat) {
        Matrix4f modelMatrix = new Matrix4f().translationRotate(boat.position, boat.rotation);

        float[][][] section = {
            { { 0.0f, 0.0f, 0.6f }, { -0.25f, 0.0f, 0.2f }, { 0.0f, 0.0f, 0.2f } }, // u
            { { 0.0f, 0.0f, 0.6f }, { 0.0f, 0.0f, 0.2f }, { 0.25f, 0.0f, 0.2f } },  // v
            { { -0.25f, 0.0f, 0.2f }, { 0.0f, 0.0f, 0.2f }, { -0.25f, 0.0f, -0.2f }, { 0.0f, 0.0f, -0.2f } }, // w
            { { 0.0f, 0.0f, 0.2f }, { 0.25f, 0.0f, 0.2f }, { 0.0f, 0.0f, -0.2f }, { 0.25f, 0.0f, -0.2f } },   // x
            { { -0.25f, 0.0f, -0.2f }, { 0.0f, 0.0f, -0.2f }, { -0.125f, 0.0f, -0.4f }, { 0.0f, 0.0f, -0.4f } }, // y
            { { 0.0f, 0.0f, -0.2f }, { 0.25f, 0.0f, -0.2f }, { 0.0f, 0.0f, -0.4f }, { 0.125f, 0.0f, -0.4f } },  // z
        };

        float[][] colors = Arrays.stream(boat.sectionHealth)
        .mapToObj(id -> switch (id) {
            case 1 -> new float[] { 1.0f, 0.0f, 0.0f };   // Red
            case 2 -> new float[] { 1.0f, 0.5f, 0.0f };   // Orange
            case 3 -> new float[] { 1.0f, 1.0f, 0.0f };   // Yellow
            default -> new float[] { 0.5f, 0.5f, 0.5f };  // Gray
        })
        .map(color -> {
            float[] dimmed = new float[color.length];
            for (int i = 0; i < color.length; i++) {
                dimmed[i] = color[i] * 0.8f;
            }
            return dimmed;
        })
        .toArray(float[][]::new);
        assert section.length == colors.length;
        for (int i = 0; i < section.length; i++) {
            float[] color = colors[i];
            float[][] vertices = section[i];

            float[] vertex_data = new float[vertices.length * 8];
            for (int j = 0; j < vertices.length; j++) {
                vertex_data[j * 8 + 0] = vertices[j][0]; // x
                vertex_data[j * 8 + 1] = vertices[j][1]; // y
                vertex_data[j * 8 + 2] = vertices[j][2]; // z
                vertex_data[j * 8 + 3] = color[0];       // r
                vertex_data[j * 8 + 4] = color[1];       // g
                vertex_data[j * 8 + 5] = color[2];       // b
                vertex_data[j * 8 + 6] = vertices[j][0]; // s
                vertex_data[j * 8 + 7] = vertices[j][2]; // t
            }

            int[] indices = {};
            if (vertices.length == 3) {
                indices = new int[] { 0, 1, 2 };
            } else if (vertices.length == 4) {
                indices = new int[] { 1, 0, 2, 1, 3, 2 };
            }
            new Mesh(vertex_data, indices).draw(modelMatrix);
        }
    }
    public void drawMast(Boat boat) {

    } 
}
