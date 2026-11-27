# [Важные изменения в Java](../README.md) / Java 17 (LTS)

Релиз состоялся в сентябре 2021 года. Поддержка до января 2029.

## Содержание

- [1. Records](#1-records)
- [2. Sealed Classes](#2-sealed-classes)
- [3. Switch Expressions](#3-switch-expressions)
- [4. Text Blocks](#4-text-blocks)
- [5. Pattern Matching для instanceof](#5-pattern-matching)
- [6. Метод Stream.toList()](#6-stream-tolist)
- [7. Коллектор Collectors.teeing()](#7-collectors-teeing)
- [8. Новые методы в String](#8-string-methods)
- [9. Новый API генераторов псевдослучайных чисел](#9-random-generators)
- [10. Helpful NullPointerExceptions](#10-helpful-npe)
- [11. Garbage Collector](#11-garbage-collector)
- [12. Другие изменения](#12-other-changes)

<a id="1-records"></a>
## 1. Records

Компактный синтаксис для создания неизменяемых (`immutable`) классов-носителей данных. При компиляции record превращается в обычный `final`-класс, унаследованный от `java.lang.Record`, а также автоматически генерируются `private final` поля, конструктор, геттеры (без префикса `get`), а также методы `equals()`, `hashCode()` и `toString()`.

```java
public record Product(String title, double price) implements Comparable<Product> {
    // Компактный конструктор
    public Product {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("'title' must not be blank");
        }
        if (price < 0) {
            throw new IllegalArgumentException("'price' cannot be negative");
        }
        title = title.trim(); // Нормализация значения перед сохранением
    }

    // Перегруженный конструктор
    public Product(String title) {
        this(title, 0.0); // Делегирование каноническому конструктору
    }

    // Статическое поле
    public static final int ZERO_PRICE = 0;

    // Статический метод
    public static int price2Int(Product o) {
        if (o == null) {
            return ZERO_PRICE;
        } else {
            return (int) (o.price * 100);
        }
    }

    // Обычный метод
    @Override
    public int compareTo(Product o) {
        if (o == null) {
            return -1;
        }
        return Integer.compare(price2Int(this), price2Int(o));
    }
}

// ... 
Product product = new Product("product", 2.5);
System.out.println(product);
System.out.println(product.title());
System.out.println(product.price());
```

[src](src/main/java/records/Main.java)

Ограничения и особенности:
- **Наследование**:	все `record` неявно `final` — от них нельзя унаследоваться, и они не могут наследоваться от других классов (кроме неявного `java.lang.Record`);
- **Интерфейсы**:	могут реализовывать любые интерфейсы (`implements Serializable`, `Comparable` и др.), включая `sealed`-интерфейсы;
- **Локальные** `record`:	можно объявлять прямо внутри методов для удобной группировки промежуточных данных в `Stream API`;
- **Мутабельность** (`Shallow Immutability`):	ссылки на поля `final`, но если внутри лежит изменяемый объект (например, `List` или `Date`), его содержимое можно изменить. Для полной неизменяемости в компактном конструкторе делают защитные копии: `items = List.copyOf(items);`.

Практические сценарии применения:
-  `DTO` и ответы `REST API`. Библиотеки сериализации (`Jackson 2.12+`, `Gson 2.10+`) поддерживают `record` из коробки без дополнительных аннотаций.
- Локальные контейнеры в `Stream API`. Удобно использовать как временные кортежи (`tuples`) при сложной обработке коллекций:

```java
record OrderSummary(String customer, int totalAmount) {}

List<OrderSummary> summaries = orders.stream()
.map(o -> new OrderSummary(o.getCustomer(), o.calculateTotal()))
.filter(s -> s.totalAmount() > 1000)
.toList();
```

- Составные ключи для `Map` и `Set`. Благодаря готовым `equals()` и `hashCode()` записи идеально подходят на роль ключей кэша (например, `record CacheKey(String region, Long userId) {}`).
- Не подходят для `JPA/Hibernate Entity`. Спецификация `JPA` требует конструктор без параметров, не-`final` классы и изменяемые поля (сеттеры), поэтому для сущностей БД по-прежнему используют обычные классы, а `record` применяют для проекций (`Spring Data JPA Projections`).


**Официальная документация:**
- [JEP 395: Records](https://openjdk.org/jeps/395)

<a id="2-sealed-classes"></a>
## 2. Sealed Classes

Позволяют строго ограничить список классов, которые могут наследоваться от базового класса или реализовывать интерфейс, с помощью ключевых слов `sealed и permits`. Наследники обязаны иметь один из модификаторов:

| Модификатор | Поведение наследника | Можно ли наследоваться дальше? |
| --- | --- | --- |
| `final`| Полностью запрещает дальнейшее наследование от этого класса (к интерфейсам не применяется).    | Нет. Ветка иерархии на этом заканчивается.    |
| `sealed`    | Позволяет продолжить наследование, но только для строго ограниченного списка классов, указанного в новом блоке permits.    | Да, ограниченно. Наследники этого класса также обязаны выбрать `final`, `sealed` или `non-sealed`.    |
| `non-sealed`    | Снимает ограничения и «распечатывает» эту ветку иерархии. Класс становится обычным публичным классом (или интерфейсом).    | Да, свободно. От него может наследоваться любой класс без ограничений.    |


```java
    // 1. Базовый запечатанный класс: разрешает только три конкретных наследника
    public sealed interface Notification permits EmailNotification, PushNotification, CustomNotification, TestNotification {}
    
    // 2. Ветка закрыта: никто не может унаследоваться от EmailNotification
    public final class EmailNotification implements Notification {}

    // 3. Ветка остается под контролем: у PushNotification могут быть только два наследника
    public sealed class PushNotification implements Notification  permits IosPush, AndroidPush {}
    // Его наследники тоже обязаны выбрать модификатор (здесь — final)
    public final class IosPush extends PushNotification {}
    public final class AndroidPush extends PushNotification {}

    // 4. Ветка открыта для всех: любой разработчик может создать свой подкласс
    public non-sealed class CustomNotification implements Notification {}
    // Обычный класс — модификатор sealed/final/non-sealed здесь уже не требуется
    public class TelegramNotification extends CustomNotification {}

    // 5. Расширение интерфейса
    public non-sealed interface TestNotification extends Notification {}
```
[src](src/main/java/sealed_classes/Main.java)

Правила для наследников:
- **Единый пакет или модуль**. Все прямые наследники из списка permits должны находиться в том же пакете (`package`), что и родительский `sealed`-класс (или в том же модуле `JPMS`, если проект использует `module-info.java`).
- **Работа с record**. Записи (`record`) в `Java` неявно являются `final`, поэтому при реализации `sealed`-интерфейса им не нужно явно писать слово `final`.
- **Опускание permits**. Если все наследники объявлены в том же `.java`-файле, что и родительский `sealed`-класс, ключевое слово `permits` можно не писать — компилятор определит список автоматически, но модификаторы `final`, sealed или `non-sealed` у наследников все равно остаются обязательными.


**Официальная документация:**
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409)

<a id="3-switch-expressions"></a>
## 3. Switch Expressions

Оператор `switch` теперь можно использовать как выражение, возвращающее результат. Новый синтаксис со стрелками `->` исключает случайное «проваливание» (`fall-through`) в следующую ветку и избавляет от необходимости писать `break`. Для возврата значения из блока кода используется ключевое слово `yield`.

```java
String dayType = switch (day) {
    case MONDAY, FRIDAY, SUNDAY -> "Short or weekend";
    case TUESDAY -> {
        System.out.println("Processing...");
        yield "An ordinary day";
    }
    default -> "Other days";
};
```

[src](src/main/java/switch_expressions/Main.java)

**Официальная документация:**
- [JEP 361: Switch Expressions](https://openjdk.org/jeps/361)

<a id="4-text-blocks"></a>
## 4. Text Blocks

Многострочные строковые литералы, обрамленные тройными кавычками """. Они автоматически форматируют отступы и избавляют от необходимости экранировать кавычки и переносы строк в `JSON`, `SQL` или `HTML`.

```java
String json = """
        {
            "name": "Java",
            "version": 17
        }
        """;
```

[src](src/main/java/text_blocks/Main.java)

**Официальная документация:**
- [JEP 378: Text Blocks](https://openjdk.org/jeps/378)

<a id="5-pattern-matching"></a>
## 5. Pattern Matching для instanceof

Позволяет проверять тип объекта и сразу объявлять переменную нужного типа без явного приведения (`cast`).

```java
// Было
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}
// Стало
if (obj instanceof String s && !s.isEmpty()) {
    System.out.println(s.length());
}
```

[src](src/main/java/instanceof_pattern_matching/Main.java)

Официальная документация:
- [JEP 394: Pattern Matching for instanceof](https://openjdk.org/jeps/394)

<a id="6-stream-tolist"></a>
## 6. Метод Stream.toList()

Позволяет преобразовать стрим в неизменяемый список напрямую без вызова `.collect(Collectors.toList())`:

```java
List<String> names = Stream.of("Alice", "Bob")
        .map(String::toUpperCase)
        .toList();
```

<a id="7-collectors-teeing"></a>
## 7. Коллектор Collectors.teeing()

Объединяет результаты работы двух независимых коллекторов над одним стримом с помощью функции слияния:

```java
double average = Stream.of(10, 20, 30)
        .collect(Collectors.teeing(
                Collectors.summingDouble(i -> i),
                Collectors.counting(),
                (sum, count) -> sum / count
        ));
```

<a id="8-string-methods"></a>
## 8. Новые методы в String

Добавлены методы `indent(int n)` для управления отступами, `transform(Function)` для цепочечных преобразований строк, а также `formatted(Object... args)` для удобного форматирования текстовых блоков.

```java
// indent
String text = "First string\nSecond string";

// Добавляем отступ в 4 пробела перед каждой строкой
String indented = text.indent(4);
System.out.println(indented);
/* Результат:
    First string
    Second string
*/

// Убираем 2 пробела из начала каждой строки
String unindented = indented.indent(-2);
System.out.println(unindented);
/* Результат:
  First string
  Second string
*/

// transform
String rawInput = "  java 17 features  ";
List<String> words = rawInput
        .transform(String::trim)                // "java 17 features"
        .transform(String::toUpperCase)         // "JAVA 17 FEATURES"
        .transform(s -> List.of(s.split(" "))); // ["JAVA", "17", "FEATURES"]
        System.out.println(words);

// formatted
// Было
String oldWay = String.format("""
        {
            "user": "%s",
            "role": "%s"
        }
        """, "Alex", "ADMIN");

// Стало:
String json = """
        {
            "user": "%s",
            "role": "%s",
            "active": %b
        }
        """.formatted("Alex", "ADMIN", true);
System.out.println(json);
```

[src](src/main/java/string_new_methods/Main.java)

<a id="9-random-generators"></a>
## 9. Новый API генераторов псевдослучайных чисел
Появился единый интерфейс RandomGenerator и фабрика `RandomGeneratorFactory`, предоставляющие доступ к современным алгоритмам (например, `LXM`) без необходимости переписывать код при смене генератора.

```java
RandomGenerator generator = RandomGeneratorFactory.of("L64X128MixRandom").create();
int randomInt = generator.nextInt(100);
```

**Официальная документация:**
- [JEP 356: Enhanced Pseudo-Random Number Generators](https://openjdk.org/jeps/356)

<a id="10-helpful-npe"></a>
## 10. Helpful NullPointerExceptions

Сообщения об исключении `NullPointerException` теперь точно указывают, какая именно переменная или результат вызова метода в цепочке оказались равны `null`.

```java
// Код:
record Address(String city){};
record User(Address address){};
User user = new User(null);
user.address.city.toLowerCase();
// Вывод:
// Cannot read field "city" because "address" is null
```

[src](src/main/java/helpful_npe/Main.java)

**Официальная документация:**
- [JEP 358: Helpful NullPointerExceptions](https://openjdk.org/jeps/358)

<a id="11-garbage-collector"></a>
## 11. Garbage Collector
`G1` остается сборщиком по умолчанию в `Java 17` и научился возвращать неиспользуемую память операционной системе во время простоя ([JEP 346](https://openjdk.org/jeps/346)).

Сборщики с низкими задержками `ZGC` (JEP 377) и Shenandoah (JEP 379) переведены в статус production-ready (в `Java 11` `ZGC` был экспериментальным).

<a id="12-other-changes"></a>
## Другие изменения

- **Строгая инкапсуляция внутренних** `API JDK`. Доступ к внутренним пакетам (таким как `sun.misc.*`) по умолчанию полностью заблокирован. Флаг `--illegal-access`, который позволял обходить это ограничение в `Java 11–16`, больше не работает — для доступа к конкретным внутренним пакетам старых библиотек требуются явные флаги `--add-opens` или `--add-exports`. Подробнее в [JEP 403](https://openjdk.org/jeps/403).
- **Утилита `jpackage`**. Инструмент для сборки нативных установщиков (`.exe`, `.msi`, `.pkg`, `.dmg`, `.deb`, `.rpm`), включающих само `Java`-приложение и минимально необходимый образ `JRE`. Подробнее в [JEP 392](https://openjdk.org/jeps/392).
- **Удаленные компоненты**. Из `JDK` окончательно удалены движок `JavaScript Nashorn` (JEP 372), сборщик мусора `CMS` (JEP 363), инструменты сжатия `Pack200` (JEP 367) и `RMI Activation` (JEP 407). `Security Manager` (JEP 411) и `Applet API` (JEP 398) помечены как устаревшие для будущего удаления.

[Полный список изменений](https://openjdk.org/projects/jdk/17/jeps-since-jdk-11)