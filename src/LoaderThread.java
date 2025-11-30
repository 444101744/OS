import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LoaderThread extends Thread {
    private final Queues queues;
    private final MemoryManager memory;
    private final AtomicBoolean fileDone;

    public LoaderThread(Queues queues, MemoryManager memory, AtomicBoolean fileDone) {
        super("LoaderThread");
        this.queues = queues;
        this.memory = memory;
        this.fileDone = fileDone;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                PCB next = queues.jobQueue.peek();
                if (next == null) {
                    // No job currently queued
                    if (fileDone.get()) break; // file done and no more jobs
                    TimeUnit.MILLISECONDS.sleep(20);
                    continue;
                }

                // Try to allocate memory for this job
                if (memory.allocate(next.memoryMB)) {

                    queues.jobQueue.take(); 
                    next.state = ProcessState.READY;
                    next.readyAcceptedTimeMs = System.currentTimeMillis();
                    queues.readyQueue.put(next);
                    System.out.printf("[Loader] Loaded P%d (%dMB). Mem %d/%d MB%n",
                            next.id, next.memoryMB, memory.used(), memory.capacity());
                } else {
                    // If the job is larger than total memory, it will never fit
                    if (next.memoryMB > memory.capacity()) {
                        System.err.printf("[Loader] P%d requires %dMB > capacity %dMB. Skipping.%n",
                                next.id, next.memoryMB, memory.capacity());
                        queues.jobQueue.take(); // discard impossible job
                    } else {
                        // Wait for memory to be freed 
                        TimeUnit.MILLISECONDS.sleep(50);
                    }
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
