import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class Scheduler {

    /**
     * Non-preemptive Shortest Job First.
     * Assumes all processes arrive at time 0.
     */
    public static SchedulerResult runSJF(List<PCB> processes) {
        if (processes.isEmpty()) {
            return new SchedulerResult(new ArrayList<>(), 0.0, 0.0);
        }

        // Make a copy so we don't mess with any external list order
        List<PCB> procs = new ArrayList<>(processes);

        // Sort by burst time, then by seq (arrival order)
        procs.sort(Comparator
                .comparingInt((PCB p) -> p.burstTimeMs)
                .thenComparingLong(p -> p.seq));

        List<GanttEntry> gantt = new ArrayList<>();
        int currentTime = 0;
        long totalWaiting = 0;
        long totalTurnaround = 0;
        int n = procs.size();

        // "Degree of multiprogramming" at acceptance: how many were in memory/ready.
        // Here, all are ready at t=0 => just use n as the threshold.
        int degreeOfMultiprogramming = n;

        for (PCB p : procs) {
            int start = currentTime;
            int end = currentTime + p.burstTimeMs;

            long arrival = p.arrivalTimeMs; // should be 0 in this project
            p.waitingTimeMs = start - arrival;
            p.turnaroundTimeMs = end - arrival;
            p.state = ProcessState.TERMINATED;

            currentTime = end;

            gantt.add(new GanttEntry(p.id, start, end));
            totalWaiting += p.waitingTimeMs;
            totalTurnaround += p.turnaroundTimeMs;

            // Starvation detection rule from spec:
            if (p.waitingTimeMs > degreeOfMultiprogramming) {
                System.out.printf(
                        ">> [SJF] Starvation detected for P%d: waited %d ms (threshold %d)%n",
                        p.id, p.waitingTimeMs, degreeOfMultiprogramming
                );
            }
        }

        double avgWait = (double) totalWaiting / n;
        double avgTurn = (double) totalTurnaround / n;

        return new SchedulerResult(gantt, avgWait, avgTurn);
    }

    /**
     * Round Robin scheduling.
     * All processes arrive at time 0.
     * @param processes list of PCBs
     * @param quantumMs time slice in ms (must be > 0)
     */
    public static SchedulerResult runRR(List<PCB> processes, int quantumMs) {
        if (processes.isEmpty()) {
            return new SchedulerResult(new ArrayList<>(), 0.0, 0.0);
        }
        if (quantumMs <= 0) {
            throw new IllegalArgumentException("Quantum must be > 0");
        }

        int n = processes.size();

        // Copy so we don't rely on the original list's order.
        List<PCB> procs = new ArrayList<>(processes);

        // Remaining burst times
        int[] remaining = new int[n];
        for (int i = 0; i < n; i++) {
            remaining[i] = procs.get(i).burstTimeMs;
            // reset stats in case Scheduler is called after another algorithm
            procs.get(i).waitingTimeMs = 0;
            procs.get(i).turnaroundTimeMs = 0;
            procs.get(i).state = ProcessState.READY;
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            queue.addLast(i); // store indices into 'procs'
        }

        List<GanttEntry> gantt = new ArrayList<>();
        int currentTime = 0;
        long totalWaiting = 0;
        long totalTurnaround = 0;
        int finishedCount = 0;

        while (!queue.isEmpty()) {
            int idx = queue.removeFirst();
            PCB p = procs.get(idx);

            int execTime = Math.min(remaining[idx], quantumMs);
            int start = currentTime;
            int end = currentTime + execTime;

            // First we simulate running
            currentTime = end;
            remaining[idx] -= execTime;
            gantt.add(new GanttEntry(p.id, start, end));

            if (remaining[idx] == 0) {
                // Process finished
                p.state = ProcessState.TERMINATED;
                p.turnaroundTimeMs = currentTime - p.arrivalTimeMs; // arrival assumed 0
                p.waitingTimeMs = p.turnaroundTimeMs - p.burstTimeMs;
                totalWaiting += p.waitingTimeMs;
                totalTurnaround += p.turnaroundTimeMs;
                finishedCount++;
            } else {
                // Still has time left, go back to the end of the queue
                queue.addLast(idx);
            }
        }

        double avgWait = (double) totalWaiting / finishedCount;
        double avgTurn = (double) totalTurnaround / finishedCount;

        return new SchedulerResult(gantt, avgWait, avgTurn);
    }
}
