package threadpool;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ThreadPoolTest {

    @Test
    void submittedTasksAreExecuted() {
        final ThreadPool executor = new ThreadPool(100, Duration.ZERO);
        final int numTasks = 100;

        try {
            for (int i = 0; i < numTasks; i++) {
                final int finalI = i;
                executor.execute(() -> {
                    System.err.println(
                        "Thread `" + Thread.currentThread().getName() + "` executes task "
                            + finalI);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } finally {
            executor.shutdown();
        }
    }

}
