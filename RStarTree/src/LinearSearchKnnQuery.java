import java.util.ArrayList;
import java.util.List;

public class LinearSearchKnnQuery extends KnnQuery {
    LinearSearchKnnQuery(int k, ArrayList<Double> coordinates) {
        super(k, coordinates);
    }

    protected void knnQuery() {
        ArrayList<Record> records = FileHandler.getDatafileRecords();
        for (Record record: records) {
            List<Double> recordCoords = List.of(record.getLAT(), record.getLON());
            KnnDistanceRecordPair pair = new KnnDistanceRecordPair(record, calcDistBetweenPoints(
                    new ArrayList<>(recordCoords),
                    coordinates));
            knn.add(pair);
            if (knn.size() > k) {
                knn.poll();
            }
        }
    }
}
