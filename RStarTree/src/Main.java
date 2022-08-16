public class Main {
    public static void main(String[] args) {
        UserInterface userInterface = new UserInterface();
        // create datafile
//        FileHandler.createDataFile(2);
//        System.out.println(FileHandler.getDatafileRecords().size());
//        FileHandler.readDatafile();

        // read datafile
//        System.out.println("Nodes");
//        FileHandler.readDatafile();

        // read datafile metadata (first block)
//        System.out.println("Metadata");
//        FileHandler.readFirstDatafileBlock();

        // create indexfile
//        FileHandler.createIndexFile();







//        FileHandler.createIndexFile();
//        BottomUp bottomUp = new BottomUp();
//        bottomUp.construct();
//        bottomUp.readIndexFile();

        // read indexfile
//        System.out.println();
//        System.out.println();
//        System.out.println("root");
//        for (int i=0;i<4;i++)
//        {
//            for (int j=0;j<2;j++)
//            {
//                System.out.print(FileHandler.getRootMBR()[i][j]+ " ");
//            }
//            System.out.println();
//        }
//        FileHandler.readFirstIndexfileBlock();
//        FileHandler.readIndexFile();

        // read indexfile metadata (first block)

//        FileHandler.readIndexFile();

        // delete
//        double LAT = 39.6877864; // 39.6877864
//        double LON = 20.8362292; // 20.8362292
//        Delete.delete(LAT, LON);
//
//        FileHandler.readIndexFile();

//        Rectangle rangeRectangle = userInput.getRangeQueryRectangle();
//        RangeQuery rangeQuery = new RangeQuery(rangeRectangle);
//        rangeQuery.print();

//        KnnQuery knnQuery = new KnnQuery(userInput.getK(), userInput.getKnnQueryPoint());
//        knnQuery.print();
//        KnnQuery knnQuery = new KnnQuery();

//        SkylineQuery skylineQuery = new SkylineQuery();

//        LinearSearchRangeQuery lsrg = new LinearSearchRangeQuery(userInterface.getRangeQueryRectangle());
//        lsrg.print();
//        LinearSearchKnnQuery lskq = new LinearSearchKnnQuery(userInterface.getK(), userInterface.getKnnQueryPoint());
//        lskq.print();
    }
}

// LAT: 41.3850909, LON: 26.6241053, ID:349, Datafile block: 1, Block slot: 9113 node to be deleted in the first block
// LAT: 41.4930671, LON: 26.5358795, ID:786, Datafile block: 1, Block slot: 20482 node to replace the deleted node ^
// LAT: 39.7160812, 39.8955962, LON: 20.5650812, 20.6217563
// LAT: 39.6877864, 39.8836309, LON: 20.6219575, 20.8595685