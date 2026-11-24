package string_new_methods;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        String text = "First string\nSecond string";

        // Добавляем отступ в 4 пробела перед каждой строкой
        String indented = text.indent(4);
        System.out.println(indented);
        /* Результат:
            First string
            Second string
        */

        // Убираем 2 пробела из начала каждой строки
        String unindented = indented.indent(-2);
        System.out.println(unindented);
        /* Результат:
          First string
          Second string
        */


        String rawInput = "  java 17 features  ";
        List<String> words = rawInput
                .transform(String::trim)                // "java 17 features"
                .transform(String::toUpperCase)         // "JAVA 17 FEATURES"
                .transform(s -> List.of(s.split(" "))); // ["JAVA", "17", "FEATURES"]
        System.out.println(words);

        // Было
        String oldWay = String.format("""
        {
            "user": "%s",
            "role": "%s"
        }
        """, "Alex", "ADMIN");

        // Стало:
        String json = """
        {
            "user": "%s",
            "role": "%s",
            "active": %b
        }
        """.formatted("Alex", "ADMIN", true);
        System.out.println(json);
    }
}
