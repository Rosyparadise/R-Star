public class KnnDistanceRectanglePair {
    private final Rectangle rectangle;
    private final double distance;

    KnnDistanceRectanglePair(Rectangle rectangle, double distance) {
        this.rectangle = rectangle;
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }
}