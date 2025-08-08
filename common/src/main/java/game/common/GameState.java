package game.common;

import java.io.Serializable;
import java.util.ArrayList;

import org.joml.Vector3f;

public class GameState implements Serializable {
    public int tick_current = 0;
    public Config config = new Config();
    public ArrayList<Boat> boats = new ArrayList<>();
    public Vector3f wind = new Vector3f();
}
