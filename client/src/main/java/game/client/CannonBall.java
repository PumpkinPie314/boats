package game.client;

import org.joml.Vector3f;
import game.common.Boat;

public class CannonBall{
    public Boat owner;
    int tick_fired;
    Vector3f position = new Vector3f();
    Vector3f velocity = new Vector3f();
}