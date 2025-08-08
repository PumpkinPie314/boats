package game.common;

import java.io.Serializable;

import org.joml.Vector3f;

public class FireEvent implements Serializable{
    int tick;
    Vector3f position = new Vector3f();
    Vector3f velocity = new Vector3f();
}
