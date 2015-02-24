package by.beacons.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQueryAdapter;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import by.beacons.R;
import by.beacons.adapters.ToolsAdapter;
import by.beacons.app.App;
import by.beacons.models.Tool;


public class MainActivity extends ActionBarActivity implements BeaconConsumer{

    private TextView mMessage;
    private View mRetry;
    private View mProgressBar;

    private ToolsAdapter mAdapter;
    private MenuItem mActionBluetooth;

    private List<Tool> mTools;

    private Map<String, by.beacons.models.Beacon> mBeaconMap = new ConcurrentHashMap<>();

    private BeaconManager mBeaconManager = App.getBeaconManager();

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessage = (TextView) findViewById(R.id.message);
        mRetry = findViewById(R.id.retry);
        mRetry.setOnClickListener(mClickListener);
        mProgressBar = findViewById(R.id.progress_bar);

        View emptyView = findViewById(R.id.empty_view);

        mAdapter = new ToolsAdapter(MainActivity.this);
        mAdapter.addOnQueryLoadListener(mOnQueryLoadListener);

        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setEmptyView(emptyView);
        listView.setAdapter(mAdapter);

        mBeaconManager.bind(MainActivity.this);

    }

    private ParseQueryAdapter.OnQueryLoadListener<Tool> mOnQueryLoadListener = new ParseQueryAdapter.OnQueryLoadListener<Tool>() {
        @Override
        public void onLoading() {
            mProgressBar.setVisibility(View.VISIBLE);
            mRetry.setVisibility(View.INVISIBLE);
            mMessage.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onLoaded(List<Tool> list, Exception e) {
            mTools = list;

            mProgressBar.setVisibility(View.INVISIBLE);
            if(e != null) {
                mRetry.setVisibility(View.VISIBLE);
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(R.string.check_internet);
            } else if(list.size() > 0) {
                for(Tool tool : list) {
                    mBeaconMap.put(tool.getBeacon().getUUID(), tool.getBeacon());
                }
                ParseObject.pinAllInBackground(list);
                if (mBeaconManager.isBound(MainActivity.this)) {
                    startScanningBeacons();
                    mAdapter.setShowExtandInfo(true);
                }
            } else {
                mRetry.setVisibility(View.VISIBLE);
                mMessage.setVisibility(View.VISIBLE);
                mMessage.setText(R.string.empty);
            }

        }
    };

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mAdapter.loadObjects();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mActionBluetooth = menu.findItem(R.id.action_bluetooth);
        updateUIActionBluetooth(App.getInst().isBluetoothEnable());
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBluetoothChangeState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBluetoothChangeState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScanningBeacons();
        mBeaconManager.unbind(MainActivity.this);
    }

    private BroadcastReceiver mBluetoothChangeState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if(state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
                    boolean enableBluetooth = state == BluetoothAdapter.STATE_ON;
                    int idMessageInfo;
                    if (enableBluetooth) {
                        startScanningBeacons();
                        mAdapter.setShowExtandInfo(true);
                        idMessageInfo = R.string.bluetooth_on;
                    } else {
                        idMessageInfo = R.string.bluetooth_off;
                        mAdapter.setShowExtandInfo(false);
                    }
                    showToastMessage(idMessageInfo);
                    updateUIActionBluetooth(enableBluetooth);
                }
            }
        }
    };

    private void updateUIActionBluetooth(boolean enable) {
        if(enable) {
            mActionBluetooth.setTitle(R.string.action_disable_bluetooth);
            mActionBluetooth.setIcon(R.drawable.ic_action_bluetooth_searching);
        } else {
            mActionBluetooth.setTitle(R.string.action_enable_bluetooth);
            mActionBluetooth.setIcon(R.drawable.ic_action_bluetooth);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bluetooth:
                int idMessageInfo;
                if(App.getInst().getStateBluetooth() == BluetoothAdapter.STATE_TURNING_OFF) {
                    idMessageInfo = R.string.whait_bluetooth_turning_off;
                    stopScanningBeacons();
                    mAdapter.setShowExtandInfo(false);
                } else if(App.getInst().getStateBluetooth() == BluetoothAdapter.STATE_TURNING_ON) {
                    idMessageInfo = R.string.whait_bluetooth_turning_on;
                } else {
                    boolean enable = !App.getInst().isBluetoothEnable();
                    App.getInst().setBluetooth(enable);
                    idMessageInfo = enable ?
                            R.string.whait_bluetooth_turning_on : R.string.whait_bluetooth_turning_off;
                }
                showToastMessage(idMessageInfo);
                return true;
            case R.id.action_settings:
                mAdapter.setShowExtandInfo(false); //TEST
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void stopScanningBeacons() {
        Region region;
        if(mTools != null && mTools.size() > 0) {
            by.beacons.models.Beacon beacon;
            for(Tool tool : mTools) {
                beacon = tool.getBeacon();
                region = new Region(
                        beacon.getBrand(),
                        Identifier.parse(beacon.getUUID()),
                        Identifier.fromInt(beacon.getMajorInt()),
                        Identifier.fromInt(beacon.getMinorInt()));
                try {
                    mBeaconManager.stopMonitoringBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    mBeaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mBeaconManager.setRangeNotifier(null);
        mBeaconManager.setMonitorNotifier(null);
    }

    private void startScanningBeacons() {
        mBeaconManager.setRangeNotifier(new CustomRangeNotifier(mBeaconMap));
        mBeaconManager.setMonitorNotifier(new CustomMonitorNotifier());
        Region region;
        if(mTools != null && mTools.size() > 0) {
            by.beacons.models.Beacon beacon;
            for(Tool tool : mTools) {
                beacon = tool.getBeacon();
                region = new Region(
                        beacon.getBrand(),
                        Identifier.parse(beacon.getUUID()),
                        Identifier.fromInt(beacon.getMajorInt()),
                        Identifier.fromInt(beacon.getMinorInt()));
                try {
                    mBeaconManager.startMonitoringBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void showToastMessage(int idMessage) {
        Toast.makeText(MainActivity.this, idMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBeaconServiceConnect() {
        startScanningBeacons();
    }

    private class CustomRangeNotifier implements RangeNotifier {

        private static final long PERIOD_UPDATE = 2000;

        private Map<String, by.beacons.models.Beacon> mBeaconMap;

        private long mPrevTime;

        public CustomRangeNotifier(Map<String, by.beacons.models.Beacon> beaconMap) {
            mBeaconMap = new HashMap<>();
            for(String key : beaconMap.keySet()) {
                mBeaconMap.put(key, new by.beacons.models.Beacon(beaconMap.get(key)));
            }
        }

        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
            long currentTime = System.currentTimeMillis();

            if(beacons.size() > 0) {
                for (Beacon beacon : beacons) {
                    by.beacons.models.Beacon modelBeacon =  mBeaconMap.get(beacon.getId1().toUuidString().toUpperCase());
                    if(modelBeacon != null) {
                        modelBeacon.setDistance(beacon.getDistance());
                    }
                }
            }
            if(currentTime - mPrevTime >= PERIOD_UPDATE) {
                mHandler.post(new NotifierBeaconDistance(mBeaconMap));
                mPrevTime = System.currentTimeMillis();
            }
        }
    }

    private class CustomMonitorNotifier implements MonitorNotifier {

        @Override
        public void didEnterRegion(Region region) {
            try {
                mBeaconManager.startRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mHandler.post(new NotifierBeaconState(region.getId1().toUuidString().toUpperCase(), true));
        }

        @Override
        public void didExitRegion(Region region) {
            try {
                mBeaconManager.stopRangingBeaconsInRegion(region);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mHandler.post(new NotifierBeaconState(region.getId1().toUuidString().toUpperCase(), false));
        }

        @Override
        public void didDetermineStateForRegion(int i, Region region) {

        }
    }

    private class NotifierBeaconDistance implements Runnable {

        private Map<String, by.beacons.models.Beacon> mBeaconMap;

        public NotifierBeaconDistance(Map<String, by.beacons.models.Beacon> beaconMap) {
            mBeaconMap = new HashMap<>();
            for(String key : beaconMap.keySet()) {
                mBeaconMap.put(key, new by.beacons.models.Beacon(beaconMap.get(key)));
            }
        }

        @Override
        public void run() {
            for(String key : mBeaconMap.keySet()) {
                by.beacons.models.Beacon beacon = MainActivity.this.mBeaconMap.get(key);
                if(beacon != null) {
                    beacon.setDistance(mBeaconMap.get(key).getDistance());
                }
            }
            mAdapter.notifyDataSetChanged();

        }
    }

    private class NotifierBeaconState implements Runnable {

        private String mUUID;
        private boolean mIsEnterRegion;

        public NotifierBeaconState(String uudi, boolean isEnterRegion) {
            mUUID = uudi.toUpperCase();
            mIsEnterRegion = isEnterRegion;
        }

        @Override
        public void run() {
            by.beacons.models.Beacon beacon = MainActivity.this.mBeaconMap.get(mUUID);
            if(beacon != null) {
                beacon.setEnterRegion(mIsEnterRegion);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
