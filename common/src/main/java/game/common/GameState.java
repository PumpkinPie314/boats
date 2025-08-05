package game.common;

import java.io.Serializable;
import java.util.ArrayList;

import org.joml.Quaternionf;

public class GameState implements Serializable {
    public Config config = new Config();
    public ArrayList<Boat> boats = new ArrayList<>();
    public Quaternionf wind_direction = new Quaternionf();
}
