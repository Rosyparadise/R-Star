import java.util.ArrayList;

public class LinearSearchRangeQuery extends RangeQuery {
    LinearSearchRangeQuery(Rectangle rectangle) {
        super(rectangle);
    }

    protected void rangeQuery() {
        ArrayList<Record> datafileRecords = FileHandler.getDatafileRecords();
        for (Record record: datafileRecords) {
            if (record.getLAT() >= rangeRectangle.getCoordinates().get(0) &&
                    record.getLAT() <= rangeRectangle.getCoordinates().get(dimensions) &&
                    record.getLON() >= rangeRectangle.getCoordinates().get(1) &&
                    record.getLON() <= rangeRectangle.getCoordinates().get(1 + dimensions)) {
                result.add(record);
            }
        }
    }
}
