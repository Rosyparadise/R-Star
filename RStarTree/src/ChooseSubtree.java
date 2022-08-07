import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;

public class ChooseSubtree {
    public static int chooseSubtree(Record record, int currentBlock)
    {
        try
        {
            int root = FileHandler.getRoot();
            String IndexfilePath = FileHandler.getIndexfilePath();
            int blockSize = FileHandler.getBlockSize();
            int leafLevel = FileHandler.getLeafLevel();
            int noOfIndexfileBlocks = FileHandler.getNoOfIndexfileBlocks();
            int dimensions = FileHandler.getDimensions();

            if (root==-1)
            {
                noOfIndexfileBlocks++;
                leafLevel++;
                FileHandler.setNoOfIndexfileBlocks(noOfIndexfileBlocks);
                FileHandler.setLeafLevel(leafLevel);

                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");

                indexfile.seek(4);
                indexfile.write(FileHandler.intToBytes(noOfIndexfileBlocks));
                indexfile.seek(8);
                indexfile.write(FileHandler.intToBytes(leafLevel));

                byte[] block = new byte[blockSize];
                System.arraycopy(FileHandler.intToBytes(leafLevel), 0, block, 0, Integer.BYTES);
                // current No of nodes/ rectangles
                System.arraycopy(FileHandler.intToBytes(0), 0, block, Integer.BYTES, Integer.BYTES);
                // parent pointer
                System.arraycopy(FileHandler.intToBytes(-1), 0, block, 2 * Integer.BYTES, Integer.BYTES);

                indexfile.seek((long) blockSize * noOfIndexfileBlocks);
                indexfile.write(block);
                indexfile.close();
            }

            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dataBlock = new byte[blockSize];
            System.arraycopy(bytes, currentBlock * blockSize, dataBlock, 0, blockSize);

            byte[] level = new byte[Integer.BYTES];
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            byte[] parentPointer = new byte[Integer.BYTES];
            System.arraycopy(dataBlock, 0, level, 0, Integer.BYTES);
            System.arraycopy(dataBlock, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

            int tempLevel = ByteBuffer.wrap(level).getInt();
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            int tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();

            // first if bracket in CS2
            if (tempLevel == leafLevel)
            {
                return currentBlock;
            }
            else if (tempLevel + 1 == leafLevel)
            {
                ArrayList<double[][]> rectangles = new ArrayList<>();
                double[][] temp = new double[dimensions][dimensions];
                int[] IDs = new int[tempCurrentNoOfEntries];
                int counter=12;
                byte[] tempSave = new byte[Double.BYTES];
                byte[] tempSaveID = new byte[Integer.BYTES];

                for (int i=0;i<tempCurrentNoOfEntries;i++)
                {
                    for (int j=0;j<2;j++)
                    {
                        for (int k=0;k<dimensions;k++)
                        {
                            System.arraycopy(dataBlock, counter, tempSave, 0, Double.BYTES);
                            temp[j][k]=ByteBuffer.wrap(tempSave).getDouble();
                            counter+=Double.BYTES;
                        }
                    }
                    rectangles.add(temp);
                    System.arraycopy(dataBlock, counter, tempSaveID, 0, Integer.BYTES);
                    IDs[i]=ByteBuffer.wrap(tempSaveID).getInt();

                    counter+=Integer.BYTES;
                    temp = new double[dimensions][dimensions];
                }

//                for (int i=0;i<IDs.length;i++)
//                {
//                    System.out.println("IDs  = " + IDs[i]);;
//                }
                int result = Split.determine_best_insertion(rectangles, record);
                return chooseSubtree(record, IDs[result]);

                // change currentblock so that the function works recursively
            }
            else
            {
                ArrayList<double[][]> rectangles = new ArrayList<>();
                double[][] temp = new double[dimensions][dimensions];
                int[] IDs = new int[tempCurrentNoOfEntries];
                int counter=12;
                byte[] tempSave = new byte[Double.BYTES];
                byte[] tempSaveID = new byte[Integer.BYTES];

                for (int i=0;i<tempCurrentNoOfEntries;i++)
                {
                    for (int j=0;j<2;j++)
                    {
                        for (int k=0;k<dimensions;k++)
                        {
                            System.arraycopy(dataBlock, counter, tempSave, 0, Double.BYTES);
                            temp[j][k]=ByteBuffer.wrap(tempSave).getDouble();
                            counter+=Double.BYTES;
                        }
                    }
                    rectangles.add(temp);
                    System.arraycopy(dataBlock, counter, tempSaveID, 0, Integer.BYTES);
                    IDs[i]=ByteBuffer.wrap(tempSaveID).getInt();

                    counter+=Integer.BYTES;
                    temp = new double[dimensions][dimensions];
                }
                int result = Split.determine_best_insertion_forRectangles(rectangles, record);
                return chooseSubtree(record, IDs[result]);
                // second if bracket in the else bracket in CS2
                // change currentblock so that the function works recursively
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        //return ChooseSubtree(leafLevel, record, currentBlock);
        return 1;
    }
}
