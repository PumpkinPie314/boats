package game.server;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    public static float sail_speed = 1.0f/5;
    public static float turn_radius = 3.0f;
    public static float wheel_turn_speed = 1.0f/500;
    public static float mast_drop_speed = 1.0f/10;
    public static float mast_raise_speed = 1.0f/20;


    public static void createDefault(){
        try {
            String default_config = """
            sail_speed = 1/5
            turn_radius = 3
            wheel_turn_speed = 1/500 # wheel speed!
            mast_drop_speed = 1/10
            mast_raise_speed = 1/20
            """;
            Files.writeString(Paths.get(Main.CONFIG_FILE), default_config);
        } catch (IOException e) {
            System.err.println("could not write/create a new config file");
        }
    }public static void load(String filepath) throws IOException {
        Pattern pattern = Pattern.compile("^(\\w+)\\s*=\\s*([^#]+?)\\s*(?:#.*)?$");
        List<String> configFields = Arrays.stream(Config.class.getFields())
                                        .map(Field::getName)
                                        .toList();

        Files.lines(Paths.get(filepath)).forEach(line -> {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.matches()) {
                System.err.println("Config parse error on line: \"" + line + "\"");
                System.exit(1);
            }

            String fieldName = matcher.group(1);
            String rawValue = matcher.group(2).trim();
            if (!configFields.contains(fieldName)) {
                System.err.println("Unknown config field: \"" + fieldName + "\"");
                System.exit(1);
            }

            try {
                Field field = Config.class.getField(fieldName);
                Class<?> type = field.getType();
                Object parsedValue;

                if (rawValue.matches("^\".*\"$") || rawValue.matches("^'.*'$")) {
                    parsedValue = rawValue.substring(1, rawValue.length() - 1);
                } else if (rawValue.matches("^-?\\d+(\\.\\d+)?$")) {
                    parsedValue = Float.parseFloat(rawValue);
                } else if (rawValue.matches("^-?\\d+\\s*/\\s*-?\\d+$")) {
                    String[] parts = rawValue.split("/");
                    float numerator = Float.parseFloat(parts[0].trim());
                    float denominator = Float.parseFloat(parts[1].trim());
                    if (denominator == 0) {
                        throw new ArithmeticException("Division by zero in config for " + fieldName);
                    }
                    parsedValue = numerator / denominator;
                } else {
                    System.err.println("Unsupported value format for field \"" + fieldName + "\": " + rawValue);
                    System.exit(1);
                    return;
                }

                if (type == float.class && parsedValue instanceof Float) {
                    field.setFloat(null, (float) parsedValue);
                } else if (type == String.class && parsedValue instanceof String) {
                    field.set(null, parsedValue);
                } else {
                    System.err.printf("Type mismatch: field %s is %s, but value \"%s\" is %s%n",
                                    fieldName, type.getSimpleName(),
                                    rawValue, parsedValue.getClass().getSimpleName());
                    System.exit(1);
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                System.err.println("Reflection error: " + e);
                System.exit(1);
            }
        });
    }
}
