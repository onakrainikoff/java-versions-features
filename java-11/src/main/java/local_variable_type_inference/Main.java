package local_variable_type_inference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.NonNull;

public class Main {
    public static void main(String[] args) {
        // Раньше
        Map<String, List<String>> userRolesOld = new HashMap<>();

        // Теперь
        var userRoles = new HashMap<String, List<String>>();
        var message = "Hello, Java 11!"; // Тип String

        // В лямбдах (Java 11) — можно ставить аннотации
        BiFunction<Integer, Integer, Integer> sum = (@NonNull var a, var b) -> a + b;
    }
}
