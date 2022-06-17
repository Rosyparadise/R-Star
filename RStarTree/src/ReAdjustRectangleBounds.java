import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ReAdjustRectangleBounds {

    public static void reAdjustRectangleBounds(int blockId, int parentBlockId)
    {
        if (blockId >= 1)
        {
            try
            {
                String IndexfilePath = FileHandler.getIndexfilePath();
                int blockSize = FileHandler.getBlockSize();
                int leafLevel = FileHandler.getLeafLevel();

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
                int tempParentPointer;

                // NoOfEntries + block level + parent pointer
                int bytecounter = 3 * Integer.BYTES;

                byte[] minLatArray = new byte[Double.BYTES];
                byte[] maxLatArray = new byte[Double.BYTES];
                byte[] minLonArray = new byte[Double.BYTES];
                byte[] maxLonArray = new byte[Double.BYTES];

                double minLat = 0.0, maxLat = 0.0, minLon = 0.0, maxLon = 0.0;
                boolean flag = false;
                if (tempBlockLevel == leafLevel && blockId > 1)
                {
                    flag = true;

                    byte[] LatArray = new byte[Double.BYTES];
                    byte[] LonArray = new byte[Double.BYTES];

                    System.arraycopy(dataBlock, bytecounter, LatArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + Double.BYTES, LonArray, 0, Double.BYTES);

                    // LAT + LON + record id
                    bytecounter += 2 * Double.BYTES + Integer.BYTES;

                    minLat = ByteBuffer.wrap(LatArray).getDouble();
                    maxLat = ByteBuffer.wrap(LatArray).getDouble();
                    minLon = ByteBuffer.wrap(LonArray).getDouble();
                    maxLon = ByteBuffer.wrap(LonArray).getDouble();

                    for (int i = 1; i < tempNoOfEntries; i++)
                    {
                        System.arraycopy(dataBlock, bytecounter, LatArray, 0, Double.BYTES);
                        System.arraycopy(dataBlock, bytecounter + Double.BYTES, LonArray, 0, Double.BYTES);

                        if (ByteBuffer.wrap(LatArray).getDouble() < minLat)
                        {
                            minLat = ByteBuffer.wrap(LatArray).getDouble();
                        }

                        if (ByteBuffer.wrap(LatArray).getDouble() > maxLat)
                        {
                            maxLat = ByteBuffer.wrap(LatArray).getDouble();
                        }

                        if (ByteBuffer.wrap(LonArray).getDouble() < minLon)
                        {
                            minLon = ByteBuffer.wrap(LonArray).getDouble();
                        }

                        if (ByteBuffer.wrap(LonArray).getDouble() > maxLon)
                        {
                            maxLon = ByteBuffer.wrap(LonArray).getDouble();
                        }

                        bytecounter += 2 * Double.BYTES + Integer.BYTES;
                    }
                }
                else if (tempBlockLevel < leafLevel)//!=root
                {
                    flag = true;

                    System.arraycopy(dataBlock, bytecounter, minLatArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + Double.BYTES, minLonArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + 2 * Double.BYTES, maxLatArray, 0, Double.BYTES);
                    System.arraycopy(dataBlock, bytecounter + 3 * Double.BYTES, maxLonArray, 0, Double.BYTES);

                    minLat = ByteBuffer.wrap(minLatArray).getDouble();
                    maxLat = ByteBuffer.wrap(maxLatArray).getDouble();
                    minLon = ByteBuffer.wrap(minLonArray).getDouble();
                    maxLon = ByteBuffer.wrap(maxLonArray).getDouble();

                    bytecounter += 4 * Double.BYTES + Integer.BYTES;

                    for (int i = 1; i < tempNoOfEntries; i++)
                    {
                        System.arraycopy(dataBlock, bytecounter, minLatArray, 0, Double.BYTES);
                        System.arraycopy(dataBlock, bytecounter + Double.BYTES, minLonArray, 0, Double.BYTES);
                        System.arraycopy(dataBlock, bytecounter + 2 * Double.BYTES, maxLatArray, 0, Double.BYTES);
                        System.arraycopy(dataBlock, bytecounter + 3 * Double.BYTES, maxLonArray, 0, Double.BYTES);

                        if (ByteBuffer.wrap(minLatArray).getDouble() < minLat)
                        {
                            minLat = ByteBuffer.wrap(minLatArray).getDouble();
                        }

                        if (ByteBuffer.wrap(maxLatArray).getDouble() > maxLat)
                        {
                            maxLat = ByteBuffer.wrap(maxLatArray).getDouble();
                        }

                        if (ByteBuffer.wrap(minLonArray).getDouble() < minLon)
                        {
                            minLon = ByteBuffer.wrap(minLonArray).getDouble();
                        }

                        if (ByteBuffer.wrap(minLonArray).getDouble() > maxLon)
                        {
                            maxLon = ByteBuffer.wrap(minLonArray).getDouble();
                        }

                        bytecounter += 4 * Double.BYTES + Integer.BYTES;
                    }
                }

                if (flag)
                {
                    System.arraycopy(bytes, parentBlockId * blockSize, dataBlock, 0, blockSize);

                    System.arraycopy(dataBlock, Integer.BYTES, noOfEntries, 0, Integer.BYTES);
                    System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

                    tempNoOfEntries = ByteBuffer.wrap(noOfEntries).getInt();
                    tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();

                    bytecounter = 3 * Integer.BYTES;

                    for (int i = 0; i < tempNoOfEntries; i++)
                    {
                        byte[] childBlockIdArray = new byte[Double.BYTES];
                        System.arraycopy(dataBlock, bytecounter + 4 * Double.BYTES, childBlockIdArray, 0, Double.BYTES);

                        if (ByteBuffer.wrap(childBlockIdArray).getInt() == blockId)
                        {
                            System.arraycopy(dataBlock, bytecounter, minLatArray, 0, Double.BYTES);
                            System.arraycopy(dataBlock, bytecounter + Double.BYTES, minLonArray, 0, Double.BYTES);
                            System.arraycopy(dataBlock, bytecounter + 2 * Double.BYTES, maxLatArray, 0, Double.BYTES);
                            System.arraycopy(dataBlock, bytecounter + 3 * Double.BYTES, maxLonArray, 0, Double.BYTES);

                            if (!(ByteBuffer.wrap(minLatArray).getDouble() == minLat && ByteBuffer.wrap(maxLatArray).getDouble() == maxLat
                                    && ByteBuffer.wrap(minLonArray).getDouble() == minLon && ByteBuffer.wrap(maxLonArray).getDouble() == maxLon)){
                                System.arraycopy(FileHandler.doubleToBytes(minLat), 0, dataBlock, bytecounter, Double.BYTES);
                                System.arraycopy(FileHandler.doubleToBytes(minLon), 0, dataBlock, bytecounter + Double.BYTES, Double.BYTES);
                                System.arraycopy(FileHandler.doubleToBytes(maxLat), 0, dataBlock, bytecounter + 2 * Double.BYTES, Double.BYTES);
                                System.arraycopy(FileHandler.doubleToBytes(maxLon), 0, dataBlock, bytecounter + 3 * Double.BYTES, Double.BYTES);

                                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");

                                indexfile.seek((long) blockSize * parentBlockId);
                                indexfile.write(dataBlock);
                                indexfile.close();

                                if (parentBlockId != 1){
                                    reAdjustRectangleBounds(parentBlockId, tempParentPointer);
                                }
                            }
                            break;
                        }
                        bytecounter += 4 * Double.BYTES + Integer.BYTES;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
