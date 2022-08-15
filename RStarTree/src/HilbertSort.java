import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by zsu00 on 6/1/2017.
 */
public class HilbertSort {
    //keep track of sorted list
    Queue<Record> sortedList;
    Queue<Record> unsortedList;
    //size of square
    double S;

    public HilbertSort(Queue<Record> List, double sizeOfSpace) {
        this.unsortedList = List;
        S = sizeOfSpace;
        this.sortedList = new LinkedList();

    }

    //helper method to call hilbertSort method
    public Queue<Record> hilbertHelper() {
        hilbertSort(S, unsortedList);
        return sortedList;
    }


    //recursively divides square into quadrants to sort items
    private void hilbertSort(double S, Queue<Record> listToSort) {
        //put all unsorted location into proper quadrant
        Queue<Record> quadrant1 = new LinkedList<Record>();
        Queue<Record> quadrant2 = new LinkedList<Record>();
        Queue<Record> quadrant3 = new LinkedList<Record>();
        Queue<Record> quadrant4 = new LinkedList<Record>();

        while (!listToSort.isEmpty()) {
            Record item = listToSort.remove();
            //check the x,y values of each location and placed it into corresponding quadrant
            if (item.getLAT() >= 0 && item.getLAT() <= S / 2 &&
                    0 <= item.getLON() && item.getLON() <= S / 2) {
                quadrant1.add(item);
            } else if (item.getLAT() >= 0 && item.getLAT() <= S / 2 &&
                    S/2 <= item.getLON() && item.getLON() <= S) {
                quadrant2.add(item);
            } else if (item.getLAT() >= S/2 && item.getLAT() <= S &&
                    S/2 <= item.getLON() && item.getLON() <= S) {
                quadrant3.add(item);
            } else if (item.getLAT() >= S / 2 && item.getLAT() <= S &&
                    0 <= item.getLON() && item.getLON() <= S / 2) {
                quadrant4.add(item);
            }
        }


        //visit the quadrant by order to check if there is only one item in the quadrant
        // if true add that item to sorted list
        //otherwise further divide that quadrant till there is only one item in the quadrant
        //put items to sortedList based on 1,2,3,4 quadrant order
        if (quadrant1.size() > 0) {
            if (quadrant1.size() == 1) {
                sortedList.add(quadrant1.remove());
            } else {
                //iterates through elements to change x, y by rotation;
                for (Record item : quadrant1) {
                    double temp = item.getLAT();
                    item.setLAT(item.getLON());
                    item.setLON(temp);
                }
                hilbertSort(S/2, quadrant1);
            }
        }

        if (quadrant2.size() > 0) {
            if (quadrant2.size() == 1) {
                sortedList.add(quadrant2.remove());
            } else {
                    for (Record item : quadrant2) {
                    item.setLON(item.getLON() - S/2);
                }
                hilbertSort(S/2, quadrant2);
            }
        }

        if (quadrant3.size() > 0) {
            if (quadrant3.size() == 1) {
                sortedList.add(quadrant3.remove());
            } else {
                for (Record item : quadrant3) {
                    item.setLAT(item.getLAT() - S/2);
                    item.setLON(item.getLON() - S/2);
                }
                hilbertSort(S/2, quadrant3);
            }
        }

        if (quadrant4.size() > 0) {
            if (quadrant4.size() == 1) {
                sortedList.add(quadrant4.remove());
            } else {
                //iterates through and change x,y by rotation
                for (Record item : quadrant4) {
                    double temp = item.getLON();
                    item.setLON(S-item.getLAT());
                    item.setLAT(S/2 - temp);
                }
                hilbertSort(S/2, quadrant4);
            }
        }
    }
}