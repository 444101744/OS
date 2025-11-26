import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
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
        Thread.sleep(500);

        // Phase 2 hack: stop loader since we don't free memory yet
        loader.interrupt();
        loader.join();

        System.out.println("\n--- Ready Queue Snapshot (before scheduling) ---");
        for (PCB p : queues.readyQueue) {
            System.out.println(p);
        }
        System.out.printf("Memory usage: %d/%d MB%n", memory.used(), memory.capacity());
        System.out.printf("Jobs still in JobQueue (not loaded due to memory cap): %d%n",
                queues.jobQueue.size());

        // Take a copy of the ready queue into a list for scheduling
        List<PCB> processes = new ArrayList<>(queues.readyQueue);
        if (processes.isEmpty()) {
            System.out.println("No processes in ready queue. Nothing to schedule.");
            return;
        }

        // ---- Scheduling menu ----
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        System.out.println("\nChoose scheduling algorithm:");
        System.out.println("1) Shortest Job First (non-preemptive)");
        System.out.println("2) Round Robin (q = 6 ms) ");
        System.out.println("3) Priority Scheduling [NOT IMPLEMENTED YET]");
        System.out.print("Enter choice: ");

        int choice = scanner.nextInt();
        SchedulerResult result;

        switch (choice) {
            case 1:
                result = Scheduler.runSJF(processes);
                System.out.println("\n=== SJF Results ===");
                printResults(result, processes);
                break;

            case 2:
                result = Scheduler.runRR(processes, 6);
                System.out.println("\n=== Round Robin (q=6 ms) Results ===");
                printResults(result, processes);
                break;

            case 3:
                System.out.println("Priority Scheduling is not implemented yet.");
                break;

            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void printResults(SchedulerResult result, List<PCB> processes) {
        System.out.println("\nGantt Chart:");
        // Simple text Gantt: | P1 (0-10) | P2 (10-25) | ...
        for (GanttEntry e : result.gantt) {
            System.out.printf("| P%d (%d-%d) ", e.processId, e.startTime, e.endTime);
        }
        System.out.println("|");

        System.out.println("\nPer-process stats:");
        for (PCB p : processes) {
            System.out.printf(
                "P%d: waiting=%d ms, turnaround=%d ms%n",
                p.id, p.waitingTimeMs, p.turnaroundTimeMs
            );
        }

        System.out.printf("%nAverage waiting time: %.2f ms%n", result.avgWaitingTime);
        System.out.printf("Average turnaround time: %.2f ms%n", result.avgTurnaroundTime);
    }
}
