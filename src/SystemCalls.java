public class SystemCalls {

    // ---- Process control ----

    public static PCB sysCreateProcess(int id, int burstTimeMs, int priority, int memoryMB) {
        PCB pcb = new PCB(id, burstTimeMs, priority, memoryMB);
        pcb.state = ProcessState.NEW;
        pcb.arrivalTimeMs = 0; 
        return pcb;
    }

    public static void sysAdmitToJobQueue(PCB pcb, Queues queues) throws InterruptedException {
        
        pcb.state = ProcessState.NEW;
        queues.jobQueue.put(pcb);
    }

    // ---- Info / statistics ----

    public static void sysPrintProcessInfo(PCB pcb) {
        System.out.printf(
                "Process %d: burst=%d ms, prio=%d, mem=%d MB, state=%s, waiting=%d, turnaround=%d%n",
                pcb.id,
                pcb.burstTimeMs,
                pcb.priority,
                pcb.memoryMB,
                pcb.state,
                pcb.waitingTimeMs,
                pcb.turnaroundTimeMs
        );
    }

    // ---- Memory management ----

    public static void sysPrintMemoryInfo(MemoryManager memory) {
        System.out.printf(
                "Memory usage: %d/%d MB%n",
                memory.used(),
                memory.capacity()
        );
    }

        public static void sysTerminateProcess(PCB pcb, MemoryManager memory) {
        pcb.state = ProcessState.TERMINATED;
        memory.free(pcb.memoryMB);
        System.out.printf("sysTerminateProcess: P%d terminated, freed %d MB%n",
                pcb.id, pcb.memoryMB);
    }

}
