package by.beacons.app;


import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.parse.Parse;
import com.parse.ParseObject;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;

import by.beacons.models.Beacon;
import by.beacons.models.Tool;

public class App extends Application{

    private static final String PARSE_APPLICATION_ID = "gRgzsuChqGzk5E946LFnk9jUJXpu0a7waW6sEeoi";

    private static final String PARSE_CLIENT_KEY = "9Be7VprUzPte7TYPyf57qlMH3n6oUbAick8VF53l";

    private static BeaconManager sBeaconManager;

    private static App sApp;

    public static App getInst() {
        return sApp;
    }

    public static BeaconManager getBeaconManager() {
        return sBeaconManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApp = App.this;
        initParse();
        initBeaconManager();
    }

    private void initParse() {
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, PARSE_APPLICATION_ID, PARSE_CLIENT_KEY);

        ParseObject.registerSubclass(Tool.class);
        ParseObject.registerSubclass(Beacon.class);
    }

    private void initBeaconManager() {
        sBeaconManager = BeaconManager.getInstanceForApplication(App.this);
        sBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected() && netInfo.isAvailable();
    }

    public void enableBluetooth(Activity activity, int requestEnableBt) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestEnableBt);
        }
    }

    public boolean isBluetoothEnable() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public int getStateBluetooth() {
        return BluetoothAdapter.getDefaultAdapter().getState();
    }

    public boolean setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        boolean result = true;
        if (enable && !isEnabled) {
            result = bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
            result = bluetoothAdapter.disable();
        }
        return result;
    }
}
