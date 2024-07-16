package me.raulsmartin.storyland;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.raulsmartin.storyland.database.DatabaseHandler;

public class MainActivity extends AppCompatActivity {

    private Button createRoomButton, joinRoomButton, createStoryButton;
    private CreateStoryDialogFragment createStoryDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try (Cursor c = getContentResolver().query(StoryProvider.PLAYERS_CONTENT_URI, null, null, null, null)) {
            if (c == null || !c.moveToFirst()) {
                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
                return;
            }
        }
        createRoomButton = findViewById(R.id.createRoomButton);
        joinRoomButton = findViewById(R.id.joinRoomButton);
        createStoryButton = findViewById(R.id.createStoryButton);

        createStoryDialog = new CreateStoryDialogFragment();

        createRoomButton.setOnClickListener(view -> startActivity(new Intent(this, RoomActivity.class)));
        joinRoomButton.setOnClickListener(view -> startActivity(new Intent(this, RoomActivity.class).putExtra("joinRoom", true)));

        createStoryButton.setOnClickListener(this::onCreateStoryClick);
    }

    public void onCreateStoryClick(View view) {
        createStoryDialog.show(getSupportFragmentManager(), CreateStoryDialogFragment.TAG);
    }

    public static class CreateStoryDialogFragment extends DialogFragment {
        public static final String TAG = "CreateStoryDialogFragment";

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(requireActivity());
            View layout = requireActivity().getLayoutInflater().inflate(R.layout.dialog_create_story, null);
            dialogBuilder.setTitle(R.string.create_story_title);
            dialogBuilder.setIcon(R.drawable.ic_add_24dp);
            dialogBuilder.setView(layout);
            dialogBuilder.setPositiveButton(R.string.dialog_continue, null);
            dialogBuilder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> requireDialog().cancel());
            return dialogBuilder.create();
        }

        @Override
        public void onResume() {
            super.onResume();
            AlertDialog dialog = ((AlertDialog)requireDialog());
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                EditText storyTitle = dialog.findViewById(R.id.storyTitleField);
                if (storyTitle != null && storyTitle.getText() != null && !storyTitle.getText().toString().trim().isEmpty()) {
                    createStory(storyTitle);
                } else {
                    Toast.makeText(requireActivity(), R.string.error_field_title_empty, Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void createStory(EditText storyTitle) {
            String title = storyTitle.getText().toString().trim();
            try (Cursor c = requireActivity().getContentResolver().query(StoryProvider.STORIES_CONTENT_URI, null, DatabaseHandler.COLUMN_TITLE + " = ?", new String[]{title}, null)) {
                if (c != null && !c.moveToFirst()) {
                    ContentValues values = new ContentValues();
                    values.put(DatabaseHandler.COLUMN_TITLE, title);
                    values.put(DatabaseHandler.COLUMN_PLAYERS, 2);
                    Uri uri = requireActivity().getContentResolver().insert(StoryProvider.STORIES_CONTENT_URI, values);
                    if (uri != null) {
                        Intent intent = new Intent(requireActivity(), CreateStoryActivity.class);
                        intent.putExtra("storyId", ContentUris.parseId(uri));
                        intent.putExtra("storyTitle", storyTitle.getText().toString().trim());
                        intent.putExtra("storyPlayers", 2);
                        startActivity(intent);
                        requireDialog().dismiss();
                    } else {
                        Toast.makeText(requireActivity(), R.string.error_ocurred, Toast.LENGTH_LONG).show();
                    }
                } else {
                    storyTitle.setError(getString(R.string.title_in_use));
                    Toast.makeText(requireActivity(), R.string.error_story_already_exists, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}