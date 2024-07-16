package me.raulsmartin.storyland;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;

import me.raulsmartin.storyland.adapter.BluetoothListAdapter;
import me.raulsmartin.storyland.adapter.PlayerListAdapter;
import me.raulsmartin.storyland.bluetooth.BluetoothHandler;
import me.raulsmartin.storyland.bluetooth.BluetoothMessage;
import me.raulsmartin.storyland.database.DatabaseHandler;
import me.raulsmartin.storyland.database.StoryItem;
import me.raulsmartin.storyland.database.StoryInfo;

public class RoomActivity extends AppCompatActivity {

    private BluetoothHandler bluetoothHandler;

    private AutoCompleteTextView selectStoryDropdown;

    private TextView infoText;
    private Button searchButton, readyButton;
    private RecyclerView recyclerView;
    private LinearProgressIndicator progressBar;

    private BluetoothListAdapter bluetoothListAdapter;
    private PlayerListAdapter playerListAdapter;

    private boolean joinRoom = false;
    private int lastMessageLength = -1;
    private long selectedStoryId = -1;

    private int sequencePos = 0;

    private boolean readyToStart = false;
    private boolean waitingToSend = false;
    private boolean finished = false;

    private ArrayList<String> storySequence = new ArrayList<>();
    private String storyInfo = null;
    private String playerName = null;
    private String otherPlayerName = null;

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (BluetoothMessage.from(msg.what)) {
                case WRITE:
                    break;
                case READ:
                    String readMessage = (String) msg.obj;
                    processMessage(readMessage);
                    break;
                case DEVICE_CONNECTED:
                    startSendingStoryInfo();
                    break;
                case DATA_NOT_SENT:
                    Toast.makeText(getApplicationContext(), R.string.error_data_not_sent, Toast.LENGTH_SHORT).show();
                    break;
                case DEVICE_FOUND:
                    bluetoothListAdapter.addItem((BluetoothDevice) msg.obj);
                    break;
                case DISCOVERY_FINISHED:
                    searchButton.setText(R.string.search_devices);
                    progressBar.setVisibility(View.GONE);
                    break;
                case BLUETOOTH_ENABLED:
                    if (!joinRoom) {
                        bluetoothHandler.startListening();
                    }
                    break;
                case DISCOVERABLE_ENABLED:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case DISCOVERABLE_DISABLED:
                    progressBar.setVisibility(View.GONE);
                    break;
                case CONNECTION_FAILED:
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_game_detected, msg.obj), Toast.LENGTH_LONG).show();
                    break;
                case CONNECTION_LOST:
                    Toast.makeText(getApplicationContext(), R.string.error_connection_lost, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    public void startSendingStoryInfo() {
        bluetoothHandler.write(("PLAY|" + playerName.length() + "|" + playerName).getBytes());
        if (!joinRoom) {
            if (readyToStart) {
                infoText.setText(R.string.sync_info_text);
                sendStoryInfo();
            } else {
                waitingToSend = true;
                infoText.setText(R.string.verify_info_text);
            }
        } else {
            infoText.setText(R.string.waiting_info_text);
        }
    }

    public void sendStoryInfo() {
        try (Cursor c = getContentResolver().query(StoryProvider.STORIES_CONTENT_URI, null, DatabaseHandler.COLUMN_TITLE + " = ?", new String[]{selectStoryDropdown.getText().toString()}, null)) {
            if (c != null && c.moveToFirst()) {
                StoryInfo info = StoryInfo.from(c);
                selectedStoryId = info.getId();
                storyInfo = info.prepareToSend();
                lastMessageLength = storyInfo.length();
                bluetoothHandler.write(("INFO|" + lastMessageLength + "|" + storyInfo).getBytes());
            } else {
                Toast.makeText(this, R.string.error_story_not_found, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void sendNextStorySequence() {
        if (storySequence.isEmpty()) {
            try (Cursor c = getContentResolver().query(StoryProvider.STORY_SEQUENCE_CONTENT_URI, null, DatabaseHandler.COLUMN_STORY_ID + " = ?", new String[]{String.valueOf(selectedStoryId)}, null)) {
                if (c != null && c.moveToFirst()) {
                    do {
                        StoryItem sequence = StoryItem.from(c);
                        storySequence.add(sequence.prepareToSend());
                    } while (c.moveToNext());
                } else {
                    Toast.makeText(this, R.string.error_story_not_found, Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (sequencePos < storySequence.size()) {
            lastMessageLength = storySequence.get(sequencePos).length();
            bluetoothHandler.write(("ITEM|" + lastMessageLength + "|" + storySequence.get(sequencePos)).getBytes());
            sequencePos++;
        } else {
            infoText.setText(R.string.starting_info_text);
            finished = true;
            checkInfoToStart();
        }
    }

    private void checkInfoToStart() {
        if (otherPlayerName != null && finished) {
            if (!joinRoom) {
                bluetoothHandler.write("START|0|".getBytes());
            }
            startGame();
        }
    }

    public void processMessage(String readMessage) {
        int pos = 0;
        char[] chars = readMessage.toCharArray();
        StringBuilder buffer = new StringBuilder();
        do {
            if (chars[pos] != '|') {
                buffer.append(chars[pos]);
                pos++;
            } else {
                int orPos = pos;
                char lastChar;
                do {
                    lastChar = chars[++pos];
                } while (lastChar != '|');

                int length = Integer.parseInt(readMessage.substring(orPos + 1, pos));
                String data = readMessage.substring(pos + 1, pos + length + 1);

                if (buffer.toString().equals("ITEM")) {
                    storySequence.add(data);
                    sendAck(data);
                } else if (buffer.toString().equals("INFO")) {
                    storyInfo = data;
                    sendAck(data);
                    infoText.setText(R.string.sync_info_text);
                } else if (buffer.toString().equals("PLAY")) {
                    otherPlayerName = data;
                    if (!joinRoom) {
                        playerListAdapter.addPlayer(otherPlayerName);
                    }
                    checkInfoToStart();
                } else if (buffer.toString().equals("START")) {
                    finished = true;
                    checkInfoToStart();
                } else if (buffer.toString().equals("ACK")) {
                    if (lastMessageLength == Integer.parseInt(data)){
                        sendNextStorySequence();
                    }
                }

                buffer = new StringBuilder();
                pos += length + 1;
            }
        } while (pos < readMessage.length());
    }

    private void sendAck(String data) {
        bluetoothHandler.write(("ACK|" + String.valueOf(data.length()).length() + "|" + data.length()).getBytes());
    }

    private void startGame() {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("playerName", playerName);
        intent.putExtra("playerNo", joinRoom ? 1 : 0);
        intent.putExtra("storyInfo", storyInfo);
        intent.putExtra("otherPlayerName", otherPlayerName);
        intent.putStringArrayListExtra("storySequence", storySequence);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothHandler = BluetoothHandler.getInstance(this);

        joinRoom = getIntent().getBooleanExtra("joinRoom", false);
        if (joinRoom) {
            prepareJoinRoom();
        } else {
            prepareCreateRoom();
        }

        try (Cursor c = getContentResolver().query(StoryProvider.PLAYERS_CONTENT_URI, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                playerName = c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_PLAYER_NAME));
                if (!joinRoom) {
                    playerListAdapter.addPlayer(getString(R.string.player_self, playerName));
                }
            }
        } catch (IllegalArgumentException exception) {
            finish();
            return;
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                bluetoothHandler.stopAll();
                finish();
            }
        });

        bluetoothHandler.registerActivity(this, handler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bluetoothHandler.unregisterActivity(this, handler);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bluetoothHandler.requestEnableBluetooth(this)) return;

        if (!joinRoom) {
            bluetoothHandler.startListening();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        bluetoothHandler.onBluetoothEnabled(this, requestCode, resultCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        bluetoothHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public void prepareJoinRoom() {
        setContentView(R.layout.activity_join_room);

        searchButton = findViewById(R.id.searchButton);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        infoText = findViewById(R.id.infoText);

        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(this, MaterialDividerItemDecoration.VERTICAL);
        divider.setLastItemDecorated(false);
        divider.setDividerInsetEnd(24);
        divider.setDividerInsetStart(24);

        bluetoothListAdapter = new BluetoothListAdapter(device -> bluetoothHandler.connect(device));
        recyclerView.addItemDecoration(divider);
        recyclerView.setAdapter(bluetoothListAdapter);

        searchButton.setOnClickListener(view -> {
            if (bluetoothHandler.toggleDiscovery()) {
                searchButton.setText(R.string.cancel_search);
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    public void prepareCreateRoom() {
        setContentView(R.layout.activity_create_room);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ArrayList<String> titles = new ArrayList<>();

        try (Cursor c = getContentResolver().query(StoryProvider.STORIES_CONTENT_URI, null, null, null, null)) {
            if (c != null) {
                if (c.moveToFirst()) {
                    try {
                        do {
                            titles.add(c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_TITLE)));
                        } while (c.moveToNext());
                    } catch (IllegalArgumentException exception) {
                        Toast.makeText(this, R.string.error_content, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                } else {
                    Toast.makeText(this, R.string.error_no_stories_detected, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, R.string.error_content, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        selectStoryDropdown = findViewById(R.id.selectStoryDropdown);
        readyButton = findViewById(R.id.readyButton);
        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.playerList);
        infoText = findViewById(R.id.infoText);

        infoText.setText(bluetoothHandler.getDeviceName());

        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(this, MaterialDividerItemDecoration.VERTICAL);
        divider.setLastItemDecorated(false);
        divider.setDividerInsetEnd(24);
        divider.setDividerInsetStart(24);

        playerListAdapter = new PlayerListAdapter();

        recyclerView.addItemDecoration(divider);
        recyclerView.setAdapter(playerListAdapter);

        if (selectStoryDropdown instanceof MaterialAutoCompleteTextView) {
            ((MaterialAutoCompleteTextView) selectStoryDropdown).setSimpleItems(titles.toArray(new String[0]));
            selectStoryDropdown.setText(titles.get(0), false);
        }

        readyButton.setOnClickListener(view -> {
            readyToStart = true;
            readyButton.setEnabled(false);
            selectStoryDropdown.setEnabled(false);
            if (waitingToSend) {
                startSendingStoryInfo();
            }
        });

        bluetoothHandler.requestDiscoverable(this);
    }
}