import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class Insert {
    private static boolean overflow_first_time = false;
    private static int overflowLevel = -1;

    public static void insert(Record record){
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

                if (blockId==1)
                    Split.calculateMBRpointbypoint(FileHandler.getRootMBR(),record, tempCurrentNoOfEntries == 0,false);
                else
                    ReadjustMBR.reAdjustRectangleBounds(blockId,parentPointer,record,false);

                tempCurrentNoOfEntries++;
                indexfile.seek((long) blockId * blockSize + Integer.BYTES);
                indexfile.write(ConversionToBytes.intToBytes(tempCurrentNoOfEntries));
                if (FileHandler.getRoot() ==-1)
                    FileHandler.setRoot(blockId);


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

    public static void overflowTreatment(int treeLevel,int blockid, Record troublemaker)
    {
        if (treeLevel!=overflowLevel)
        {
            overflow_first_time=true;
            overflowLevel=treeLevel;
        }
        if (treeLevel!=0 && overflow_first_time)
        {
            //System.out.println("----------BEFORE REINSERT----------");
            overflow_first_time=false;
            //FileHandler.readIndexFile();

            Split.reinsert(blockid,troublemaker);
            //System.out.println("-----------AFTER REINSERT----------");
            //FileHandler.readIndexFile();
        }
        else
        {
            //System.out.println("----------BEFORE SPLIT----------");
            //FileHandler.readIndexFile();

            //System.out.println("split");
            Split.split(blockid, troublemaker);
            //System.out.println("-----------AFTER SPLIT----------");
            //FileHandler.readIndexFile();

            overflowLevel = -1;
        }
    }

    // mass datafile insert during datafile build
    public static void datafileMassInsert(ArrayList<Record> records) {
        int blockSize = FileHandler.getBlockSizedatafile();
        int dimensions = FileHandler.getDimensions();
        int noOfDatafileBlocks = FileHandler.getNoOfDatafileBlocks();
//        data to save
        int byteCounter =0;

        byte[] blockData = new byte[blockSize];

        byte[] delimiterArray = ConversionToBytes.charToBytes(FileHandler.getDelimiter());
        byte[] blockSeparatorArray = ConversionToBytes.charToBytes(FileHandler.getBlockSeparator());

        byte[] name = null;
        ArrayList<byte []> coordsByteArrays = new ArrayList<>();
        byte[] nodeId;

        try {
            for (Record record: records) {
                coordsByteArrays.clear();
                // Adding the current byteCounter with the bytes of the incoming node in the var tempByteCounter
                int tempByteCounter = byteCounter + Long.BYTES + dimensions * Double.BYTES + delimiterArray.length + blockSeparatorArray.length;
                if (record.getName() != null){
                    name = record.getName().getBytes(StandardCharsets.UTF_8);
                    tempByteCounter += name.length;
                }

                // If tempByteCounter is greater than blockSize then the block (blockData) gets written in the file
                // blockData is instantiated again to get empty, the byteCounter resets
                // the metadata in first block get updated
                if (tempByteCounter >= blockSize){
                    System.arraycopy(blockSeparatorArray, 0, blockData, byteCounter, blockSeparatorArray.length);
//                    byteCounter += blockSeparatorArray.length;

                    RandomAccessFile file = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
                    file.seek((long) noOfDatafileBlocks * blockSize);
                    file.write(blockData);

                    noOfDatafileBlocks++;
                    FileHandler.setNoOfDatafileBlocks(FileHandler.getNoOfDatafileBlocks() + 1);
                    byteCounter = 0;
                    blockData = new byte[blockSize];
                    file.seek(8);
                    file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
                    file.close();
                }

                nodeId = ConversionToBytes.longToBytes(record.getNodeId());

                // the arrays of serialized data get copied in the block
                System.arraycopy(nodeId, 0, blockData, byteCounter, Long.BYTES);
                byteCounter += Long.BYTES;
                for (int i = 0; i < dimensions; i++) {
                    coordsByteArrays.add(ConversionToBytes.doubleToBytes(record.getCoords().get(i)));
                    System.arraycopy(coordsByteArrays.get(i), 0, blockData, byteCounter, Double.BYTES);
                    byteCounter += Double.BYTES;
                }
                if (name != null){
                    System.arraycopy(name, 0, blockData, byteCounter, name.length);
                    byteCounter += name.length;
                }
                System.arraycopy(delimiterArray, 0, blockData, byteCounter, delimiterArray.length);
                byteCounter += delimiterArray.length;

                name = null;
            }
            // write the last block
            System.arraycopy(blockSeparatorArray, 0, blockData, byteCounter, blockSeparatorArray.length);

            RandomAccessFile file = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
            file.seek((long) noOfDatafileBlocks * blockSize);
            file.write(blockData);

            noOfDatafileBlocks++;
            FileHandler.setNoOfDatafileBlocks(noOfDatafileBlocks);
            file.seek(8);
            file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //if user adds node manually to the R* Tree, also add it to the datafile.
    public static void datafileRecordInsert(Record record) {
        int blockSize = FileHandler.getBlockSizedatafile();
        int dimensions = FileHandler.getDimensions();
        int noOfDatafileBlocks = FileHandler.getNoOfDatafileBlocks();
//        data to save
        int byteCounter =0;

        byte[] dataBlock = new byte[blockSize];
        File datafile = new File(FileHandler.getDatafilePath());

        byte[] delimiterArray = ConversionToBytes.charToBytes(FileHandler.getDelimiter());
        byte[] blockSeparatorArray = ConversionToBytes.charToBytes(FileHandler.getBlockSeparator());

        char newlinestr;

        byte[] name = null;
        ArrayList<byte []> coordsByteArrays = new ArrayList<>();
        byte[] nodeId;
        try {
            byte[] bytes = Files.readAllBytes(datafile.toPath());
            System.arraycopy(bytes, (noOfDatafileBlocks - 1) * blockSize, dataBlock, 0, blockSize);
            boolean flag = true;
            while (flag){
                System.arraycopy(dataBlock, byteCounter+24, delimiterArray,0, delimiterArray.length);

                newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                int tempcounter=0;

                // count bytes until you meet the delimiter if not already met above
                while (newlinestr!=FileHandler.getDelimiter()) {
                    tempcounter+=1;
                    System.arraycopy(dataBlock, byteCounter+24+tempcounter, delimiterArray,0, delimiterArray.length);
                    newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                }

                if (tempcounter!=0) {
                    byteCounter+=26+tempcounter;
                } else {
                    byteCounter+=26;
                }

                // if datablock has the blockSeparator (#) at some point it means the end of the data read in the
                // current block
                System.arraycopy(dataBlock, byteCounter, blockSeparatorArray, 0, 2);
                if (ByteBuffer.wrap(blockSeparatorArray).getChar()==FileHandler.getBlockSeparator())
                    flag = false;

            }
            // Adding the current byteCounter with the bytes of the incoming node in the var tempByteCounter
            int tempByteCounter = byteCounter + Long.BYTES + dimensions * Double.BYTES + delimiterArray.length + blockSeparatorArray.length;
            if (record.getName() != null){
                name = record.getName().getBytes(StandardCharsets.UTF_8);
                tempByteCounter += name.length;
            }

            if (tempByteCounter >= blockSize){
//                    byteCounter += blockSeparatorArray.length;
                RandomAccessFile file = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");

                noOfDatafileBlocks++;
                FileHandler.setNoOfDatafileBlocks(FileHandler.getNoOfDatafileBlocks() + 1);
                byteCounter = 0;
                dataBlock = new byte[blockSize];
                file.seek(8);
                file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
                file.close();
            }

            tempByteCounter = byteCounter;
            nodeId = ConversionToBytes.longToBytes(record.getNodeId());

            System.arraycopy(nodeId, 0, dataBlock, byteCounter, Long.BYTES);
            byteCounter += Long.BYTES;
            for (int i = 0; i < dimensions; i++) {
                coordsByteArrays.add(ConversionToBytes.doubleToBytes(record.getCoords().get(i)));
                System.arraycopy(coordsByteArrays.get(i), 0, dataBlock, byteCounter, Double.BYTES);
                byteCounter += Double.BYTES;
            }

            if (name != null){
                System.arraycopy(name, 0, dataBlock, byteCounter, name.length);
                byteCounter += name.length;
            }
            System.arraycopy(delimiterArray, 0, dataBlock, byteCounter, delimiterArray.length);
            byteCounter += delimiterArray.length;
            System.arraycopy(blockSeparatorArray, 0, dataBlock, byteCounter, blockSeparatorArray.length);

            RandomAccessFile file1 = new RandomAccessFile(FileHandler.getDatafilePath(), "rw");
            file1.seek((long) (noOfDatafileBlocks - 1) * blockSize);
            file1.write(dataBlock);
            file1.close();


            FileHandler.setRecords(FileHandler.getDatafileRecords());
            FileHandler.setRoot(0);
            if (name == null) {
                insert( new Record(
                        record.getCoords().get(0),
                        record.getCoords().get(1),
                        noOfDatafileBlocks,
                        tempByteCounter,
                        FileHandler.getDatafileRecords().size() - 1,
                        record.getNodeId()
                ));

            }
            else {
                insert( new Record(
                        record.getCoords().get(0),
                        record.getCoords().get(1),
                        noOfDatafileBlocks,
                        tempByteCounter,
                        FileHandler.getDatafileRecords().size() - 1,
                        record.getName(),
                        record.getNodeId()
                ));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
