package me.raulsmartin.storyland.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.raulsmartin.storyland.R;

public class BluetoothListAdapter extends RecyclerView.Adapter<BluetoothListAdapter.ViewHolder> {
    private final List<BluetoothDevice> bluetoothDevices;
    private final OnItemClickListener listener;

    public BluetoothListAdapter(OnItemClickListener listener) {
        this.bluetoothDevices = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth_list, parent, false);

        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.updateBluetoothItem(bluetoothDevices.get(position));
    }

    @Override
    public int getItemCount() {
        return bluetoothDevices.size();
    }

    public void addItem(@NonNull BluetoothDevice item) {
        if (!bluetoothDevices.contains(item)) {
            bluetoothDevices.add(item);
            notifyItemInserted(bluetoothDevices.indexOf(item));
        }
    }

    public void removeItem(@NonNull BluetoothDevice item) {
        if (bluetoothDevices.contains(item)) {
            int pos = bluetoothDevices.indexOf(item);
            bluetoothDevices.remove(item);
            notifyItemRemoved(pos);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private BluetoothDevice bluetoothItem;

        public ViewHolder(View view, OnItemClickListener listener) {
            super(view);

            nameText = view.findViewById(R.id.nameText);

            view.setOnClickListener(v -> {
                if (bluetoothItem != null)
                    listener.onItemClick(bluetoothItem);
            });
        }

        @SuppressLint("MissingPermission")
        public void updateBluetoothItem(@NonNull BluetoothDevice item) {
            bluetoothItem = item;

            nameText.setText(item.getName());
        }
    }
}
