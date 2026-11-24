package records;

public class Main {
    public static void main(String[] args) {
        Product product = new Product("product", 2.5);
        System.out.println(product);
        System.out.println(product.title());
        System.out.println(product.price());

        Product product2 = new Product("product2", 5);
        System.out.println(product2);
        System.out.println(product2.title());
        System.out.println(product2.price());

        System.out.println(product2.equals(product));
        System.out.println(product2.compareTo(product));

        // new Product(null);



    }
}
