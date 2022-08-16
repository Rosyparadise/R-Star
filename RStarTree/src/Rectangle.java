import java.util.ArrayList;

public class Rectangle {
//    private final double minLAT, minLON, maxLAT, maxLON;
    private final ArrayList<Double> coordinates;
    private int childPointer;

    Rectangle(ArrayList<Double> coordinates, int childPointer) {
        this.coordinates = new ArrayList<>(coordinates);
        this.childPointer = childPointer;
    }

    Rectangle(ArrayList<Double> coordinates) {
        this.coordinates = new ArrayList<>(coordinates);
    }

    public static void tempSort(ArrayList<Double[][]> a, int b, int c, ArrayList<Integer> d) //one more variable to alternate between lower and higher
    {
        for (int i = 0; i < a.size(); i++)
        {
            for (int j = a.size() - 1; j > i; j--)
            {
                if (b==0)
                {
                    if (c==0)
                    {
                        if (a.get(i)[0][0] > a.get(j)[0][0])
                        {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);

                            Integer tmp1 = d.get(i);
                            d.set(i,d.get(j));
                            d.set(j,tmp1);
                        }
                    }
                    else
                    {
                        if (a.get(i)[1][0] > a.get(j)[1][0])
                        {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);

                            Integer tmp1 = d.get(i);
                            d.set(i,d.get(j));
                            d.set(j,tmp1);
                        }
                    }
                }
                else
                {
                    if (c==0)
                    {
                        if (a.get(i)[0][1] > a.get(j)[0][1])
                        {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);

                            Integer tmp1 = d.get(i);
                            d.set(i,d.get(j));
                            d.set(j,tmp1);
                        }
                    }
                    else
                    {
                        if (a.get(i)[1][1] > a.get(j)[1][1])
                        {
                            Double[][] tmp = a.get(i);
                            a.set(i, a.get(j));
                            a.set(j, tmp);

                            Integer tmp1 = d.get(i);
                            d.set(i,d.get(j));
                            d.set(j,tmp1);
                        }
                    }
                }
            }
        }
    }

    public ArrayList<Double> getCoordinates() {
        return this.coordinates;
    }

    public int getChildPointer() {
        return childPointer;
    }
}
