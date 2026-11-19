package stream_api_improvements;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        // Stream: брать элементы, пока условие истинно
        Stream.of(1, 2, 3, 4, 1, 2)
                .takeWhile(n -> n < 4) // Вернет 1, 2, 3 (остановится на 4)
                .forEach(System.out::println);
        // Optional: действие при наличии и отсутствии значения
        Optional<String> opt = Optional.empty();
        opt.ifPresentOrElse(
                val -> System.out.println("Found: " + val),
                () -> System.out.println("Empty")
        );
        // Predicate.not()
        List<String> nonBlank = List.of("A", " ", "B").stream()
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toList());
        System.out.println(nonBlank);
    }
}
