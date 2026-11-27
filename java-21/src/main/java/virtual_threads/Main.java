package virtual_threads;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        exampleCreationVirtualThreads();
        exampleVirtualThreadPerTaskExecutor1();
        exampleVirtualThreadPerTaskExecutor2();
        exampleSemaphore();
        Thread.sleep(Duration.ofSeconds(1));
    }


    public static void exampleCreationVirtualThreads() throws InterruptedException {
        // Быстрый запуск фонового виртуального потока
        Thread vThread = Thread.startVirtualThread(() -> {
            System.out.println("Thread: " + Thread.currentThread());
        });
        vThread.join();

        // Тонкая настройка через Builder (имя, наследование ThreadLocal и т.д.)
        Thread.Builder.OfVirtual builder = Thread.ofVirtual()
                .name("test-", 0)
                .uncaughtExceptionHandler((Thread t, Throwable e) -> {
                    e.printStackTrace();
                });
        Thread vt1 = builder.unstarted(() -> System.out.println("Thread: " + Thread.currentThread() + " start with name"));
        vt1.start();
        Thread vt2 = builder.start(() -> System.out.println("Thread: " + Thread.currentThread() + " start with name"));
        vt1.join();
        vt2.join();
    }

    public static void exampleVirtualThreadPerTaskExecutor1() {
        // Основной способ для production: ExecutorService без пулинга
        // На каждую задачу создается новый виртуальный поток, который умирает после выполнения
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Используем CompletableFuture
            var completableFutures = IntStream.range(0, 1000).mapToObj(i ->
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(Duration.ofMillis(100)); // Имитация IO
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return i;
                    }, executor)
            ).toList();
            int sum = completableFutures.stream()
                        .map(CompletableFuture::join)
                        .mapToInt(Integer::intValue).sum();
            System.out.println("Sum=" + sum);
        }
    }

    public static void exampleVirtualThreadPerTaskExecutor2() {
        // Основной способ для production: ExecutorService без пулинга
        // На каждую задачу создается новый виртуальный поток, который умирает после выполнения
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // Используем Future
            List<Future<Integer>> results = new ArrayList<>();
            IntStream.range(0, 1000).forEach(i -> {
                Future<Integer> future = executor.submit(() -> {
                            Thread.sleep(Duration.ofMillis(100));
                            return i;
                        }
                );
                results.add(future);
            });
            int sum = results.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).mapToInt(Integer::intValue).sum();
            System.out.println("Sum=" + sum);
        }
    }


    public static void exampleSemaphore() {
        final Semaphore limiter = new Semaphore(5);
        final HttpClient client = HttpClient.newHttpClient();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 20).forEach(i -> {
                executor.submit(() -> {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://postman-echo.com/get?i=" + i))
                            .GET().build();
                    try {
                        limiter.acquire();
                        Thread.sleep(Duration.ofSeconds(1));
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        System.out.println("Thread: " + Thread.currentThread() + " response=" + response.body());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        limiter.release();
                    }
                });
            });
        }

    }

}
