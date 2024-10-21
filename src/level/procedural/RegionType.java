package level.procedural;

import java.util.Collection;
import java.util.HashMap;

public class RegionType {
    private static final HashMap<String, RegionType> valueMap = new HashMap<>();

    public final String s;

    RegionType(String s) {
        this.s = s;
    }

    public static void add(String s) {
        valueMap.put(s, new RegionType(s));
    }

    public static Collection<RegionType> values() {
        return valueMap.values();
    }

    public static RegionType getRegionType(String s) {
        for (RegionType type : RegionType.values()) {
            if (type.s.equals(s))
                return type;
        }
        throw new IllegalArgumentException("Unknown region: " + s);
    }

    @Override
    public String toString() {
        return s;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RegionType r)
            return r.s.equals(s);
        return false;
    }
}
