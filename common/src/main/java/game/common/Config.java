package game.common;

import java.io.Serializable;

public class Config implements Serializable{
    public float fps = 60;
    public float sail_speed = 1;
    public float turn_speed = 1;
    public float wheel_turn_speed = 1;
    public float sail_turn_speed = 1;
    public float mast_drop_speed = 1;
    public float mast_raise_speed = 1;
    public float wind_drag = 1;
    public float water_drag = 1;
}