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
    Map<Integer, RangeSpeedInfo> altitudeMap  = new HashMap<>();

    public Survey() {
        super();
        initMap();
    }

    public void initMap() {
        altitudeMap.clear();
        for(int i = 0; i<2100; i+=100) {
            altitudeMap.put(new Integer(i), new RangeSpeedInfo(new Integer(i)));
        }
    }

    public void rangeLock(Integer range, boolean status) {
        RangeSpeedInfo rangeObj = altitudeMap.get(range);
        if (rangeObj != null)
            rangeObj.setLocked(status);
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

    public RangeSpeedInfo getRangeSpeedInfo(Integer range) {
        return altitudeMap.get(range);
    }

    public Map.Entry<Float, Float> getMaxSpeedData(Integer range) {
        Map.Entry<Float, Float> result = null;
        RangeSpeedInfo rangeObj = getRangeSpeedInfo(range);
        if (rangeObj != null) {
            SortedMap<Float, Float> currAltMap = rangeObj.getSpeedDirValues();
            Float lastKey = currAltMap.lastKey();
            result = new AbstractMap.SimpleEntry<Float, Float>(lastKey, currAltMap.get(lastKey));
        }
        return result;
    }


    private void autoLockLowerRange(Integer currentRange) {
        Integer lowerRange = new Integer(currentRange.intValue()-200);
        if(altitudeMap.containsKey(lowerRange)) {
            rangeLock(lowerRange, true);
        }
    }

    public boolean isRangeLocked(Integer range) {
        boolean result = false;
        RangeSpeedInfo rangeObj = getRangeSpeedInfo(range);
        if (rangeObj != null) {
            result = rangeObj.isLocked();
        }
        return result;
    }

    public RangeSpeedInfo addSpeedData(Integer range, float speed, float bearing) {
        autoLockLowerRange(range);
        RangeSpeedInfo rangeObj = getRangeSpeedInfo(range);
        if (rangeObj != null) {
            if (!rangeObj.isLocked()) {
                rangeObj.insertSpeedData(speed, bearing);
            }
        }
        return rangeObj;
    }

}
