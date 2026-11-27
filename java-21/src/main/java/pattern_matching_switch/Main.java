package pattern_matching_switch;

public class Main {

    public static void main(String[] args) {
        System.out.println(formatObjectOld(null));
        System.out.println(formatObject(null));

        System.out.println(formatObjectOld(new Object()));
        System.out.println(formatObject(new Object()));

        System.out.println(formatObjectOld(1));
        System.out.println(formatObject(1));

        System.out.println(formatObjectOld(0));
        System.out.println(formatObject(0));

        System.out.println(formatObjectOld("Test"));
        System.out.println(formatObject("Test"));
    }

    static String formatObject(Object obj) {
        return switch (obj) {
            case null -> "Empty value";
            case Integer i when i > 0 -> "Positive number: " + i;
            case Integer i -> "Number: " + i;
            case String s -> "String length " + s.length();
            default -> "Unknown type";
        };
    }

    static String formatObjectOld(Object obj) {
        if(obj == null) {
            return "Empty value";
        } else if (obj instanceof Integer i) {
            return i > 0 ? "Positive number: " + i : "Number: " + i;
        } else if (obj instanceof String s) {
            return "String length " + s.length();
        } else {
            return "Unknown type";
        }
    }
}
