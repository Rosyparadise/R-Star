import java.util.*;

public class SkylineQuery {
    Stack<Integer> pointers;
    ArrayList<Record> result;

    SkylineQuery() {
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
                            // TODO check functionality for discard of dominated rectangles in tree with >=3 levels
//                            System.out.println(rectangle.getChildPointer());
                            if (!result.isEmpty()) {
                                for (Record record: result) {
                                    if (record.getLON() <= rectangle.getMinLON() &&
                                            record.getLON() <= rectangle.getMaxLON() &&
                                            record.getLON() <= rectangle.getMinLAT() &&
                                            record.getLON() <= rectangle.getMaxLAT()
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
                print();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print() {
        System.out.println("There are " + result.size() + " entries in the skyline: ");
        for (Record record: result) {
            System.out.println("LAT: " + record.getLAT() +
                    ", LON: " + record.getLON() +
                    ", ID:" + record.getId() +
                    ", Datafile block: " + FileHandler.getRecord(record.getId() ).getRecordLocation().getBlock() +
                    ", Block slot: " + FileHandler.getRecord(record.getId()).getRecordLocation().getSlot());
        }
    }
}
