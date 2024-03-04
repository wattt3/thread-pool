package threadpool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool implements Executor {

    private static final Runnable SHUTDOWN_TASK = () -> {
    };

    private final int maxNumThreads;
    private final BlockingQueue<Runnable> queue = new LinkedTransferQueue<>();
    private final AtomicBoolean shutdown = new AtomicBoolean();
    private final Set<Thread> threads = new HashSet<>();
    private final Lock threadsLock = new ReentrantLock();
    private final AtomicInteger numThreads = new AtomicInteger();
    private final AtomicInteger numActiveThreads = new AtomicInteger();

    public ThreadPool(int maxNumThreads) {
        this.maxNumThreads = maxNumThreads;
    }

    private Thread newThread() {
        numThreads.incrementAndGet();
        return new Thread(() -> {
            for (; ; ) {
                try {
                    final Runnable task = queue.take();
                    if (task == SHUTDOWN_TASK) {
                        break;
                    } else {
                        task.run();
                    }
                } catch (Throwable t) {
                    if (!(t instanceof InterruptedException)) {
                        System.err.println("Unexpected exception: ");
                        t.printStackTrace();
                    }
                } finally {
                    numThreads.decrementAndGet();
                }
            }
            System.err.println("Shutting thread `" + Thread.currentThread().getName() + "`");
        });
    }

    @Override
    public void execute(Runnable command) {
        if (shutdown.get()) {
            throw new RejectedExecutionException();
        }

        queue.add(command);
        addThreadIfNecessary();

        if (shutdown.get()) {
            queue.remove(command);
            throw new RejectedExecutionException();
        }
    }

    private void addThreadIfNecessary() {
        final int numActiveThreads = this.numActiveThreads.get();
        if (needsMoreThreads(numActiveThreads)) {
            addThreadIfNecessaryLocked();
        }
    }

    private void addThreadIfNecessaryLocked() {
        threadsLock.lock();
        try {
            final int numActiveThreads2 = this.numActiveThreads.get();
            if (needsMoreThreads(numActiveThreads2)) {
                final Thread newThread = newThread();
                threads.add(newThread);
                newThread.start();
            }
        } finally {
            threadsLock.unlock();
        }
    }

    private boolean needsMoreThreads(int numActiveThreads) {
        return numActiveThreads < maxNumThreads && numActiveThreads >= numThreads.get();
    }

    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            for (int i = 0; i < maxNumThreads; i++) {
                queue.add(SHUTDOWN_TASK);
            }
        }

        // FIXME: Fix the race condition where a new thread is added by execute()
        for (Thread thread : threads) {
            do {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    // Do not propagate to prevent incomplete shutdown.
                }
            } while (thread.isAlive());
        }
    }
}
