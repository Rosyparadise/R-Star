import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class FileHandler {
    private static int root=-1;
    private static int noOfDatafileBlocks = 0;
    private static int noOfIndexfileBlocks = 0;
    private static int leafLevel = -1;
    private static final String OsmfilePath = "map2.osm";
    private static final String DatafilePath = "datafile.dat";
    private static final String IndexfilePath = "indexfile.dat";
    private static int dimensions; //2 for testing
    private static double[][] rootMBR;
    private static final char delimiter = '$';
    private static final char blockSeperator = '#';
    private static final int blockSize = 512; //32KB (KB=1024B) // 512 | 32768
    private static final ArrayList<Record> records = new ArrayList<>();
    private static Queue<Integer> emptyBlocks = new LinkedList<>();

    static void createDataFile(int dimensions){
        if (dimensions >= 2){
            FileHandler.dimensions = dimensions;
            FileHandler.createFirstDatafileBlock();
            FileHandler.insertDatafileNodes();
        } else {
            System.out.println("Dimension number should be at least 2");
            System.exit(0);
        }
    }

    private static void createFirstDatafileBlock(){
        try {
            FileHandler.noOfDatafileBlocks++;
            //+noOfnodesInDatafile?
            byte[] dimensionArray = ConversionToBytes.intToBytes(dimensions);
            byte[] blocksizeArray = ConversionToBytes.intToBytes(blockSize);
            byte[] noOfBlocksArray = ConversionToBytes.intToBytes(noOfDatafileBlocks);
            byte[] blockData = new byte[blockSize];
            //bytecounter for blockData
            int bytecounter = 0;

            // Copies dimensionArray in blockData starting from bytecounter(0) then increments by
            // dimensionArray size. Copies blocksizeArray starting from bytecounter(dimensionArray.length) then increments
            // by blocksizeArray size etc.
            System.arraycopy(dimensionArray, 0, blockData, bytecounter, dimensionArray.length);
            bytecounter += dimensionArray.length;
            System.arraycopy(blocksizeArray, 0, blockData, bytecounter, blocksizeArray.length);
            bytecounter += blocksizeArray.length;
            System.arraycopy(noOfBlocksArray, 0, blockData, bytecounter, noOfBlocksArray.length);

            RandomAccessFile file = new RandomAccessFile(DatafilePath, "rw");
            file.write(blockData);
            file.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void readFirstDatafileBlock(){
        try{
            File file = new File(DatafilePath);
            //byte arrays to save serialized data from datafile inorder to deserialize them afterwards and print them
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dimensionArray = new byte[4];
            byte[] blocksizeArray = new byte[4];
            byte[] noOfBlocksArray = new byte[4];

            System.arraycopy(bytes, 0, dimensionArray, 0, dimensionArray.length);
            System.arraycopy(bytes, 4, blocksizeArray, 0, blocksizeArray.length);
            System.arraycopy(bytes, 8, noOfBlocksArray, 0, noOfBlocksArray.length);

            int tempDimension = ByteBuffer.wrap(dimensionArray).getInt();
            int tempBlockSize = ByteBuffer.wrap(blocksizeArray).getInt();
            int tempnoOfBlocks = ByteBuffer.wrap(noOfBlocksArray).getInt();
            System.out.println("Dimensions: " + tempDimension + "\nBlock size: " + tempBlockSize + "\nNumber of blocks: " + tempnoOfBlocks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertDatafileNodes(){
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new File(OsmfilePath));
            //get rid of white spaces
            doc.getDocumentElement().normalize();

            //node block represents the <node> we are currently processing
            Node block;
            //noOfNodes is the number of <nodes> in the .osm file
            long noOfNodes = doc.getElementsByTagName("node").getLength();

            //data to save
            int bytecounter=0;

            //where the data will be saved after being converted into byte arrays
            byte[] b_id, b_lat, b_lon, b_name=null;
            byte[] delimiterArray = ConversionToBytes.charToBytes(FileHandler.delimiter);
            byte[] blockSeparatorArray = ConversionToBytes.charToBytes(FileHandler.blockSeperator);
            byte[] blockData = new byte[blockSize];

            for (int i = 0; i < noOfNodes; i++)
            {

                block = doc.getElementsByTagName("node").item(i);
                //get its attributes
                NamedNodeMap attrList = block.getAttributes();

                //and into byte[] form
                b_id = ConversionToBytes.longToBytes(Long.parseLong(attrList.getNamedItem("id").getNodeValue()));
                b_lat = ConversionToBytes.doubleToBytes(Double.parseDouble(attrList.getNamedItem("lat").getNodeValue()));
                b_lon = ConversionToBytes.doubleToBytes(Double.parseDouble(attrList.getNamedItem("lon").getNodeValue()));

                //if <node> has children>
                if (block.getChildNodes().getLength() > 0)
                {
                    NodeList children = block.getChildNodes();

                    //for every child
                    for (int j=1; j < children.getLength(); j+=2)
                    {
                        //get its attributes and check if there is one called k with the value name
                        if (children.item(j).getAttributes().getNamedItem("k").getNodeValue().equals("name"))
                        {
                            //if there is, save the value of attribute v as the name
                            b_name = children.item(j).getAttributes().getNamedItem("v").getNodeValue().getBytes(StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                // Adding the current bytecounter with the bytes of the incoming node in the var tempbytecounter
                int tempbytecounter = bytecounter + b_id.length + b_lat.length + b_lon.length + delimiterArray.length + blockSeparatorArray.length;
                if (b_name != null){
                    tempbytecounter += b_name.length;
                }

                // If tempbytecounter is greater than blockSize then the block (blockData) gets written in the file
                // blockData is instantiated again to get empty, the bytecounter resets
                // the metadata in first block get updated
                if (tempbytecounter >= blockSize){
                    System.arraycopy(blockSeparatorArray, 0, blockData, bytecounter, blockSeparatorArray.length);
//                    bytecounter += blockSeparatorArray.length;

                    RandomAccessFile file = new RandomAccessFile(DatafilePath, "rw");
                    file.seek((long) noOfDatafileBlocks * blockSize);
                    file.write(blockData);

                    noOfDatafileBlocks++;
                    bytecounter = 0;
                    blockData = new byte[blockSize];
                    file.seek(8);
                    file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
                    file.close();
                }

                // the arrays of serialized data get copied in the block
                System.arraycopy(b_id, 0, blockData, bytecounter, b_id.length);
                bytecounter += b_id.length;
                System.arraycopy(b_lat, 0, blockData, bytecounter, b_lat.length);
                bytecounter += b_lat.length;
                System.arraycopy(b_lon, 0, blockData, bytecounter, b_lon.length);
                bytecounter += b_lon.length;
                if (b_name != null){
                    System.arraycopy(b_name, 0, blockData, bytecounter, b_name.length);
                    bytecounter += b_name.length;
                }
                System.arraycopy(delimiterArray, 0, blockData, bytecounter, delimiterArray.length);
                bytecounter += delimiterArray.length;

                b_name=null;
            }
            // write the last block
            System.arraycopy(blockSeparatorArray, 0, blockData, bytecounter, blockSeparatorArray.length);

            RandomAccessFile file = new RandomAccessFile(DatafilePath, "rw");
            file.seek((long) noOfDatafileBlocks * blockSize);
            file.write(blockData);

            noOfDatafileBlocks++;
            file.seek(8);
            file.write(ConversionToBytes.intToBytes(noOfDatafileBlocks));
            file.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void readDatafile(){
        try{
            File file = new File(DatafilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            // byte arrays to store the serialized data from datafile
            byte[] delimiterArray = new byte[2];
            byte[] blockSeperatorArray = new byte[2];
            byte[] NodeIdArray = new byte[8];
            byte[] LatArray = new byte[8];
            byte[] LonArray = new byte[8];

            // variable to store the deserialized data
            long tempNodeId;
            double tempLat,tempLon;
            String tempName="";
            char newlinestr;

            // for each block after the first one copy from the bytes array which contains all the bytes of the datafile
            // into the dataBlock
            for (int i=1; i < noOfDatafileBlocks; i++){
                byte[] dataBlock = new byte[blockSize];
                System.arraycopy(bytes, i * blockSize, dataBlock, 0, blockSize);

                int bytecounter = 0;
                boolean flag = true;

                // for every node inside the dataBlock copy into the appropriate array, deserialize and print the result
                while (flag){
                    System.arraycopy(dataBlock, bytecounter, NodeIdArray, 0, NodeIdArray.length);
                    System.arraycopy(dataBlock, bytecounter+8, LatArray, 0, LatArray.length);
                    System.arraycopy(dataBlock, bytecounter+16, LonArray, 0, LonArray.length);
                    System.arraycopy(dataBlock, bytecounter+24, delimiterArray,0, delimiterArray.length);

                    tempNodeId = ByteBuffer.wrap(NodeIdArray).getLong();
                    tempLat = ByteBuffer.wrap(LatArray).getDouble();
                    tempLon = ByteBuffer.wrap(LonArray).getDouble();
                    newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                    int tempcounter=0;

                    // count bytes until you meet the delimiter if not already met above
                    while (newlinestr!=delimiter)
                    {
                        tempcounter+=1;
                        System.arraycopy(dataBlock, bytecounter+24+tempcounter, delimiterArray,0, delimiterArray.length);
                        newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                    }

                    // read name if there is one
                    if (tempcounter!=0) {
                        byte[] nameArray = new byte[tempcounter];
                        System.arraycopy(dataBlock, bytecounter+24, nameArray,0, tempcounter);
                        tempName = new String(nameArray);
                    }

                    System.out.println("Node id: " + tempNodeId);
                    System.out.println("LAT: " + tempLat);
                    System.out.println("LON: " + tempLon);

                    if (tempcounter!=0) {
                        System.out.println("Name: "+ tempName);
                        bytecounter+=26+tempcounter;
                    } else {
                        bytecounter+=26;
                    }
                    System.out.println();

                    // if datablock has the blockSeparator (#) at some point it means the end of the data read in the
                    // current block
                    System.arraycopy(dataBlock, bytecounter, blockSeperatorArray, 0, 2);
                    if (ByteBuffer.wrap(blockSeperatorArray).getChar()==blockSeperator)
                        flag = false;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void createIndexFile(){
        rootMBR = new double[(int)Math.pow(2,dimensions)][dimensions];
        FileHandler.createFirstIndexfileBlock();
        FileHandler.insertIndexfileNodes();
    }

    private static void createFirstIndexfileBlock(){
        try {
            byte[] blocksizeArray = ConversionToBytes.intToBytes(blockSize);
            byte[] noOfBlocksArray = ConversionToBytes.intToBytes(noOfIndexfileBlocks);
            byte[] leafLevelArray = ConversionToBytes.intToBytes(leafLevel);
            byte[] blockData = new byte[blockSize];
            //bytecounter for blockData
            int bytecounter = 0;

            // Copies blocksizeArray in blockData starting from bytecounter(0) then increments by
            // blocksizeArray size. Copies noOfBlocksArray starting from bytecounter(dimensionArray.length) then increments
            // by noOfBlocksArray size etc.
            System.arraycopy(blocksizeArray, 0, blockData, bytecounter, blocksizeArray.length);
            bytecounter += blocksizeArray.length;
            System.arraycopy(noOfBlocksArray, 0, blockData, bytecounter, noOfBlocksArray.length);
            bytecounter += noOfBlocksArray.length;
            System.arraycopy(leafLevelArray, 0, blockData, bytecounter, leafLevelArray.length);

            RandomAccessFile file = new RandomAccessFile(IndexfilePath, "rw");

            file.write(blockData);

            file.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void readFirstIndexfileBlock(){
        try{
            File file = new File(IndexfilePath);
            //byte arrays to save serialized data from indexfile inorder to deserialize them afterwards and print them
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] blocksizeArray = new byte[4];
            byte[] noOfBlocksArray = new byte[4];
            byte[] leafLevelArray = new byte[4];

            System.arraycopy(bytes, 0, blocksizeArray, 0, blocksizeArray.length);
            System.arraycopy(bytes, 4, noOfBlocksArray, 0, noOfBlocksArray.length);
            System.arraycopy(bytes, 8, leafLevelArray, 0, leafLevelArray.length);


            int tempBlockSize = ByteBuffer.wrap(blocksizeArray).getInt();
            int tempnoOfBlocks = ByteBuffer.wrap(noOfBlocksArray).getInt();
            int tempLeafLevel = ByteBuffer.wrap(leafLevelArray).getInt();

            System.out.println("Block size: " + tempBlockSize + "\nNumber of blocks: " + tempnoOfBlocks + "\nLeaf level: " + tempLeafLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void insertIndexfileNodes(){
        // read datafile and add every node in the arraylist of Records. (Read is mainly copied by readDatafile()
        // function, so it will get cleaned up at some point and its just a temp solution)
        try {
            File file = new File(DatafilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            // byte arrays to store the serialized data from datafile
            byte[] delimiterArray = new byte[2];
            byte[] blockSeperatorArray = new byte[2];
            byte[] LatArray = new byte[8];
            byte[] LonArray = new byte[8];

            double tempLat,tempLon;
            char newlinestr;

            // save all nodes from datafile in the records arraylist
            for (int i=1; i < noOfDatafileBlocks; i++){
                byte[] dataBlock = new byte[blockSize];
                System.arraycopy(bytes, i * blockSize, dataBlock, 0, blockSize);

                int bytecounter = 0;
                boolean flag = true;

                while (flag){
                    System.arraycopy(dataBlock, bytecounter+8, LatArray, 0, LatArray.length);
                    System.arraycopy(dataBlock, bytecounter+16, LonArray, 0, LonArray.length);
                    System.arraycopy(dataBlock, bytecounter+24, delimiterArray,0, delimiterArray.length);

                    tempLat = ByteBuffer.wrap(LatArray).getDouble();
                    tempLon = ByteBuffer.wrap(LonArray).getDouble();
                    newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                    int tempcounter=0;

                    while (newlinestr!=delimiter)
                    {
                        tempcounter+=1;
                        System.arraycopy(dataBlock, bytecounter+24+tempcounter, delimiterArray,0, delimiterArray.length);
                        newlinestr = ByteBuffer.wrap(delimiterArray).getChar();
                    }

                    // new record and addition to the arraylist
                    Record record = new Record(tempLat, tempLon, i, bytecounter, records.size()); //id?
                    records.add(record);

                    if (tempcounter!=0) {
                        bytecounter+=26+tempcounter;
                    } else {
                        bytecounter+=26;
                    }

                    System.arraycopy(dataBlock, bytecounter, blockSeperatorArray, 0, 2);
                    if (ByteBuffer.wrap(blockSeperatorArray).getChar()==blockSeperator) {
                        flag = false;
                    }
                }
            }

            // iterate the arraylist of Records and insert each node to the r* tree using the Insert method
            int counter = 0;
            for (Record record : records) {
                Insert.insert(leafLevel, record);
                counter++;
                //1639 to cause first split
                //2398 to cause first reinsert
                //2899 first reinsert for map2.osm
                //3179 first split after reinsert
                if (counter == 246)
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void readIndexFile() {
        try {
            Queue<Integer> pointers = new LinkedList<>();

            if (FileHandler.getNoOfIndexfileBlocks() > 1) {
                pointers.add(1);
                int blockId, level;

                while (!pointers.isEmpty()) {
                    blockId = pointers.peek();
                    level = getMetaDataOfRectangle(blockId).get(0);

                    if (level != leafLevel){
                        ArrayList<Rectangle> rectangles = getRectangleEntries(blockId);

                        System.out.println("Block No: " + blockId +
                                ", Level: " + level +
                                ", No of rectangles: " + rectangles.size() +
                                ", Leaf level: " + leafLevel +
                                ", Parent block id: " + getMetaDataOfRectangle(blockId).get(2) +
                                "\nRecords: ");


                        for (Rectangle rectangle: rectangles) {
                            System.out.println(
                                    "LAT: " + rectangle.getMinLAT() +
                                            ", " + rectangle.getMaxLAT() +
                                            ", LON: " + rectangle.getMinLON() +
                                            ", " + rectangle.getMaxLON()
                            );
                            pointers.add(rectangle.getChildPointer());
                        }
                    } else {
                        ArrayList<Record> records = getRecords(blockId);

                        System.out.println("Block No: " + blockId +
                                ", Level: " + level +
                                ", No of entries: " + records.size() +
                                ", Parent block id: " + getMetaDataOfRectangle(blockId).get(2) +
                                "\nRecords: ");

                        for (Record record: records) {
                            System.out.println("LAT: " + record.getLAT() +
                                    ", LON: " + record.getLON() +
                                    ", ID:" + record.getId() +
                                    ", Datafile block: " + record.getRecordLocation().getBlock() +
                                    ", Block slot: " + record.getRecordLocation().getSlot());
                        }
                    }
                    System.out.println();
                    pointers.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int calculateMaxBlockRectangles(){
        // Save in metadataSize the size of the number of rectangles (Integer), tree level (Integer) and parent pointer
        // (Integer)
        int metadataSize = 3 * Integer.BYTES;

        // Increment rectangleInfoSize by the size of the LAT (double) and LON (double) for each dimension plus the
        // childpointer (Integer)
        int rectangleInfoSize = 2 * dimensions * Double.BYTES + Integer.BYTES;

        // Return the number of rectangles that a block can have, which is total block size minus the size of metadata
        // minus the left childpointer of the first rectangle (the first rectangle is the only one with two childpointers)
        // and all mod with rectangleInfoSize
        return (blockSize - metadataSize) / rectangleInfoSize;
    }

    public static int calculateMaxBlockNodes(){
        // Save in metadataSize the size of the number of nodes (Integer), level (Integer) and parent pointer (Integer)
        int metadataSize = 3 * Integer.BYTES;

        // Increment nodeInfoSize by the size of the LAT (double) and LON (double) of the node plus the record id
        // (Integer) of the node in the record arraylist, that can be used to get the location (block, byte) of the node
        // in datafile.dat
        int nodeInfoSize = 2 * Double.BYTES + Integer.BYTES;

        // Return the number of rectangles that a block can have, which is total block size minus the size of metadata
        // minus the left childpointer of the first rectangle (the first rectangle is the only one with two childpointers)
        // and all mod with rectangleInfoSize
        return (blockSize - metadataSize) / nodeInfoSize;
    }

    public static ArrayList<Integer> getMetaDataOfRectangle(int id) {
        ArrayList<Integer> metadata = new ArrayList<>();
        byte[] levelArray = new byte[Integer.BYTES];
        byte[] noOfEntries = new byte[Integer.BYTES];
        byte[] parentPointer = new byte[Integer.BYTES];

        try {
            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());

            System.arraycopy(bytes, id * blockSize, levelArray, 0, Integer.BYTES);
            System.arraycopy(bytes, id * blockSize + Integer.BYTES, noOfEntries, 0, Integer.BYTES);
            System.arraycopy(bytes, id * blockSize + 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

            metadata.add(ByteBuffer.wrap(levelArray).getInt());
            metadata.add(ByteBuffer.wrap(noOfEntries).getInt());
            metadata.add(ByteBuffer.wrap(parentPointer).getInt());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return metadata;
    }

    public static ArrayList<Rectangle> getRectangleEntries(int id) {
        ArrayList<Rectangle> rectangles = new ArrayList<>();

        try {
            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());

            byte[] block = new byte[blockSize];
            System.arraycopy(bytes, id * blockSize, block, 0, blockSize);

            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();

            byte[] minLAT = new byte[Double.BYTES];
            byte[] minLON = new byte[Double.BYTES];
            byte[] maxLAT = new byte[Double.BYTES];
            byte[] maxLON = new byte[Double.BYTES];
            byte[] childPointer = new byte[Integer.BYTES];

            int byteCounter = 3 * Integer.BYTES;

            for (int i = 0; i < tempCurrentNoOfEntries; i++) {
                System.arraycopy(block, byteCounter, minLAT, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + Double.BYTES, minLON, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + 2 * Double.BYTES, maxLAT, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + 3 * Double.BYTES, maxLON, 0, Double.BYTES);
                System.arraycopy(block, byteCounter + 4 * Double.BYTES, childPointer, 0, Integer.BYTES);

                Rectangle rectangle = new Rectangle(
                        ByteBuffer.wrap(minLAT).getDouble(),
                        ByteBuffer.wrap(minLON).getDouble(),
                        ByteBuffer.wrap(maxLAT).getDouble(),
                        ByteBuffer.wrap(maxLON).getDouble(),
                        ByteBuffer.wrap(childPointer).getInt()
                );

                rectangles.add(rectangle);

                byteCounter += 4 * Double.BYTES + Integer.BYTES;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return rectangles;
    }

    public static ArrayList<Record> getRecords(int id) {
        ArrayList<Record> result = new ArrayList<>();

        try {
            File file = new File(FileHandler.getIndexfilePath());
            byte[] bytes = Files.readAllBytes(file.toPath());

            byte[] block = new byte[blockSize];
            System.arraycopy(bytes, id * blockSize, block, 0, blockSize);

            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();

            byte[] recordId = new byte[Integer.BYTES];

            int byteCounter = 3 * Integer.BYTES;

            for (int i = 0; i < tempCurrentNoOfEntries; i++) {
                System.arraycopy(block, byteCounter + 2 * Double.BYTES, recordId, 0, Integer.BYTES);

                result.add(records.get(ByteBuffer.wrap(recordId).getInt()));

                byteCounter += 2 * Double.BYTES + Integer.BYTES;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getIndexfilePath() {
        return IndexfilePath;
    }

    public static String getDatafilePath() {
        return DatafilePath;
    }

    public static int getBlockSize() {
        return blockSize;
    }

    public static int getLeafLevel() {
        return leafLevel;
    }

    public static double[][] getRootMBR() {
        return rootMBR;
    }

    public static int getRoot() {
        return root;
    }

    public static void setRoot(int root) {
        FileHandler.root = root;
    }

    public static int getDimensions() {
        return dimensions;
    }

    public static int getNoOfIndexfileBlocks() {
        return noOfIndexfileBlocks;
    }
    public static void setNoOfIndexfileBlocks(int noOfIndexfileBlocks) {
        FileHandler.noOfIndexfileBlocks = noOfIndexfileBlocks;
    }

    public static void setLeafLevel(int leafLevel) {
        FileHandler.leafLevel = leafLevel;
    }

    public static Record getRecord(int id) {
        return records.get(id);
    }

    public static Queue<Integer> getEmptyBlocks() {
        return emptyBlocks;
    }

    public static void setEmptyBlocks(Queue<Integer> emptyBlocks) {
        FileHandler.emptyBlocks = emptyBlocks;
    }
}