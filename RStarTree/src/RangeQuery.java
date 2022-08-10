import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;

public class RangeQuery extends Query {
    private static double minLat, minLon, maxLat, maxLon;

    public static void rangeQuery() {
//        getUserInput();
        minLat = 39.7160812;
        minLon = 20.5650812;
        maxLat = 39.8955962;
        maxLon = 20.6217563;

        ArrayList<Record> result = new ArrayList<>();
        try {
            Queue<Integer> pointers = new LinkedList<>();
            int blockSize = FileHandler.getBlockSize();
            int dimensions = FileHandler.getDimensions();

            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());

            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                pointers.add(1);

                while (!pointers.isEmpty()) {
                    byte[] block = new byte[blockSize];
                    System.arraycopy(bytes, pointers.peek() * blockSize, block, 0, blockSize);

                    byte[] level = new byte[Integer.BYTES];
                    byte[] currentNoOfEntries = new byte[Integer.BYTES];
                    byte[] parentPointer = new byte[Integer.BYTES];

                    System.arraycopy(block, 0, level, 0, Integer.BYTES);
                    System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
                    System.arraycopy(block, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

                    int tempLevel = ByteBuffer.wrap(level).getInt();
                    int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();

                    if (tempLevel == FileHandler.getLeafLevel()) {
                        byte[] LATarray = new byte[Double.BYTES];
                        byte[] LONarray = new byte[Double.BYTES];
                        byte[] RecordIdArray = new byte[Integer.BYTES];
                        int bytecounter = 3 * Integer.BYTES;

                        for (int j = 0; j < tempCurrentNoOfEntries; j++){
                            System.arraycopy(block, bytecounter, LATarray, 0, Double.BYTES);
                            System.arraycopy(block, bytecounter + Double.BYTES, LONarray, 0, Double.BYTES);
                            System.arraycopy(block, bytecounter + 2 * Double.BYTES, RecordIdArray, 0, Integer.BYTES);
                            bytecounter += 2 * Double.BYTES + Integer.BYTES;

                            double LAT = ByteBuffer.wrap(LATarray).getDouble();
                            double LON = ByteBuffer.wrap(LONarray).getDouble();
                            int recordId = ByteBuffer.wrap(RecordIdArray).getInt();

                            if (minLat <= LAT && LAT <= maxLat && minLon <= LON && LON <= maxLon) {
                                Record record = new Record(LAT, LON,
                                        FileHandler.getRecord(recordId).getRecordLocation().getBlock(),
                                        FileHandler.getRecord(recordId).getRecordLocation().getSlot(),
                                        recordId);
                                result.add(record);
                            }
                        }
                    } else {
                        int bytecounter = 3 * Integer.BYTES;
                        byte[][][] pointsArray= new byte[2][dimensions][Double.BYTES];
                        byte[] childPointer = new byte[Integer.BYTES];

                        Double rectMinLat, rectMinLon, rectMaxLat, rectMaxLon;
                        rectMinLat = rectMinLon = rectMaxLat = rectMaxLon = 0.0;

                        for (int j=0;j < tempCurrentNoOfEntries; j++)
                        {
                            for (int k=0;k<2;k++)
                            {
                                for (int c=0;c<dimensions;c++)
                                {
                                    System.arraycopy(block,bytecounter,pointsArray[k][c],0,Double.BYTES);
                                    bytecounter+=Double.BYTES;
                                    if (k == 0 && c == 0) {
                                        rectMinLat = ByteBuffer.wrap(pointsArray[k][c]).getDouble();
                                    } else if (k == 0 && c == 1) {
                                        rectMinLon = ByteBuffer.wrap(pointsArray[k][c]).getDouble();
                                    } else if (k == 1 && c == 0) {
                                        rectMaxLat = ByteBuffer.wrap(pointsArray[k][c]).getDouble();
                                    } else {
                                        rectMaxLon = ByteBuffer.wrap(pointsArray[k][c]).getDouble();
                                    }
                                }
                            }
                            pointsArray= new byte[2][dimensions][Double.BYTES];
                            System.arraycopy(block, bytecounter, childPointer, 0, Integer.BYTES);

                            if (rectMinLat <= maxLat && rectMaxLat >= minLat && rectMinLon <= maxLon && rectMaxLon >= minLon) {
                                pointers.add(ByteBuffer.wrap(childPointer).getInt());
                            }

                            bytecounter+=Integer.BYTES;
                        }
                    }
                    pointers.remove();
                }
                print(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void print(ArrayList<Record> result) {
        System.out.println("\nThere are " + result.size() + " entries found in the given range:\n");
        for (Record record: result) {
            System.out.println("LAT: " + record.getLAT() + ", LON: " + record.getLON() + ", ID:" + record.getId() +
                    ", Datafile block: " + FileHandler.getRecord(record.getId() ).getRecordLocation().getBlock() +
                    ", Block slot: " + FileHandler.getRecord(record.getId()).getRecordLocation().getSlot());
        }
    }

    private static void getUserInput() {
        System.out.println("Insert the rectangle coordinates (they should be only positive and not overlap in the same " +
                "axis): ");
        Scanner scanner = new Scanner(System.in);
        int counter = 0;

        while (true) {
            try {
                if (counter == 0) {
                    System.out.print("Minimum LAT: ");
                    minLat = scanner.nextDouble();
                } else if ( counter == 1) {
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

            if (++counter == 4) {
                break;
            }
        }
    }
}
