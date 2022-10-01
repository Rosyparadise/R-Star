public class Main {
    public static void main(String[] args) {
        UserInterface userInterface = new UserInterface();
        // create datafile
        //FileHandler.createDataFile(2);
//        System.out.println(FileHandler.getDatafileRecords().size());
//        FileHandler.readDatafile();

        // read datafile
//        System.out.println("Nodes");
//        FileHandler.readDatafile();

        // read datafile metadata (first block)
//        System.out.println("Metadata");
//        FileHandler.readFirstDatafileBlock();

        // create indexfile








        /*
        FileHandler.createIndexFile(false);
        BottomUp bottomUp = new BottomUp();
        bottomUp.construct();
        //Delete.delete(39.7160812,20.6005205);
        //Insert.insert(40,new Record(40,20,14183));
        FileHandler.readIndexFile();
        Delete.delete(39.801048,20.7306138);
        FileHandler.readIndexFile();



        // read indexfile
        System.out.println();
        System.out.println();
        System.out.println("root");
        for (int i=0;i<4;i++)
        {
            for (int j=0;j<2;j++)
            {
                System.out.print(FileHandler.getRootMBR()[i][j]+ " ");
            }
            System.out.println();
        }
        */






        /*
        FileHandler.createIndexFile(true);

        FileHandler.readFirstIndexfileBlock();
        FileHandler.debug();
        Delete.delete( 39.7799435, 20.7441187);
        System.out.println();
        System.out.println("root");
        for (int i=0;i<4;i++)
        {
            for (int j=0;j<2;j++)
            {
                System.out.print(FileHandler.getRootMBR()[i][j]+ " ");
            }
            System.out.println();
        }
        System.out.println("RECORDS SIZE = " + FileHandler.getRecords().size());

         */







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