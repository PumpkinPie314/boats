package game.common;

import java.io.Serializable;
import org.joml.Quaternionf;

public class InputState implements Serializable {
    public boolean saildown = false;
    public boolean sailup = false;
    public boolean wheelleft = false;
    public boolean wheelright = false;
    public boolean sailleft = false;
    public boolean sailright = false;
    public Quaternionf cannon_rotation = new Quaternionf();
}
