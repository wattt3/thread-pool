package threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadPool implements Executor {

    private final BlockingQueue<Runnable> queue = new LinkedTransferQueue<>();
    private final Thread[] threads;
    private final AtomicBoolean started = new AtomicBoolean();

    private boolean shutdown;

    public ThreadPool(int numThreads) {
        threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                    while (!shutdown) {
                        // 사용자가 임의적으로 InterruptedException 던지는 것 방지
                        Runnable task = null;
                        try {
                            task = queue.take();
                        } catch (InterruptedException e) {
                        }
                        if (task != null) {
                            try {
                                task.run();
                            } catch (Throwable t) {
                                if (!(t instanceof InterruptedException)) {
                                    System.err.println("Unexpected exception: ");
                                    t.printStackTrace();
                                }
                            }
                        }
                    }
            });
            // constructor 안에서 start 했을 때 queue를 참조하고 있으니까 호출이 완료될 때 까지, 그 전에 어느 시점에서 queue가 초기화될지 모호하다
            // 시작하자마자 queue를 참조하려고 하는데, 아직 constructor(초기화)가 끝나지 않았을 수 있다.
            // threads[i].start(); => lazy하게 변경
        }
    }

    @Override
    public void execute(Runnable command) {
        if (started.compareAndSet(false, true)) {
            for (Thread thread : threads) {
                thread.start();
            }
        }
        queue.add(command);
    }

    public void shutdown() {
        shutdown = true;
        for (Thread thread : threads) {
            while (thread.isAlive()) {
                // interrupt 이후 사용자가 InterruptedException을 잡아서 아무런 행동하지 않을 수 있음.
                // => shutdown flag를 사용하는 이유
                thread.interrupt();
                try {
                    // isAlive할 때 무한 루프를 타면서 interrupt를 하기 떄문에 성능이 좋지 않다.
                    // wait for this thread to die.
                    thread.join();
                } catch (InterruptedException e) {
                    // Do not propagate to prevent incomplete shutdown.
                }
            }
        }
    }
}
