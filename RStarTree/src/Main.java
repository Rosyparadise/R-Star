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
        FileHandler.readIndexFile();

        // read indexfile metadata (first block)
//        FileHandler.readFirstIndexfileBlock();
    }
}
