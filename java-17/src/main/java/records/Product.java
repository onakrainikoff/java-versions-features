package records;

public record Product(String title, double price) implements Comparable<Product>{
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
            return (int)(o.price * 100);
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
