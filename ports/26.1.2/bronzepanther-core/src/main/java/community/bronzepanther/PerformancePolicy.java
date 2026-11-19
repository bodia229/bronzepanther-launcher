package community.bronzepanther;

/** A bounded, deliberately slow controller. One low frame never triggers a chunk rebuild. */
public final class PerformancePolicy {
    private int slow, fast;
    private long nextChange;
    public void reset() { slow = fast = 0; nextChange = 0; }
    public int sample(int fps, int distance, int ceiling, long seconds) {
        if (fps <= 0 || seconds < nextChange) return distance;
        slow = fps < 35 ? slow + 1 : 0;
        fast = fps > 70 ? fast + 1 : 0;
        if (slow >= 3 && distance > 4) {
            slow = fast = 0; nextChange = seconds + 45;
            return Math.max(4, distance - 2);
        }
        if (fast >= 12 && distance < ceiling) {
            slow = fast = 0; nextChange = seconds + 60;
            return Math.min(ceiling, distance + 1);
        }
        return distance;
    }
}
