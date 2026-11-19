package new_string_methods;

public class Main {

    public static void main(String[] args) {
        String text = "  \n  Hello\nWorld  \t ";
        System.out.println("   ".isBlank()); // true
        System.out.println("Java ".repeat(3)); // "Java Java Java "
        System.out.println(text.strip()); // "Hello\nWorld"

        // Разделение на строки и фильтрация
        "First\n\nSecond".lines()
                .filter(line -> !line.isBlank())
                .forEach(System.out::println);
    }
}
