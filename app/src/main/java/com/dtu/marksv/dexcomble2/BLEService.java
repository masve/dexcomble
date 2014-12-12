package com.dtu.marksv.dexcomble2;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.dtu.marksv.dexcomble2.BLEConstants.AuthStatus;
import com.dtu.marksv.dexcomble2.BLEConstants.Command;
import com.dtu.marksv.dexcomble2.BLEConstants.MSGCode;
import com.dtu.marksv.dexcomble2.BLEConstants.Receiver;
import com.dtu.marksv.dexcomble2.utils.Formatter;
import com.dtu.marksv.dexcomble2.utils.PacketManager;
import com.dtu.marksv.dexcomble2.utils.ParseEGV;

import java.nio.charset.StandardCharsets;

/**
 * Created by mark on 01/11/14.
 */
public class BLEService extends Service {

    private static final String TAG = "BLEService";
    private static final int AUTH_SLEEPTIME  = 10000;

    public static final String DEVICE_NAME =  "DEXCOMRX";
    public static final String EXTENDED_SN1 = "SM42390263000000";
    public static final String EXTENDED_SN2 = "SM42390264000000";
    private static final int EGV_UPDATE_INTERVAL = 1000 * 30; // 30 seconds
    public static byte[] AUTH_CODE = EXTENDED_SN2.getBytes(StandardCharsets.US_ASCII);

    Handler handler = new Handler();
    boolean isGettingEGV = false;
    


