import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class BottomUp {

    Queue<Record> subset_recs;
    ArrayList<Record> records;
    int leaflevel;
    int blockID;


    public BottomUp()
    {
        records=FileHandler.getRecords();
        subset_recs = new LinkedList<>();
        leaflevel=0;
        blockID = 1;

    }

    public void construct()
    {
        double S=0;
        for (Record record : records) {
            subset_recs.add(record.copyRecord());
            if (record.getLAT() > S)
                S = record.getLAT();
            if (record.getLON() > S)
                S = record.getLON();
        }


        HilbertSort sort = new HilbertSort(subset_recs, S);
        Queue<Record> sortedList = sort.hilbertHelper();

        ArrayList<Integer> IDs = new ArrayList<>();

        while (!sortedList.isEmpty())
            IDs.add(sortedList.remove().getId());


        try
        {
            RandomAccessFile indexfile = new RandomAccessFile("indexfileBU.dat", "rw");
            int max_records=FileHandler.calculateMaxBlockNodes();
            int blockSize = FileHandler.getBlockSize();
            leaflevel = getLevelsofTree(IDs.size());


            int iterations =(int) Math.ceil((double) IDs.size()/max_records);
            double[][][] MBRs = new double[iterations][4][2];
            int[] MBRs_ID = new int[iterations];


            ArrayList<Integer> leaf_sizes = new ArrayList<>();
            for (int i=0;i<iterations;i++)
            {
                if (i==iterations-1)
                {
                    if(IDs.size()-(i*max_records)<Math.floor(max_records*Split.getM()))
                    {
                        int need = (int) (Math.floor(max_records*Split.getM()) - (IDs.size()-(i*max_records)));
                        leaf_sizes.add((int) Math.floor(max_records*Split.getM()));
                        leaf_sizes.set(i-1,leaf_sizes.get(i-1)-need);
                    }
                    else
                        leaf_sizes.add(IDs.size()-(i*max_records));
                }
                else
                    leaf_sizes.add(max_records);
            }



            for (int k=0;k<iterations;k++) {

                byte[] block = new byte[blockSize];

                System.arraycopy(ConversionToBytes.intToBytes(leaflevel), 0, block, 0, Integer.BYTES);
                System.arraycopy(ConversionToBytes.intToBytes(leaf_sizes.get(k)), 0, block, Integer.BYTES, Integer.BYTES);

                int counter = 3 * Integer.BYTES;

                for (int i = 0; i < leaf_sizes.get(k); i++) {
                    Record temp = records.get(IDs.remove(0));
                    System.arraycopy(ConversionToBytes.doubleToBytes(temp.getLAT()), 0, block, counter, Double.BYTES);
                    counter += Double.BYTES;
                    System.arraycopy(ConversionToBytes.doubleToBytes(temp.getLON()), 0, block, counter, Double.BYTES);
                    counter += Double.BYTES;
                    System.arraycopy(ConversionToBytes.intToBytes(temp.getId()), 0, block, counter, Integer.BYTES);
                    counter += Integer.BYTES;
                    Split.calculateMBRpointbypoint(MBRs[k], temp, i == 0, false);
                }
                MBRs_ID[k]=blockID;

                try {
                    indexfile.seek(blockID * blockSize);
                    indexfile.write(block);
                    blockID++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            int max_rectangles=FileHandler.calculateMaxBlockRectangles();
            iterations =(int) Math.ceil((double) MBRs.length/max_rectangles);
            ArrayList<Integer> nonleaf_sizes;
            double[][][] newMBR;
            int[] newMBR_ID;


                while (iterations>1)
                {
                    nonleaf_sizes = new ArrayList<>();
                    leaflevel--;

                    newMBR = new double[iterations][][];
                    newMBR_ID = new int[iterations];


                    for (int i=0;i<iterations;i++)
                    {
                        if (i==iterations-1)
                        {
                            if(MBRs.length-(i*max_rectangles)<Math.floor(max_rectangles*Split.getM()))
                            {
                                int need = (int) (Math.floor(max_rectangles*Split.getM()) - (MBRs.length-(i*max_rectangles)));
                                nonleaf_sizes.add((int) Math.floor(max_rectangles*Split.getM()));
                                nonleaf_sizes.set(i-1,nonleaf_sizes.get(i-1)-need);
                            }
                            else
                                nonleaf_sizes.add(MBRs.length-(i*max_rectangles));
                        }
                        else
                            nonleaf_sizes.add(max_rectangles);
                    }

                    int MBR_ID_counter=0;
                    for (int z=0;z<iterations;z++) {


                        byte[] block = new byte[blockSize];
                        System.arraycopy(ConversionToBytes.intToBytes(leaflevel), 0, block, 0, Integer.BYTES);
                        System.arraycopy(ConversionToBytes.intToBytes(nonleaf_sizes.get(z)), 0, block, Integer.BYTES, Integer.BYTES);

                        int counter = 3 * Integer.BYTES;
                        ArrayList<Double[][]> tempmbr = new ArrayList<>();
                        for (int i=0;i<nonleaf_sizes.get(z);i++) {
                            Double[][] temp = Split.rectangle_to_points(MBRs[MBR_ID_counter]);

                            try {

                                indexfile.seek(MBRs_ID[MBR_ID_counter]*blockSize+2*Integer.BYTES);
                                indexfile.write(ConversionToBytes.intToBytes(blockID));


                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            MBR_ID_counter++;


                            for (int j=0;j<FileHandler.getDimensions();j++)
                            {
                                for (int k=0;k<FileHandler.getDimensions();k++)
                                {
                                    System.arraycopy(ConversionToBytes.doubleToBytes(temp[j][k]), 0, block, counter, Double.BYTES);
                                    counter+= Double.BYTES;
                                }
                            }
                            tempmbr.add(temp);
                            System.arraycopy(ConversionToBytes.intToBytes(MBRs_ID[i]), 0, block, counter, Integer.BYTES);

                            counter+=Integer.BYTES;
                        }

                        newMBR[z]=Split.calculateMBROfRectangles(tempmbr);
                        newMBR_ID[z]=blockID;

                        try {
                            indexfile.seek(blockID * blockSize);
                            indexfile.write(block);
                            blockID++;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    MBRs=newMBR;
                    MBRs_ID=newMBR_ID;

                    iterations =(int) Math.ceil((double) MBRs.length/max_rectangles);
                }

            leaflevel--;
            byte[] block = new byte[blockSize];
            System.arraycopy(ConversionToBytes.intToBytes(leaflevel), 0, block, 0, Integer.BYTES);
            System.arraycopy(ConversionToBytes.intToBytes(MBRs.length), 0, block, Integer.BYTES, Integer.BYTES);
            System.arraycopy(ConversionToBytes.intToBytes(-1), 0, block, Integer.BYTES*2, Integer.BYTES);
            int counter = 3 * Integer.BYTES;
            for (int i=0;i<MBRs.length;i++)
            {
                Double[][] temp = Split.rectangle_to_points(MBRs[i]);
                try {

                    indexfile.seek(MBRs_ID[i]*blockSize+2*Integer.BYTES);
                    indexfile.write(ConversionToBytes.intToBytes(blockID));


                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (int j=0;j<FileHandler.getDimensions();j++)
                {
                    for (int k=0;k<FileHandler.getDimensions();k++)
                    {
                        System.arraycopy(ConversionToBytes.doubleToBytes(temp[j][k]), 0, block, counter, Double.BYTES);
                        counter+= Double.BYTES;
                    }
                }
                System.arraycopy(ConversionToBytes.intToBytes(MBRs_ID[i]), 0, block, counter, Integer.BYTES);
                counter+=Integer.BYTES;
            }

            try {
                indexfile.seek(blockID * blockSize);
                indexfile.write(block);
                blockID++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }



    public int getLevelsofTree(int size)
    {
        int temp=(int) Math.ceil((double) size/FileHandler.calculateMaxBlockNodes());
        int result=0;
        while (temp>1)
        {
            temp =(int) Math.ceil((double) temp/FileHandler.calculateMaxBlockRectangles());
            result++;

        }
        return result;
    }

    public void readIndexFile(){
        try {
            File file = new File("indexfileBU.dat");
            byte[] bytes = Files.readAllBytes(file.toPath());
            for (int i = 1; i < blockID; i++){
                byte[] block = new byte[FileHandler.getBlockSize()];
                System.arraycopy(bytes, i * FileHandler.getBlockSize(), block, 0, FileHandler.getBlockSize());

                byte[] level = new byte[Integer.BYTES];
                byte[] currentNoOfEntries = new byte[Integer.BYTES];
                byte[] parentPointer = new byte[Integer.BYTES];

                System.arraycopy(block, 0, level, 0, Integer.BYTES);
                System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
                System.arraycopy(block, 2 * Integer.BYTES, parentPointer, 0, Integer.BYTES);

                int tempLevel = ByteBuffer.wrap(level).getInt();
                int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
                int tempParentPointer = ByteBuffer.wrap(parentPointer).getInt();

                if (tempLevel==getLevelsofTree(records.size())) {

                    System.out.println("Block No: " + i + ", Level: " + tempLevel + ", no of entries: " + tempCurrentNoOfEntries +
                            ", Parent block id: " + tempParentPointer + "\nRecords: ");

                    byte[] LATarray = new byte[Double.BYTES];
                    byte[] LONarray = new byte[Double.BYTES];
                    byte[] RecordIdArray = new byte[Integer.BYTES];
                    int bytecounter = 3 * Integer.BYTES;

                    for (int j = 0; j < tempCurrentNoOfEntries; j++) {
                        System.arraycopy(block, bytecounter, LATarray, 0, Double.BYTES);

                        System.arraycopy(block, bytecounter + Double.BYTES, LONarray, 0, Double.BYTES);
                        System.arraycopy(block, bytecounter + 2 * Double.BYTES, RecordIdArray, 0, Integer.BYTES);
                        bytecounter += 2 * Double.BYTES + Integer.BYTES;

                        double LAT = ByteBuffer.wrap(LATarray).getDouble();
                        double LON = ByteBuffer.wrap(LONarray).getDouble();
                        int recordId = ByteBuffer.wrap(RecordIdArray).getInt();

                        System.out.println("LAT: " + LAT + ", LON: " + LON + ", ID:" + recordId);
                    }
                }

                else
                {
                    System.out.println("Block No: " + i + ", Level: " + tempLevel +
                            ", Parent block id: " + tempParentPointer + "\nRecords: ");

                    int bytecounter = 3 * Integer.BYTES;
                    byte[][][] pointsArray= new byte[2][FileHandler.getDimensions()][Double.BYTES];

                    for (int j=0;j < tempCurrentNoOfEntries; j++)
                    {
                        for (int k=0;k<2;k++)
                        {
                            for (int c=0;c<FileHandler.getDimensions();c++)
                            {
                                System.arraycopy(block,bytecounter,pointsArray[k][c],0,Double.BYTES);
                                bytecounter+=Double.BYTES;
                                System.out.print(ByteBuffer.wrap(pointsArray[k][c]).getDouble() + " ");
                            }
                        }
                        System.out.println();
                        pointsArray= new byte[2][FileHandler.getDimensions()][Double.BYTES];
                        bytecounter+=Integer.BYTES;

                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}

