package game.server;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Boat {
    public Vector3f position = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public Quaternionf rotation = new Quaternionf();
    
    public float mast_down_percent = 0f;
    public float wheel_turn_percent = 0f;
    public float cananon_turn_percent = 0f;

}
