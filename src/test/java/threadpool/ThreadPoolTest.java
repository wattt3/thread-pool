package threadpool;

import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

class ThreadPoolTest {

    @Test
    void submittedTasksAreExecuted() throws InterruptedException {
        final ThreadPool executor = new ThreadPool(2);
        final int numTasks = 100;
        final CountDownLatch latch = new CountDownLatch(numTasks);

        try {
            for (int i = 0; i < numTasks; i++) {
                final int finalI = i;
                executor.execute(() -> {
                    System.err.println(
                        "Thread `" + Thread.currentThread().getName() + "` executes task "
                            + finalI);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    latch.countDown();
                });
            }

        } finally {
            executor.shutdown();
        }
    }

}
