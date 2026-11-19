package try_with_resources_improvement;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Files.write(Path.of("./build/example.txt"), List.of("String 1", "String 2", "String 3"));


        BufferedReader reader = new BufferedReader(new FileReader("./build/example.txt"));
        // теперь можно передать переменную напрямую:
        try (reader) {
            System.out.println(reader.readLine());
        }
    }
}
