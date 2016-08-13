package com.kucode.oriound;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSService extends Service implements TextToSpeech.OnInitListener{

    private TextToSpeech tts;
    private String text;

    @Override
    public void onCreate() {
        tts = new TextToSpeech(this, this);
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.ENGLISH);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTSService", "Language is not available.");
            }
            else {
            }
        } else {
            Log.e("TTSService", "Could not initialize TextToSpeech.");
        }
        speakOut();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        if(intent == null) {
            Log.e("TTSService", "Intent is null");
            return 0;
        }
        else {
            text = intent.getStringExtra("text");
            speakOut();
            return 1;
        }
    }

    private void speakOut() {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
