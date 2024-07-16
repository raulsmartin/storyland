package me.raulsmartin.storyland.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.raulsmartin.storyland.R;

@SuppressLint("MissingPermission")
public class BluetoothHandler {
    private static final String TAG = "BluetoothHandler";
    private static final String SERVICE_NAME = "Storyland";
    private static final UUID SERVICE_UUID = UUID.fromString("bbdf2fb5-5ae2-4201-8975-4c5c38240828");

    private static BluetoothHandler instance;

    private final BluetoothAdapter bluetoothAdapter;
    private final List<Handler> handlers; // handler that gets info from Bluetooth service
    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    public static final int REQUEST_ENABLE_BT = 1001;
    public static final int REQUEST_DISCOVERABLE_BT = 1002;
    public static final int REQUEST_PERMISSIONS_BT = 1003;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null && !device.getName().isEmpty()) {
                    for (Handler handler : handlers) {
                        handler.obtainMessage(BluetoothMessage.DEVICE_FOUND.ordinal(), device).sendToTarget();
                    }
                }
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                int msg = scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE ?
                        BluetoothMessage.DISCOVERABLE_ENABLED.ordinal() :
                        BluetoothMessage.DISCOVERABLE_DISABLED.ordinal();

                for (Handler handler : handlers) {
                    handler.obtainMessage(msg).sendToTarget();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                for (Handler handler : handlers) {
                    handler.obtainMessage(BluetoothMessage.DISCOVERY_FINISHED.ordinal()).sendToTarget();
                }
            }
        }
    };

    private BluetoothHandler(@NonNull Activity activity) {
        handlers = new ArrayList<>();

        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) || bluetoothAdapter == null) {
            Toast.makeText(activity, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            activity.finish();
            return;
        }

        requestBluetoothPermissions(activity);
    }

    public static BluetoothHandler getInstance(@NonNull Activity activity) {
        if (instance == null) {
            instance = new BluetoothHandler(activity);
        }

        return instance;
    }

    private synchronized void registerHandler(@NonNull Handler handler) {
        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    private synchronized void unregisterHandler(@NonNull Handler handler) {
        handlers.remove(handler);
    }

    public synchronized void startListening() {
        Log.d(TAG, "startListening");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public synchronized void connect(@NonNull BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        for (Handler handler : handlers) {
            handler.obtainMessage(BluetoothMessage.DEVICE_CONNECTED.ordinal()).sendToTarget();
        }
    }

    public synchronized void stopAll() {
        Log.d(TAG, "stopAll");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
    }

    public String getDeviceName() {
        String name = bluetoothAdapter.getName();
        return name != null ? name : bluetoothAdapter.getAddress();
    }

    public synchronized void registerActivity(@NonNull Activity activity, @NonNull Handler handler) {
        registerHandler(handler);

        activity.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        activity.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        activity.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    public synchronized void unregisterActivity(@NonNull Activity activity, @NonNull Handler handler) {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        unregisterHandler(handler);

        activity.unregisterReceiver(receiver);
    }

    public boolean requestEnableBluetooth(@NonNull Activity activity) {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return true;
        }
        return false;
    }

    public void requestDiscoverable(@NonNull Activity activity) {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        activity.startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);
    }

    public void requestBluetoothPermissions(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSIONS_BT);
                }
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.BLUETOOTH, android.Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_PERMISSIONS_BT);
                }
            } else {
                if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS_BT);
                }
            }
        }
    }

    public void onBluetoothEnabled(@NonNull Activity activity, int requestCode, int resultCode) {
        if (requestCode == BluetoothHandler.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(activity, R.string.error_bluetooth_denied, Toast.LENGTH_LONG).show();
            }

            for (Handler handler : handlers) {
                handler.obtainMessage(BluetoothMessage.BLUETOOTH_ENABLED.ordinal()).sendToTarget();
            }
        }
    }

    public void onRequestPermissionsResult(@NonNull Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == BluetoothHandler.REQUEST_PERMISSIONS_BT) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])) {
                        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(activity);
                        dialogBuilder.setTitle(R.string.permissions_title);
                        dialogBuilder.setIcon(R.drawable.ic_bluetooth_24dp);
                        dialogBuilder.setMessage(R.string.permissions_description);
                        dialogBuilder.setPositiveButton(R.string.dialog_positive, (dialog, which) -> requestBluetoothPermissions(activity));
                        dialogBuilder.setNegativeButton(R.string.dialog_negative, (dialog, which) -> Toast.makeText(activity, R.string.error_permissions_denied, Toast.LENGTH_LONG).show());
                        dialogBuilder.show();
                    } else {
                        Toast.makeText(activity, R.string.error_permissions_denied, Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                    return;
                }
            }
        }
    }

    public void write(byte[] bytes) {
        ConnectedThread r;

        synchronized (this) {
            if (connectedThread == null) return;
            r = connectedThread;
        }

        r.write(bytes);
    }

    public boolean toggleDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            return false;
        }

        bluetoothAdapter.startDiscovery();
        return true;
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket BEGIN acceptThread " + this);

            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    connected(socket, socket.getRemoteDevice());
                    cancel();
                    break;
                }
            }
            Log.i(TAG, "END acceptThread");
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectThread");

            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "Could not close the client socket", e2);
                }

                for (Handler handler : handlers) {
                    handler.obtainMessage(BluetoothMessage.CONNECTION_FAILED.ordinal(), mmDevice.getName()).sendToTarget();
                }
                return;
            }

            synchronized (BluetoothHandler.this) {
                connectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];
            int numBytes;

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    numBytes = mmInStream.read(buffer);

                    if (numBytes != 0) {
                        String message = new String(buffer, 0, numBytes);
                        for (Handler handler : handlers) {
                            handler.obtainMessage(BluetoothMessage.READ.ordinal(), message).sendToTarget();
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);

                    for (Handler handler : handlers) {
                        handler.obtainMessage(BluetoothMessage.CONNECTION_LOST.ordinal()).sendToTarget();
                    }
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                for (Handler handler : handlers) {
                    handler.obtainMessage(BluetoothMessage.WRITE.ordinal(), bytes).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                for (Handler handler : handlers) {
                    handler.obtainMessage(BluetoothMessage.DATA_NOT_SENT.ordinal()).sendToTarget();
                }
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connected socket", e);
            }
        }
    }
}
