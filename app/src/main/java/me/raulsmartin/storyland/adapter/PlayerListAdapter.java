package me.raulsmartin.storyland.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import me.raulsmartin.storyland.R;

public class PlayerListAdapter extends RecyclerView.Adapter<PlayerListAdapter.ViewHolder> {
    private final List<String> playerList;

    public PlayerListAdapter() {
        this.playerList = new ArrayList<>();
    }

    @NonNull
    @Override
    public PlayerListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_list, parent, false);

        return new PlayerListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerListAdapter.ViewHolder holder, int position) {
        holder.updateBluetoothItem(playerList.get(position));
    }

    @Override
    public int getItemCount() {
        return playerList.size();
    }

    public void addPlayer(@NonNull String player) {
        if (!playerList.contains(player)) {
            playerList.add(player);
            notifyItemInserted(playerList.indexOf(player));
        }
    }

    public void removePlayer(@NonNull String player) {
        if (playerList.contains(player)) {
            int pos = playerList.indexOf(player);
            playerList.remove(player);
            notifyItemRemoved(pos);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private MaterialCardView mainView;

        public ViewHolder(View view) {
            super(view);
            if (view instanceof MaterialCardView) mainView = (MaterialCardView) view;
            nameText = view.findViewById(R.id.nameText);
        }

        public void updateBluetoothItem(@NonNull String name) {
            if (name.endsWith("(Tú)")) {
                mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_primaryContainer));
                nameText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onPrimaryContainer));
            } else {
                mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_secondaryContainer));
                nameText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onSecondaryContainer));
            }

            nameText.setText(name);
        }
    }
}
