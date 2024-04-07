import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;

public class Monitor {
    private final ThreadPool threadPool;

    public Monitor(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public void start() throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();
        while (true) {
            screen.clear();
            TextGraphics textGraphics = screen.newTextGraphics();
            textGraphics.putString(0, 0, "Queue size: " + threadPool.getQueue().size());
            textGraphics.putString(0, 1, "Active threads: " + threadPool.getActiveCount());
            int row = 2;
            for (Thread thread : threadPool.getThreads()) {
                textGraphics.putString(0, row++, "Thread: " + thread.getName() + ", State: " + thread.getState().name());
            }
            screen.refresh();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
        screen.stopScreen();
    }
}
