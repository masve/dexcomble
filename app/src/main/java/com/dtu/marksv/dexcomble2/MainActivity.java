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
    public static final String EXTENDED_SN = "SM42390263000000";
    public static byte[] AUTH_CODE = EXTENDED_SN.getBytes(StandardCharsets.US_ASCII);

    /* SERVICE: Device Information  */
    public static final UUID INFO_SERVICE = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    public static final UUID INFO_MODEL_NUMBER_CHAR = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    public static final UUID INFO_HARDWARE_REVISION_CHAR = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    public static final UUID INFO_FIRMWARE_REVISION_CHAR = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    public static final UUID INFO_MANUFACTURER_NAME_CHAR = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");

    /* SERVICE: TX Power  */
    public static final UUID TX_POWER_SERVICE = UUID.fromString("00001804-0000-1000-8000-00805F9B34FB");
    public static final UUID TX_POWER_LEVEL_CHAR = UUID.fromString("00002A07-0000-1000-8000-00805F9B34FB");

    /* SERVICE: Gen4RcvService  */
    public static final UUID RECEIVER_SERVICE = UUID.fromString("F0ACA0B1-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID RECEIVER_AUTH_CHAR = UUID.fromString("F0ACACAC-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID RECEIVER_STATUS_CHAR = UUID.fromString("F0ACB0CD-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID RECEIVER_HEARTBEAT_CHAR = UUID.fromString("F0AC2B18-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID RECEIVER_ARRAY_SVR_CHAR = UUID.fromString("F0ACB20A-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID RECEIVER_ARRAY_CLIENT_CHAR = UUID.fromString("F0ACB20B-EBFA-F96F-28DA-076C35A521DB");
    public static final UUID RECEIVER_SMARTPHONE_CMD_CHAR = UUID.fromString("F0ACB0CC-EBFA-F96F-28DA-076C35A521DB");

    /* SERVICE: ShareTestService  */

    /* DESCRIPTORS */
    public static final UUID RECEIVER_STATUS_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID RECEIVER_ARRAY_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final String STATUS_VALID = "1";
    public static final String STATUS_INVALID = "0";
    public static final String STATUS_NOT_ENTERED = "X";

    private BluetoothAdapter bluetoothAdapter;
    private SparseArray<BluetoothDevice> devices;
    private BluetoothGatt connectedGatt;
    private ProgressDialog progress;
    private boolean isConnected = false;

    private TextView nameTextView, addressTextView, deviceTypeTextView, bondTextView;
    private TextView connectedTextView, authTextView;
    private TextView sugarTextView;

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
                connectedGatt = device.connectGatt(this, false, gattCallback);
                //Display progress UI
                handler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to " + device.getName() + "..."));
                return super.onOptionsItemSelected(item);
        }
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

    /* Callback for GATT */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + " -> " + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                isConnected = true;
                /*
                 * Once successfully connected, we must next discover all the services on the
                 * device before we can read and write their characteristics.
                 */
                gatt.discoverServices();

                handler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering Services..."));
                handler.sendEmptyMessage(MSG_CONNECTION_STATUS);

            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                handler.sendEmptyMessage(MSG_CLEAR);
                handler.sendEmptyMessage(MSG_DISMISS);
                handler.sendEmptyMessage(MSG_CONNECTION_STATUS);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
                gatt.close();
                gatt = null;
                isConnected = false;
                handler.sendEmptyMessage(MSG_CLEAR);
                handler.sendEmptyMessage(MSG_DISMISS);
                handler.sendEmptyMessage(MSG_CONNECTION_STATUS);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered");
            handler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Securing Communication..."));

            reset();
//            readNextChar(gatt);
//            setNextNotify(gatt);
            stateMachine(gatt);
//            BluetoothGattService service = gatt.getService(RECEIVER_SERVICE);
//            BluetoothGattCharacteristic characteristic = service.getCharacteristic(RECEIVER_STATUS_CHAR);
//
//            gatt.readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            
            /* If a read has been called on RECEIVER_STATUS_CHAR read authentication ack code  */
            if (characteristic.getUuid().equals(RECEIVER_STATUS_CHAR)) {
                String statusCode = new String(characteristic.getValue());
                String statusStr = "";

                if (statusCode.equals(STATUS_VALID)) {
                    statusStr = "VALID";
                    handler.sendEmptyMessage(MSG_DISMISS);
                } else if (statusCode.equals(STATUS_INVALID)) {
                    statusStr = "INVALID";
                } else if (statusCode.equals(STATUS_NOT_ENTERED)) {
                    statusStr = "CODE NOT ENTERED";
                }

                Log.d(TAG, "Authentication Status Response: " + statusStr);
                if (statusStr.equals("VALID")) {
                    advance();
                    stateMachine(gatt);
                }

                handler.sendMessage(Message.obtain(null, MSG_AUTH_STATUS, statusStr));
            }

            if (characteristic.getUuid().equals(RECEIVER_ARRAY_CLIENT_CHAR)) {
                Log.d(TAG, "Reading result from server");
                for (int i = 0; i < characteristic.getValue().length; i++) {
                    Log.d(TAG, " - array[" + i + "] : " + characteristic.getValue()[i]);
                }
            }


//            setNextNotify(gatt);
//            setNextNotify(gatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            /* Check authentication status, if auth char is written to */
            if (characteristic.getUuid().equals(RECEIVER_AUTH_CHAR)) {
                BluetoothGattCharacteristic status_char = gatt.getService(RECEIVER_SERVICE)
                        .getCharacteristic(RECEIVER_STATUS_CHAR);

                /* Read the authentication ack response */
                gatt.readCharacteristic(status_char);
            }
            if (characteristic.getUuid().equals(RECEIVER_ARRAY_SVR_CHAR)) {
                Log.d(TAG, "Command Written!");
                advance();
                stateMachine(gatt);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(RECEIVER_STATUS_CHAR)) {
                handler.sendMessage(Message.obtain(null, MSG_STATUS, characteristic));
            }
            if (characteristic.getUuid().equals(RECEIVER_ARRAY_CLIENT_CHAR)) {
                Log.d(TAG, "Read some Characteristic Value: ");
                for (int i = 0; i < characteristic.getValue().length; i++) {
                    Log.d(TAG, " - array[" + i + "] : " + characteristic.getValue()[i]);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {


            if (descriptor.getUuid().equals(RECEIVER_ARRAY_CLIENT_CONFIG)) {
//                BluetoothGattCharacteristic characteristic = gatt.getService(RECEIVER_SERVICE)
//                        .getCharacteristic(RECEIVER_ARRAY_CLIENT_CHAR);
//                gatt.readCharacteristic(characteristic);
                Log.d(TAG, "Wrote to: " + RECEIVER_ARRAY_CLIENT_CONFIG.toString() + " Status: " + status);
                advance();
                stateMachine(gatt);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: " + rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }

        private int state = 0;

        private void reset() {
            state = 0;
        }

        private void advance() {
            state++;
        }

        public void stateMachine(BluetoothGatt gatt) {
            switch (state) {
                case 1:
                    authenticate(gatt);
                    break;
                case 0:
                    /* Set Notify for Responses */
                    setNotify(gatt);
                    break;
                case 2:
//                    readCharacteristic(gatt);
//                    getDescInfo(gatt);
                    pingDevice(gatt);
                case 3:
                    readCmdReponse(gatt);
                    break;
            }
        }

        private void setNotify(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (0) {
                case 0:
                    Log.d(TAG, "Setting Notify for Command Client Responses");
                    characteristic = gatt.getService(RECEIVER_SERVICE)
                            .getCharacteristic(RECEIVER_ARRAY_CLIENT_CHAR);
                    break;
                default:
                    return;
            }
            //Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true);
            //Enabled remote notifications
            BluetoothGattDescriptor desc = characteristic.getDescriptor(RECEIVER_ARRAY_CLIENT_CONFIG);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        private void authenticate(BluetoothGatt gatt) {
            /* Writing authentication code */
            Log.d(TAG, "Authenticating...");
            BluetoothGattCharacteristic authChar = gatt.getService(RECEIVER_SERVICE)
                    .getCharacteristic(RECEIVER_AUTH_CHAR);
            authChar.setValue(AUTH_CODE);
            gatt.writeCharacteristic(authChar);
        }

        private void readCmdReponse(BluetoothGatt gatt) {
            Log.d(TAG, "Reading command response...");
            BluetoothGattCharacteristic characteristic = gatt.getService(RECEIVER_SERVICE)
                    .getCharacteristic(RECEIVER_ARRAY_CLIENT_CHAR);

            gatt.readCharacteristic(characteristic);
        }

        private void pingDevice(BluetoothGatt gatt) {
            Log.d(TAG, "Pinging device...");
            BluetoothGattCharacteristic characteristic = gatt.getService(RECEIVER_SERVICE)
                    .getCharacteristic(RECEIVER_ARRAY_SVR_CHAR);

            byte[] ping = PacketManager.getPacket(PacketManager.PING);
            Log.d(TAG, "Pinging with: ");
            for (int i = 0; i < ping.length; i++) {
                Log.d(TAG, " - array[" + i + "] : " + ping[i]);
            }

            characteristic.setValue(ping);
            gatt.writeCharacteristic(characteristic);
        }

        private void getDescInfo(BluetoothGatt gatt) {
            List<BluetoothGattCharacteristic> listChars = gatt.getService(RECEIVER_SERVICE)
                    .getCharacteristics();
            for (BluetoothGattCharacteristic entry : listChars) {
                for (BluetoothGattDescriptor desc : entry.getDescriptors()) {
                    Log.d(TAG, "CHAR: " + entry.getUuid().toString() + " / DESC: " + desc.getUuid().toString());
                }
            }
        }

    };

    /**
     * Handler
     * Handles UI updates from callback background thread to UI thread.
     */
    private static final int MSG_PROGRESS = 101;
    private static final int MSG_DISMISS = 102;
    private static final int MSG_CLEAR = 201;
    private static final int MSG_CONNECTION_STATUS = 301;
    private static final int MSG_AUTH_STATUS = 302;
    private static final int MSG_STATUS = 401;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_PROGRESS:
                    progress.setMessage((String) msg.obj);
                    if (!progress.isShowing()) {
                        progress.show();
                    }
                    break;
                case MSG_DISMISS:
                    progress.hide();
                    break;
                case MSG_CLEAR:
                    //clearDisplay();
                    break;
                case MSG_CONNECTION_STATUS:
                    setConnectionStatus(isConnected);
                    break;
                case MSG_AUTH_STATUS:
                    setAuthStatus((String) msg.obj);
                    break;
                case MSG_STATUS:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.w(TAG, "Error obtaining status value");
                        return;
                    }
                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < characteristic.getValue().length; i++) {
                        result.append(characteristic.getValue()[i]);
                    }

                    Log.d(TAG, "Status Code: " + result.toString());
                    break;
            }
        }
    };

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

    public void setConnectionStatus(boolean isConnected) {
        connectedTextView.setText("" + isConnected);
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
}
