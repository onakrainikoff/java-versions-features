package unnamed_variables;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


public class Main {
    static void main() {
        // В циклах
        List<String> list = List.of("A", "B", "C");
        int count =0;
        for (String _ : list) {
            count ++;
        }
        IO.println(count);

        // В лямбдах
        AtomicLong counter= new AtomicLong();
        list.forEach(_-> counter.incrementAndGet());
        IO.println(counter);

        // В блоках catch
        String input = "test";
        try {
            int number = Integer.parseInt(input);
        } catch (NumberFormatException _) {
           IO.println("Incorrect number format");
        }

        // В Pattern Matching
        record Point(int x, int y){};
        var obj = new Point(100,1);
        if (obj instanceof Point(int x, _)) {
            IO.println("X: " + x);
        }
    }
}
