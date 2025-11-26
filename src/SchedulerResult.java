import java.util.List;

public class SchedulerResult {
    public final List<GanttEntry> gantt;
    public final double avgWaitingTime;
    public final double avgTurnaroundTime;

    public SchedulerResult(List<GanttEntry> gantt,
                           double avgWaitingTime,
                           double avgTurnaroundTime) {
        this.gantt = gantt;
        this.avgWaitingTime = avgWaitingTime;
        this.avgTurnaroundTime = avgTurnaroundTime;
    }
}
