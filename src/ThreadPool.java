import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    private final int coreThreadNum;
    private final int maxThreadNum;
    final AtomicInteger idleCount = new AtomicInteger(0);
    private final int capacity;
    private final BlockingQueue<Runnable> workQueue;
    private final ArrayList<Thread> threads = new ArrayList<>();
    private final int timeout;
    private final RejectedExecutionHandler rejectedExecutionHandler;

    ThreadPool() {
        this(5, 10, 5000, 20, new ArrayBlockingQueue<>(20), runnable -> {
            throw new RuntimeException("Runnable: " + runnable + " was rejected.");
        });
    }

    ThreadPool(int coreThreadNum, int maxThreadNum, int timeout, int capacity, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler rejectedExecutionHandler) {
        this.coreThreadNum = coreThreadNum;
        this.maxThreadNum = maxThreadNum;
        this.timeout = timeout;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.workQueue = workQueue;
        this.capacity = capacity;
    }

    public void push(Runnable runnable) throws InterruptedException {
        if (threads.size() < coreThreadNum) {
            createThread();
        }
        if (workQueue.size() >= capacity) {
            if (threads.size() < maxThreadNum) {
                createThread();
            } else {
                rejectedExecutionHandler.rejectedExecution(runnable);
                return;
            }
        }
        workQueue.put(runnable);
        synchronized (workQueue) {
            workQueue.notify();
        }
    }

    synchronized void createThread() {
        Thread t = new Thread(() -> {
            ThreadLocal<Boolean> isActiveLocal = new ThreadLocal<>();
            isActiveLocal.set(true);
            while (true) {
                Runnable runnable;
                synchronized (workQueue) {
                    runnable = workQueue.poll();
                    while (runnable == null) {
                        try {
                            System.out.println(Thread.currentThread().getId() + ": Waiting");
                            idleCount.incrementAndGet();
                            isActiveLocal.set(false);
                            workQueue.wait(timeout);
                            isActiveLocal.set(true);
                            idleCount.decrementAndGet();
                            runnable = workQueue.poll();
                            if (runnable == null && idleCount.get() >= coreThreadNum) {
                                threads.remove(Thread.currentThread());
                                return;
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(Thread.currentThread().getId() + ": Wake up");
                    }
                    workQueue.notify(); // 队列满时只有 push 方法在等待，唤醒该方法让新的任务添加到队列中
                }
                runnable.run();
            }
        });
        threads.add(t);
        t.start();
    }

    public Queue<Runnable> getQueue() {
        return workQueue;
    }

    public int getActiveCount() {
        return threads.size() - idleCount.get();
    }

    public ArrayList<Thread> getThreads() {
        return threads;
    }

    public interface RejectedExecutionHandler {
        void rejectedExecution(Runnable r);
    }
}
