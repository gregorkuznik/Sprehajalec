package com.kucode.oriound;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTSService extends Service implements TextToSpeech.OnInitListener{

    private TextToSpeech textToSpeech;
    private String text;
    private boolean languageSlovene;

    @Override
    public void onCreate() {
        textToSpeech = new TextToSpeech(this, this);
        languageSlovene = false;
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("sl_SI"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = textToSpeech.setLanguage(Locale.ENGLISH);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTSService", "Languages not available.");
                }
            }
            else {
                languageSlovene = true;
            }
        } else {
            Log.e("TTSService", "Could not initialize TextToSpeech.");
        }
        speakOut();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
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

    // - Text transform for TTS -
    private String transformText(String text) {
        // TODO: Transform text for tts
        StringBuilder sb = new StringBuilder();
        text = text.toLowerCase();
        for(int i =0; i<text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case 'c' :
                    sb.append("ts");
                    break;
                case 'č' :
                    sb.append("ch");
                    break;
                case 'š' :
                    sb.append("sh");
                    break;
                case 'j' :
                    sb.append("y");
                    break;
                case 'ž' :
                    sb.append(c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private void speakOut() {
        if (!languageSlovene) {
            text = transformText(text);
        }
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
