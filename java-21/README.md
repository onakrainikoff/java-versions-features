# [Важные изменения в Java](../README.md) / Java 21 (LTS)

Релиз состоялся в сентябре 2023 года. Поддержка до января 2029.

## Содержание

- [1. Виртуальные потоки (Virtual Threads)](#1-virtual-threads)
- [2. Сопоставление с образцом в switch (Pattern Matching for switch)](#2-pattern_matching_switch)
- [3. Паттерны записей (Record Patterns)](#3-record_patterns)
- [4. Последовательные коллекции (Sequenced Collections)](#4-sequenced_collections)
- [5. Key Encapsulation Mechanism AP](#5-key_encapsulation)
- [6. Garbage Collector](#6-garbage_collector)
- [7. Другие изменения](#7-other)

<a id="1-virtual-threads"></a>
## 1. Виртуальные потоки (Virtual Threads)

**Виртуальные потоки** — это легковесные потоки, управляемые самой `JVM`, а не операционной системой. Они кардинально меняют подход к многопоточности в `Java`, позволяя писать простой синхронный (блокирующий) код, который по пропускной способности не уступает сложному асинхронному или реактивному коду.

<details>
<summary>Более подробно о виртуальных потоках</summary>

#### Традиционные потоки в Java (Platform Threads):

это тонкие обертки над потоками операционной системы. Они дорого стоят (около 1–2 МБ памяти на стек потока) и долго переключаются, поэтому их количество обычно ограничено несколькими тысячами через пулы потоков (например, в `Tomcat` или `HikariCP`).

#### Виртуальные потоки управляются самой JVM, а не ОС:

- **`Carrier` `Threads` (потоки-носители)**. `JVM` создает небольшой пул платформенных потоков (по умолчанию равный количеству ядер `CPU` на базе `ForkJoinPool`) и планирует выполнение виртуальных потоков поверх них.
- **Монтирование и размонтирование (`Mounting` / `Unmounting`)**. Когда виртуальный поток встречает блокирующую операцию ввода-вывода (сетевой запрос, чтение из БД, `Thread.sleep`), JVM сохраняет его стек в обычной куче (`Heap`) и открепляет (`unmounts`) от потока-носителя. Поток ОС тут же начинает выполнять другой готовый виртуальный поток.
- **Дешевизна**. Пустой виртуальный поток занимает всего несколько сотен байт памяти, а его создание и переключение происходят в пользовательском пространстве без системных вызовов ОС.

| Характеристика | Платформенные потоки (Platform) | Виртуальные потоки (Virtual) |
|---|---|---|
| Кто управляет  | Операционная система | JVM   |
| Размер стека  | Фиксированный (~1 МБ)  | Динамический в Heap (от сотен байт)   |
| Оптимальное количество  | Сотни / тысячи  | Сотни тысяч / миллионы|
| Паттерн использования  | Пулинг (`FixedThreadPool`)  | Создание под каждую задачу (`newVirtualThreadPerTaskExecutor`)  |
| Для каких задач                       | `CPU`-bound (тяжелые вычисления)  | `I/O`-bound (сеть, БД, файловая система)  |

#### Способы создания и запуска:

```java
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

// Основной способ для production: ExecutorService без пулинга
// На каждую задачу создается новый виртуальный поток, который умирает после выполнения
// Используем CompletableFuture
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
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

// Используем Future
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Integer>> results = new ArrayList<>();
    IntStream.range(0, 1000).forEach(i -> {
        Future<Integer> future = executor.submit(() -> {
           Thread.sleep(Duration.ofMillis(100));
           return i;
        });
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
```

#### Переход на виртуальные потоки требует изменения привычных паттернов проектирования:

- **Никогда не объединяйте виртуальные потоки в пул**. Пулы нужны для переиспользования дорогих ресурсов. Виртуальные потоки дешевые — создавайте новый на каждый входящий запрос. Если нужно ограничить параллелизм (например, чтобы не «положить» базу данных 10 000 одновременных запросов), используйте `Semaphore`.
- **Проблема `Pinning` («пригвождения»)**. В `Java 21` виртуальный поток не может открепиться от потока-носителя, если он блокируется внутри блока `synchronized` или при вызове нативного кода (`JNI`). Если таких блокировок много, потоки ОС окажутся заблокированными, и производительность упадет. Решение в `Java 21`: заменяйте `synchronized` на `ReentrantLock` в местах, где происходят `I/O`-операции (для выявления проблемы используйте флаг `JVM` `-Djdk.tracePinnedThreads=full`). (Примечание: в `Java 24` проблема `pinning` для `synchronized` была устранена на уровне `JVM`).
- **Осторожнее с `ThreadLocal`**. Поскольку виртуальных потоков могут быть миллионы, хранение тяжелых объектов (например, кэшей или буферов) в `ThreadLocal` приведет к резкому росту потребления памяти (`OutOfMemoryError`). Для передачи контекста лучше использовать `Immutable`-объекты или механизм `ScopedValue` (в `Java 21` он в статусе `Preview`).
- **Не ускоряют `CPU`-bound задачи**. Если поток непрерывно нагружает процессор математикой или парсингом без ожидания `I/O`, виртуальные потоки не дадут прироста — для таких задач по-прежнему нужны обычные пулы размером с количество ядер `CPU`.

#### Как использовать примитивы синхронизации в Java 21:

Почти весь пакет `java.util.concurrent` был адаптирован разработчиками JDK под виртуальные потоки. Но есть строгое правило: внутри критической секции нельзя блокировать поток ОС.

| Примитив / Механизм | Безопасен ли в Java 21? | Как использовать правильно |
|--- |--- |---|
| `ReentrantLock` / `ReadWriteLock`   | Да (Рекомендуется)  | При ожидании лока или вызове `Condition.await()` виртуальный поток корректно открепляется от потока ОС. Используйте вместо `synchronized` везде, где есть `I/O`.  |
| `Semaphore`, `CountDownLatch`, `CyclicBarrier`, `Phaser`   | Да   | Полностью поддерживают открепление виртуальных потоков при ожидании (`acquire()`, `await()`).  |
| `BlockingQueue` (`LinkedBlockingQueue`, `ArrayBlockingQueue`)   | Да   | Методы `put()` и `take()` корректно усыпляют виртуальный поток без удержания потока-носителя.  |
| `CompletableFuture` (`get(),` `join()`)   | Да   | Ожидание результата через `.get()` или `.join()` внутри виртуального потока теперь дешево и не блокирует поток ОС |
| `synchronized` и `Object.wait()`   | Осторожно (`Pinning`)   | В `Java 21` привязывает виртуальный поток к потоку ОС. Допустимо только для быстрых операций в памяти (изменить счетчик, обновить `HashMap`). Запрещено, если внутри есть сеть, БД, файлы или `Thread.sleep()`.<br/>В `Java 24` или новее, JEP 491 устранил `pinning` внутри `synchronized`, но для `Java 21 LTS` использование ReentrantLock остается обязательным стандартом). |


#### Как заменить пул потоков на Semaphore:

Раньше мы ограничивали нагрузку на базу данных или внешний `API` размером пула (например, `Executors.newFixedThreadPool(10)`). С виртуальными потоками вы запускаете все задачи сразу, но перед обращением к ресурсу запрашиваете разрешение у семафора.
Когда семафор занят, виртуальный поток не блокирует поток ОС — `JVM` просто открепляет (unmounts) его и отправляет в очередь ожидания семафора, пока не освободится слот.

```java
final HttpClient client = HttpClient.newHttpClient();
final Semaphore limiter = new Semaphore(5);

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 20).forEach(i -> {
        executor.submit(() -> {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://postman-echo.com/get?i=" + i)).GET().build();
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
```

</details>

[src](src/main/java/virtual_threads/Main.java)

**Официальная документация:**
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Virtual Threads](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)

<a id="2-pattern_matching_switch"></a>
## 2. Сопоставление с образцом в switch (Pattern Matching for switch)

Оператор и выражение `switch` теперь поддерживают проверку типов, обработку `null` внутри case и уточняющие условия `when` (guarded patterns)

```java
// До
static String formatObjectOld(Object obj) {
    if(obj == null) {
        return "Empty value";
    } else if (obj instanceof Integer i) {
        return i > 0 ? "Positive number: " + i : "Number: " + i;
    } else if (obj instanceof String s) {
        return "String length " + s.length();
    } else {
        return "Unknown type";
    }
}

// Теперь
static String formatObject(Object obj) {
    return switch (obj) {
        case null -> "Empty value";
        case Integer i when i > 0 -> "Positive number: " + i;
        case Integer i -> "Number: " + i;
        case String s -> "String length " + s.length();
        default -> "Unknown type";
    };
}
```

[src](src/main/java/pattern_matching_switch/Main.java)

**Официальная документация:**
- [JEP 441: Pattern Matching for switch](https://openjdk.org/jeps/441)
- [Pattern Matching for switch Expressions and Statements](https://docs.oracle.com/en/java/javase/21/language/pattern-matching-switch.html#GUID-E69EEA63-E204-41B4-AA7F-D58B26A3B232)

<a id="3-record_patterns"></a>
## 3. Паттерны записей (Record Patterns)

Позволяют деконструировать объекты `record` напрямую в `instanceof` и `switch`, извлекая их компоненты без ручного вызова геттеров (включая вложенные записи).

```java
record Point(int x, int y) {}
record Line(Point start, Point end) {}

static void printStartX(Object obj) {
    // Деконструкция вложенных record-классов
    if (obj instanceof Line(Point(int x, int y), Point end)) {
        System.out.println("The X coordinate of first point: " + x);
        System.out.println("The Y coordinate of first point: " + x);
        System.out.println("The second point: " + end);
    }
}
```

[src](src/main/java/record_patterns/Main.java)

**Официальная документация:**
- [JEP 440: Record Patterns](https://openjdk.org/jeps/440)

<a id="4-sequenced_collections"></a>
## 4. Последовательные коллекции (Sequenced Collections)

В иерархию коллекций добавлены интерфейсы `SequencedCollection`, `SequencedSet` и `SequencedMap`. Они унифицируют доступ к первому и последнему элементам, а также получение коллекции в обратном порядке для `List`, `Deque`, `LinkedHashSet` и `SortedSet`.

```java
SequencedCollection<String> list = new ArrayList<>(List.of("B", "C"));
list.addFirst("A"); // [A, B, C]
list.addLast("D");  // [A, B, C, D]

String first = list.getFirst(); // "A"
SequencedCollection<String> reversed = list.reversed(); 
```

[src](src/main/java/sequenced_collections/Main.java)

**Официальная документация:**
- [JEP 431: Sequenced Collections](https://openjdk.org/jeps/431)
- [Oracle Sequenced Collections](https://docs.oracle.com/en/java/javase/21/core/creating-sequenced-collections-sets-and-maps.html)

<a id="5-key_encapsulation"></a>
## 5. Key Encapsulation Mechanism AP

Добавлен стандартный криптографический `API` для механизмов инкапсуляции ключей (`KEM`), используемых для безопасного обмена симметричными ключами с помощью асимметричной криптографии и подготовки к постквантовым алгоритмам

```java
// Отправитель генерирует общий секрет и инкапсулирует его публичным ключом получателя
KEM kem = KEM.getInstance("DHKEM");
KEM.Encapsulator encapsulator = kem.newEncapsulator(receiverPublicKey);
KEM.Encapsulated encapsulated = encapsulator.encapsulate();

SecretKey secretKey = encapsulated.key();
byte[] encapsulationMessage = encapsulated.encapsulation();
```

**Официальная документация:**
- [JEP 452: Key Encapsulation Mechanism API](https://openjdk.org/jeps/452)

<a id="6-garbage_collector"></a>
## 6. Garbage Collector

В `Java 21` по умолчанию используется `G1 GC`.

Сборщик мусора `ZGC` получил поддержку разделения объектов на поколения (молодые и старые). Это позволяет чаще и дешевле собирать недавно созданные объекты, снижая нагрузку на `CPU` и потребление памяти при сохранении субмиллисекундных пауз.

```bash
# Включение поколенческого ZGC при запуске приложения
java -XX:+UseZGC -XX:+ZGenerational -jar app.jar
```
Официальная документация:
- [JEP 439: Generational ZGC](https://openjdk.org/jeps/439)

<a id="7-other"></a>
## 7. Другие изменения
- **Ограничение динамической загрузки агентов (JEP 451)**. `JVM` теперь выводит предупреждение при попытке динамически загрузить `Java`-агент в работающий процесс (например, через `Mockito` или `ByteBuddy` без явного флага `-XX:+EnableDynamicAgentLoading`).
- **Устаревание `Thread.stop()` и `finalize()`**. Вызов `Thread.stop()` теперь выбрасывает `UnsupportedOperationException`, а механизм финализации помечен на удаление (JEP 421).
- **Preview-фичи (требуют флага --enable-preview)**. В состав `JDK 21` вошли предварительные версии `Structured Concurrency` (JEP 453), `Scoped Values` (JEP 446), `Foreign Function & Memory API` (JEP 442), `Unnamed Patterns and Variables` _ (JEP 443) и `Unnamed Classes` / `Instance Main Methods` (JEP 445).


[Полный список изменений](https://openjdk.org/projects/jdk/21/jeps-since-jdk-17) 