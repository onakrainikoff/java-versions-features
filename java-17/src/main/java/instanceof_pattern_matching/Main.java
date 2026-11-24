package instanceof_pattern_matching;

public class Main {
    public static void main(String[] args) {
        Object obj = "Test";

        // Было
        if (obj instanceof String) {
            String s = (String) obj;
            System.out.println(s.length());
        }

        // Стало
        if (obj instanceof String s && !s.isEmpty()) {
            System.out.println(s.length());
        }
    }
}
