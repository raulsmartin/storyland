package me.raulsmartin.storyland.bluetooth;

public enum BluetoothMessage {
    NONE, READ, WRITE, DEVICE_CONNECTED, DATA_NOT_SENT, BLUETOOTH_ENABLED, DEVICE_FOUND, DISCOVERY_FINISHED, DISCOVERABLE_ENABLED, DISCOVERABLE_DISABLED, CONNECTION_LOST, CONNECTION_FAILED;

    public static BluetoothMessage from(int ordinal) {
        if (ordinal < values().length) {
            return values()[ordinal];
        }

        return NONE;
    }
}
