import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class Insert {
    private static int overflowCounter = 0;
    private static int overflowLevel =- 1;

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

            // check if it can be added
            if (tempCurrentNoOfEntries < FileHandler.calculateMaxBlockNodes()){
                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");
                // calculate the byte address which the node info will be written in the indexfile.
                // So, block location (blockId * blockSize), metadata size (2 * Integer.BYTES), currently added nodes
                // (tempCurrentNoOfEntries * (2 * Double.BYTES + Integer.BYTES)
                int ByteToWrite = 3 * Integer.BYTES + tempCurrentNoOfEntries * (2 * Double.BYTES + Integer.BYTES) + blockId * blockSize;
                byte[] datablock = new byte[2 * Double.BYTES + Integer.BYTES];

                System.arraycopy(FileHandler.doubleToBytes(record.getLAT()), 0, datablock, 0, Double.BYTES);
                System.arraycopy(FileHandler.doubleToBytes(record.getLON()), 0, datablock, Double.BYTES, Double.BYTES);
                System.arraycopy(FileHandler.intToBytes(record.getId()), 0, datablock, 2 * Double.BYTES, Integer.BYTES);

                indexfile.seek(ByteToWrite);
                indexfile.write(datablock);

                //HAVE TO CHANGE LATER, WORKS NOW CAUSE WE ONLY HAVE ROOT
                Split.calculateMBRpointbypoint(FileHandler.getRootMBR(),record, tempCurrentNoOfEntries == 0);


                tempCurrentNoOfEntries++;
                indexfile.seek((long) blockId * blockSize + Integer.BYTES);
                indexfile.write(FileHandler.intToBytes(tempCurrentNoOfEntries));
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
        if (treeLevel!=0 && overflowCounter==0)
        {
            if (overflowLevel==treeLevel)
            {
                //reinsert overflowcounter = 0 and level = -1
            }
            else
            {
                overflowCounter=0;
                overflowLevel=treeLevel;
            }

            //do smth
        }
        else
        {
            Split.split(blockid,troublemaker);
        }
    }
}
