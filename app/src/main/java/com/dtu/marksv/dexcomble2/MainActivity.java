/**
 * https://github.com/devunwired/accessory-samples/blob/master/BluetoothGatt/src/com/example/bluetoothgatt/MainActivity.java
 */

package com.dtu.marksv.dexcomble2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback {

    public static final String TAG = "BluetoothGatt";

    public static final String DEVICE_NAME = "DEXCOMRX";

    private BluetoothAdapter bluetoothAdapter;
    private SparseArray<BluetoothDevice> devices;
    private BluetoothGatt connectedGatt;
    private ProgressDialog progress;
    private boolean isConnected = false;

    private TextView nameTextView, addressTextView, deviceTypeTextView, bondTextView;
    private TextView connectedTextView, authTextView;
    private TextView sugarTextView;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);

        /* Initiate UI components */
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        addressTextView = (TextView) findViewById(R.id.addressTextView);
        connectedTextView = (TextView) findViewById(R.id.connectedTextView);
        authTextView = (TextView) findViewById(R.id.authTextView);
        deviceTypeTextView = (TextView) findViewById(R.id.deviceTypeTextView);
        bondTextView = (TextView) findViewById(R.id.bondTextView);
        sugarTextView = (TextView) findViewById(R.id.sugarTextView);

        /* Initiate bluetooth */
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();

        devices = new SparseArray<BluetoothDevice>();

        /* Setting Receivers */
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(bluetoothReceiver, filter);

        /* Initiate progress window */
        progress = new ProgressDialog(this);
        progress.setIndeterminate(true);
        progress.setCancelable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        /* Check if bluetooth is enabled. If not ask to enable it. */
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            /* Bluetooth is disabled */
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        /* Make sure dialog is hidden */
        progress.dismiss();
        /* Cancel any scans in progress */
        handler.removeCallbacks(stopRunnable);
        handler.removeCallbacks(startRunnable);
        bluetoothAdapter.stopLeScan(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        /* Disconnect from connected device */
        if (connectedGatt != null) {
            connectedGatt.disconnect();
            connectedGatt = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Add the "scan" option to the menu */
        getMenuInflater().inflate(R.menu.main, menu);
        /* Add any device elements we've discovered to the overflow menu */
        for (int i = 0; i < devices.size(); i++) {
            BluetoothDevice device = devices.valueAt(i);
            menu.add(0, devices.keyAt(i), 0, device.getName());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                setProgressBarIndeterminateVisibility(true);

                devices.clear();

                /**
                 * Disable bluetooth.
                 * Receiver listens on disable event and enables bluetooth again.
                 * When bluetooth is on, start scan.
                 */
//                startScan();
                bluetoothAdapter.disable();
                return true;
            default:
                /* Obtain the discovered device to connect with */
                BluetoothDevice device = devices.get(item.getItemId());
                Log.i(TAG, "Connecting to " + device.getName());
                setDeviceInfo(device);
                /*
                 * Make a connection with the device using the special LE-specific
                 * connectGatt() method, passing in a callback for GATT events
                 */
                //connectedGatt = device.connectGatt(this, false, gattCallback);
                startBLEService(device.getAddress());

                //Display progress UI
                setProgressMsg("Connecting to " + device.getName() + "...");
                return super.onOptionsItemSelected(item);
        }
    }

    private void startBLEService(String address) {
        Intent intent = new Intent(this, BLEService.class);
//        Bundle extras = intent.getExtras();
//        extras.putString("address", address);

        Bundle extras = new Bundle();
        extras.putString("address", address);
        intent.putExtras(extras);
        startService(intent);
    }

    private Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    private Runnable startRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };


    /*  */
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getExtras().getInt(BluetoothAdapter.EXTRA_STATE);
            if (state == BluetoothAdapter.STATE_OFF) {
                Log.d(TAG, "Restarting bluetooth adapter...");
                bluetoothAdapter.enable();
            } else if (state == BluetoothAdapter.STATE_ON) {
                Log.d(TAG, "Starting Scan...");
                startScan();
            }
        }
    };


    private void startScan() {
        bluetoothAdapter.startLeScan(this);

        handler.postDelayed(stopRunnable, 2500);
    }

    private void stopScan() {
        bluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.i(TAG, "New LE Device: " + device.getName() + " @ " + rssi);

        /* Only interested in specific devices. Filter on name. */
        if (DEVICE_NAME.equals(device.getName())) {
            devices.put(device.hashCode(), device);

            invalidateOptionsMenu();
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BLEService.MSG_PROGRESS)) {
                String progressText = intent.getStringExtra(BLEService.EXTRA_DATA);
                setProgressMsg(progressText);
            } else if (action.equals(BLEService.MSG_DISMISS)) {
                progress.hide();
            } else if (action.equals(BLEService.MSG_CLEAR)) {
                //clearDisplay();
            } else if (action.equals(BLEService.MSG_CONNECTION_STATUS)) {
                setConnectionStatus(intent.getStringExtra(BLEService.EXTRA_DATA));
            } else if (action.equals(BLEService.MSG_AUTH_STATUS)) {
                setAuthStatus(intent.getStringExtra(BLEService.EXTRA_DATA));
            }

        }
    };




    /**
     * Handler
     * Handles UI updates from callback background thread to UI thread.
     */
