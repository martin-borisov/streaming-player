package mb.player.media.audio;

public class Accumulator {

    private long value;
    
    public void add(long val) {
        value += val;
    }
    
    public void reset() {
        value = 0;
    }

    public long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Accumulator [value=" + value + "]";
    }
}
