import java.util.ArrayList;

public class SkylinePair {
    private int id;
    private ArrayList<Double> coordinates;

    SkylinePair(int id, ArrayList<Double> coordinates) {
        this.id = id;
        this.coordinates = coordinates;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Double> getCoordinates() {
        return coordinates;
    }
}
