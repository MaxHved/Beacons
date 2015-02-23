package by.beacons.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
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
                        idMessageInfo = R.string.bluetooth_on;
                    } else {
                        stopScanningBeacons();
                        idMessageInfo = R.string.bluetooth_off;
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
                    mBeaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startScanningBeacons() {
        mBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                for(by.beacons.models.Beacon beacon : mBeaconMap.values()) {
                    beacon.setDistance(null);
                }
                for (Beacon beacon : beacons) {
//                        Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().getDistance() + " meters away.");
//                    Log.i("Test", "Name: " + beacon.getBluetoothName() + " Address: " + beacon.getBluetoothAddress()
//                            + " ID1: " + beacon.getId1()
//                            + " ID2: " + beacon.getId2()
//                            + " ID3: " + beacon.getId3()
//                            + " Distance: " + beacon.getDistance()
//                            + " RSSI:" + beacon.getRssi()
//                            + " TxPower:" + beacon.getTxPower());
                    Log.v("Test", beacon.getId1().toHexString());
                    Log.v("Test", beacon.getId1().toUuidString());
                    by.beacons.models.Beacon modelBeacon =  mBeaconMap.get("20CAE8A0-A9CF-11E3-A5E2-0800200C9A66");
                    Log.v("Test", "" + (modelBeacon != null));
                    if(modelBeacon != null) {
                        modelBeacon.setDistance(beacon.getDistance());
                    }
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });

            }
        });
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
                    mBeaconManager.startRangingBeaconsInRegion(region);
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

    }
}
