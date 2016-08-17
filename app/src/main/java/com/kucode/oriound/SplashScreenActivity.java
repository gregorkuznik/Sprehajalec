package com.kucode.oriound;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class SplashScreenActivity extends ActionBarActivity {
    private final int DISPLAY_LENGTH = 1000;
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            finish();
            return;
        }

        // Setting volume control stream to media
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, this.MODE_PRIVATE);
        String pin = sp.getString("user_pin",null);
        if(pin!= null) {
            setPin("8PET93");
            /*
            SharedPreferences.Editor editor = sp.edit();
            editor.remove("user_pin");
            editor.commit();
            Toast.makeText(this, pin, Toast.LENGTH_SHORT);
            */
        }
        else {
            try {
                new InsertUserAsync().execute("").get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashScreenActivity.this, MenuActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right,
                        R.anim.slide_out_left);
                finish();
            }
        };
        handler.postDelayed(runnable,DISPLAY_LENGTH);
    }

    class InsertUserAsync extends AsyncTask<String, Integer, String> {

        public InsertUserAsync() {
        }

        @Override
        protected String doInBackground(String... params) {

            String result = "";
            InputStream is = null;

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(Constants.APP_SERVICE_URL+"createUser?username="+"app");
                HttpResponse response = httpclient.execute(httppost);
                result = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                Log.e("user_insert", "Error in http connection " + e.toString());
            }

            return result;
        }

        protected void onPostExecute(String result){
            try {
                JSONObject jObject = new JSONObject(result);
                String pin = jObject.getString("Pin");
                setPin(pin);
            } catch (Exception e) {
                Log.e("user_insert", "Error Parsing Data " + e.toString());
            }
        }
    }

    public void setPin (String pin) {
        SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("user_pin", pin);
        editor.commit();
    }

    public void onClick(View v) {
        handler.removeCallbacks(runnable);
        Intent intent = new Intent(SplashScreenActivity.this, MenuActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right,
                R.anim.slide_out_left);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.removeCallbacks(runnable);
    }
}
