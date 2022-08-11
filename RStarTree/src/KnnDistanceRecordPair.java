public class KnnDistanceRecordPair {
    private final Record record;
    private final double distance;

    KnnDistanceRecordPair(Record record, double distance) {
        this.record = record;
        this.distance = distance;
    }

    public Record getRecord() {
        return record;
    }

    public double getDistance() {
        return distance;
    }
}