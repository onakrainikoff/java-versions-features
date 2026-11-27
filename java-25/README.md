# [Важные изменения в Java](../README.md) / Java 25 (LTS)


Релиз состоялся в сентябре 2025 года. Поддержка до января 2033.

## Содержание
- [1. Гибкие тела конструкторов (Flexible Constructor Bodies)](#1-flexible_constructor_bodies)
- [2. Неименованные переменные и паттерны (Unnamed Variables & Patterns)](#2-unnamed_variables)
- [3.Компактные экземплярные методы main (Compact Instance Main Methods)](#3-compact_instance_main_methods)
- [4. Промежуточные операции в Stream API (Stream Gatherers)](#4-stream_gatherers)
- [5. Scoped Values](#5-scoped_values)
- [6. Синхронизация виртуальных потоков без пиннинга](#6-synchronize_virtual_threads)
- [7. Garbage Collector](#7-gc)
- [8. Другие изменения](#8-other)


<a id="1-flexible_constructor_bodies"></a>
## 1. Гибкие тела конструкторов (Flexible Constructor Bodies)

Теперь в конструкторах разрешено выполнять инструкции до вызова `super(...)` или `this(...)`. Это позволяет валидировать аргументы и инициализировать поля до создания родительского класса, но обращаться к `this` до вызова `super()` по-прежнему запрещено.

```java
public class PositiveBigInteger extends BigInteger {
    public PositiveBigInteger(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("The value must be positive");
        }
        super(String.valueOf(value)); // Раньше super() обязан был быть первой строкой
    }
}
```

[src](src/main/java/flexible_constructor_bodies/Main.java)

**Официальная документация:**
- [JEP 513: Flexible Constructor Bodies](https://openjdk.org/jeps/513)

<a id="2-unnamed_variables"></a>
## 2. Неименованные переменные и паттерны (Unnamed Variables & Patterns)
Символ подчеркивания `_` можно использовать для обозначения переменных или компонентов паттернов, которые объявляются, но не используются. Это избавляет от лишнего визуального шума.

```java
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
```

[src](src/main/java/unnamed_variables/Main.java)

**Официальная документация:**
- [JEP 456: Unnamed Variables & Patterns](https://openjdk.org/jeps/456)

<a id="3-compact_instance_main_methods"></a>
## 3.Компактные экземплярные методы main (Compact Instance Main Methods)

Для небольших программ и скриптов больше не требуется объявлять `public class`, `static` и `String[] args`. Также появился класс `java.lang.IO` для упрощенного консольного ввода-вывода.

```java
void main() {
    String name = IO.readln("What is your name? ");
    IO.println("Hi, " + name + "!");
}
```

[src](src/main/java/compact_instance_main_methods/Main.java)

**Официальная документация:**
- [JEP 512: Compact Source Files and Instance Main Methods](https://openjdk.org/jeps/512)

<a id="4-stream_gatherers"></a>
## 4. Промежуточные операции в Stream API (Stream Gatherers)

Расширение `Stream API`, позволяющее создавать собственные промежуточные операции (аналог `Collector`, но для середины пайплайна). В класс `java.util.stream.Gatherers` добавлены готовые реализации: `windowFixed`, `windowSliding`, `fold`, `scan` и `mapConcurrent`.

```java
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
```

[src](src/main/java/stream_gatherers/Main.java)

**Официальная документация:**
- [JEP 485: Stream Gatherers](https://openjdk.org/jeps/485)

<a id="5-scoped_values"></a>
## 5. Scoped Values

Современная и безопасная замена `ThreadLocal`, оптимизированная под миллионы виртуальных потоков (`Project Loom`). Данные передаются вниз по стеку вызовов строго в рамках определенной области видимости и являются неизменяемыми.

```java
private static final ScopedValue<Map<String, String>> CONTEXT = ScopedValue.newInstance();

static void main() throws InterruptedException {
    Map<String, String> context = new HashMap<>();
    context.put("id", "1");

    ScopedValue.where(CONTEXT, context).run(()->{
        //будет id=1
        IO.println("id=%s".formatted(CONTEXT.get().get("id")));

        // будет java.util.NoSuchElementException: ScopedValue not bound
        Thread.startVirtualThread(()->{
            IO.println("id=%s".formatted(CONTEXT.get().get("id")));
        });
    });
    Thread.sleep(Duration.ofSeconds(1));
}
```

**Официальная документация:**
- [JEP 506: Scoped Values](https://openjdk.org/jeps/506)

<a id="6-synchronize_virtual_threads"></a>
## 6. Синхронизация виртуальных потоков без пиннинга

Виртуальные потоки больше не блокируют (не «пришпиливают» / pin) поток-носитель ОС при входе в блоки `synchronized`. Проблема с вынужденным переписыванием `synchronized` на `ReentrantLock` полностью решена.

**Официальная документация:**
- [JEP 491: Synchronize Virtual Threads without Pinning](https://openjdk.org/jeps/491)

<a id="7-gc"></a>
## 7. Garbage Collector

В Java 25 по умолчанию используется `G1 GC`.

`Generational ZGC`: включается флагом `-XX:+UseZGC` (флаг `-XX:+ZGenerational` больше не нужен и считается устаревшим). Подходит для систем с минимальными требованиями к задержкам (sub-millisecond pause times).

`Generational Shenandoah`: включается флагами `-XX:+UseShenandoahGC` `-XX:ShenandoahGCMode=generational` (в `JDK 25` стабилизирован как продакшен-функция через JEP 521).

<a id="8-other"></a>
## 8. Другие изменения

- **Компактные заголовки объектов** — [JEP 519](https://openjdk.org/jeps/519): уменьшает размер заголовка каждого объекта в 64-битной JVM с 96–128 бит до 64 бит. Включается флагом `-XX:+UseCompactObjectHeaders` и снижает потребление памяти в куче на 10–20% для микросервисов.
- **AOT-кэширование классов и профилей методов (`Project Leyden`)** — [JEP 483](https://openjdk.org/jeps/483), [JEP 514](https://openjdk.org/jeps/514), [JEP 515](https://openjdk.org/jeps/515): позволяет сохранить загруженные классы и профили `JIT`-компиляции в `AOT`-кэш при тестовом прогоне (`-XX:AOTCacheOutput=app.aot`), чтобы последующие запуски приложения стартовали и выходили на пиковую производительность в разы быстрее.
- **Улучшения `Java Flight Recorder (JFR)`** — [JEP 509](https://openjdk.org/jeps/509), [JEP 518](https://openjdk.org/jeps/518), [JEP 520](https://openjdk.org/jeps/520): добавлено профилирование процессорного времени (`CPU-Time Profiling` на `Linux`), кооперативная выборка стеков без необходимости дожидаться `safepoint` и встроенная трассировка времени выполнения конкретных методов.
- **Ограничение доступа к `sun.misc.Unsafe`** — [JEP 471](https://openjdk.org/jeps/471), [JEP 498](https://openjdk.org/jeps/498): методы доступа к памяти в Unsafe объявлены устаревшими на удаление и при вызове выводят предупреждения в консоль (готовится полный запрет в пользу `FFM API` и `VarHandle`).
- **Предупреждения при использовании `JNI`** — [JEP 472](https://openjdk.org/jeps/472): загрузка нативных библиотек через `JNI` теперь требует явного разрешения флагом `--enable-native-access=ALL-UNNAMED` (или именем модуля), иначе `JVM` выдает предупреждение.
- **Окончательное отключение `Security Manager`** — [JEP 486](https://openjdk.org/jeps/486): попытка включить `Security Manager` через `-Djava.security.manager` теперь приводит к ошибке запуска.
- **Удаление устаревших методов `Thread`**: методы `Thread.suspend()`, `Thread.resume()` и `ThreadGroup.stop()` полностью удалены из `JDK` и выбрасывают `NoSuchMethodError`.
- **Отключение процессоров аннотаций по умолчанию (с JDK 23)**: компилятор `javac` больше не запускает `Annotation Processors` автоматически, если не указан флаг `-proc:full` или путь через `-processorpath`.


[Полный список изменений](https://openjdk.org/projects/jdk/25/jeps-since-jdk-21)