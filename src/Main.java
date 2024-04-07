import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool();
        Monitor monitor = new Monitor(threadPool);
        new Thread(() -> {
            try {
                monitor.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        try {
            makeSome(threadPool, 1);
        } catch (Exception ignore) {
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            makeSome(threadPool, 2);
        } catch (Exception ignore) {
        }
    }

    private static void makeSome(ThreadPool threadPool, int batch) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            threadPool.push(() -> {
                try {
                    System.out.println(Thread.currentThread().getId() + ": (" + batch + ") start " + finalI);
                    Thread.sleep(finalI * 100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(Thread.currentThread().getId() + ": (" + batch + ") sleep " + finalI * 100 + " ms");
            });
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}