    @Override
    public void onCreate() {
        super.onCreate();

        stopEGVUpdater();
        
        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        bluetoothDeviceAddress = intent.getStringExtra("address");
        connect(bluetoothDeviceAddress);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                stopEGVUpdater();
                gatt.discoverServices();

                broadcastUpdate(MSGCode.PROGRESS.getValue(), "Discovering Services...");
                broadcastUpdate(MSGCode.CONNECTION_STATUS.getValue(), String.valueOf(isConnected));

            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                Log.i(TAG, "Disconnected");
                stopEGVUpdater();
                broadcastUpdate(MSGCode.CLEAR.getValue());
                broadcastUpdate(MSGCode.DISMISS.getValue());
                broadcastUpdate(MSGCode.CONNECTION_STATUS.getValue(), String.valueOf(isConnected));


                Log.i(TAG, "Trying to reconnect...");

                // try to reconnect
                connect(bluetoothDeviceAddress);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                Log.i(TAG, "Disconnected");
                stopEGVUpdater();
                gatt.disconnect();
                close();
                isConnected = false;
                broadcastUpdate(MSGCode.CLEAR.getValue());
                broadcastUpdate(MSGCode.DISMISS.getValue());
                broadcastUpdate(MSGCode.CONNECTION_STATUS.getValue(), String.valueOf(isConnected));


            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered");
            broadcastUpdate(MSGCode.PROGRESS.getValue(), "Securing Communication...");

            try {
                BluetoothGattCharacteristic authChar = gatt.getService(Receiver.SERVICE.getValue())
                        .getCharacteristic(Receiver.AUTH_CHAR.getValue());
            } catch (NullPointerException npe) {
                Log.e(TAG, "Services not discovered. Retrying...");
                gatt.discoverServices();
            }

            /*
             * Reset custom BLE State Machine and start from beginning.
             */
            resetStateMachine();
            stateMachine(gatt);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            /* If a read has been called on Receiver.STATUS_CHAR read authentication ack code  */
            if (characteristic.getUuid().equals(Receiver.STATUS_CHAR.getValue())) {
                String statusCode = new String(characteristic.getValue());
                String statusStr = "";

                if (statusCode.equals(AuthStatus.VALID.getValue())) {
                    statusStr = "VALID";
                    broadcastUpdate(MSGCode.DISMISS.getValue());
                } else if (statusCode.equals(AuthStatus.INVALID.getValue())) {
                    statusStr = "INVALID";
                } else if (statusCode.equals(AuthStatus.NOT_ENTERED.getValue())) {
                    statusStr = "CODE NOT ENTERED";
                }

                Log.i(TAG, "Authentication Status Response: " + statusStr);
                if (statusStr.equals("VALID")) {
                    advanceStateMachine();
                    stateMachine(gatt);
                }

                broadcastUpdate(MSGCode.AUTH_STATUS.getValue(), statusStr);
            }

            if (characteristic.getUuid().equals(Receiver.ARRAY_CLIENT_CHAR.getValue())) {
                Log.i(TAG, "Reading result from server");
                byte[] response = characteristic.getValue();
                for (int i = 0; i < response.length; i++) {
                    Log.i(TAG, " - array[" + i + "] : " + response[i]);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            /* Check authentication status, if auth char is written to */

            if (characteristic.getUuid().equals(Receiver.AUTH_CHAR.getValue())) {

                try {
                    Log.i(TAG, "Sleeping for " + AUTH_SLEEPTIME + " ms");
                    Thread.sleep(AUTH_SLEEPTIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                BluetoothGattCharacteristic status_char = gatt.getService(Receiver.SERVICE.getValue())
                        .getCharacteristic(Receiver.STATUS_CHAR.getValue());

                /* Read the authentication ack response */
                gatt.readCharacteristic(status_char);
            }
            if (characteristic.getUuid().equals(Receiver.ARRAY_SVR_CHAR.getValue())) {
                Log.i(TAG, "Command Written!");

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            byte[] data = characteristic.getValue();

            if (characteristic.getUuid().equals(Receiver.STATUS_CHAR.getValue())) {
                Status(characteristic);
            }
            if (characteristic.getUuid().equals(Receiver.HEARTBEAT_CHAR.getValue())) {
                Log.d(TAG, "HEARTBEAT");
            }
            if (characteristic.getUuid().equals(Receiver.ARRAY_CLIENT_CHAR.getValue())) {
                //Log.i(TAG, "Returned from Gatt Server: ");

                Log.i(TAG, Formatter.beautifulBytes(data));

                ResponseManager responseManager = ResponseManager.getInstance();
                responseManager.processResponse(data);

                if(responseManager.processResult() == null) {
                    // do nothing, not finished yet
                } else if(responseManager.processResult() == Command.GET_EGV_PAGE_RANGE) {
                    getLatestEGV(gatt, responseManager.getAccumulatedResponse());
                    responseManager.resetResponseBuffer();
                } else if(responseManager.processResult() == Command.GET_EGV_PAGE) {
                    String egv = String.valueOf(ParseEGV.getLatestEGV(responseManager.getAccumulatedResponse()));
                    Log.i(TAG, "LAST EGV VALUE: " + egv);
                    broadcastUpdate(MSGCode.EGV_Update.getValue(), egv);
                    isGettingEGV = false;
                    responseManager.resetResponseBuffer();
                }



                /*

                if response is a egv page range
                    then make request packet for egv read and send to svr

                if response is egv value
                    then publish value to UI
                 */



            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {


            if (descriptor.getUuid().equals(Receiver.ARRAY_CLIENT_CONFIG.getValue())) {
                Log.i(TAG, "Wrote to: " + Receiver.ARRAY_CLIENT_CONFIG.getValue().toString() + " Status: " + status);
                advanceStateMachine();
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


    };

    // Application Specific BLE Stuff

    private void getLatestEGV(BluetoothGatt gatt, byte[] pageRangeResponse) {
        Log.i(TAG, "Getting latest EGV value...");
        BluetoothGattCharacteristic characteristic = gatt.getService(Receiver.SERVICE.getValue())
                .getCharacteristic(Receiver.ARRAY_SVR_CHAR.getValue());

        byte[] egvCmd = PacketManager.getPacket(Command.GET_EGV_PAGE, pageRangeResponse);
        Log.i(TAG, "Sending EGV request command: ");
        Log.i(TAG, Formatter.beautifulBytes(egvCmd));

        ResponseManager responseManager = ResponseManager.getInstance();
        responseManager.setNextCommand(Command.GET_EGV_PAGE);

        characteristic.setValue(egvCmd);
        writeCharacteristic(characteristic);
    }

    private int state = 0;
    private boolean responseIsPageRange;

    private void resetStateMachine() {
        state = 0;
    }

    private void advanceStateMachine() {
        state++;
    }

    public void stateMachine(BluetoothGatt gatt) {
        switch (state) {
            case 0:
                authenticate(gatt);
                break;
            case 1:
                setIndicate(gatt);
                break;
            case 2:
                //getEGVPageRange(gatt);
                startEGVUpdater();
                break;
            default:
                Log.d(TAG, "stateMachine() default: " + state);
                break;
        }
    }

    private void setIndicate(BluetoothGatt gatt) {
        Log.i(TAG, "Setting Indicate for Command Client Responses");
        BluetoothGattCharacteristic characteristic = gatt.getService(Receiver.SERVICE.getValue())
                .getCharacteristic(Receiver.ARRAY_CLIENT_CHAR.getValue());

        setCharacteristicNotification(characteristic, true);
    }

    private void authenticate(BluetoothGatt gatt) {
            /* Writing authentication code */
        Log.i(TAG, "Authenticating...");
        BluetoothGattCharacteristic authChar = gatt.getService(Receiver.SERVICE.getValue())
                .getCharacteristic(Receiver.AUTH_CHAR.getValue());
        authChar.setValue(AUTH_CODE);
        writeCharacteristic(authChar);
    }

    private void pingDevice(BluetoothGatt gatt) {
        Log.i(TAG, "Pinging device...");
        BluetoothGattCharacteristic characteristic = gatt.getService(Receiver.SERVICE.getValue())
                .getCharacteristic(Receiver.ARRAY_SVR_CHAR.getValue());

        byte[] ping = PacketManager.getPacket(Command.PING, null);
        Log.i(TAG, "Pinging with: ");
        for (int i = 0; i < ping.length; i++) {
            Log.i(TAG, " - array[" + i + "] : " + ping[i]);
        }

        characteristic.setValue(ping);
        writeCharacteristic(characteristic);
    }

    private void getBattery(BluetoothGatt gatt) {

    }

    private void getEGV(BluetoothGatt gatt) {

    }

    private void getEGVPageRange(BluetoothGatt gatt) {
        Log.i(TAG, "Getting EGV page range...");
        BluetoothGattCharacteristic characteristic = gatt.getService(Receiver.SERVICE.getValue())
                .getCharacteristic(Receiver.ARRAY_SVR_CHAR.getValue());

        byte[] rangeCmd = PacketManager.getPacket(Command.GET_EGV_PAGE_RANGE, null);
        Log.i(TAG, "Sending EGV page range command: ");
        Log.i(TAG, Formatter.beautifulBytes(rangeCmd));

        ResponseManager responseManager = ResponseManager.getInstance();
        responseManager.setNextCommand(Command.GET_EGV_PAGE_RANGE);

        characteristic.setValue(rangeCmd);
        writeCharacteristic(characteristic);
    }

    // Broadcasting

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String msg) {
        final Intent intent = new Intent(action);
        Bundle extras = new Bundle();
        extras.putString(MSGCode.EXTRA_DATA.getValue(), msg);
        intent.putExtras(extras);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


        sendBroadcast(intent);
    }

    //LOGGING

    public void Status(BluetoothGattCharacteristic characteristic) {
        if (characteristic.getValue() == null) {
            Log.w(TAG, "Error obtaining status value");
            return;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < characteristic.getValue().length; i++) {
            result.append(characteristic.getValue()[i]);
        }

        Log.d(TAG, "Status Code: " + result.toString());
    }




    // Standard BLE Stuff

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private boolean isConnected = false;

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }



    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to CMD Responses.
        if (characteristic.getUuid().equals(Receiver.ARRAY_CLIENT_CHAR.getValue())) {
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(Receiver.ARRAY_CLIENT_CONFIG.getValue());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }


    public boolean connect(String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    // EGV UPDATER

    private Runnable egvUpdater = new Runnable() {
        @Override
        public void run() {

            if (!isGettingEGV) {
                Log.i(TAG, "Starting new EGV Procedure...");
                isGettingEGV = true;
                getEGVPageRange(bluetoothGatt);
            } else {
                Log.i(TAG, "Getting-EGV-Procedure already in progress");
            }

            handler.postDelayed(this, EGV_UPDATE_INTERVAL);
        }
    };

    private void startEGVUpdater() {
        egvUpdater.run();

    }

    private void stopEGVUpdater() {
        handler.removeCallbacks(egvUpdater);
        isGettingEGV = false;
    }

}

