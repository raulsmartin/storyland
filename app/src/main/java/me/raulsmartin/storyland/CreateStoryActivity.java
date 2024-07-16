package me.raulsmartin.storyland;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;

import me.raulsmartin.storyland.adapter.StoryListAdapter;
import me.raulsmartin.storyland.database.ActionType;
import me.raulsmartin.storyland.database.StoryItem;

public class CreateStoryActivity extends AppCompatActivity {

    private long storyId = -1;
    private String title = null;
    private int players = 2;

    private TextView subtitleText;
    private RecyclerView storyList;
    private NewStoryItemSheet newStoryItemSheet;
    private FloatingActionButton addButton;
    private Button doneButton;

    private StoryListAdapter storyListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_story);

        storyId = getIntent().getLongExtra("storyId", -1);
        title = getIntent().getStringExtra("storyTitle");
        players = getIntent().getIntExtra("storyPlayers", 2);

        if (storyId == -1) {
            finish();
            return;
        }

        subtitleText = findViewById(R.id.subtitleText);
        storyList = findViewById(R.id.storyList);
        addButton = findViewById(R.id.addButton);
        doneButton = findViewById(R.id.doneButton);

        if (players < 3) findViewById(R.id.player3).setVisibility(View.GONE);

        storyListAdapter = new StoryListAdapter(this);
        storyList.setAdapter(storyListAdapter);

        newStoryItemSheet = new NewStoryItemSheet(storyListAdapter, storyId, players);

        subtitleText.setText(title);

        addButton.setOnClickListener(view -> newStoryItemSheet.show(getSupportFragmentManager(), NewStoryItemSheet.TAG));
        doneButton.setOnClickListener(view -> {
            if (!storyListAdapter.isLastFinishItem()) {
                storyListAdapter.addItem(new StoryItem(storyId, ActionType.FINISH));
            }
            finish();
        });

        storyListAdapter.addItem(new StoryItem(storyId, ActionType.START));
    }

    public static class NewStoryItemSheet extends BottomSheetDialogFragment {
        public static final String TAG = "NewStoryItemSheet";

        private final StoryListAdapter adapter;
        private final long storyId;
        private final int numPlayers;

        public NewStoryItemSheet(StoryListAdapter adapter, long storyId, int numPlayers) {
            this.adapter = adapter;
            this.storyId = storyId;
            this.numPlayers = numPlayers;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.sheet_new_story_item, container, false);
        }

        @Override
        public void onResume() {
            super.onResume();
            ArrayList<String> action = new ArrayList<>();
            ArrayList<String> player = new ArrayList<>();

            EditText message = requireView().findViewById(R.id.messageField);

            AutoCompleteTextView actions = requireView().findViewById(R.id.actionDropdown);
            if (actions instanceof MaterialAutoCompleteTextView) {
                for (ActionType type : ActionType.values()) {
                    if (!type.equals(ActionType.START)){
                        action.add(getString(type.getName()));
                    }
                }
                ((MaterialAutoCompleteTextView) actions).setSimpleItems(action.toArray(new String[0]));
                if (actions.getText().toString().isEmpty()) {
                    actions.setText(action.get(0), false);
                }
            }
            AutoCompleteTextView players = requireView().findViewById(R.id.playerDropdown);
            if (players instanceof MaterialAutoCompleteTextView) {
                for (int i = 0; i < numPlayers; i++) {
                    player.add(getString(R.string.player_no, (i + 1)));
                }
                ((MaterialAutoCompleteTextView) players).setSimpleItems(player.toArray(new String[0]));
                if (players.getText().toString().isEmpty()) {
                    players.setText(player.get(0), false);
                }
            }

            requireView().findViewById(R.id.addButton).setOnClickListener(v -> {
                StoryItem item = new StoryItem(storyId, ActionType.from(action.indexOf(actions.getText().toString()) + 1), player.indexOf(players.getText().toString()), message.getText().toString());
                if (adapter.addItem(item)) {
                    message.setText("");
                    actions.setText(action.get(0), false);
                    players.setText(player.get(0), false);
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), R.string.error_add_sequence, Toast.LENGTH_LONG).show();
                }
            });

        }
    }
}