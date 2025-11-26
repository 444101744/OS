import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileReaderThread extends Thread {
    private final Queues queues;
    private final String path;
    private final AtomicBoolean doneFlag;

    public FileReaderThread(Queues queues, String path, AtomicBoolean doneFlag) {
        super("FileReaderThread");
        this.queues = queues;
        this.path = path;
        this.doneFlag = doneFlag;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            if (!Files.exists(Path.of(path)))
                throw new IllegalArgumentException("job file not found: " + path);

            try (BufferedReader br = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    // skip comments / markers
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("["))
                        continue;

                    // Format: id:burst:priority;memory
                    // Example: 1:25:4;500
                    String[] halves = line.split(";");
                    if (halves.length != 2) {
                        throw new IllegalArgumentException("Bad line: " + line);
                    }

                    String left = halves[0];
                    int memMB = Integer.parseInt(halves[1].trim());

                    String[] leftParts = left.split(":");
                    if (leftParts.length != 3) {
                        throw new IllegalArgumentException("Bad left part: " + left);
                    }

                    int id = Integer.parseInt(leftParts[0].trim());
                    int burst = Integer.parseInt(leftParts[1].trim());
                    int prio = Integer.parseInt(leftParts[2].trim());

                    PCB pcb = new PCB(id, burst, prio, memMB);
                    pcb.arrivalTimeMs = 0; // all arrive at t=0
                    queues.jobQueue.put(pcb);
                }
            }
        } catch (Exception e) {
            System.err.println("[FileReaderThread] ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            doneFlag.set(true);
        }
    }
}
