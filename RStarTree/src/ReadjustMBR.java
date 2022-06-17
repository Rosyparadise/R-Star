import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ReadjustMBR {

    public static void reAdjustRectangleBounds(int blockId, int parentBlockId,Object troublemaker)
    {
            try
            {
                String IndexfilePath = FileHandler.getIndexfilePath();
                int blockSize = FileHandler.getBlockSize();
                int leafLevel = FileHandler.getLeafLevel();
                int dimensions = FileHandler.getDimensions();

                RandomAccessFile bytes = new RandomAccessFile(IndexfilePath, "rw");
                byte[] dataBlock = new byte[blockSize];
                bytes.seek((long) parentBlockId * blockSize);
                bytes.readFully(dataBlock, 0, blockSize);

                byte[] blockLevel = new byte[Integer.BYTES];
                byte[] noOfEntries = new byte[Integer.BYTES];
                byte[] parentPointer = new byte[Integer.BYTES];

                System.arraycopy(dataBlock, 0, blockLevel, 0, Integer.BYTES);
                System.arraycopy(dataBlock, Integer.BYTES, noOfEntries, 0, Integer.BYTES);
                System.arraycopy(dataBlock, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

                int tempBlockLevel = ByteBuffer.wrap(blockLevel).getInt();
                int tempNoOfEntries = ByteBuffer.wrap(noOfEntries).getInt();
                int tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();

                double[][] MBR = new double[dimensions][dimensions];
                byte[] tempForDoubles = new byte[Double.BYTES];

                int bytecounter = 3 * Integer.BYTES;

                outer: for (int i = 0; i < tempNoOfEntries; i++)
                {
                    byte[] childBlockIdArray = new byte[Double.BYTES];
                    System.arraycopy(dataBlock, bytecounter + 4 * Double.BYTES, childBlockIdArray, 0, Integer.BYTES);

                    if (ByteBuffer.wrap(childBlockIdArray).getInt() == blockId)
                    {
                        for (int j = 0; j < 2; j++)
                        {
                            for (int k = 0; k < dimensions; k++)
                            {
                                System.arraycopy(dataBlock, bytecounter, tempForDoubles, 0, Double.BYTES);
                                MBR[j][k] = ByteBuffer.wrap(tempForDoubles).getDouble();
                                bytecounter += Double.BYTES;
                            }
                        }
                        break outer;

                    }
                    else
                        bytecounter += 4 * Double.BYTES + Integer.BYTES;
                }

                double[][] rectangle = new double[(int) Math.pow(2, dimensions)][dimensions];
                double[][] rectangleNEW = new double[(int) Math.pow(2, dimensions)][dimensions];

                Split.points_to_rectangle(MBR, rectangle);
                for (int i=0;i<rectangle.length;i++)
                    System.arraycopy(rectangle[i], 0, rectangleNEW[i], 0, dimensions);


                if (tempBlockLevel+1==leafLevel)
                    Split.calculateMBRpointbypoint(rectangle, (Record) troublemaker,false);
                else
                {
                    double[][] mbr = (double[][]) troublemaker;
                    for (int l=0;l<mbr.length;l++)
                       Split.calculateMBRpointbypoint(rectangle,new Record(mbr[l][0],mbr[l][1],-1),false);

                }

                boolean flag=false;

                outer: for (int i=0;i<rectangle.length;i++)
                {
                    for (int j=0;j<rectangle[0].length;j++)
                    {
                        if (rectangle[i][j]!=rectangleNEW[i][j])
                        {
                            flag=true;
                            break outer;
                        }
                    }
                }

                if (flag)
                {
                    byte[][] newMBR = new byte[dimensions][dimensions];
                    bytecounter-=Double.BYTES*4;
                    for (int i=0;i<rectangle.length;i+=rectangle.length-1)
                    {
                        for (int j=0;j<rectangle[0].length;j++)
                        {
                            System.arraycopy(FileHandler.doubleToBytes(rectangle[i][j]), 0,dataBlock,bytecounter,Double.BYTES);
                            bytecounter+=Double.BYTES;
                        }
                    }
                    bytes.seek((long) parentBlockId *blockSize);
                    bytes.write(dataBlock);
                    if (tempParentPointer==-1)
                    {
                        for (int l=0;l<rectangle.length;l++)
                            Split.calculateMBRpointbypoint(FileHandler.getRootMBR(),new Record(rectangle[l][0],rectangle[l][1],-1),false);

                        return;
                    }
                    reAdjustRectangleBounds(parentBlockId,tempParentPointer,rectangle);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
    }
}
