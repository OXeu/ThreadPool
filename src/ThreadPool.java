import com.sun.jmx.remote.internal.ArrayQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    private final int coreThreadNum;
    private final int maxThreadNum;
    private final AtomicInteger idleCount = new AtomicInteger(0);
    private final static int CAPACITY = 20;
    private final ArrayQueue<Runnable> workQueue = new ArrayQueue<>(CAPACITY);
    private final ArrayList<Thread> threads = new ArrayList<>();

    ThreadPool() {
        this(5, 10);
    }

    ThreadPool(int coreThreadNum, int maxThreadNum) {
        this.coreThreadNum = coreThreadNum;
        this.maxThreadNum = maxThreadNum;
    }

    public void push(Runnable runnable) throws InterruptedException {
        if (idleCount.get() <= 0) {
            if (threads.size() < coreThreadNum) {
                createThread();
            }
        }
        synchronized (workQueue) {
            if (threads.size() < maxThreadNum && workQueue.size() >= CAPACITY) {
                createThread();
                workQueue.wait(); // 释放工作队列锁让刚创建的线程能够从队列中取出数据
            }
            workQueue.add(runnable);
            workQueue.notify();
        }
    }

    synchronized void createThread() {
        Thread t = new Thread(() -> {
            while (true) {
                Runnable runnable;
                synchronized (workQueue) {
                    while (workQueue.isEmpty()) {
                        try {
                            System.out.println(Thread.currentThread().getId() + ": Waiting");
                            idleCount.incrementAndGet();
                            workQueue.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        idleCount.decrementAndGet();
                        System.out.println(Thread.currentThread().getId() + ": Wake up");
                    }
                    runnable = workQueue.remove(0);
                    workQueue.notify(); // 队列满时只有 push 方法在等待，唤醒该方法让新的任务添加到队列中
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        threads.add(t);
        t.start();
    }

    public ArrayQueue<Runnable> getQueue() {
        return workQueue;
    }

    public int getActiveCount() {
        return threads.size() - idleCount.get();
    }

    public ArrayList<Thread> getThreads() {
        return threads;
    }
}
