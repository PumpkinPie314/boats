package game.common;

import java.io.Serializable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Boat implements Serializable {
    public Vector3f position = new Vector3f();
    public Vector3f velocity = new Vector3f();
    public Quaternionf rotation = new Quaternionf();
    public Quaternionf cannonRotation = new Quaternionf();
    
    public int[] sectionDamage = new int[6];
    public float mast_down_percent = 0f;
    public float wheel_turn_percent = 0f;
    public float sail_turn_percent = 0f;
}
