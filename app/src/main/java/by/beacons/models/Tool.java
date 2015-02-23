package by.beacons.models;


import com.parse.ParseClassName;
import com.parse.ParseObject;

@ParseClassName("Tools")
public class Tool extends ParseObject{

    public static final String NAME = "name";
    public static final String BEACON = "beacon";

    public Tool() {

    }

    public String getName() {
        return getString(NAME);
    }

    public Beacon getBeacon() {
        return (Beacon) getParseObject(BEACON);
    }


}
