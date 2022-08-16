import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Delete {
    private static final int minEntries = 670; // 670 | FileHandler.calculateMaxBlockNodes() * 40 / 100 | 969
    private static final int minRectangles = FileHandler.calculateMaxBlockRectangles() * 40 / 100;

    public static void delete(double LAT, double LON)
    {
        boolean result = delete(LAT, LON, 1);
        if (result)
        {
            System.out.println("The node with LAT: " + LAT + ", and LON: " + LON + ", was successfully deleted.");
        }
        else
        {
            System.out.println("The node with the given coordinates didn't get found.");
        }
    }

    private static boolean delete(double LAT, double LON, int blockId) {
        try {
            int blockSize = FileHandler.getBlockSize();
            int leafLevel = FileHandler.getLeafLevel();
            String IndexfilePath = FileHandler.getIndexfilePath();

            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dataBlock = new byte[blockSize];
            System.arraycopy(bytes, blockId * blockSize, dataBlock, 0, blockSize);

            byte[] blockLevel = new byte[Integer.BYTES];
            byte[] noOfEntries = new byte[Integer.BYTES];
            byte[] parentPointer = new byte[Integer.BYTES];

            System.arraycopy(dataBlock, 0, blockLevel, 0, Integer.BYTES);
            System.arraycopy(dataBlock, Integer.BYTES, noOfEntries, 0, Integer.BYTES);
            System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

            int tempBlockLevel = ByteBuffer.wrap(blockLevel).getInt();
            int tempNoOfEntries = ByteBuffer.wrap(noOfEntries).getInt();
            int tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();

            // NoOfEntries + block level + parent pointer
            int bytecounter = 3 * Integer.BYTES;

            if (tempBlockLevel == leafLevel) {
                byte[] LatArray = new byte[Double.BYTES];
                byte[] LonArray = new byte[Double.BYTES];

                for (int i = 0; i < tempNoOfEntries; i++) {
                    System.arraycopy(dataBlock, bytecounter, LatArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + Double.BYTES, LonArray, 0, Double.BYTES);

                    if (LAT == ByteBuffer.wrap(LatArray).getDouble() && LON == ByteBuffer.wrap(LonArray).getDouble()) {
                        int tempBytecounter = bytecounter + (tempNoOfEntries - i - 1) * (2 * Double.BYTES + Integer.BYTES);

                        // copy the data from the last entry into the space of the entry that gets deleted
                        System.arraycopy(dataBlock, tempBytecounter, dataBlock, bytecounter, Double.BYTES);
                        System.arraycopy(dataBlock, tempBytecounter + Double.BYTES, dataBlock, bytecounter + Double.BYTES, Double.BYTES);
                        System.arraycopy(dataBlock, tempBytecounter + 2 * Double.BYTES, dataBlock, bytecounter + 2 * Double.BYTES, Integer.BYTES);

                        // empty the data from the entry copied
                        System.arraycopy(new byte[Double.BYTES], 0, dataBlock, tempBytecounter, Double.BYTES);
                        System.arraycopy(new byte[Double.BYTES], 0, dataBlock, tempBytecounter + Double.BYTES, Double.BYTES);
                        System.arraycopy(new byte[Integer.BYTES], 0, dataBlock, tempBytecounter + 2 * Double.BYTES, Integer.BYTES);

                        // decrease the noOfEntries
                        tempNoOfEntries--;
                        System.arraycopy(ConversionToBytes.intToBytes(tempNoOfEntries), 0, dataBlock, Integer.BYTES, Integer.BYTES);

                        RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");

                        indexfile.seek((long) blockSize * blockId);
                        indexfile.write(dataBlock);
                        indexfile.close();

                        // If the entry was not in the root and the minimum number of entries wasn't reached, the
                        // rectangle bounds are readjusted
                        // else delete the rectangle and reinsert the nodes
                        if (blockId != 1 && tempNoOfEntries >= minEntries) {
                            ReAdjustRectangleBounds.reAdjustRectangleBounds(blockId, tempParentPointer);
                        } else if (blockId != 1) {
                            byte[] idArray = new byte[Integer.BYTES];

                            // Arraylist that will hold the nodes' data of the rectangle that will be deleted
                            ArrayList<Record> nodesToReInstert = new ArrayList<>();

                            // Start from the first node after the metadata
                            // Loop until the number of nodes is reached
                            // Increment by the size of each node
                            for (int j = 3 * Integer.BYTES;
                                 j < tempNoOfEntries * (2 * Double.BYTES + Integer.BYTES) + 3 * Integer.BYTES;
                                 j += 2 * Double.BYTES + Integer.BYTES) {
                                System.arraycopy(dataBlock, j, LatArray, 0, Double.BYTES);
                                System.arraycopy(dataBlock, j + Double.BYTES, LonArray, 0, Double.BYTES);
                                System.arraycopy(dataBlock, j + 2 * Double.BYTES, idArray, 0, Integer.BYTES);
                                Record record = new Record(ByteBuffer.wrap(LatArray).getDouble(),
                                        ByteBuffer.wrap(LonArray).getDouble(), ByteBuffer.wrap(idArray).getInt());
                                nodesToReInstert.add(record);
                            }

                            // delete the block
                            System.arraycopy(new byte[blockSize], 0, dataBlock, 0, blockSize);
                            indexfile = new RandomAccessFile(IndexfilePath, "rw");

                            indexfile.seek((long) blockSize * blockId);
                            indexfile.write(dataBlock);
                            indexfile.close();
                            ReAdjustRectangleBounds.reAdjustRectangleBounds(blockId, tempParentPointer);

                            // Reinsert the entries from the deleted rectangle
                            for (Record record: nodesToReInstert) {
                                Insert.insert(leafLevel, record);
                            }
                            // TODO metadata totnoofblocks
//                            upwardsDeletionCheck(tempParentPointer);
                        }

                        return true;
                    }

                    // LAT + LON + recordId
                    bytecounter += 2 * Double.BYTES + Integer.BYTES;
                }
                return false;
            } else {
                byte[] firstLatArray = new byte[Double.BYTES];
                byte[] firstLonArray = new byte[Double.BYTES];
                byte[] secondLatArray = new byte[Double.BYTES];
                byte[] secondLonArray = new byte[Double.BYTES];
                byte[] childPointer = new byte[Double.BYTES];

                for (int i = 0; i < tempNoOfEntries; i++) {
                    System.arraycopy(dataBlock, bytecounter, firstLatArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + Double.BYTES, firstLonArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + 2 * Double.BYTES, secondLatArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + 3 * Double.BYTES, secondLonArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + 4 * Double.BYTES, childPointer, 0, Integer.BYTES);

                    bytecounter += 4 * Double.BYTES + Integer.BYTES;

                    if (ByteBuffer.wrap(firstLatArray).getDouble() <= LAT && LAT <= ByteBuffer.wrap(secondLatArray).getDouble()
                            && ByteBuffer.wrap(firstLonArray).getDouble() <= LON && LON <= ByteBuffer.wrap(secondLonArray).getDouble()) {
                        return delete(LAT, LON, ByteBuffer.wrap(childPointer).getInt());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    private static void upwardsDeletionCheck(int blockId) {
//        try {
//            System.out.println(1);
//            int blockSize = FileHandler.getBlockSize();
//            int dimensions = FileHandler.getDimensions();
//
//            File file = new File(FileHandler.getIndexfilePath());
//            byte[] bytes = Files.readAllBytes(file.toPath());
//            byte[] dataBlock = new byte[blockSize];
//            System.arraycopy(bytes, blockId * blockSize, dataBlock, 0, blockSize);
//
//            byte[] noOfEntries = new byte[Integer.BYTES];
//            byte[] parentPointer = new byte[Integer.BYTES];
//
//            System.arraycopy(dataBlock, Integer.BYTES, noOfEntries, 0, Integer.BYTES);
//            System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);
//
//            int tempNoOfEntries = ByteBuffer.wrap(noOfEntries).getInt();
//            int tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();
//
//            if (blockId != 1 && tempNoOfEntries < minRectangles ){ // first block check
//                ArrayList<Rectangle> rectangles = new ArrayList<>();
//                byte[] minLon = new byte[Double.BYTES];
//                byte[] minLat = new byte[Double.BYTES];
//                byte[] maxLon = new byte[Double.BYTES];
//                byte[] maxLat = new byte[Double.BYTES];
//                byte[] childPointer = new byte[Integer.BYTES];
//
//                for (int i = 3 * Integer.BYTES;
//                     i < tempNoOfEntries * (2 * dimensions * Double.BYTES + Integer.BYTES) + 3 * Integer.BYTES;
//                     i += 2 * dimensions * Double.BYTES + Integer.BYTES) {
//                    // copy the rectangles' data from the block that needs to be deleted and add them the rectangles
//                    // arraylist
//                    System.arraycopy(dataBlock, i, minLon, 0, Double.BYTES);
//                    System.arraycopy(dataBlock, i + Double.BYTES, minLat, 0, Double.BYTES);
//                    System.arraycopy(dataBlock, i + 2 * Double.BYTES, maxLon, 0, Double.BYTES);
//                    System.arraycopy(dataBlock, i + 3 * Double.BYTES, maxLat, 0, Double.BYTES);
//                    System.arraycopy(dataBlock, i + 4 * Double.BYTES, maxLon, 0, Integer.BYTES);
//
//                    List<Double> coordinates = List.of(
//                            ByteBuffer.wrap(minLat).getDouble(),
//                            ByteBuffer.wrap(minLon).getDouble(),
//                            ByteBuffer.wrap(maxLat).getDouble(),
//                            ByteBuffer.wrap(maxLon).getDouble()
//                    );
//
//                    Rectangle rectangle = new Rectangle(
//                            new ArrayList<>(coordinates),
//                            ByteBuffer.wrap(childPointer).getInt()
//                    );
//
//                    rectangles.add(rectangle);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
