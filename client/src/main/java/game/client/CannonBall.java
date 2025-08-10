package game.client;

import org.joml.Vector3f;

public class CannonBall{
    public int owner_boat_id = -1;
    int tick_fired;
    Vector3f position = new Vector3f();
    Vector3f velocity = new Vector3f();
}