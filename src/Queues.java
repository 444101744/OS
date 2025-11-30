import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Queues {
    
    public final BlockingQueue<PCB> jobQueue = new LinkedBlockingQueue<>();
    public final BlockingQueue<PCB> readyQueue = new LinkedBlockingQueue<>();
}
