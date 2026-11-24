package helpful_npe;

public class Main {
    public static void main(String[] args) {
        record Address(String city){};
        record User(Address address){};
        User user = new User(null);
        user.address.city.toLowerCase();
    }
}
