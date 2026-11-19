package private_interface_methods;

public class Main {
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

    public static void main(String[] args) {
        var logger = new Logger(){};
        logger.logInfo("log info");
        logger.logError("log error");
    }

}
