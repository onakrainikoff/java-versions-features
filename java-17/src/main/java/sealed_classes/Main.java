package sealed_classes;

public class Main {

    // 1. Базовый запечатанный класс: разрешает только три конкретных наследника
    public static sealed interface Notification permits EmailNotification, PushNotification, CustomNotification, TestNotification {}


    // 2. Ветка закрыта: никто не может унаследоваться от EmailNotification
    public static final class EmailNotification implements Notification {}


    // 3. Ветка остается под контролем: у PushNotification могут быть только два наследника
    public static sealed class PushNotification implements Notification  permits IosPush, AndroidPush {}
    // Его наследники тоже обязаны выбрать модификатор (здесь — final)
    public static final class IosPush extends PushNotification {}
    public static final class AndroidPush extends PushNotification {}


    // 4. Ветка открыта для всех: любой разработчик может создать свой подкласс
    public static non-sealed class CustomNotification implements Notification {}
    // Обычный класс — модификатор sealed/final/non-sealed здесь уже не требуется
    public static class TelegramNotification extends CustomNotification {}

    // 5. Расширение интерфейса
    public static non-sealed interface TestNotification extends Notification {}

    public static void main(String[] args) {
    }
}
