package game.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.glfw.GLFW;


public class Config {
    public static String serverip;
    public static int serverport;
    public static int connectiontimout;
    public static int sailup;
    public static int saildown;
    public static int wheelleft;
    public static int wheelright;
    public static int sailleft;
    public static int sailright;
    
    public static void createDefault(){
        try {
            String default_config = """
            serverip = localhost
            serverport = 52025
            connectiontimout = 1000
            saildown = GLFW_KEY_W
            sailup = GLFW_KEY_S
            wheelleft = GLFW_KEY_A
            wheelright = GLFW_KEY_D
            sailleft = GLFW_KEY_X
            sailright = GLFW_KEY_C

            """;
            Files.writeString(Paths.get(Main.CONFIG_FILE), default_config);
        } catch (IOException e) {
            System.err.println("could not write/create a new config file");
        }
    }

    public static void load(String filepath){
        try {
            Files.lines(Paths.get(filepath))
                .filter(s -> s.matches("\\s*\\w+\\s*=\\s*\\w+\\s*"))
                .map(line -> line.split("\\s*=\\s*", 2))
                .forEach(pair -> {
                    String fieldName = pair[0].trim();
                    String value = pair[1].trim();
                    try {
                        Field field = Config.class.getField(fieldName);
                        Class<?> type = field.getType();
                        if (type == String.class) {
                            field.set(null, value);
                        } else if (type == int.class) {
                            if (value.matches("\\d+")){
                                field.setInt(null, Integer.parseInt(value));
                            } else {
                                Field glfwField = GLFW.class.getField(value);
                                int intValue = glfwField.getInt(null);
                                field.setInt(null, intValue);
                            }
                        }
                    } catch (IllegalAccessException | SecurityException | NoSuchFieldException e) {
                        System.err.println("failed to parse config file: " + e);
                    }
            });  
        } catch (IOException e) {
            System.err.println("failed to parse config file: " + e);
        }
        for (Field field: Config.class.getFields()){
            try {
                Object value = field.get(null);
                Class <?> type = field.getType();
                if (type == String.class && value != null) continue;
                if (type == int.class && (Integer) value != 0) continue;
                System.out.println("WARNING!: " + field.getName() + " may be missing from config");
            } catch (IllegalAccessException | IllegalArgumentException e) {
                System.err.println("failed to itterate over parsed config file: "+e);
            }
        }
    }
    // wow recursion is so cool! 
    // also try catch is ugly

}
