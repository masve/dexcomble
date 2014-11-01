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
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Created by mark on 01/11/14.
 */
public class BLEService extends Service {

    private static final String TAG = "BLEService";

    public static final String MSG_PROGRESS = "101";
    public static final String MSG_DISMISS = "102";
    public static final String MSG_CLEAR = "201";
    public static final String MSG_CONNECTION_STATUS = "301";
    public static final String MSG_AUTH_STATUS = "302";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    public static final String DEVICE_NAME = "DEXCOMRX";
    public static final String EXTENDED_SN = "SM42390263000000";
    public static final String EXTENDED_SN2 = "SM42390264000000";
    public static byte[] AUTH_CODE = EXTENDED_SN2.getBytes(StandardCharsets.US_ASCII);

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

    /* BLE Stuff */
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private boolean isConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();

        initialize();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connect(intent.getStringExtra("address"));
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
                gatt.discoverServices();

                broadcastUpdate(MSG_PROGRESS, "Discovering Services...");
                broadcastUpdate(MSG_CONNECTION_STATUS, String.valueOf(isConnected));

            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                /*
                 * If at any point we disconnect, send a message to clear the weather values
                 * out of the UI
                 */
                broadcastUpdate(MSG_CLEAR);
                broadcastUpdate(MSG_DISMISS);
                broadcastUpdate(MSG_CONNECTION_STATUS, String.valueOf(isConnected));
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 * If there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
                close();
                isConnected = false;
                broadcastUpdate(MSG_CLEAR);
                broadcastUpdate(MSG_DISMISS);
                broadcastUpdate(MSG_CONNECTION_STATUS, String.valueOf(isConnected));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Services Discovered");
            broadcastUpdate(MSG_PROGRESS, "Securing Communication...");

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
                    broadcastUpdate(MSG_DISMISS);
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

                broadcastUpdate(MSG_AUTH_STATUS, statusStr);
            }

            if (characteristic.getUuid().equals(RECEIVER_ARRAY_CLIENT_CHAR)) {
                Log.d(TAG, "Reading result from server");
                for (int i = 0; i < characteristic.getValue().length; i++) {
                    Log.d(TAG, " - array[" + i + "] : " + characteristic.getValue()[i]);
                }
            }
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
                readCmdReponse(gatt);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(RECEIVER_STATUS_CHAR)) {
                Status(characteristic);
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
                case 0:
                    setNotify(gatt);
                    break;
                case 1:
                    authenticate(gatt);
                    break;
                case 2:
                    pingDevice(gatt);
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

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String msg) {
        final Intent intent = new Intent(action);
        Bundle extras = new Bundle();
        extras.putString(EXTRA_DATA, msg);
        intent.putExtras(extras);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);


        sendBroadcast(intent);
    }


    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to CMD Responses.
        if (characteristic.getUuid().equals(RECEIVER_ARRAY_CLIENT_CHAR)) {
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(RECEIVER_ARRAY_CLIENT_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }


    public boolean connect(String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
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
}

