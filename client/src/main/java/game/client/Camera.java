package game.client;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {
    // used for view matrix
    private static Vector3f position = new Vector3f();
    private static Quaternionf rotation = new Quaternionf();
    //used for projection
    private static float left;
    private static float right;
    private static float bottom;
    private static float top;
    private static float far;

    public static void setPositionView(Vector3f new_position, Quaternionf new_rotation) {
        position = new_position;
        rotation = new_rotation;
        // both transformation are inverted because the view matrix transforms the world around the camera.
        Matrix4f viewMatrix = new Matrix4f()
            .identity()
            .translate(new Vector3f(position).negate())
            .rotate(new Quaternionf(rotation).invert());
        
        Opengl.setViewMatrix(viewMatrix);
    }
    public static void setProjection(float left ,float right ,float bottom ,float top ,float far){
        Matrix4f projectionMatrix = new Matrix4f();
        projectionMatrix.setOrtho(left, right, bottom, top, 0.1f, far);
        Opengl.setProjectionMatrix(projectionMatrix);
    }
    
    public static Vector3f    getPosition() {return position;}
    public static Quaternionf getRotation() {return rotation;}
}
