import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private static final char blockSeparator = '#';
    private static boolean bottomUp = false;
    private static BottomUp btm= null;
    private static int blockSize = 512; //32KB (KB=1024B) // 512 | 32768
    private static final int blockSizedatafile = 32768;
    private static ArrayList<Record> records = new ArrayList<>();
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
            byte[] dimensionArray = ConversionToBytes.intToBytes(dimensions);
            byte[] blocksizeArray = ConversionToBytes.intToBytes(blockSizedatafile);
            byte[] noOfBlocksArray = ConversionToBytes.intToBytes(noOfDatafileBlocks);
            byte[] blockData = new byte[blockSizedatafile];
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


    static void retrieveOldFileInfo(){
        try{
            File file = new File(DatafilePath);
            //byte arrays to save serialized data from datafile inorder to deserialize them afterwards and print them
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] dimensionArray = new byte[4];
            byte[] noOfBlocksArray = new byte[4];

            System.arraycopy(bytes, 0, dimensionArray, 0, dimensionArray.length);
            System.arraycopy(bytes, 8, noOfBlocksArray, 0, noOfBlocksArray.length);

            dimensions = ByteBuffer.wrap(dimensionArray).getInt();
            noOfDatafileBlocks = ByteBuffer.wrap(noOfBlocksArray).getInt();

            file = new File(IndexfilePath);
            //byte arrays to save serialized data from indexfile inorder to deserialize them afterwards and print them
            bytes = Files.readAllBytes(file.toPath());
            noOfBlocksArray = new byte[4];
            byte[] leafLevelArray = new byte[4];

            System.arraycopy(bytes, 4, noOfBlocksArray, 0, noOfBlocksArray.length);
            System.arraycopy(bytes, 8, leafLevelArray, 0, leafLevelArray.length);

            noOfIndexfileBlocks = ByteBuffer.wrap(noOfBlocksArray).getInt();
            leafLevel = ByteBuffer.wrap(leafLevelArray).getInt();

            records = getDatafileRecords();
            rootMBR = new double[(int)Math.pow(2,dimensions)][dimensions];
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
            long nodeId;
            String name;
            ArrayList<Record> recordsToInsert = new ArrayList<>();
            long noOfNodes = doc.getElementsByTagName("node").getLength();

            for (int i = 0; i < noOfNodes; i++) {
                name = "";
                ArrayList<Double> coords = new ArrayList<>();
                block = doc.getElementsByTagName("node").item(i);
                //get its attributes
                NamedNodeMap attrList = block.getAttributes();
                nodeId = Long.parseLong(attrList.getNamedItem("id").getNodeValue());
                coords.add(Double.parseDouble(attrList.getNamedItem("lat").getNodeValue()));
                coords.add(Double.parseDouble(attrList.getNamedItem("lon").getNodeValue()));
                if (block.getChildNodes().getLength() > 0)
                {
                    NodeList children = block.getChildNodes();

                    //for every child
                    for (int j=1; j < children.getLength(); j+=2)
                    {
                        //get its attributes and check if there is one called k with the value name
                        if (children.item(j).getAttributes().getNamedItem("k").getNodeValue().equals("name:en"))
                        {
                            //if there is, save the value of attribute v as the name
                            name = children.item(j).getAttributes().getNamedItem("v").getNodeValue();
                            break;
                        }
                    }
                }
                recordsToInsert.add(new Record(coords, nodeId, name));
            }
            Insert.datafileMassInsert(recordsToInsert);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //saves data file records to arraylist. (used to insert and after that, read index file by matching entries using id)
    static ArrayList<Record> getDatafileRecords() {
        ArrayList<Record> datafileRecords = new ArrayList<>();
        try{
            File file = new File(DatafilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            // byte arrays to store the serialized data from datafile
            byte[] delimiterArray = new byte[2];
            byte[] blockSeparatorArray = new byte[2];
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
                byte[] dataBlock = new byte[blockSizedatafile];
                System.arraycopy(bytes, i * blockSizedatafile, dataBlock, 0, blockSizedatafile);

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

                    Record record;
                    if (tempcounter!=0) {
                        record = new Record(tempLat, tempLon, i, bytecounter, datafileRecords.size(), tempName, tempNodeId);
                        bytecounter+=26+tempcounter;
                    } else {
                        record = new Record(tempLat, tempLon, i, bytecounter, datafileRecords.size(), tempNodeId);
                        bytecounter+=26;
                    }
                    datafileRecords.add(record);

                    // if datablock has the blockSeparator (#) at some point it means the end of the data read in the
                    // current block
                    System.arraycopy(dataBlock, bytecounter, blockSeparatorArray, 0, 2);
                    if (ByteBuffer.wrap(blockSeparatorArray).getChar()==blockSeparator)
                        flag = false;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return datafileRecords;
    }


    //point by point or bottom-up
    static void createIndexFile(boolean pbp){
        FileHandler.createFirstIndexfileBlock();
        records = new ArrayList<>(getDatafileRecords());
        if (pbp)
        {
            rootMBR = new double[(int)Math.pow(2,dimensions)][dimensions];
            FileHandler.insertIndexfileNodes();

        }
    }
    //metadata for index file
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


    private static void insertIndexfileNodes(){
        int counter = 0;
        for (Record record : records) {
            Insert.insert(record);
            counter++;
            if (counter==10000)
                break;
        }
    }


    public static void readIndexFile() {
        try {
            Queue<Integer> pointers = new LinkedList<>();
            BufferedWriter writer = new BufferedWriter(new FileWriter("treeOutput.txt"));

            if (FileHandler.getNoOfIndexfileBlocks() >= 1) {
                pointers.add(1);
                int blockId, level;

                while (!pointers.isEmpty()) {
                    blockId = pointers.peek();
                    level = getMetaDataOfRectangle(blockId).get(0);
                    if (level != leafLevel){
                        ArrayList<Rectangle> rectangles = getRectangleEntries(blockId);

                        writer.write("Block No: " + blockId +
                                ", Level: " + level +
                                ", No of rectangles: " + rectangles.size() +
                                ", Leaf level: " + leafLevel +
                                ", Parent block id: " + getMetaDataOfRectangle(blockId).get(2) +
                                "\nRecords: \n");


                        for (Rectangle rectangle: rectangles) {
                            writer.write(
                                    "LAT: " + rectangle.getCoordinates().get(0) +
                                            ", " + rectangle.getCoordinates().get(dimensions) +
                                            ", LON: " + rectangle.getCoordinates().get(1) +
                                            ", " + rectangle.getCoordinates().get(1 + dimensions) + "\n"
                            );
                            pointers.add(rectangle.getChildPointer());
                        }
                    } else {
                        ArrayList<Record> records = getRecords(blockId);

                        writer.write("Block No: " + blockId +
                                ", Level: " + level +
                                ", No of entries: " + records.size() +
                                ", Parent block id: " + getMetaDataOfRectangle(blockId).get(2) +
                                "\nRecords:" + "\n");

                        for (Record record: records) {
                            writer.write("LAT: " + record.getLAT() +
                                    ", LON: " + record.getLON() +
                                    ", Datafile block: " + record.getRecordLocation().getBlock() +
                                    ", Block slot: " + record.getRecordLocation().getSlot());

                            if (record.getName() != null && !record.getName().equals("")) {
                                writer.write(", Name: " + record.getName());
                            }

                            if (record.getNodeId() != 0) {
                                writer.write(", Node ID: " + record.getId());
                            }
                            writer.write("\n");

                        }
                    }
                    writer.write("\n");
                    pointers.remove();
                }
                writer.close();
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

    //used for read index file
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

    //used by readindexfile
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

                List<Double> coordinates = List.of(
                        ByteBuffer.wrap(minLAT).getDouble(),
                        ByteBuffer.wrap(minLON).getDouble(),
                        ByteBuffer.wrap(maxLAT).getDouble(),
                        ByteBuffer.wrap(maxLON).getDouble()
                );

                Rectangle rectangle = new Rectangle(
                        new ArrayList<>(coordinates),
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
    public static int getBlockSizedatafile() {
        return blockSizedatafile;
    }
    public static void setBlockSize(int newblockSize) {blockSize=newblockSize;}


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
    public static void setRootMBR(double[][] rtmbr){rootMBR=rtmbr;}

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

    public static ArrayList<Record> getRecords()
    {
        return records;
    }

    public static Queue<Integer> getEmptyBlocks() {
        return emptyBlocks;
    }

    public static void setEmptyBlocks(Queue<Integer> emptyBlocks) {
        FileHandler.emptyBlocks = emptyBlocks;
    }

    public static void setDimensions(int dimensions) {
        FileHandler.dimensions = dimensions;
    }

    public static char getDelimiter() {
        return delimiter;
    }

    public static char getBlockSeparator() {
        return blockSeparator;
    }

    public static void setBottomUp(boolean bottomUp) {
        FileHandler.bottomUp = bottomUp;
    }

    public static boolean isBottomUp() {
        return bottomUp;
    }

    public static int getNoOfDatafileBlocks() {
        return noOfDatafileBlocks;
    }

    public static void setNoOfDatafileBlocks(int noOfDatafileBlocks) {
        FileHandler.noOfDatafileBlocks = noOfDatafileBlocks;
    }

    public static void setRecords(ArrayList<Record> records) {
        FileHandler.records = records;
    }

    static void setBtm(BottomUp a)
    {
        btm=a;
    }

    static BottomUp getBtm(){return btm;}
}