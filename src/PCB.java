import java.util.concurrent.atomic.AtomicLong;

public class PCB {
    private static final AtomicLong SEQ = new AtomicLong(0);

    public final long seq;           // insertion order (stability)
    public final int id;
    public final int burstTimeMs;
    public final int priority;       // 1 (lowest) .. 128 (highest)
    public final int memoryMB;

    // runtime fields (Phase 3 will update these)
    public volatile long arrivalTimeMs = 0;       // t=0 for all processes in spec
    public volatile long readyAcceptedTimeMs = -1;

    public volatile long waitingTimeMs = 0;
    public volatile long turnaroundTimeMs = 0;

    public volatile ProcessState state = ProcessState.NEW;

    public PCB(int id, int burstTimeMs, int priority, int memoryMB) {
        this.seq = SEQ.getAndIncrement();
        this.id = id;
        this.burstTimeMs = burstTimeMs;
        this.priority = priority;
        this.memoryMB = memoryMB;
    }

    @Override
    public String toString() {
        return "PCB{" +
                "id=" + id +
                ", burst=" + burstTimeMs +
                "ms, prio=" + priority +
                ", mem=" + memoryMB +
                "MB, state=" + state +
                '}';
    }
}
