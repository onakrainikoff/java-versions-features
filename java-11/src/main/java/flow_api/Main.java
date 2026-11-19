package flow_api;

import java.util.concurrent.Flow.*;
import java.util.concurrent.SubmissionPublisher;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // Создаем издателя
        try (SubmissionPublisher<String> publisher = new SubmissionPublisher<>()) {
            // Создаем подписчика с пошаговым запросом данных
            Subscriber<String> subscriber = new Subscriber<String>() {
                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription subscription) {
                    this.subscription = subscription;
                    System.out.printf("[%d] The subscription is completed. Requesting 1 element.\n", Thread.currentThread().getId());
                    subscription.request(1); // Запрос первой порции
                }

                @Override
                public void onNext(String item) {
                    System.out.printf("[%d] Received: %s\n", Thread.currentThread().getId(), item);
                    // После обработки запрашиваем следующий элемент
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                }

                @Override
                public void onComplete() {
                    System.out.printf("[%d] The data flow is completed.", Thread.currentThread().getId());
                }
            };

            // Подписываемся и публикуем данные
            System.out.printf("[%d] Start.\n", Thread.currentThread().getId());
            publisher.subscribe(subscriber);
            publisher.submit("Event 1");
            publisher.submit("Event 2");
            publisher.submit("Event 3");
            Thread.sleep(500);
            System.out.printf("[%d] Stop.\n", Thread.currentThread().getId());
        } // Метод close() у SubmissionPublisher автоматически вызовет onComplete()
        // Даем асинхронным задачам в ForkJoinPool время завершиться
        Thread.sleep(500);
    }
}
