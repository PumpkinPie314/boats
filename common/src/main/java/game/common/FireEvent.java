package game.common;

import java.io.Serializable;

import org.joml.Vector3f;

public class FireEvent implements Serializable, Cloneable{
    public int tick_fired;
    public Vector3f position = new Vector3f();
    public Vector3f velocity = new Vector3f();
}
