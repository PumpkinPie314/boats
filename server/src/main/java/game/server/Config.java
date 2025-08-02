package game.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;;

public class Config {
    public static float sail_speed = 1/5;
    public static float turn_speed = 3;
    public static float wheel_turn_speed = 1/500;
    public static float sail_turn_speed = 1/20;
    public static float mast_drop_speed = 1/10;
    public static float mast_raise_speed = 1/20;
    public static float wind_drag = 1/20;
    public static float water_drag = 1/20;


    public static void createDefault(){
        try {
            String default_config = ""; // TODO!
            Files.writeString(Paths.get(Main.CONFIG_FILE), default_config);
        } catch (IOException e) {
            System.err.println("could not write/create a new config file");
        }
    }
    public static void load(String filepath) throws IOException {
        ArrayList<String> availableFields = Arrays.stream(Config.class.getFields()).map(Field::getName).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> assignedFields = new ArrayList<>(){};
        Files.lines(Paths.get(filepath)).forEach(l -> {
            String line = l;
            if (line.startsWith("#")) return; // line is a comment
            if (line.contains("#")) line = line.substring(0, line.indexOf('#')); // strip end of line comments
            String[] parts = line.split("=");
            String lhs = parts[0].trim();
            String rhs = parts[1].trim();
            try {
                Field field = Config.class.getField(lhs);
                if (field.getType() == float.class) {field.set(null, parseFloat(rhs)); System.out.println("set " + lhs + " to " + rhs + " (" + parseFloat(rhs) +")");}
                else if (field.getType() == String.class) field.set(null, rhs);
                else {
                    throw new IllegalArgumentException("config field type: " + field.getType().getName() + " is not supported yet!");
                }
                assignedFields.add(lhs);
            } catch (NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("error reading config file: " + lhs + " is not a valid config field");
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        availableFields.removeAll(assignedFields);
        if (!availableFields.isEmpty()) throw new IllegalStateException(
            "config file did not define the fallowing feilds: " +
            availableFields.stream().map(f->{
                return "\n" + f + " = ";
            }).collect(Collectors.joining())
        );
    }
    public static float parseFloat(String s) {
        if (s.contains("/")) {
            String[] fraction = s.split("/");
            Float numerator = parseFloat(fraction[0].trim());
            Float denominator = parseFloat(fraction[1].trim());
            return numerator/denominator;
        }
        if (s.contains("tau")) return parseFloat(s.replace("tau", "").trim()) * (float) Math.TAU;
        if (s.contains("pi")) return parseFloat(s.replace("pi", "").trim()) * (float) Math.PI;
        // if (s.contains("dt")) return parseFloat(s.replace("dt", "").trim()) * 1/Main.target_fps;
        return Float.parseFloat(s);
    }
}
