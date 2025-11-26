import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) throws Exception {
        String jobFile = args.length > 0 ? args[0] : "job.txt";

        // Create a sample job file if missing
        if (!Files.exists(Path.of(jobFile))) {
            List<String> sample = List.of(
                "[Begin of job.txt]",
                "1:25:4;500",
                "2:13:3;700",
                "3:20:3;100",
                "4:50:2;900",
                "5:10:128;200",
                "[End of job.txt]"
            );
            try (FileWriter fw = new FileWriter(jobFile)) {
                for (String s : sample) {
                    fw.write(s + System.lineSeparator());
                }
            }
            System.out.println("Created sample " + jobFile);
        }

        Queues queues = new Queues();
        MemoryManager memory = new MemoryManager(2048);
        AtomicBoolean fileDone = new AtomicBoolean(false);

        Thread reader = new FileReaderThread(queues, jobFile, fileDone);
        Thread loader = new LoaderThread(queues, memory, fileDone);

        reader.start();
        loader.start();

        // Wait until file is fully read
        reader.join();

        // Give Loader some time to load whatever fits into memory
        Thread.sleep(500); // half a second is enough for our simple test

        // Phase 2 only: stop loader, because no scheduler will free memory yet
        loader.interrupt();
        loader.join();

        System.out.println("\n--- Ready Queue Snapshot ---");
        for (PCB p : queues.readyQueue) {
            System.out.println(p);
        }
        System.out.printf("Memory usage: %d/%d MB%n", memory.used(), memory.capacity());
        System.out.printf("Jobs still in JobQueue (not loaded due to memory cap): %d%n",
                queues.jobQueue.size());

        // In Phase 3, once the scheduler exists and calls memory.free(p.memoryMB),
        // we will REMOVE the interrupt hack and let Loader run normally until
        // jobQueue is empty and fileDone == true.
    }
}
