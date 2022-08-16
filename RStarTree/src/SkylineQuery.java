import java.util.*;

public class SkylineQuery {
    private final Stack<Integer> pointers;
    private final ArrayList<Record> result;
    private final int dimensions;

    SkylineQuery() {
        this.dimensions = FileHandler.getDimensions();
        pointers = new Stack<>();
        result = new ArrayList<>();
        this.skylineQuery();
    }

    private void skylineQuery() {
        try {
            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                pointers.add(1);
                int blockId, level;

                while (!pointers.isEmpty()){
                    blockId = pointers.peek();
                    pointers.pop();

                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);

                    if (level != FileHandler.getLeafLevel()) {
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);

                        outerLoop:
                        for (Rectangle rectangle: rectangles) {
//                            System.out.println(rectangle.getChildPointer());
                            if (!result.isEmpty()) {
                                for (Record record: result) {
                                    if (record.getLON() <= rectangle.getCoordinates().get(0) &&
                                            record.getLON() <= rectangle.getCoordinates().get(dimensions) &&
                                            record.getLON() <= rectangle.getCoordinates().get(1) &&
                                            record.getLON() <= rectangle.getCoordinates().get(1 + dimensions)
                                    ) {
                                        continue outerLoop;
                                    }
                                }
                            }

                            pointers.push(rectangle.getChildPointer());
                        }
                    } else {
                        ArrayList<Record> records = FileHandler.getRecords(blockId);

                        for (Record record: records) {
                            boolean condition = false;
                            if (result.isEmpty()) {
                                condition = true;
                            } else {
                                Iterator<Record> iterator = result.iterator();

                                while (iterator.hasNext()) {
                                    Record record1 = iterator.next();
                                    if (record1.getLON() <= record.getLON() && record1.getLAT() <= record.getLAT()){
                                        condition = false;
                                        break;
                                    } else if (record.getLON() < record1.getLON() && record.getLAT() < record1.getLAT()) {
                                        condition = true;
                                        iterator.remove();
                                    } else {
                                        condition = true;
                                    }
                                }
                            }

                            if (condition) {
                                result.add(record);
                                result.sort((r1, r2) -> Double.compare(r2.getLAT(), r1.getLAT()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void print() {
        System.out.println("There are " + result.size() + " entries in the skyline: ");
        for (Record record: result) {
            System.out.print("LAT: " + record.getLAT() +
                    ", LON: " + record.getLON() +
                    ", Datafile block: " + FileHandler.getRecord(record.getId() ).getRecordLocation().getBlock() +
                    ", Block slot: " + FileHandler.getRecord(record.getId()).getRecordLocation().getSlot());

            if (record.getName() != null && !record.getName().equals("")) {
                System.out.print(", Name: " + record.getName());
            }

            if (record.getNodeId() != 0) {
                System.out.print(", Node ID: " + record.getNodeId());
            }

            System.out.println();
        }
    }
}
