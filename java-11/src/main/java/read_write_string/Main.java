package read_write_string;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Path path = Path.of("./build/example.txt"); // Path.of() тоже появился в Java 11
        // Запись строки в файл
        Files.writeString(path, "Write string to file");
        // Чтение весь файла в строку
        String content = Files.readString(path);
        System.out.println(content);
        // Запись списка строк в файл
        Files.write(path, List.of("String 1", "String 2", "String 3"));
        // Чтение строк в список
        List<String> list = Files.readAllLines(path);
        System.out.println(list);
    }
}
