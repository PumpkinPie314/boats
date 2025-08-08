package game.common;

import java.io.Serializable;

public class Config implements Serializable{
    public float fps = 60;
    public float sail_speed = 0;
    public float turn_speed = 0;
    public float wheel_turn_speed = 0;
    public float mast_drop_speed = 0;
    public float mast_raise_speed = 0;
    public float cannon_angle_limit = 0;
    public float sail_turn_speed = 0;
    public float sail_angle_limit = 0;
    public float wind_drag = 0;
    public float water_drag = 0;
}