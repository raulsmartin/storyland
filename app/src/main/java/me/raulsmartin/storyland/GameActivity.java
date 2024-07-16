package me.raulsmartin.storyland;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import me.raulsmartin.storyland.bluetooth.BluetoothHandler;
import me.raulsmartin.storyland.bluetooth.BluetoothMessage;
import me.raulsmartin.storyland.database.ActionType;
import me.raulsmartin.storyland.database.DatabaseHandler;
import me.raulsmartin.storyland.database.StoryInfo;
import me.raulsmartin.storyland.database.StoryItem;
import me.raulsmartin.storyland.game.GameMessage;
import me.raulsmartin.storyland.game.GameThread;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private BluetoothHandler bluetoothHandler;

    private static final int FORCE_THRESHOLD = 350;
    private static final int TIME_THRESHOLD = 100;
    private static final int SHAKE_TIMEOUT = 500;
    private static final int SHAKE_DURATION = 1000;
    private static final int SHAKE_COUNT = 3;

    private float lastX=-1.0f, lastY=-1.0f, lastZ=-1.0f;
    private long lastTime;
    private int shakeCount = 0;
    private long lastShake;
    private long lastForce;

    private int playerNo;

    private TextToSpeech tts;

    private SensorManager sensorManager;
    private Sensor sensor;

    private StoryItem startSequence;

    private String storyTitle, playerName, otherPlayerName;

    private GameThread gameThread;

    private ImageView storyImage;
    private Button option1, option2;
    private TextView messageText, helpText;

    public boolean[] canStart = new boolean[]{ false, false };

    private final Handler blueHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (BluetoothMessage.from(msg.what)) {
                case READ:
                    String readMessage = (String) msg.obj;
                    processBluetoothMessage(readMessage);
                    break;
                case DATA_NOT_SENT:
                    Toast.makeText(getApplicationContext(), R.string.error_data_not_sent, Toast.LENGTH_SHORT).show();
                    break;
                case CONNECTION_LOST:
                    Toast.makeText(getApplicationContext(), R.string.error_connection_lost, Toast.LENGTH_LONG).show();
                    finish();
                    break;
            }
        }
    };

    private final Handler gameHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (GameMessage.from(msg.what)) {
                case TALK:
                    String message = (String) msg.obj;
                    message = message.replaceAll("%player1%", playerNo == 0 ? playerName : otherPlayerName).replaceAll("%player2%", playerNo == 0 ? otherPlayerName : playerName);
                    messageText.setText(message);
                    messageText.setTextColor(getResources().getColor(R.color.md_theme_secondary));
                    if (msg.arg1 == playerNo) {
                        messageText.setTextColor(getResources().getColor(R.color.md_theme_primary));
                        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED);
                    }
                    break;
                case NEXT:
                    StoryItem nextSequence = (StoryItem) msg.obj;
                    handleView(nextSequence);
                    if (msg.arg1 == playerNo) {
                        String nextSequenceId = String.valueOf(nextSequence.getSequence());
                        bluetoothHandler.write(("NEXT|" + nextSequenceId.length() + "|" + nextSequenceId).getBytes());
                    }
                    break;
                case UPDATE_IMAGE:
                    setStoryImage((int) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        bluetoothHandler = BluetoothHandler.getInstance(this);
        bluetoothHandler.registerActivity(this, blueHandler);
        List<StoryItem> storySequence = new ArrayList<>();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                setupLanguageAndVoice();
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String utteranceId) {
                        gameThread.finishedTalking();
                    }

                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onError(String utteranceId) {
                    }
                });
            } else {
                Toast.makeText(this, R.string.error_no_engine_tts, Toast.LENGTH_LONG).show();
            }
            canStart[0] = true;
            bluetoothHandler.write(("READY|0|").getBytes());
            checkGameThread();
        });

        playerName = getIntent().getStringExtra("playerName");
        playerNo = getIntent().getIntExtra("playerNo", 0);
        String storyInfo = getIntent().getStringExtra("storyInfo");
        StoryInfo info = storyInfo != null ? StoryInfo.from(storyInfo) : null;
        if (info == null) {
            Toast.makeText(this, R.string.error_received_story, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        storyTitle = info.getTitle();

        otherPlayerName = getIntent().getStringExtra("otherPlayerName");
        ArrayList<String> strStorySequence = getIntent().getStringArrayListExtra("storySequence");
        if (strStorySequence == null || strStorySequence.isEmpty()) {
            Toast.makeText(this, R.string.error_received_story, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        for (String storyItem : strStorySequence) {
            StoryItem item = StoryItem.from(storyItem);
            if (item != null) {
                storySequence.add(item);
                if (item.getAction().equals(ActionType.START)) {
                    startSequence = item;
                }
            }
        }

        if (startSequence == null) {
            Toast.makeText(this, R.string.error_received_story, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        gameThread = new GameThread(gameHandler, startSequence, storySequence);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                bluetoothHandler.stopAll();
                finish();
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor == null) {
            Toast.makeText(this, R.string.error_no_accelerometer, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        storyImage = findViewById(R.id.storyImage);
        messageText = findViewById(R.id.messageText);
        helpText = findViewById(R.id.helpText);
        option1 = findViewById(R.id.option1Button);
        option2 = findViewById(R.id.option2Button);

        setStoryImage(startSequence.getImageId());

        option1.setOnClickListener(view -> gameThread.optionSelected(1));
        option2.setOnClickListener(view -> gameThread.optionSelected(2));

        option1.setVisibility(View.GONE);
        option2.setVisibility(View.GONE);

        messageText.setText(storyTitle);
        helpText.setText(R.string.two_player_text);
    }

    private void setupLanguageAndVoice() {
        Locale desiredLocale = new Locale("es", "ES");
        tts.setLanguage(desiredLocale);
        try (Cursor c = getContentResolver().query(StoryProvider.PLAYERS_CONTENT_URI, null, null, null, null)) {
            if (c!=null && c.moveToFirst()) {
                tts.setPitch(c.getFloat(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_VOICE_PITCH)));
                tts.setSpeechRate(c.getFloat(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_VOICE_SPEED)));
                String voiceName = c.getString(c.getColumnIndexOrThrow(DatabaseHandler.COLUMN_VOICE_NAME));
                if (voiceName != null) {
                    for (Voice voice : tts.getVoices()) {
                        if (voice.getName().equals(voiceName)) {
                            tts.setVoice(voice);
                            break;
                        }
                    }
                } else {
                    tts.setVoice(tts.getDefaultVoice());
                }
            }
        }
    }

    public void checkGameThread() {
        if (canStart[0] && canStart[1]) {
            gameThread.start();
        }
    }

    public void handleView(StoryItem nextSequence) {
        switch (nextSequence.getAction()) {
            case TALK:
            case CONTINUE:
            case WAIT:
                option1.setVisibility(View.GONE);
                option2.setVisibility(View.GONE);
                helpText.setVisibility(View.GONE);
                break;
            case SHAKE:
                option1.setVisibility(View.GONE);
                option2.setVisibility(View.GONE);
                helpText.setVisibility(View.VISIBLE);
                if (this.playerNo == nextSequence.getPlayer()) {
                    helpText.setText(R.string.shake_phone_text);
                } else {
                    helpText.setText(getString(R.string.wait_shake_text,  otherPlayerName));
                }
                break;
            case DECISION:
                if (this.playerNo == nextSequence.getPlayer()) {
                    option1.setVisibility(View.VISIBLE);
                    option2.setVisibility(View.VISIBLE);
                    helpText.setVisibility(View.GONE);
                    option1.setText(nextSequence.getOption1());
                    option2.setText(nextSequence.getOption2());
                } else {
                    option1.setVisibility(View.GONE);
                    option2.setVisibility(View.GONE);
                    helpText.setVisibility(View.VISIBLE);
                    helpText.setText(getString(R.string.wait_decision_text,  otherPlayerName));
                }
                break;
            case FINISH:
                messageText.setText(R.string.the_end_text);
                messageText.setTextColor(getResources().getColor(R.color.md_theme_onSurface));
                option1.setVisibility(View.GONE);
                option2.setVisibility(View.GONE);
                helpText.setVisibility(View.GONE);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        bluetoothHandler.unregisterActivity(this, blueHandler);
        gameThread.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - lastForce) > SHAKE_TIMEOUT) {
            shakeCount = 0;
        }

        if ((now - lastTime) > TIME_THRESHOLD) {
            long diff = now - lastTime;
            float speed = Math.abs(event.values[0] + event.values[1] + event.values[2] - lastX - lastY - lastZ) / diff * 10000;
            if (speed > FORCE_THRESHOLD) {
                if ((++shakeCount >= SHAKE_COUNT) && (now - lastShake > SHAKE_DURATION)) {
                    lastShake = now;
                    shakeCount = 0;
                    gameThread.hasShaken();
                }
                lastForce = now;
            }
            lastTime = now;
            lastX = event.values[0];
            lastY = event.values[1];
            lastZ = event.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public void setStoryImage(int imageId) {
        switch (imageId) {
            case 0:
                storyImage.setImageResource(R.drawable.ic_park);
                break;
            case 1:
                storyImage.setImageResource(R.drawable.ic_kids_talking);
                break;
            case 2:
                storyImage.setImageResource(R.drawable.ic_kid_and_bee);
                break;
            case 3:
                storyImage.setImageResource(R.drawable.ic_just_bee);
                break;
        }
    }

    public void processBluetoothMessage(String readMessage) {
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

                if (buffer.toString().equals("NEXT")) {
                    gameThread.continueGame(Integer.parseInt(data));
                } else if (buffer.toString().equals("READY")) {
                    canStart[1] = true;
                    checkGameThread();
                } else if (buffer.toString().equals("PLAY")) {
                    otherPlayerName = data;
                }

                buffer = new StringBuilder();
                pos += length + 1;
            }
        } while (pos < readMessage.length());
    }
}