package hu.fotoamg.windspeed.nmea.Types;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class RangeSpeedInfo {
    SortedMap<Float, Float> speedDirValues = new TreeMap<Float, Float>();
    Integer rangeKey = null;
    boolean locked = false;
    int count =  0;
    float speedSum = 0f;
    float dirSum = 0f;

    public RangeSpeedInfo(Integer rangeKey) {
        this.rangeKey = rangeKey;
    }

    public SortedMap<Float, Float> getSpeedDirValues() {
        return speedDirValues;
    }

    public Integer getRangeKey() {
        return rangeKey;
    }

    public void setRangeKey(Integer rangeKey) {
        this.rangeKey = rangeKey;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public int getCount() {
        return count;
    }

    public float getSpeedSum() {
        return speedSum;
    }

    public float getDirSum() {
        return dirSum;
    }

    public void insertSpeedData(float speed, float bearing) {
        speedDirValues.put(new Float(speed), new Float(bearing));
        count++;
        speedSum += speed;
        dirSum += bearing;
    }

    public float getAvgSpeed() {
        return speedSum / count;
    }

    public float getAvgDir() {
        return dirSum / count;
    }

    public Map.Entry<Float, Float> getMedianSpeedData() {
        Map.Entry<Float, Float> medianEntry = null;
        int medianIndex = (speedDirValues.size()) / 2;
        Set mapEntrySet = speedDirValues.entrySet();
        Iterator mapIterator = mapEntrySet.iterator();
        int counter = 0;
        while (mapIterator.hasNext() && (counter <= medianIndex)) {
            Map.Entry<Float, Float> m = (Map.Entry<Float, Float>) mapIterator.next();
            if (counter >= medianIndex) {
                medianEntry = m;
                break;
            }
            counter++;
        }
        return medianEntry;
    }

}
