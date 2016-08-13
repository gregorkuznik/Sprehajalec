package com.kucode.oriound;

import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Setting volume control stream to media
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Typeface tp = Typeface.createFromAsset(getAssets(), Constants.FONT_PATH);
        ((Button)findViewById(R.id.favourite_button)).setTypeface(tp);
        ((Button)findViewById(R.id.favourite_button)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onButtonLongClick(v);
                return true;
            }
        });
        ((Button)findViewById(R.id.free_button)).setTypeface(tp);
        ((Button)findViewById(R.id.free_button)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onButtonLongClick(v);
                return true;
            }
        });
        ((Button)findViewById(R.id.settings_button)).setTypeface(tp);
        ((Button)findViewById(R.id.settings_button)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onButtonLongClick(v);
                return true;
            }
        });
    }

    public void onButtonClick(View view) {
        if(view.equals(findViewById(R.id.favourite_button))) {
            Intent intent = new Intent(this, FavouriteListActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,
                    R.anim.slide_out_left);
        }
        else if(view.equals(findViewById(R.id.free_button))) {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,
                    R.anim.slide_out_left);
        }
        else if(view.equals(findViewById(R.id.settings_button))){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right,
                    R.anim.slide_out_left);
        }
    }

    public void onButtonLongClick(View view) {
        Intent serviceIntent = new Intent(this, TTSService.class);
        serviceIntent.putExtra("text", ((Button) view).getText());
        startService(serviceIntent);
    }
}
