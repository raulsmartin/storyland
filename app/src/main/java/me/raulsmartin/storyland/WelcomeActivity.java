package me.raulsmartin.storyland;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.raulsmartin.storyland.database.DatabaseHandler;

public class WelcomeActivity extends AppCompatActivity {

    private TextToSpeech tts;

    private Button continueButton;
    private EditText nameField;
    private AutoCompleteTextView selectedVoiceDropdown;
    private Slider pitchSlider, rateSlider;

    private List<Voice> voices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        continueButton = findViewById(R.id.continueButton);
        nameField = findViewById(R.id.nameField);
        selectedVoiceDropdown = findViewById(R.id.selectVoiceDropdown);
        pitchSlider = findViewById(R.id.pitchSlider);
        rateSlider = findViewById(R.id.rateSlider);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                setupLanguageAndVoice();
            } else {
                Toast.makeText(this, R.string.error_no_engine_tts, Toast.LENGTH_LONG).show();
            }
        });

        pitchSlider.setEnabled(false);
        rateSlider.setEnabled(false);
        selectedVoiceDropdown.setEnabled(false);
        selectedVoiceDropdown.setText(getString(R.string.voice_by_default), false);

        pitchSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                tts.setPitch(slider.getValue());
                tts.speak(getString(R.string.player_voice_text), TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        rateSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                tts.setSpeechRate(slider.getValue());
                tts.speak(getString(R.string.player_voice_text), TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        selectedVoiceDropdown.setOnItemClickListener((parent, view, position, id) -> {
            tts.setVoice(voices.get(position));
            tts.speak(getString(R.string.player_voice_text), TextToSpeech.QUEUE_FLUSH, null, null);
        });

        nameField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                v.clearFocus();
            }
            return false;
        });

        continueButton.setOnClickListener(this::onContinueButtonClick);
    }

    private void setupLanguageAndVoice() {
        Locale desiredLocale = new Locale("es", "ES");
        voices = new ArrayList<>();

        if (tts.isLanguageAvailable(desiredLocale) != TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(desiredLocale);
            tts.setPitch(pitchSlider.getValue());
            tts.setSpeechRate(rateSlider.getValue());

            pitchSlider.setEnabled(true);
            rateSlider.setEnabled(true);

            for (Voice voice : tts.getVoices()) {
                if (desiredLocale.equals(voice.getLocale()) && !voice.isNetworkConnectionRequired()) {
                    voices.add(voice);
                }
            }

            if (!voices.isEmpty()) {
                tts.setVoice(voices.get(0));

                List<String> strVoices = new ArrayList<>();
                for (int i = 1; i <= voices.size(); i++) {
                    strVoices.add(getString(R.string.voice_no, i));
                }
                if (selectedVoiceDropdown instanceof MaterialAutoCompleteTextView) {
                    ((MaterialAutoCompleteTextView) selectedVoiceDropdown).setSimpleItems(strVoices.toArray(new String[0]));
                    selectedVoiceDropdown.setText(strVoices.get(0), false);
                    selectedVoiceDropdown.setEnabled(strVoices.size() > 1);
                }
            } else {
                tts.setVoice(tts.getDefaultVoice());
            }
        } else {
            Toast.makeText(this, R.string.error_no_spanish_tts, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    public void onContinueButtonClick(View view) {
        String name = nameField.getText().toString().trim();
        String voiceName = tts != null && tts.getVoice() != null ? tts.getVoice().getName() : null;
        float voicePitch = pitchSlider.getValue();
        float voiceSpeed = rateSlider.getValue();

        if (!name.isEmpty()) {
            ContentValues values = new ContentValues();
            values.put(DatabaseHandler.COLUMN_PLAYER_NAME, name);
            values.put(DatabaseHandler.COLUMN_VOICE_NAME, voiceName);
            values.put(DatabaseHandler.COLUMN_VOICE_PITCH, voicePitch);
            values.put(DatabaseHandler.COLUMN_VOICE_SPEED, voiceSpeed);
            if (getContentResolver().insert(StoryProvider.PLAYERS_CONTENT_URI, values) != null) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, R.string.error_ocurred, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, R.string.error_no_name, Toast.LENGTH_LONG).show();
        }
    }
}