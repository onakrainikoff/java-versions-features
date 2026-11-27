package record_patterns;

public class Main {

    static record Point(int x, int y) {}
    static record Line(Point start, Point end) {}

    public static void main(String[] args) {
        printStartX(new Line(new Point(-100, 100), new Point(0, 0)));
    }

    static void printStartX(Object obj) {
        // Деконструкция вложенных record-классов
        if (obj instanceof Line(Point(int x, int y), Point end)) {
            System.out.println("The X coordinate of first point: " + x);
            System.out.println("The Y coordinate of first point: " + x);
            System.out.println("The second point: " + end);
        }
    }
}
