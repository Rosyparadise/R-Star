import java.util.*;

public class KnnQuery {
    private int k;
    private final PriorityQueue<KnnDistanceRecordPair> knn;
    private final PriorityQueue<KnnDistanceRectanglePair> pointers;
    private double lat, lon;

    KnnQuery() {
//        getUserInput();
        k = 2;
        lat = 39.8836309;
        lon = 20.8595685;
        knn = new PriorityQueue<>(k, (o1, o2) -> Double.compare(o2.getDistance(), o1.getDistance()));
        pointers = new PriorityQueue<>(Comparator.comparingDouble(KnnDistanceRectanglePair::getDistance));
        this.knnQuery();
    }
    private void knnQuery() {
        try {
            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                int blockId = 1, level;

                do {
                    level = FileHandler.getMetaDataOfRectangle(blockId).get(0);

                    if (level != FileHandler.getLeafLevel()){
                        ArrayList<Rectangle> rectangles = FileHandler.getRectangleEntries(blockId);

                        for (Rectangle rectangle: rectangles) {
                            double distance = calcDistBetweenPointAndRectangle(lat, lon, rectangle);
                            if (!knn.isEmpty() && knn.size() == k && distance >= knn.peek().getDistance()) {
                                continue;
                            }
                            KnnDistanceRectanglePair pair = new KnnDistanceRectanglePair(rectangle, distance);
                            pointers.add(pair);
                        }
                    } else {
                        boolean condition = blockId == 1 || knn.isEmpty() || (pointers.peek() != null && knn.peek() != null &&
                                calcDistBetweenPointAndRectangle(lat, lon, pointers.peek().getRectangle()) < Objects.requireNonNull(knn.peek()).getDistance()
                        );

                        if (condition) {
                            ArrayList<Record> records = FileHandler.getRecords(blockId);

                            for (Record record: records) {
                                KnnDistanceRecordPair pair = new KnnDistanceRecordPair(record, calcDistBetweenPoints(
                                        record.getLAT(), record.getLON(), lat, lon));
                                knn.add(pair);
                                if (knn.size() > k) {
                                    knn.poll();
                                }
                            }
                        }
                    }
                    if (blockId != 1) {
                        pointers.remove();
                    }

                    if (pointers.peek() != null){
                        blockId = pointers.peek().getRectangle().getChildPointer();
                    }
                } while (!pointers.isEmpty());
                List<KnnDistanceRecordPair> pairs = new ArrayList<>(knn);
                print(pairs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getUserInput() {
        System.out.println("Insert the K and the coordinates of the point (k should be positive number >=1 and " +
                "coordinates should also be positive): ");
        Scanner scanner = new Scanner(System.in);
        int counter = 0;

        do {
            try {
                if (counter == 0) {
                    System.out.print("K: ");
                    k = scanner.nextInt();
                } else if (counter == 1) {
                    System.out.print("LAT: ");
                    lat = scanner.nextDouble();
                } else if (counter == 2) {
                    System.out.println("Lon: ");
                    lon = scanner.nextDouble();
                }
            } catch (InputMismatchException e) {
                invalidArgs("type");
                counter = -1;
                scanner.nextLine();
            }

            if (counter != -1 && (lat < 0 || lon < 0)) {
                invalidArgs("negative");
                counter = -1;
                scanner.nextLine();
            }

        } while (++counter != 3);
    }

    private void invalidArgs(String desc) {
        if (desc.equals("type")) {
            System.out.println("Invalid arguments. K should be an Integer and coordinates should be " +
                    "Double/Float or Integer.");
        } else if (desc.equals("negative")) {
           System.out.println("Invalid arguments. Coordinates should only be positive.");
        }
    }

    private double calcDistBetweenPoints(double x1, double y1, double x2, double y2) {
        double dx = Math.abs(x2 - x1);
        double dy = Math.abs(y2 - y1);
        return Math.hypot(dx, dy);
    }

    private double calcDistBetweenPointAndRectangle(double lat, double lon, Rectangle rectangle) {
        double dLAT = Math.max(rectangle.getMinLAT() - lat, Math.max(0, lat - rectangle.getMaxLAT()));
        double dLON = Math.max(rectangle.getMinLON() - lon, Math.max(0, lon - rectangle.getMaxLON()));
        return Math.hypot(dLAT, dLON);
    }

    private void print(List<KnnDistanceRecordPair> knnDistanceRecordPairs){
        Collections.reverse(knnDistanceRecordPairs);
        System.out.println("The " + k + " nearest neighbors are: ");
        for (int i = 0; i < knnDistanceRecordPairs.size(); i++){
            System.out.println(i + 1 + ")" +
                    " Distance: " + knnDistanceRecordPairs.get(i).getDistance() +
                    ", LAT: " + knnDistanceRecordPairs.get(i).getRecord().getLAT() +
                    ", LON: " + knnDistanceRecordPairs.get(i).getRecord().getLON() +
                    ", ID: " + knnDistanceRecordPairs.get(i).getRecord().getId() +
                    ", Datafile block: " + knnDistanceRecordPairs.get(i).getRecord().getRecordLocation().getBlock() +
                    ", Block slot: " + knnDistanceRecordPairs.get(i).getRecord().getRecordLocation().getSlot()
            );
        }
    }
}
