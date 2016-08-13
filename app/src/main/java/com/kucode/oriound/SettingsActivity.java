package com.kucode.oriound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class SettingsActivity extends ActionBarActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setting volume control stream to media
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Typeface tf = Typeface.createFromAsset(getAssets(), Constants.FONT_PATH);

        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, getApplicationContext().MODE_PRIVATE);
        String pin = sp.getString("user_pin",null);
        ((TextView)findViewById(R.id.pin_output)).setText("PIN: " + pin);
        ((TextView)findViewById(R.id.pin_output)).setTypeface(tf);
        ((TextView)findViewById(R.id.pin_output)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onViewLongClick(v);
                return true;
            }
        });
    }

    public void onViewLongClick(View view) {
        Intent serviceIntent = new Intent(this, TTSService.class);
        serviceIntent.putExtra("text", ((TextView) view).getText().toString().replaceAll(".(?!$)", "$0 "));
        startService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
