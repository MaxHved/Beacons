package by.beacons.models;


import com.parse.ParseClassName;
import com.parse.ParseObject;

@ParseClassName("Beacons")
public class Beacon extends ParseObject{

    public static final String UUID = "pUUID";
    public static final String MAJOR = "Major";
    public static final String MINOR = "Minor";
    public static final String BRAND = "Brand";
    private Double mDistance;
    private boolean mIsEnterRegion;

    public Beacon() {
    }

    public Beacon(Beacon beacon) {
        super();
        put(UUID, beacon.getUUID());
        put(MAJOR, beacon.getMajor());
        put(MINOR, beacon.getMinor());
        put(BRAND, beacon.getBrand());
        mDistance = beacon.getDistance();
    }

    public String getUUID() {
        return getString(UUID);
    }

    public String getMajor() {
        return getString(MAJOR);
    }

    public int getMajorInt() {
        return Integer.valueOf(getString(MAJOR));
    }

    public String getMinor() {
        return getString(MINOR);
    }

    public int getMinorInt() {
        return Integer.valueOf(getString(MINOR));
    }

    public String getBrand() {
        return getString(BRAND);
    }

    public Double getDistance() {
        return mDistance;
    }

    public void setDistance(Double distance) {
        mDistance = distance;
    }

    public boolean isEnterRegion() {
        return mIsEnterRegion;
    }

    public void setEnterRegion(boolean isEnterRegion) {
        mIsEnterRegion = isEnterRegion;
    }
}
