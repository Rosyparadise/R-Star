public class Record {
    private double LAT;
    private double LON;
    private RecordLocation recordLocation;
    private int id;

    public Record(double LAT, double LON, int block, long slot, int id){
        this.LAT = LAT;
        this.LON = LON;
        recordLocation = new RecordLocation(block, slot);
        this.id = id;
    }

    public RecordLocation getRecordLocation(){
        return recordLocation;
    }

    public double getLAT(){
        return LAT;
    }

    public double getLON(){
        return LON;
    }

    public int getId(){
        return id;
    }
}
