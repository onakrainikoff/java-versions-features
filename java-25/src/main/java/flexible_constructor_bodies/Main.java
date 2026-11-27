package flexible_constructor_bodies;

import java.math.BigInteger;

public class Main {
    static void main() {
        IO.println(new PositiveBigInteger(1));
    }

    public static class PositiveBigInteger extends BigInteger {
        public PositiveBigInteger(long value) {
            if (value <= 0) {
                throw new IllegalArgumentException("The value must be positive");
            }
            super(String.valueOf(value)); // Раньше super() обязан был быть первой строкой
        }
    }
}
