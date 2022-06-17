import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;

public class Split {
    private static final double m=0.4;
    private static final double p=0.3;

    public static void reinsert(int blockId, Record troublemaker)
    {
        String IndexfilePath = FileHandler.getIndexfilePath();
        int blockSize = FileHandler.getBlockSize();
        int dimensions = FileHandler.getDimensions();

        try {
            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] block = new byte[blockSize];
            System.arraycopy(bytes, blockId * blockSize, block, 0, blockSize);

            byte[] treeLevelBytes = new byte[Integer.BYTES];
            byte[] currentNoOfEntries = new byte[Integer.BYTES];
            byte[] parentPointerArray = new byte[Integer.BYTES];

            System.arraycopy(block, 0, treeLevelBytes, 0, Integer.BYTES);
            System.arraycopy(block, Integer.BYTES, currentNoOfEntries, 0, Integer.BYTES);
            System.arraycopy(block, 2 * Integer.BYTES, parentPointerArray, 0, Integer.BYTES);

            int treeLevel = ByteBuffer.wrap(treeLevelBytes).getInt();
            int tempCurrentNoOfEntries = ByteBuffer.wrap(currentNoOfEntries).getInt();
            int parentPointer = ByteBuffer.wrap(parentPointerArray).getInt();

            ArrayList<Record> tempRecords = new ArrayList<>();

            int bytecounter = 3 * Integer.BYTES;
            byte[] LATarray = new byte[Double.BYTES];
            byte[] LONarray = new byte[Double.BYTES];
            byte[] RecordIdArray = new byte[Integer.BYTES];

            for (int j = 0; j < tempCurrentNoOfEntries; j++) {
                System.arraycopy(block, bytecounter, LATarray, 0, Double.BYTES);
                System.arraycopy(block, bytecounter + Double.BYTES, LONarray, 0, Double.BYTES);
                System.arraycopy(block, bytecounter + 2 * Double.BYTES, RecordIdArray, 0, Integer.BYTES);
                bytecounter += 2 * Double.BYTES + Integer.BYTES;

                tempRecords.add(new Record(ByteBuffer.wrap(LATarray).getDouble(), ByteBuffer.wrap(LONarray).getDouble(), ByteBuffer.wrap(RecordIdArray).getInt()));
            }
            tempRecords.add(troublemaker); //MIGHT NOT NEED RecordsDup
            double[][] mbr = calculateMBR(tempRecords);
            double[] mbr_midpoint = {(mbr[0][0] + mbr[3][0]) / 2.0,(mbr[0][1] + mbr[3][1]) / 2.0};
            for (int i = 0; i < tempRecords.size(); i++)
            {
                for (int j = tempRecords.size() - 1; j > i; j--)
                {
                    if (calcDistance(mbr_midpoint,tempRecords.get(i)) > calcDistance(mbr_midpoint,tempRecords.get(j)))
                    {
                        Record tmp = tempRecords.get(i);
                        tempRecords.set(i, tempRecords.get(j));
                        tempRecords.set(j, tmp);
                    }
                }
            }
            int amountToReInsert= (int) Math.floor(tempRecords.size()*p);
            ArrayList<Record> toReinsert = new ArrayList<>();
            for (int i=0;i<amountToReInsert;i++)
            {
                toReinsert.add(tempRecords.get(i));
            }
            //continue

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calcDistance(double[] mbr_midpoint, Record record)
    {
        return Math.sqrt((mbr_midpoint[1] - record.getLON()) * (mbr_midpoint[1] - record.getLON()) + (mbr_midpoint[0] - record.getLAT()) * (mbr_midpoint[0] - record.getLAT()));
    }
    public static void split(int blockId,Record troublemaker)
    {
        String IndexfilePath = FileHandler.getIndexfilePath();
        int blockSize = FileHandler.getBlockSize();
        int dimensions = FileHandler.getDimensions();

        try {
            File file = new File(IndexfilePath);
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] block = new byte[blockSize];
            System.arraycopy(bytes, blockId * blockSize, block, 0, blockSize);
            byte[] noOfBlocksArray = new byte[4];
            System.arraycopy(block, 4, noOfBlocksArray, 0, noOfBlocksArray.length);
            int tempCurrentNoOfEntries = ByteBuffer.wrap(noOfBlocksArray).getInt();
            ArrayList<Record> tempRecords = new ArrayList<>();

            int bytecounter = 3 * Integer.BYTES;
            byte[] LATarray = new byte[Double.BYTES];
            byte[] LONarray = new byte[Double.BYTES];
            byte[] RecordIdArray = new byte[Integer.BYTES];

            for (int j=0;j<tempCurrentNoOfEntries;j++)
            {
                System.arraycopy(block, bytecounter, LATarray, 0, Double.BYTES);
                System.arraycopy(block, bytecounter + Double.BYTES, LONarray, 0, Double.BYTES);
                System.arraycopy(block, bytecounter + 2 * Double.BYTES, RecordIdArray, 0, Integer.BYTES);
                bytecounter += 2 * Double.BYTES + Integer.BYTES;

                tempRecords.add(new Record(ByteBuffer.wrap(LATarray).getDouble(),ByteBuffer.wrap(LONarray).getDouble(),ByteBuffer.wrap(RecordIdArray).getInt()));
            }
            tempRecords.add(troublemaker); //MIGHT NOT NEED RecordsDup
            ArrayList<Record> recordsDup = new ArrayList<>(tempRecords);
            double margin_value=Double.MAX_VALUE;

            ArrayList<Record> first = new ArrayList<>();
            ArrayList<Record> second = new ArrayList<>();

            ArrayList<Record> axisLeastMargin= new ArrayList<>();

            //THIS NEEDS TO BE CHANGED TO WORK FOR ANY NUMBER OF DIMENSIONS
            for (int i=0;i<dimensions;i++)
            {
                if (i==0)
                {
                    Record.tempSort(recordsDup,0);
                    double temp=chooseSplitAxis(recordsDup,blockId);
                    if (temp<margin_value) {
                        margin_value = temp;
                        axisLeastMargin.addAll(recordsDup);
                    }
                }
                else
                {
                    Record.tempSort(recordsDup,1);
                    double temp=chooseSplitAxis(recordsDup,blockId);
                    if (temp<margin_value) {
                        margin_value = temp;
                        axisLeastMargin.addAll(recordsDup);
                    }
                }
            }


            int result_split = chooseSplitIndex(axisLeastMargin);

            for (int l=0;l<result_split;l++)
                first.add(axisLeastMargin.get(l));


            double[][] firstMBR;
            firstMBR=calculateMBR(first);

            for (int l=result_split;l<axisLeastMargin.size();l++)
                second.add(axisLeastMargin.get(l));

            double[][] secondMBR;
            secondMBR=calculateMBR(second);


            writeAfterSplit(first,second,firstMBR,secondMBR,blockId);
            calculateMBRpointbypoint(FileHandler.getRootMBR(),troublemaker,false);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeAfterSplit(ArrayList<Record> first, ArrayList<Record> second, double[][] firstMBR, double[][] secondMBR, int blockId)
    {
        int leafLevel = FileHandler.getLeafLevel();
        String IndexfilePath = FileHandler.getIndexfilePath();
        int blockSize = FileHandler.getBlockSize();
        int dimensions = FileHandler.getDimensions();

        if (blockId==1 && leafLevel==0)
        {
            File file = new File(IndexfilePath);
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                byte[] dataBlock = new byte[blockSize];
                System.arraycopy(bytes, blockId*blockSize,dataBlock,0,Integer.BYTES);
                System.arraycopy(FileHandler.intToBytes(2),0,dataBlock,Integer.BYTES,Integer.BYTES);
                System.arraycopy(FileHandler.intToBytes(-1),0,dataBlock,Integer.BYTES*2,Integer.BYTES);
                int counter = 3 * Integer.BYTES;

                for (int i=0;i<Math.pow(2,dimensions);i+=Math.pow(2,dimensions)-1)
                {
                    for (int j=0;j<dimensions;j++)
                    {
                        System.arraycopy(FileHandler.doubleToBytes(firstMBR[i][j]), 0,dataBlock,counter,Double.BYTES);
                        counter+=Double.BYTES;
                    }
                }
                FileHandler.setNoOfIndexfileBlocks(FileHandler.getNoOfIndexfileBlocks() + 1);
                System.arraycopy(FileHandler.intToBytes(FileHandler.getNoOfIndexfileBlocks()),0,dataBlock,counter,Integer.BYTES);
                counter+=Integer.BYTES;


                leafLevel++;
                FileHandler.setLeafLevel(leafLevel);

                byte[] dataBlock1 = new byte[blockSize];

                System.arraycopy(FileHandler.intToBytes(leafLevel), 0,dataBlock1,0,Integer.BYTES);
                System.arraycopy(FileHandler.intToBytes(first.size()),0,dataBlock1,Integer.BYTES,Integer.BYTES);
                System.arraycopy(FileHandler.intToBytes(blockId), 0, dataBlock1, 2 * Integer.BYTES, Integer.BYTES);

                int counter1 = 3 * Integer.BYTES;
                for (Record record : first) {
                    System.arraycopy(FileHandler.doubleToBytes(record.getLAT()), 0, dataBlock1, counter1, Double.BYTES);
                    counter1 += Double.BYTES;
                    System.arraycopy(FileHandler.doubleToBytes(record.getLON()), 0, dataBlock1, counter1, Double.BYTES);
                    counter1 += Double.BYTES;
                    System.arraycopy(FileHandler.intToBytes(record.getId()), 0, dataBlock1, counter1, Integer.BYTES);
                    counter1 += Integer.BYTES;
                }

                for (int i=0;i<Math.pow(2,dimensions);i+=Math.pow(2,dimensions)-1)
                {
                    for (int j=0;j<dimensions;j++)
                    {
                        System.arraycopy(FileHandler.doubleToBytes(secondMBR[i][j]), 0,dataBlock,counter,Double.BYTES);
                        counter+=Double.BYTES;
                    }

                }
                FileHandler.setNoOfIndexfileBlocks(FileHandler.getNoOfIndexfileBlocks() + 1);
                System.arraycopy(FileHandler.intToBytes(FileHandler.getNoOfIndexfileBlocks()),0,dataBlock,counter,Integer.BYTES);


                byte[] dataBlock2 = new byte[blockSize];

                System.arraycopy(FileHandler.intToBytes(leafLevel), 0,dataBlock2,0,Integer.BYTES);
                System.arraycopy(FileHandler.intToBytes(second.size()),0,dataBlock2,Integer.BYTES,Integer.BYTES);
                System.arraycopy(FileHandler.intToBytes(blockId), 0, dataBlock2, 2 * Integer.BYTES, Integer.BYTES);

                int counter2 = 3 * Integer.BYTES;
                for (Record record : second) {
                    System.arraycopy(FileHandler.doubleToBytes(record.getLAT()), 0, dataBlock2, counter2, Double.BYTES);
                    counter2 += Double.BYTES;
                    System.arraycopy(FileHandler.doubleToBytes(record.getLON()), 0, dataBlock2, counter2, Double.BYTES);
                    counter2 += Double.BYTES;
                    System.arraycopy(FileHandler.intToBytes(record.getId()), 0, dataBlock2, counter2, Integer.BYTES);

                    counter2 += Integer.BYTES;
                }
                RandomAccessFile indexfile = new RandomAccessFile(IndexfilePath, "rw");
                indexfile.seek((long) (FileHandler.getNoOfIndexfileBlocks() - 2) *blockSize);
                indexfile.write(dataBlock);
                indexfile.seek((long) (FileHandler.getNoOfIndexfileBlocks() - 1) *blockSize);
                indexfile.write(dataBlock1);
                indexfile.seek((long) FileHandler.getNoOfIndexfileBlocks() *blockSize);
                indexfile.write(dataBlock2);
                //System.out.println("health x1,y1 = ("+ secondMBR[0][0] + " " + secondMBR[0][1]+") " + "x2,y1 = ("+ secondMBR[1][0] + " " + secondMBR[1][1]+") " + "x1,y2 = ("+ secondMBR[2][0] + " " + secondMBR[2][1]+") " + "x2,y2 = ("+ secondMBR[3][0] + " " + secondMBR[3][1]+") ");


                byte[] tempMetaData = FileHandler.intToBytes(FileHandler.getNoOfIndexfileBlocks());
                indexfile.seek(Integer.BYTES);
                indexfile.write(tempMetaData);
                tempMetaData = FileHandler.intToBytes(leafLevel);
                indexfile.seek(Integer.BYTES*2);
                indexfile.write(tempMetaData);


                indexfile.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }



        }
    }

    private static int chooseSplitIndex(ArrayList<Record> axisLeastMargin)
    {
        double overlap;
        double area=0;
        double min_overlap=Double.MAX_VALUE;
        int result = 0;
        for (int k=1;k<FileHandler.calculateMaxBlockNodes()-Math.floor(2*m*FileHandler.calculateMaxBlockNodes())+2;k++)
        {
            ArrayList<Record> firstTemp = new ArrayList<>();
            ArrayList<Record> secondTemp = new ArrayList<>();


            for (int l=0;l<(int)Math.floor(m*FileHandler.calculateMaxBlockNodes()-1)+k;l++)
                firstTemp.add(axisLeastMargin.get(l));


            double[][] firstMBR;
            firstMBR=calculateMBR(firstTemp);

            for (int l=(int)Math.floor(m*FileHandler.calculateMaxBlockNodes()-1)+k;l<axisLeastMargin.size();l++)
                secondTemp.add(axisLeastMargin.get(l));

            double[][] secondMBR;
            secondMBR=calculateMBR(secondTemp);

            overlap=calcOverlap(firstMBR,secondMBR);
            if (overlap<min_overlap)
            {
                area=calcArea(firstMBR,secondMBR)-overlap;
                min_overlap=overlap;
                result=(int)Math.floor(m*FileHandler.calculateMaxBlockNodes()-1)+k;
            }
            else if (overlap==min_overlap)
            {
                double b = calcArea(firstMBR,secondMBR)-overlap;
                if (b<area)
                {
                    area=b;
                    result=(int)Math.floor(m*FileHandler.calculateMaxBlockNodes()-1)+k;
                }
            }
        }
        return result;
    }

    private static double calcArea(double[][] firstMBR,double[][] secondMBR)
    {
        double a =  (firstMBR[2][1]-firstMBR[0][1])*(firstMBR[1][0]-firstMBR[0][0]);
        double b =  (secondMBR[2][1]-secondMBR[0][1])*(secondMBR[1][0]-secondMBR[0][0]);
        return (a+b);
    }

    private static double calcAreaDiff(double[][] firstMBR,double[][] secondMBR)
    {
        double a =  (firstMBR[2][1]-firstMBR[0][1])*(firstMBR[1][0]-firstMBR[0][0]);
        double b =  (secondMBR[2][1]-secondMBR[0][1])*(secondMBR[1][0]-secondMBR[0][0]);
        return (a-b);
    }

    private static double calcArea(double[][] firstMBR)
    {
        double a =  (firstMBR[2][1]-firstMBR[0][1])*(firstMBR[1][0]-firstMBR[0][0]);
        return (a);
    }


    private static double chooseSplitAxis(ArrayList<Record> recordsDup, int blockId) //BLOCKID TO GET PARENT AND THEREFORE MBR OF PARENT
    {
        double margin_value=0;
        for (int k=1;k<FileHandler.calculateMaxBlockNodes()-Math.floor(2*m*FileHandler.calculateMaxBlockNodes())+2;k++)
        {
            ArrayList<Record> firstTemp = new ArrayList<>();
            ArrayList<Record> secondTemp = new ArrayList<>();


            for (int l=0;l<Math.floor(m*FileHandler.calculateMaxBlockNodes()-1)+k;l++)
                firstTemp.add(recordsDup.get(l));


            double[][] firstMBR;
            firstMBR=calculateMBR(firstTemp);
            margin_value+=calcMargin(firstMBR,blockId);

            for (int l=(int)Math.floor(m*FileHandler.calculateMaxBlockNodes()-1)+k;l<recordsDup.size();l++)
                secondTemp.add(recordsDup.get(l));


            double[][] secondMBR;
            secondMBR=calculateMBR(secondTemp);
            margin_value+=calcMargin(secondMBR,blockId);
        }
        return margin_value;
    }

    private static double calcMargin(double[][] childMBR, int blockId)
    {
        double[][] rootMBR = FileHandler.getRootMBR();

        double margin_value=0;
        if (blockId==1)
        {
            margin_value+=Math.abs(rootMBR[2][1]-childMBR[2][1]); //top margin
            margin_value+=Math.abs(rootMBR[0][1]-childMBR[0][1]); //bottom margin
            margin_value+=Math.abs(rootMBR[0][0]-childMBR[0][0]); //left margin
            margin_value+=Math.abs(rootMBR[1][0]-childMBR[1][0]); //left margin

            //block is root so we use rootMBR. (I think this is how margin-values work, not sure though)

        }
        else
        {
            //get parent's MBR via child's pointer to parent and calculate
        }
        return margin_value;
    }

    private static double[][] calculateMBR(ArrayList<Record> firstTemp)
    {
        int dimensions = FileHandler.getDimensions();

        double[] tempFirstPoint1 = new double[2];
        double[] tempSecondPoint1 = new double[2];
        tempFirstPoint1[0]=Double.MAX_VALUE;
        tempFirstPoint1[1]=Double.MAX_VALUE;
        tempSecondPoint1[0]=-1;
        tempSecondPoint1[1]=-1;
        double[][] firstMBR = new double[(int)Math.pow(2,dimensions)][dimensions];
        for (Record record : firstTemp) {
            if (record.getLAT() < tempFirstPoint1[0])
                tempFirstPoint1[0] = record.getLAT();
            if (record.getLON() < tempFirstPoint1[1])
                tempFirstPoint1[1] = record.getLON();

            if (record.getLAT() > tempSecondPoint1[0])
                tempSecondPoint1[0] = record.getLAT();
            if (record.getLON() > tempSecondPoint1[1])
                tempSecondPoint1[1] = record.getLON();
        }
        firstMBR[0][0]=tempFirstPoint1[0];firstMBR[0][1]=tempFirstPoint1[1];
        firstMBR[1][0]=tempSecondPoint1[0];firstMBR[1][1]=tempFirstPoint1[1];
        firstMBR[2][0]=tempFirstPoint1[0];firstMBR[2][1]=tempSecondPoint1[1];
        firstMBR[3][0]=tempSecondPoint1[0];firstMBR[3][1]=tempSecondPoint1[1];

        return firstMBR;


    }

    private static double calcOverlap(double[][] a, double[][] b)
    {
        // Area of 1st Rectangle
        double area1 = Math.abs(a[0][0] - a[3][0]) * Math.abs(a[0][1] - a[3][1]);


        // Area of 2nd Rectangle
        double area2 = Math.abs(b[0][0] - b[3][0]) * Math.abs(b[0][1] - b[3][1]);


        double x_dist = Math.min(a[3][0], b[3][0]) - Math.max(a[0][0], b[0][0]);
        double y_dist = Math.min(a[3][1], b[3][1]) - Math.max(a[0][1], b[0][1]);
        double areaI = 0;
        if( x_dist > 0 && y_dist > 0 )
            areaI = x_dist * y_dist;

        return (area1 + area2 - areaI);
    }

    public static int determine_best_insertion_forRectangles(ArrayList<double[][]> rectangles, Record record)
    {
        int dimensions = FileHandler.getDimensions();

        double[][] temp1 = new double[(int)Math.pow(2,dimensions)][dimensions];
        double[][] temp3 = new double[(int)Math.pow(2,dimensions)][dimensions];

        double area_diff=0;
        double area=0;
        double least_diff=Double.MAX_VALUE;
        int result=0;

        for (int i=0;i<rectangles.size();i++)
        {
            points_to_rectangle(rectangles.get(i),temp1);
            for (int b=0;b<temp1.length;b++)
                System.arraycopy(temp1[b], 0, temp3[b], 0, temp1[0].length);
            calculateMBRpointbypoint(temp1,record,false);

            area_diff=calcAreaDiff(temp1,temp3);

            if (area_diff<least_diff)
            {
                least_diff=area_diff;
                result=i;
                area=calcArea(temp1);
            }
            else if (area_diff==least_diff)
            {
                double b = calcArea(temp1);
                if (b<area)
                {
                    area=b;
                    result=i;
                }
            }
        }
        return result;
    }

    public static int determine_best_insertion(ArrayList<double[][]> rectangles, Record record)
    {
        int dimensions = FileHandler.getDimensions();

        double[][] temp1 = new double[(int)Math.pow(2,dimensions)][dimensions];
        double[][] temp2 = new double[(int)Math.pow(2,dimensions)][dimensions];
        double[][] temp3 = new double[(int)Math.pow(2,dimensions)][dimensions];

        double temp_overlap=0;
        double area_diff=0;
        double area=0;
        double least_overlap=Double.MAX_VALUE;
        int result=0;

        for (int i=0;i<rectangles.size();i++)
        {
            points_to_rectangle(rectangles.get(i),temp1);
            for (int b=0;b<temp1.length;b++)
                System.arraycopy(temp1[b], 0, temp3[b], 0, temp1[0].length);
            calculateMBRpointbypoint(temp1,record,false);


            for (int j=0;j<rectangles.size();j++)
            {
                if (j!=i)
                {
                    points_to_rectangle(rectangles.get(j),temp2);
                    temp_overlap+=calcOverlap(temp1,temp2);
                }
            }
            if (temp_overlap<least_overlap)
            {
                least_overlap=temp_overlap;
                result=i;
                area_diff=calcAreaDiff(temp1,temp3);
                area=calcArea(temp1);
            }
            else if (temp_overlap==least_overlap)
            {
                double b = calcAreaDiff(temp1,temp3);
                if (b<area_diff)
                {
                    area_diff=b;
                    result=i;
                    area=calcArea(temp1);
                }
                if (b==area_diff)
                {
                    double c = calcArea(temp1);
                    if (c<area)
                    {
                        result=i;
                        area=c;
                    }
                }
            }
            temp_overlap=0;
        }

        return result;
    }

    public static void points_to_rectangle(double[][] points,double[][] rectangle)
    {
        rectangle[0][0] = points[0][0];rectangle[0][1] = points[0][1];
        rectangle[1][0] = points[1][0];rectangle[1][1] = points[0][1];
        rectangle[2][0] = points[0][0];rectangle[2][1] = points[1][1];
        rectangle[3][0] = points[1][0];rectangle[3][1] = points[1][1];
    }

    public static void calculateMBRpointbypoint(double[][] firstMBR, Record a,boolean isFirstEntry)
    {


        if (isFirstEntry)
        {
            firstMBR[0][0]=a.getLAT();firstMBR[0][1]=a.getLON();
            firstMBR[1][0]=a.getLAT();firstMBR[1][1]=a.getLON();
            firstMBR[2][0]=a.getLAT();firstMBR[2][1]=a.getLON();
            firstMBR[3][0]=a.getLAT();firstMBR[3][1]=a.getLON();
        }
        else
        {
            if (a.getLAT()<firstMBR[0][0])
            {
                firstMBR[0][0]=a.getLAT();
                firstMBR[2][0]=a.getLAT();
            }
            if (a.getLAT()>firstMBR[1][0])
            {
                firstMBR[1][0]=a.getLAT();
                firstMBR[3][0]=a.getLAT();
            }
            if (a.getLON()<firstMBR[0][1])
            {
                firstMBR[0][1]=a.getLON();
                firstMBR[1][1]=a.getLON();
            }
            if (a.getLON()>firstMBR[2][1])
            {
                firstMBR[2][1]=a.getLON();
                firstMBR[3][1]=a.getLON();
            }
        }
    }
}
