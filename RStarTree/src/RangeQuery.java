import java.util.*;

public class RangeQuery {
    private double minLat, minLon, maxLat, maxLon;
    private final ArrayList<Record> result;
    private final Queue<Integer> pointers;

    RangeQuery() {
//        getUserInput();
        minLat = 39.7160812;
        minLon = 20.5650812;
        maxLat = 39.8955962;
        maxLon = 20.6217563;
        result = new ArrayList<>();
        pointers = new LinkedList<>();
        this.rangeQuery();
    }
    private void rangeQuery() {
        try {
            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                pointers.add(1);
                int blockId, level;

                while (!pointers.isEmpty()) {
                    blockId = pointers.peek();
                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);

                    if (level != FileHandler.getLeafLevel()){
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);

                        for (Rectangle rectangle: rectangles) {
                            if (rectangle.getMinLAT() <= maxLat &&
                                    rectangle.getMaxLAT() >= minLat &&
                                    rectangle.getMinLON() <= maxLon &&
                                    rectangle.getMaxLON() >= minLon) {
                                pointers.add(rectangle.getChildPointer());
                            }
                        }
                    } else {
                        ArrayList<Record> records = FileHandler.getRecords(blockId);

                        for (Record record: records) {
                            if (record.getLAT() >= minLat &&
                                    record.getLAT() <= maxLat &&
                                    record.getLON() >= minLon &&
                                    record.getLON() <= maxLon) {
                                result.add(record);
                            }
                        }
                    }
                    pointers.remove();
                }
                print();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print() {
        System.out.println("\nThere are " + result.size() + " entries found in the given range:\n");
        for (Record record: result) {
            System.out.println("LAT: " + record.getLAT() +
                    ", LON: " + record.getLON() +
                    ", ID:" + record.getId() +
                    ", Datafile block: " + FileHandler.getRecord(record.getId() ).getRecordLocation().getBlock() +
                    ", Block slot: " + FileHandler.getRecord(record.getId()).getRecordLocation().getSlot());
        }
    }

    private void getUserInput() {
        System.out.println("Insert the rectangle coordinates (they should be only positive and not overlap in the same " +
                "axis): ");
        Scanner scanner = new Scanner(System.in);
        int counter = 0;

        do {
            try {
                if (counter == 0) {
                    System.out.print("Minimum LAT: ");
                    minLat = scanner.nextDouble();
                } else if (counter == 1) {
                    System.out.print("Minimum LON: ");
                    minLon = scanner.nextDouble();
                } else if (counter == 2) {
                    System.out.print("Maximum LAT: ");
                    maxLat = scanner.nextDouble();
                } else {
                    System.out.print("Maximum LON: ");
                    maxLon = scanner.nextDouble();
                }
            } catch (InputMismatchException e) {
                invalidArgs("type");
                counter = -1;
                scanner.nextLine();
            }

            if (counter != -1 && (minLon < 0 || maxLon < 0 || minLat < 0 || maxLat < 0)) {
                invalidArgs("negative");
                counter = -1;
                scanner.nextLine();
            }

            if ((minLat != 0 && minLat == maxLat) || (minLon != 0 && minLon == maxLon)) {
                invalidArgs("overlap");
                counter = -1;
                scanner.nextLine();
            }

        } while (++counter != 4);
    }

    private void invalidArgs(String desc) {
        switch (desc) {
            case "negative" -> System.out.println("Invalid arguments. Coordinates should only be positive.");
            case "overlap" ->
                    System.out.println("Invalid arguments. Coordinates between the same axis shouldn't overlap.");
            case "type" -> System.out.println("Invalid arguments. Coordinates should be Double/Float or Integers");
        }
    }
}