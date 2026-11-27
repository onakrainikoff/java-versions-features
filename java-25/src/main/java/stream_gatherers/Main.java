package stream_gatherers;

import java.util.List;
import java.util.stream.Gatherers;

public class Main {
    static void main() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);

        // Разбиение потока на фиксированные окна размером 3
        List<List<Integer>> windows = numbers.stream()
                .gather(Gatherers.windowFixed(3))
                .toList();
        IO.println(windows); // Результат: [[1, 2, 3], [4, 5]]

        // Разбиение потока на скользящие окна размером 3
        windows = numbers.stream()
                .gather(Gatherers.windowSliding(3))
                .toList();
        IO.println(windows); // Результат: [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

        // Сворачиваем числа в 1 строку и продолжаем работу со стримом (Промежуточная свертка)
        String result = numbers.stream()
                .gather(Gatherers.fold(() -> "Numbers:", (acc, n) -> acc + " " + n))
                .map(String::toUpperCase) // Продолжаем цепочку после свертки
                .findFirst()
                .orElse("");
        IO.println(result); // Результат: NUMBERS: 1 2 3 4 5


        // Накопительная свертка / префиксные суммы
        List<Integer> sums = numbers.stream()
                .gather(Gatherers.scan(() -> 0, Integer::sum))
                .toList();
        IO.println(sums); // Результат: [1, 3, 6, 10, 15]

        // Параллельный map:
        // выполняет функцию преобразования для каждого элемента в отдельном виртуальном потоке
        // сохраняет исходный порядок элементов в потоке
        List<String> strs = numbers.stream()
                .gather(Gatherers.mapConcurrent(3, String::valueOf))
                .toList();
        System.out.println(strs); // Результат: [1, 2, 3, 4, 5]
    }
}
