package threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadPool implements Executor {

    private final BlockingQueue<Runnable> queue = new LinkedTransferQueue<>();
    private final Thread[] threads;
    private final AtomicBoolean started = new AtomicBoolean();

    public ThreadPool(int numThreads) {
        threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                try {
                    for (;;){
                        final Runnable task = queue.take();
                        task.run();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
}