//    public static final int MSG_PROGRESS = 101;
//    public static final int MSG_DISMISS = 102;
//    public static final int MSG_CLEAR = 201;
//    public static final int MSG_CONNECTION_STATUS = 301;
//    public static final int MSG_AUTH_STATUS = 302;
//    public static final int MSG_STATUS = 401;
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            BluetoothGattCharacteristic characteristic;
//            switch (msg.what) {
//                case MSG_PROGRESS:
//                    progress.setMessage((String) msg.obj);
//                    if (!progress.isShowing()) {
//                        progress.show();
//                    }
//                    break;
//                case MSG_DISMISS:
//                    progress.hide();
//                    break;
//                case MSG_CLEAR:
//                    //clearDisplay();
//                    break;
//                case MSG_CONNECTION_STATUS:
//                    setConnectionStatus(isConnected);
//                    break;
//                case MSG_AUTH_STATUS:
//                    setAuthStatus((String) msg.obj);
//                    break;
//                case MSG_STATUS:
//                    characteristic = (BluetoothGattCharacteristic) msg.obj;
//                    if (characteristic.getValue() == null) {
//                        Log.w(TAG, "Error obtaining status value");
//                        return;
//                    }
//                    StringBuilder result = new StringBuilder();
//                    for (int i = 0; i < characteristic.getValue().length; i++) {
//                        result.append(characteristic.getValue()[i]);
//                    }
//
//                    Log.d(TAG, "Status Code: " + result.toString());
//                    break;
//            }
//        }
//    };

    /* Methods to update the UI */
    public void setNameTextView(String name) {
        nameTextView.setText(name);
    }

    public void setAddressTextView(String address) {
        addressTextView.setText(address);
    }

    public void setDeviceInfo(BluetoothDevice device) {
        nameTextView.setText(device.getName());
        addressTextView.setText(device.getAddress());

        String deviceType = "---";

        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                deviceType = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                deviceType = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                deviceType = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                deviceType = "DEVICE_TYPE_UNKNOWN";
                break;
        }

        deviceTypeTextView.setText(deviceType);

        String bond = "---";

        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                bond = "BOND_BONDED";
                break;
            case BluetoothDevice.BOND_BONDING:
                bond = "BOND_BONDING";
                break;
            case BluetoothDevice.BOND_NONE:
                bond = "BOND_NONE";
                break;
        }

        bondTextView.setText(bond);
    }

    public void setConnectionStatus(String status) {
        connectedTextView.setText(status);
    }

    public void setAuthStatus(String status) {
        authTextView.setText(status);
    }

    public void setSugarLevel(String level) {
        authTextView.setText(level);
    }

    public void clearDisplay() {
        nameTextView.setText("---");
        addressTextView.setText("---");

    }

    private void setProgressMsg(String progressText) {
        progress.setMessage(progressText);
        if (!progress.isShowing()) {
            progress.show();
        }
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.MSG_PROGRESS);
        intentFilter.addAction(BLEService.MSG_DISMISS);
        intentFilter.addAction(BLEService.MSG_CLEAR);
        intentFilter.addAction(BLEService.MSG_CONNECTION_STATUS);
        intentFilter.addAction(BLEService.MSG_AUTH_STATUS);
        return intentFilter;
    }
}


