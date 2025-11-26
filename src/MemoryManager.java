public class MemoryManager {
    private final int capacityMB;
    private int usedMB = 0;

    public MemoryManager(int capacityMB) {
        this.capacityMB = capacityMB;
    }

    public synchronized boolean canAllocate(int reqMB) {
        return reqMB <= (capacityMB - usedMB);
    }

    public synchronized boolean allocate(int reqMB) {
        if (reqMB <= (capacityMB - usedMB)) {
            usedMB += reqMB;
            return true;
        }
        return false;
    }

    public synchronized void free(int mb) {
        usedMB -= mb;
        if (usedMB < 0) usedMB = 0;
        notifyAll();
    }

    public synchronized int used() { return usedMB; }
    public int capacity() { return capacityMB; }
}
