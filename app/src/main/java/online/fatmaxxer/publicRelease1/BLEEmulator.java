package com.example.ble;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.UUID;

public class BLEEmulator extends Service {

    private static final String CHANNEL_ID = "BLEEmulatorChannel";
    private static final int NOTIF_ID = 1001;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    public static final UUID SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    public static final UUID CHARAC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic alpha1SmO2Charac;
    private boolean btServiceInitialized = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service BLE")
                .setContentText("Émulation du périphérique BLE active...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int serviceType = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            }
            startForeground(NOTIF_ID, notification, serviceType);
        } else {
            startForeground(NOTIF_ID, notification);
        }

        startServer();
        return START_STICKY;
    }

    @SuppressLint("MissingPermission")
    public void startServer() {
        if (!hasBluetoothPermissions()) {
            return;
        }

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }

        mBluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback);
        if (mBluetoothGattServer == null) {
            return;
        }

        registerService();
    }

    @SuppressLint("MissingPermission")
    private void registerService() {
        if (!hasBluetoothPermissions() || mBluetoothGattServer == null) {
            return;
        }

        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        alpha1SmO2Charac = new BluetoothGattCharacteristic(
                CHARAC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        service.addCharacteristic(alpha1SmO2Charac);

        mBluetoothGattServer.addService(service);
    }

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                btServiceInitialized = true;
                startAdvertising();
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void startAdvertising() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) {
            return;
        }

        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }
    };

    @SuppressLint("MissingPermission")
    public void updateAlpha1SmO2(BluetoothDevice device, byte[] payload) {
        if (!btServiceInitialized || alpha1SmO2Charac == null || !hasBluetoothPermissions()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mBluetoothGattServer.notifyCharacteristicChanged(device, alpha1SmO2Charac, false, payload);
        } else {
            alpha1SmO2Charac.setValue(payload);
            mBluetoothGattServer.notifyCharacteristicChanged(device, alpha1SmO2Charac, false);
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Emulator Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}