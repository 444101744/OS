import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class Scheduler {

    /**
     * Shortest Job First.
     * Assumes all processes arrive at time 0.
     */
    public static SchedulerResult runSJF(List<PCB> processes) {
        if (processes.isEmpty()) {
            return new SchedulerResult(new ArrayList<>(), 0.0, 0.0);
        }

        
        List<PCB> procs = new ArrayList<>(processes);

        
        procs.sort(Comparator
                .comparingInt((PCB p) -> p.burstTimeMs)
                .thenComparingLong(p -> p.seq));

        List<GanttEntry> gantt = new ArrayList<>();
        int currentTime = 0;
        long totalWaiting = 0;
        long totalTurnaround = 0;
        int n = procs.size();

        
        
        int degreeOfMultiprogramming = n;

        for (PCB p : procs) {
            int start = currentTime;
            int end = currentTime + p.burstTimeMs;

            long arrival = p.arrivalTimeMs; 
            p.waitingTimeMs = start - arrival;
            p.turnaroundTimeMs = end - arrival;
            p.state = ProcessState.TERMINATED;

            currentTime = end;

            gantt.add(new GanttEntry(p.id, start, end));
            totalWaiting += p.waitingTimeMs;
            totalTurnaround += p.turnaroundTimeMs;

            // Starvation detection:
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
     * @param quantumMs time slice in ms
     */
    public static SchedulerResult runRR(List<PCB> processes, int quantumMs) {
        if (processes.isEmpty()) {
            return new SchedulerResult(new ArrayList<>(), 0.0, 0.0);
        }
        if (quantumMs <= 0) {
            throw new IllegalArgumentException("Quantum must be > 0");
        }

        int n = processes.size();

        
        List<PCB> procs = new ArrayList<>(processes);

        
        int[] remaining = new int[n];
        for (int i = 0; i < n; i++) {
            remaining[i] = procs.get(i).burstTimeMs;
            
            procs.get(i).waitingTimeMs = 0;
            procs.get(i).turnaroundTimeMs = 0;
            procs.get(i).state = ProcessState.READY;
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            queue.addLast(i); 
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

            
            currentTime = end;
            remaining[idx] -= execTime;
            gantt.add(new GanttEntry(p.id, start, end));

            if (remaining[idx] == 0) {
                
                p.state = ProcessState.TERMINATED;
                p.turnaroundTimeMs = currentTime - p.arrivalTimeMs; // arrival assumed 0
                p.waitingTimeMs = p.turnaroundTimeMs - p.burstTimeMs;
                totalWaiting += p.waitingTimeMs;
                totalTurnaround += p.turnaroundTimeMs;
                finishedCount++;
            } else {
                
                queue.addLast(idx);
            }
        }

        double avgWait = (double) totalWaiting / finishedCount;
        double avgTurn = (double) totalTurnaround / finishedCount;

        return new SchedulerResult(gantt, avgWait, avgTurn);
    }

        /**
     * Non-preemptive Priority Scheduling with simple aging.
     * Higher number = higher priority.
     * All processes arrive at time 0.
     *
     * @param processes      list of PCBs
     * @param agingIntervalMs every this many ms of waiting, process priority is increased by 1 (capped at 128)
     */
    public static SchedulerResult runPriority(List<PCB> processes, int agingIntervalMs) {
        if (processes.isEmpty()) {
            return new SchedulerResult(new ArrayList<>(), 0.0, 0.0);
        }
        if (agingIntervalMs <= 0) {
            throw new IllegalArgumentException("Aging interval must be > 0");
        }

        int n = processes.size();
        List<PCB> procs = new ArrayList<>(processes);

        // Remaining burst times
        int[] remaining = new int[n];
        boolean[] finished = new boolean[n];

        for (int i = 0; i < n; i++) {
            PCB p = procs.get(i);
            remaining[i] = p.burstTimeMs;
            finished[i] = false;
            // reset stats
            p.waitingTimeMs = 0;
            p.turnaroundTimeMs = 0;
            p.state = ProcessState.READY;
        }

        List<GanttEntry> gantt = new ArrayList<>();
        int currentTime = 0;
        long totalWaiting = 0;
        long totalTurnaround = 0;
        int finishedCount = 0;

        
        
        int degreeOfMultiprogramming = n;


        while (finishedCount < n) {
            int bestIdx = -1;
            int bestEffPriority = Integer.MIN_VALUE;

            // Pick the highest effective priority (base priority + aging), tie by seq
            for (int i = 0; i < n; i++) {
                if (finished[i]) continue;

                PCB p = procs.get(i);
                long waitingSoFar = currentTime - p.arrivalTimeMs; // arrival = 0
                if (waitingSoFar < 0) waitingSoFar = 0;

                int ageBoost = (int) (waitingSoFar / agingIntervalMs);
                int effPriority = p.priority + ageBoost;
                if (effPriority > 128) effPriority = 128;

                if (bestIdx == -1 || effPriority > bestEffPriority ||
                        (effPriority == bestEffPriority && p.seq < procs.get(bestIdx).seq)) {
                    bestIdx = i;
                    bestEffPriority = effPriority;
                }
            }

            PCB current = procs.get(bestIdx);
            int start = currentTime;
            int execTime = remaining[bestIdx]; // non-preemptive: run to completion
            int end = start + execTime;

            currentTime = end;
            remaining[bestIdx] = 0;
            finished[bestIdx] = true;
            finishedCount++;

            current.state = ProcessState.TERMINATED;
            current.turnaroundTimeMs = currentTime - current.arrivalTimeMs;
            current.waitingTimeMs = current.turnaroundTimeMs - current.burstTimeMs;

            totalWaiting += current.waitingTimeMs;
            totalTurnaround += current.turnaroundTimeMs;

            gantt.add(new GanttEntry(current.id, start, end));

            
            if (current.waitingTimeMs > degreeOfMultiprogramming) {
            System.out.printf(
                ">> [PRIO] Starvation detected for P%d: waited %d ms (threshold %d)%n",
                    current.id, current.waitingTimeMs, degreeOfMultiprogramming
            );
        }
        }

        double avgWait = (double) totalWaiting / n;
        double avgTurn = (double) totalTurnaround / n;

        return new SchedulerResult(gantt, avgWait, avgTurn);
    }
}
