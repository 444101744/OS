public class GanttEntry {
    public final int processId;
    public final int startTime;
    public final int endTime;

    public GanttEntry(int processId, int startTime, int endTime) {
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
