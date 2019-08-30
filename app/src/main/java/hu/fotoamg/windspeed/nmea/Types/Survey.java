package hu.fotoamg.windspeed.nmea.Types;

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

    public void rangLock(Integer range, boolean status) {
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

    private void autoLockLowerRange(Integer currentRange) {
        Integer lowerRange = new Integer(currentRange.intValue()-200);
        if(altitudeMap.containsKey(lowerRange)) {
            lockedRanges.add(lowerRange);
        }
    }

    public Map.Entry<Float, Float> addSpeedData(Integer range, float speed, float bearing) {
        autoLockLowerRange(range);
        Map.Entry<Float, Float> medianEntry = null;
        SortedMap<Float, Float> actualAltMap = altitudeMap.get(range);

        if (!lockedRanges.contains(range) && actualAltMap != null) {
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
