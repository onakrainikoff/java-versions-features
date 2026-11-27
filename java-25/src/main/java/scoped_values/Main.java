package scoped_values;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Main {
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
}
