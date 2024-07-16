package me.raulsmartin.storyland.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import me.raulsmartin.storyland.R;
import me.raulsmartin.storyland.StoryProvider;
import me.raulsmartin.storyland.database.ActionType;
import me.raulsmartin.storyland.database.StoryItem;

public class StoryListAdapter extends RecyclerView.Adapter<StoryListAdapter.ViewHolder> {

    private final List<StoryItem> storyItems;
    private final Context context;

    public StoryListAdapter(@NonNull Context context) {
        storyItems = new ArrayList<>();

        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_list, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.updateStoryItem(storyItems.get(position));
    }

    @Override
    public int getItemCount() {
        return storyItems.size();
    }

    public boolean addItem(@NonNull StoryItem storyItem) {
        if (!storyItems.contains(storyItem)) {
            storyItems.add(storyItem);
            int pos = storyItems.indexOf(storyItem);  //0
            storyItem.setSequence(pos);
            storyItem.setNextSequence(-1);
            if (pos > 0) {
                storyItems.get(pos - 1).setNextSequence(pos);
                context.getContentResolver().update(ContentUris.withAppendedId(StoryProvider.STORY_SEQUENCE_CONTENT_URI, storyItems.get(pos - 1).getId()), storyItems.get(pos - 1).getValues(), null, null);
                notifyItemChanged(pos - 1);
            }
            notifyItemInserted(pos);
            Uri uri = context.getContentResolver().insert(StoryProvider.STORY_SEQUENCE_CONTENT_URI, storyItem.getValues());
            if (uri != null) {
                storyItems.get(pos).setId(ContentUris.parseId(uri));
                return true;
            }
        }
        return false;
    }

    public void removeItem(@NonNull StoryItem storyItem) {
        if (storyItems.contains(storyItem)) {
            int pos = storyItems.indexOf(storyItem);
            storyItems.remove(storyItem);
            notifyItemRemoved(pos);
        }
    }

    public boolean isLastFinishItem() {
        return storyItems.get(storyItems.size() - 1).getAction().equals(ActionType.FINISH);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView actionIcon, dragIcon;
        TextView messageText;
        MaterialCardView mainView;

        public ViewHolder(@NonNull View view) {
            super(view);
            if (view instanceof MaterialCardView) mainView = (MaterialCardView) view;
            actionIcon = view.findViewById(R.id.actionIcon);
            dragIcon = view.findViewById(R.id.dragIcon);
            messageText = view.findViewById(R.id.messageText);
        }

        public void updateStoryItem(@NonNull StoryItem storyItem) {
            switch (storyItem.getPlayer()) {
                case 0:
                    mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_primaryContainer));
                    actionIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onPrimaryContainer));
                    dragIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onPrimaryContainer));
                    messageText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onPrimaryContainer));
                    break;
                case 1:
                    mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_secondaryContainer));
                    actionIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onSecondaryContainer));
                    dragIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onSecondaryContainer));
                    messageText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onSecondaryContainer));
                    break;
                case 2:
                    mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_tertiaryContainer));
                    actionIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onTertiaryContainer));
                    dragIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onTertiaryContainer));
                    messageText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onTertiaryContainer));
                    break;
                default:
                    mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_surfaceContainerHighest));
                    actionIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onSurface));
                    dragIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onSurface));
                    messageText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onSurface));
                    break;
            }

            messageText.setText(storyItem.getMessage());

            actionIcon.setVisibility(View.VISIBLE);
            switch (storyItem.getAction()) {
                case TALK:
                    actionIcon.setImageResource(R.drawable.ic_talk_24dp);
                    break;
                case SHAKE:
                    actionIcon.setImageResource(R.drawable.ic_shake_24dp);
                    break;
                case WAIT:
                    actionIcon.setImageResource(R.drawable.ic_wait_24dp);
                    messageText.setText("Esperar");
                    break;
                case START:
                    actionIcon.setImageResource(R.drawable.ic_start_24dp);
                    messageText.setText("COMIENZO");
                    break;
                case FINISH:
                    actionIcon.setImageResource(R.drawable.ic_finish_24dp);
                    messageText.setText("FINAL");
                    mainView.setCardBackgroundColor(mainView.getResources().getColor(R.color.md_theme_errorContainer));
                    actionIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onErrorContainer));
                    dragIcon.setColorFilter(mainView.getResources().getColor(R.color.md_theme_onErrorContainer));
                    messageText.setTextColor(mainView.getResources().getColor(R.color.md_theme_onErrorContainer));
                    break;
                default:
                    actionIcon.setVisibility(View.GONE);
                    break;
            }

            /*ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainView.getLayoutParams();
            params.setMargins(getPixels(24), getPixels(8), getPixels(16), getPixels(8));
            mainView.setLayoutParams(params);*/
        }

        private int getPixels(int dp) {
            return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    mainView.getContext().getResources().getDisplayMetrics()));
        }
    }
}
