package hu.fotoamg.windspeed.nmea.Types;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class Survey {
    public static final int ALT_RADIUS = 20;
    Map<Integer, SortedMap<Float, Float>> altitudeMap  = new HashMap<>();
    Set<Integer> lockedRanges = new HashSet<>();

    public Survey() {
        super();
        initMap();
    }

    public void initMap() {
        lockedRanges.clear();
        altitudeMap.clear();
        for(int i = 0; i<2100; i+=100) {
            altitudeMap.put(new Integer(i), new TreeMap<Float, Float>());
        }
    }

    public void rangeLock(Integer range, boolean status) {
        if (status) {
            lockedRanges.add(range);
        } else {
            lockedRanges.remove(range);
        }
    }

    public Integer getAltRangeKey(float alt) {
        Integer key = null;
        Set<Integer> keys = altitudeMap.keySet();
        for (Integer currKey : keys) {
            if (((currKey.intValue()-20) < alt) && ((currKey.intValue()+20) > alt)) {
                key = currKey;
                break;
            }
        }
        return key;
    }

    public SortedMap<Float, Float> getAltSpeedMap(Integer range) {
        return altitudeMap.get(range);
    }

    public Map.Entry<Float, Float> getMaxSpeedData(Integer range) {
        SortedMap<Float, Float> currAltMap = getAltSpeedMap(range);
        Float lastKey = currAltMap.lastKey();
        return new AbstractMap.SimpleEntry<Float, Float>(lastKey, currAltMap.get(lastKey));
    }


    private void autoLockLowerRange(Integer currentRange) {
        Integer lowerRange = new Integer(currentRange.intValue()-200);
        if(altitudeMap.containsKey(lowerRange)) {
            rangeLock(lowerRange, true);
        }
    }

    public boolean isRangeLocked(Integer currentRange) {
        return lockedRanges.contains(currentRange);
    }

    public Map.Entry<Float, Float> addSpeedData(Integer range, float speed, float bearing) {
        autoLockLowerRange(range);
        Map.Entry<Float, Float> medianEntry = null;
        SortedMap<Float, Float> actualAltMap = altitudeMap.get(range);

        if (!isRangeLocked(range) && actualAltMap != null) {
            actualAltMap.put(new Float(speed), new Float(bearing));
        }
        int medianIndex = (actualAltMap.size())/2;
        Set mapEntrySet = actualAltMap.entrySet();
        Iterator mapIterator = mapEntrySet.iterator();
        int counter = 0;
        while (mapIterator.hasNext() && (counter <= medianIndex))
        {
            Map.Entry<Float, Float> m = (Map.Entry<Float, Float>)mapIterator.next();
            if (counter >= medianIndex) {
                medianEntry = m;
                break;
            }
            counter++;
        }
        return medianEntry;
    }

}
