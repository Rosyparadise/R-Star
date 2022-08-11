public class Rectangle {
    private final double minLAT, minLON, maxLAT, maxLON;
    private final int childPointer;

    Rectangle(double minLAT, double minLON, double maxLAT, double maxLON, int childPointer) {
        this.minLAT = minLAT;
        this.minLON = minLON;
        this.maxLAT = maxLAT;
        this.maxLON = maxLON;
        this.childPointer = childPointer;
    }

    public double getMaxLAT() {
        return maxLAT;
    }

    public double getMaxLON() {
        return maxLON;
    }

    public double getMinLAT() {
        return minLAT;
    }

    public double getMinLON() {
        return minLON;
    }

    public int getChildPointer() {
        return childPointer;
    }
}
