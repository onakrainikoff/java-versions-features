package collections_factory_methods;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        // Раньше
        List<String> oldList = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));

        // Теперь
        List<String> list = List.of("A", "B", "C");
        Set<Integer> set = Set.of(1, 2, 3);
        Map<String, Integer> map = Map.of("One", 1, "Two", 2);

        List<String> source = new ArrayList<>(List.of("A", "B"));
        List<String> immutableCopy = List.copyOf(source);
    }
}
