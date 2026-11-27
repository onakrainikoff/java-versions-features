# [Важные изменения в Java](../README.md) / Java 11 (LTS)

Релиз состоялся в сентябре 2018 года. Поддержка до января 2032.

## Содержание

- [1. Локальный вывод типов — ключевое слово var](#1-local-variable-type-inference)
- [2. Фабричные методы для создания коллекций](#2-collection-factory-methods)
- [3. Новый стандартный HTTP-клиент](#3-http-client)
- [4. Новые методы для работы со строками (String)](#4-string-methods)
- [5. Упрощенное чтение и запись файлов](#5-read-write-files)
- [6. Улучшенный try-with-resources](#6-try-with-resources)
- [7. Приватные методы в интерфейсах](#7-private-interface-methods)
- [8. Улучшения в Stream API и Optional](#8-stream-api)
- [9. Reactive Streams (Flow API)](#9-reactive-streams)
- [10. Адаптация JVM к Docker-контейнерам (Container Awareness)](#10-container-awareness)
- [11. Система модулей — Project Jigsaw](#11-project-jigsaw)
- [12. Garbage Collector](#12-garbage-collector)
- [13. Другие изменения](#13-other-changes)

<a id="1-local-variable-type-inference"></a>
## 1. Локальный вывод типов — ключевое слово var

Позволяет не дублировать тип локальной переменной, если компилятор может вывести его из правой части выражения. Также добавили поддержку `var` для параметров лямбда-выражений (полезно для добавления аннотаций).

```java
// Раньше
Map<String, List<String>> userRolesOld = new HashMap<>();

// Теперь
var userRoles = new HashMap<String, List<String>>();
var message = "Hello, Java 11!"; // Тип String

// В лямбдах — можно ставить аннотации
BiFunction<Integer, Integer, Integer> sum = (@NonNull var a, var b) -> a + b;
```
[src](src/main/java/local_variable_type_inference/Main.java)

**Официальная документация:** 
- [JEP 286: Local-Variable Type Inference (Java 10)](https://openjdk.org/jeps/286)
- [JEP 323: Local-Variable Syntax for Lambda Parameters (Java 11)](https://openjdk.org/jeps/323)

<a id="2-collection-factory-methods"></a>
## 2. Фабричные методы для создания коллекций

Появились методы `List.of()`, `Set.of()` и `Map.of()`, которые создают неизменяемые (`immutable`) коллекции в одну строку.
В интерфейсы `List`, `Set` и `Map` добавлен статический метод `copyOf()`, возвращающий неизменяемую копию переданной коллекции.

```java
// Раньше
List<String> oldList = Collections.unmodifiableList(Arrays.asList("A", "B", "C"));

// Теперь
List<String> list = List.of("A", "B", "C");
Set<Integer> set = Set.of(1, 2, 3);
Map<String, Integer> map = Map.of("One", 1, "Two", 2);

List<String> source = new ArrayList<>(List.of("A", "B"));
List<String> immutableCopy = List.copyOf(source);
```

[src](src/main/java/collections_factory_methods/Main.java)

**Официальная документация:**
- [JEP 269: Convenience Factory Methods for Collections](https://openjdk.org/jeps/269)

<a id="3-http-client"></a>
## 3. Новый стандартный HTTP-клиент

Устаревший HttpURLConnection заменили на современный `java.net.http.HttpClient` с поддержкой ` HTTP/2`, `WebSocket`, синхронных и асинхронных запросов.

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://example.com"))
        .GET()
        .build();

// Синхронный запрос
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());

// Асинхронный запрос
client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply(HttpResponse::body)
        .thenAccept(System.out::println);
```

[src](src/main/java/new_htt_client/Main.java)

**Официальная документация:**
- [JEP 321: HTTP Client (Standard)](https://openjdk.org/jeps/321)

<a id="4-string-methods"></a>
## 4. Новые методы для работы со строками (String)

Добавлены полезные методы, избавляющие от необходимости подключать сторонние библиотеки вроде `Apache Commons`:
- `isBlank()` — проверяет, пуста ли строка или состоит только из пробелов.
- `lines()` — разбивает строку на `Stream<String>` по переносам строк.
- `strip()` — удаляет пробелы в начале и конце (корректно работает с `Unicode`, в отличие от `trim()`).
- `repeat(int)` — повторяет строку заданное количество раз.

```java
String text = "  \n  Hello\nWorld  \t ";
System.out.println("   ".isBlank()); // true
System.out.println("Java ".repeat(3)); // "Java Java Java "
System.out.println(text.strip()); // "Hello\nWorld"

// Разделение на строки и фильтрация
"First\n\nSecond".lines()
        .filter(line -> !line.isBlank())
.forEach(System.out::println);
```

[src](src/main/java/new_string_methods/Main.java)

<a id="5-read-write-files"></a>
## 5. Упрощенное чтение и запись файлов

В класс `java.nio.file.Files` добавили методы `readString()` и `writeString()` для работы с текстовыми файлами в одну строчку.

```java
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
```

[src](src/main/java/read_write_string/Main.java)

<a id="6-try-with-resources"></a>
## 6. Улучшенный try-with-resources

В конструкцию `try-with-resources` больше не нужно объявлять новую локальную переменную, если ресурс уже объявлен как `final` или `effectively final` (не изменяется после инициализации).

```java
BufferedReader reader = new BufferedReader(new FileReader("data.txt"));
// теперь можно передать переменную напрямую:
try (reader) {
    System.out.println(reader.readLine());
}
```

[src](src/main/java/try_with_resources_improvement/Main.java)

**Официальная документация:**
- [Milling Project Coin (JEP 213)](https://openjdk.org/jeps/213)

<a id="7-private-interface-methods"></a>
## 7. Приватные методы в интерфейсах

После появления `default`-методов в Java 8 возникла проблема дублирования кода внутри интерфейсов. Теперь разрешено создавать `private` методы для переиспользования логики.

```java
public interface Logger {
    default void logInfo(String message) {
        log(message, "INFO");
    }

    default void logError(String message) {
        log(message, "ERROR");
    }

    private void log(String message, String level) {
        System.out.println("[" + level + "] " + message);
    }
}
```

[srс](src/main/java/private_interface_methods/Main.java)

**Официальная документация:**
- [Private Interface Methods](https://docs.oracle.com/javase/tutorial/java/IandI/defaultmethods.html)

<a id="8-stream-api"></a>
## 8. Улучшения в Stream API и Optional

- `Stream API`: появились `takeWhile()`, `dropWhile()` и `ofNullable()`.
- `Optional`: добавлены `isEmpty()` (обратный к `isPresent()`), `ifPresentOrElse()` и `or()`.
- `Predicate`: добавлен статический метод `Predicate.not()` для инвертирования ссылок на методы.

```java
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
```

[src](src/main/java/stream_api_improvements/Main.java)

<a id="9-reactive-streams"></a>
## 9. Reactive Streams (Flow API)

В пакет `java.util.concurrent` добавлен класс `Flow` с вложенными интерфейсами (`Publisher`, `Subscriber`, `Subscription`, `Processor`), реализующими стандарт реактивных потоков с поддержкой обратного давления (`backpressure`)
<details>
<summary>Более подробно о Flow API</summary>

### Четыре базовых интерфейса Flow

Класс `java.util.concurrent.Flow` выступает как пространство имен и содержит четыре вложенных функциональных и обычных интерфейса:
- `Flow.Publisher<T>` — поставщик элементов. Имеет единственный метод `subscribe(Subscriber<? super T> subscriber)`.
- `Flow.Subscriber<T>` — потребитель данных. Содержит четыре метода обратного вызова:
- `onSubscribe(Subscription s)` — вызывается один раз при регистрации подписки;
- `onNext(T item)` — вызывается при получении очередного элемента;
- `onError(Throwable t)` — вызывается при невосстановимой ошибке (после этого поток закрывается);
- `onComplete()` — вызывается, когда источник успешно передал все данные.
- `Flow.Subscription` — связующее звено между `Publisher` и `Subscriber`, реализующее backpressure:
- `request(long n)` — подписчик запрашивает следующие n элементов;
- `cancel() `— подписчик отменяет подписку и освобождает ресурсы.
- `Flow.Processor<T, R>` — промежуточный узел (трансформатор), который одновременно наследует и `Subscriber<T>`, и `Publisher<R>`. Он принимает данные типа `T`, преобразует их и передает дальше как `R`.

### Готовая реализация: SubmissionPublisher<T>

Вместе с интерфейсами JDK предоставляет единственную встроенную реализацию издателя — класс `SubmissionPublisher<T>`. Он асинхронно доставляет элементы подписчикам (по умолчанию используя `ForkJoinPool.commonPool()`) и автоматически управляет буферизацией для каждого подписчика.

### Важные особенности и ограничения

- Это SPI, а не полноценный фреймворк. В отличие от `Project Reactor` или `RxJava`, в `java.util.concurrent.Flow` нет готовых операторов вроде `map()`, `filter()`, `flatMap()` или `zip()`. `Flow` создан как стандартный мост для интеграции библиотек между собой.
- Блокировка при переполнении буфера. По умолчанию буфер `SubmissionPublisher` вмещает 256 элементов (`Flow.defaultBufferSize()`). Если подписчик не успевает запрашивать данные и буфер заполняется, метод `submit()` начнет блокировать поток-источник. Для неблокирующей публикации с отбрасыванием элементов используется метод `offer()`.
- Интеграция с `HttpClient`. Встроенный в Java 9/11 `java.net.http.HttpClient` активно использует `Flow.Publisher` и `Flow.Subscriber` для потоковой отправки и чтения тела HTTP-запросов и ответов без загрузки больших файлов целиком в оперативную память.
</details>


```java
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
```

[src](src/main/java/flow_api/Main.java)

**Официальная документация:**
- [JEP 266: More Concurrency Updates](https://openjdk.org/jeps/266)

<a id="10-container-awareness"></a>
## 10. Адаптация JVM к Docker-контейнерам (Container Awareness)

На `Linux`-платформах `JVM` научилась корректно определять ограничения по памяти и количеству ядер `CPU`, заданные для `Docker`-контейнера через cgroups, вместо того чтобы ориентироваться на ресурсы хост-машины. Появились флаги для процентного управления памятью кучи относительно лимита контейнера.

```bash
# Настройка размера кучи в процентах от лимита памяти контейнера
java -XX:InitialRAMPercentage=25.0 -XX:MaxRAMPercentage=75.0 -jar app.jar
```

**Официальная документация:**
- [Consolidated JDK 10 Release Notes (HotSpot)](https://www.oracle.com/java/technologies/javase/10all-relnotes.html)

<a id="11-project-jigsaw"></a>
## 11. Система модулей — Project Jigsaw

Сам `JDK` был разбит на модули (вместо монолитного `rt.jar`), а разработчики получили возможность создавать свои модули через файл `module-info.java` для строгого контроля зависимостей и инкапсуляции пакетов.

```java
module com.example.myapp {
    requires java.net.http; // Явно указываем зависимость от модуля HTTP-клиента
    exports com.example.myapp.api; // Открываем наружу только этот пакет
}
```

<details>
<summary>Более подробно о модулях</summary>

Система модулей (`JPMS` — `Java Platform Module System`, `Project Jigsaw`) стала самым масштабным архитектурным изменением со времен выхода первой версии `Java`.
Если раньше `Java`-приложение представляло собой плоский набор пакетов и классов (так называемый «ад `classpath`»), то модули добавили уровень абстракции над пакетами.

### Зачем это было нужно (Проблемы Java 8)

1. Монолитный `rt.jar`: Стандартная библиотека весила десятки мегабайт и грузилась целиком, даже если приложению требовался только вывод текста в консоль.
2. Слабая инкапсуляция: Модификатор `public` делал класс доступным абсолютно всем. Нельзя было сделать класс публичным для использования внутри вашей библиотеки, но скрыть его от внешних пользователей.
3. Проблемы с зависимостями (`JAR Hell`): `JVM` узнавала об отсутствии нужного класса только в момент выполнения (`NoClassDefFoundError`), а не на этапе старта.

### Как устроен модуль: module-info.java

Чтобы превратить обычный проект в модуль, в корень исходников (`src/main/java`) добавляется дескриптор — файл `module-info.java`. В нем описываются зависимости модуля и его публичный API.

```java
module com.example.payment {
    // 1. Что нужно этому модулю для работы
    requires java.sql;
    requires transitive com.example.logging;

    // 2. Какие пакеты видны другим модулям
    exports com.example.payment.api;

    // 3. Кому разрешено лезть внутрь через Reflection (нужно для Spring, Hibernate)
    opens com.example.payment.model to org.hibernate.orm.core, spring.core;
}
```
Основные директивы:

- `requires` — указывает, от какого модуля зависит текущий. Если модуль не найден при запуске, JVM сразу упадет с понятной ошибкой.
- `requires transitive` — транзитивная зависимость. Если модуль A зависит от B (`transitive`), а C зависит от A, то C автоматически получает доступ к B.
- `exports` — открывает указанный пакет для внешнего мира. Все остальные пакеты в модуле по умолчанию скрыты, даже если классы в них помечены как public!
- `opens` — разрешает доступ к пакету во время выполнения (`Runtime`) через механизм рефлексии (`Reflection API`), но запрещает прямое обращение на этапе компиляции.

### Влияние на стандартный JDK

Сам `JDK` был разбит примерно на 70–80 независимых модулей. Базовый модуль — java.base (в нем лежат `java.lang`, `java.util`, `java.io` и т.д.). Он подключается неявно ко всем проектам, как пакет `java.lang`.
Остальные части теперь нужно подключать явно:
- Нужен `Swing/AWT`? `requires java.desktop`;
- Нужен `JDBC`? `requires java.sql`;
- Нужен новый `HTTP`-клиент? `requires java.net.http`;

### Что это дало на практике?

1. **Надежная инкапсуляция**: Разработчики библиотек получили возможность прятать внутреннюю реализацию (`com.lib.internal.*`), не боясь, что пользователи начнут на нее завязываться.
2. **Уменьшение размера сборок (`jlink`)**: Появилась утилита `jlink`, которая позволяет собрать кастомный минимальный `JRE`, содержащий только те модули JDK, которые реально нужны вашему приложению. Вместо 200+ МБ образ `Docker` с Java-приложением может весить 30–40 МБ.
3. **Строгая проверка при старте**: `JVM` строит граф зависимостей до запуска `main()` и проверяет отсутствие циклических зависимостей и дублирующихся пакетов (`split packages`).

### Обратная совместимость (Как мигрировать?)

Создатели `Java` понимали, что весь мир не перепишет код за один день, поэтому ввели два понятия для старого кода:
- `Unnamed Module` (Безымянный модуль): Все `JAR`-файлы, запущенные по старинке через `-classpath`, попадают сюда. Они видят друг друга и все модули JDK. Именно поэтому старый код с Java 8 в большинстве случаев продолжает работать на `Java 11` без изменений.
- `Automatic Module` (Автоматический модуль): Если положить старый `JAR` (без `module-info.java`) в новый `--module-path`, `Java` сама превратит его в модуль, взяв имя из названия файла. Это позволяет постепенно переводить проект на модули сверху вниз.

</details>

**Официальная документация:**
- [JEP 261: Module System](https://openjdk.org/jeps/261)
- [JEP 200: The Modular JDK](https://openjdk.org/jeps/200)

<a id="12-garbage-collector"></a>
## 12. Garbage Collector

`G1 Garbage Collector` стал сборщиком мусора по умолчанию вместо `Parallel GC`: [JEP 248: Make G1 the Default Garbage Collector](https://openjdk.org/jeps/248).

<a id="13-other-changes"></a>
## 13. Другие изменения

- Компактные строки: внутреннее представление `String` изменено с `char[]` (2 байта на символ) на `byte[]` с флагом кодировки (1 байт для `Latin-1`), что существенно снизило потребление памяти. [JEP 254: Compact Strings](https://openjdk.org/jeps/254).
- Удаление устаревших модулей (`Java 11`): из состава `JDK` полностью удалены модули `Java EE` и `CORBA` (например, `javax.xml.bind` / `JAXB`) — теперь их нужно подключать как внешние зависимости (через `Maven` или `Gradle`). [JEP 320: Remove the Java EE and CORBA Modules](https://openjdk.org/jeps/320).

[Полный список изменений](https://www.oracle.com/java/technologies/javase/11-relnote-issues.html)