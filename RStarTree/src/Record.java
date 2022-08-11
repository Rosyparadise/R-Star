import java.util.ArrayList;

public class Record {
    private final double LAT;
    private final double LON;
    private RecordLocation recordLocation;
    private final int id;

    public Record(double LAT, double LON, int block, long slot, int id){
        this.LAT = LAT;
        this.LON = LON;
        recordLocation = new RecordLocation(block, slot);
        this.id = id;
    }
    public Record(double LAT, double LON, int id)
    {
        this.LAT = LAT;
        this.LON = LON;
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


    public static void tempSort(ArrayList<Record> a, int b)
    {
        for (int i = 0; i < a.size(); i++)
        {
            for (int j = a.size() - 1; j > i; j--)
            {
                if (b==0)
                {
                    if (a.get(i).getLAT() > a.get(j).getLAT()) {
                        Record tmp = a.get(i);
                        a.set(i, a.get(j));
                        a.set(j, tmp);
                    }
                }
                else
                {
                    if (a.get(i).getLON() > a.get(j).getLON()) {
                        Record tmp = a.get(i);
                        a.set(i, a.get(j));
                        a.set(j, tmp);
                    }
                }
            }
        }
    }
}

//TODO: Can probably fix how sorting is implemented.
