package switch_expressions;

import java.time.DayOfWeek;

public class Main {
    public static void main(String[] args) {
        DayOfWeek day = DayOfWeek.MONDAY;
        String dayType = switch (day) {
            case MONDAY, FRIDAY, SUNDAY -> "Short or weekend";
            case TUESDAY -> {
                System.out.println("Processing...");
                yield "An ordinary day";
            }
            default -> "Other days";
        };
        System.out.println(dayType);
    }
}
