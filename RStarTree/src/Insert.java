import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Insert {
    private static boolean overflow_first_time = false;
    private static boolean reinsert_just_ended = false;
    private static int overflowLevel = -1;

    public static void insert(int leafLevel, Record record){
        // call ChooseSubtree to find the best block to save the node and save it to blockId

        int blockId = ChooseSubtree.chooseSubtree(record, 1);

        try {
            String IndexfilePath = FileHandler.getIndexfilePath();
            int blockSize = FileHandler.getBlockSize();
            // read all indexfile
            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] block = new byte[blockSize];
            // copy only the block that we need for the Insert based on blockId
            System.arraycopy(bytes, blockId * blockSize, block, 0, blockSize);

            // get the current number of nodes inserted in the block
            byte[] treeLevelBytes = new byte[Integer.BYTES];
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            byte[] parentPointerArray = new byte[Integer.BYTES];

            System.arraycopy(block, 0, treeLevelBytes, 0, Integer.BYTES);
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            System.arraycopy(block, 2 * Integer.BYTES, parentPointerArray, 0, Integer.BYTES);

            int treeLevel = ByteBuffer.wrap(treeLevelBytes).getInt();
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            int parentPointer = ByteBuffer.wrap(parentPointerArray).getInt();

            // check if it can be added
            if (tempCurrentNoOfEntries < FileHandler.calculateMaxBlockNodes()){
                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");
                // calculate the byte address which the node info will be written in the indexfile.
                // So, block location (blockId * blockSize), metadata size (2 * Integer.BYTES), currently added nodes
                // (tempCurrentNoOfEntries * (2 * Double.BYTES + Integer.BYTES)
                int ByteToWrite = 3 * Integer.BYTES + tempCurrentNoOfEntries * (2 * Double.BYTES + Integer.BYTES) + blockId * blockSize;
                byte[] datablock = new byte[2 * Double.BYTES + Integer.BYTES];

                System.arraycopy(ConversionToBytes.doubleToBytes(record.getLAT()), 0, datablock, 0, Double.BYTES);
                System.arraycopy(ConversionToBytes.doubleToBytes(record.getLON()), 0, datablock, Double.BYTES, Double.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(record.getId()), 0, datablock, 2 * Double.BYTES, Integer.BYTES);

                indexfile.seek(ByteToWrite);
                indexfile.write(datablock);

                //HAVE TO CHANGE LATER, WORKS NOW CAUSE WE ONLY HAVE ROOT
                if (blockId==1)
                    Split.calculateMBRpointbypoint(FileHandler.getRootMBR(),record, tempCurrentNoOfEntries == 0,false);
                else
                    ReadjustMBR.reAdjustRectangleBounds(blockId,parentPointer,record,false);

                tempCurrentNoOfEntries++;
                indexfile.seek((long) blockId * blockSize + Integer.BYTES);
                indexfile.write(ConversionToBytes.intToBytes(tempCurrentNoOfEntries));
                if (FileHandler.getRoot() ==-1)
                    FileHandler.setRoot(blockSize);

                indexfile.close();
            }
            else if (tempCurrentNoOfEntries == FileHandler.calculateMaxBlockNodes())
            {
                overflowTreatment(treeLevel,blockId,record);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void overflowTreatment(int treeLevel,int blockid,Record troublemaker)
    {
//        System.out.println("overflow"  + troublemaker.getId());
        if (treeLevel!=overflowLevel)
        {
            overflow_first_time=true;
            overflowLevel=treeLevel;
        }
        if (treeLevel!=0 && overflow_first_time)
        {
//            System.out.println("----------BEFORE REINSERT----------");
            overflow_first_time=false;
//            FileHandler.readIndexFile();

            Split.reinsert(blockid,troublemaker);
//            System.out.println("-----------AFTER REINSERT----------");
//            FileHandler.readIndexFile();
        }
        else
        {
//            System.out.println("----------BEFORE SPLIT----------");
//            FileHandler.readIndexFile();

//            System.out.println("split");
            Split.split(blockid, troublemaker);
//            System.out.println("-----------AFTER SPLIT----------");
//            FileHandler.readIndexFile();

            overflowLevel = -1;
        }
    }
}
