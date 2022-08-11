public class Main {
    public static void main(String[] args) {
        // create datafile
        FileHandler.createDataFile(2);

        // read datafile
//        System.out.println("Nodes");
//        FileHandler.readDatafile();

        // read datafile metadata (first block)
//        System.out.println("Metadata");
//        FileHandler.readFirstDatafileBlock();

        // create indexfile
        FileHandler.createIndexFile();
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
        FileHandler.readIndexFile();
        // read indexfile metadata (first block)

        // delete
//        double LAT = 39.7160812; // 39.6877864
//        double LON = 20.6005205; // 20.8362292
//        Delete.delete(LAT, LON);
//
//        FileHandler.readIndexFile();

//        RangeQuery rangeQuery = new RangeQuery();
//        KnnQuery knnQuery = new KnnQuery();
//        SkylineQuery skylineQuery = new SkylineQuery();
    }
}

// LAT: 41.3850909, LON: 26.6241053, ID:349, Datafile block: 1, Block slot: 9113 node to be deleted in the first block
// LAT: 41.4930671, LON: 26.5358795, ID:786, Datafile block: 1, Block slot: 20482 node to replace the deleted node ^