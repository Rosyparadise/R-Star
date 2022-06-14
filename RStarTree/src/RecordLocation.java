public class RecordLocation {
    private final int block;
    private final long slot;

    public RecordLocation(int block, long slot){
        this.block = block;
        this.slot = slot;
    }

    public int getBlock(){
        return this.block;
    }

    public long getSlot(){
        return this.slot;
    }
}
