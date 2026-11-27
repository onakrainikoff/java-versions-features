package sequenced_collections;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

public class Main {

    public static void main(String[] args) {
        SequencedCollection<String> list = new ArrayList<>(List.of("B", "C"));

        list.addFirst("A");
        System.out.println(list);  // [A, B, C]

        list.addLast("D");
        System.out.println(list); // [A, B, C, D]

        String first = list.getFirst();
        System.out.println(list.getFirst()); // "A"

        SequencedCollection<String> reversed = list.reversed();
        System.out.println(reversed); // [D, C, B, A]
    }
}
