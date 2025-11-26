import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Queues {
    // Unbounded by count; memory is enforced by MemoryManager in LoaderThread
    public final BlockingQueue<PCB> jobQueue = new LinkedBlockingQueue<>();
    public final BlockingQueue<PCB> readyQueue = new LinkedBlockingQueue<>();
}
