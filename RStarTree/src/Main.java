public class Main {
    public static void main(String[] args) {
        FileHandler.createIndexFile(2);
        System.out.println("Nodes");
        FileHandler.readDatafile();

        System.out.println("Metadata");
        FileHandler.readFirstDatafileBlock();
    }
}
