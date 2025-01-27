package com.example.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String BLE_MAC_ADDRESS = "XX:XX:XX:XX:XX:XX"; // Masukkan MAC Address perangkat BLE Anda
    private static final UUID SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"); // Ganti dengan UUID Service Anda
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"); // Ganti dengan UUID Characteristic Anda
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private Button btnConnect, btnDisconnect;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic targetCharacteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnConnect = findViewById(R.id.button6);
        btnDisconnect = findViewById(R.id.button7);

        btnConnect.setOnClickListener(view -> checkAndRequestPermissions());
        btnDisconnect.setOnClickListener(view -> disconnectBLE());
    }

    //fungsi perizinan
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }


    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                connectToDevice();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                connectToDevice();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToDevice();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectToDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
                return; // Jangan lanjutkan jika izin tidak diberikan
            }
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(BLE_MAC_ADDRESS);
        if (device == null) {
            Toast.makeText(this, "Device not found. Check MAC Address", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothGatt = device.connectGatt(this, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.d("BLE", "Connected to device");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connected to device", Toast.LENGTH_SHORT).show());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            gatt.discoverServices(); // Aman untuk dijalankan jika izin sudah diberikan
                        } else {
                            Log.e("BLE", "Permission BLUETOOTH_CONNECT not granted");
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Bluetooth permissions are required to discover services", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        gatt.discoverServices(); // Tidak perlu pemeriksaan izin pada Android < 12
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("BLE", "Disconnected from device");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Disconnected from device", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = gatt.getService(SERVICE_UUID);
                    if (service != null) {
                        targetCharacteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                        if (targetCharacteristic != null) {
                            Log.d("BLE", "Characteristic found");
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Characteristic ready", Toast.LENGTH_SHORT).show());
                        } else {
                            Log.d("BLE", "Characteristic not found");
                        }
                    } else {
                        Log.d("BLE", "Service not found");
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void sendDataToBLEDevice() {
        if (targetCharacteristic == null || bluetoothGatt == null) {
            Toast.makeText(this, "Device not connected or characteristic not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!hasBluetoothPermission()) {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String data = "20";
            if (data.isEmpty()) {
                Toast.makeText(this, "Please enter a value to send", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ubah data ke byte array
            byte[] value = data.getBytes();
            targetCharacteristic.setValue(value);

            // Kirim data ke perangkat BLE
            bluetoothGatt.writeCharacteristic(targetCharacteristic);
            Toast.makeText(this, "Data sent: " + data, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void disconnectBLE() {
        if (!hasBluetoothPermission()) {
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            targetCharacteristic = null;
            Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBLE();
    }

}
