package com.kucode.oriound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;

public class FavouriteListActivity extends ActionBarActivity {

    private RelativeLayout progressBarWrapper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_list);

        // Setting volume control stream to media
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        progressBarWrapper = (RelativeLayout)findViewById(R.id.progressBarWrapper);
        progressBarWrapper.setVisibility(View.VISIBLE);

        // Read favourite paths
        new MyAsyncTask().execute("");
    }

    public void onPathClick(View view) {
        String destinationAddress = ((Button)view).getText().toString();
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("destination_address", destinationAddress);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public void onAddressLongClick(View view) {
        Intent serviceIntent = new Intent(this, TTSService.class);
        serviceIntent.putExtra("text", ((Button) view).getText());
        startService(serviceIntent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private class MyAsyncTask extends AsyncTask<String, Void, String> {

        public MyAsyncTask() {
            // TODO Auto-generated constructor stub
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            SharedPreferences sp = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, getApplicationContext().MODE_PRIVATE);
            String pin = sp.getString("user_pin",null);

            try{
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(Constants.APP_SERVICE_URL+"paths?pin="+pin);
                HttpResponse response = httpclient.execute(httppost);
                result = EntityUtils.toString(response.getEntity());
            }
            catch(Exception e){
                Log.e("log_tag", "Error in http connection " + e.toString());
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONArray jArray = new JSONArray(result);
                String[] destinations = new String[jArray.length()];
                for(int i=0; i<jArray.length();i++){
                    destinations[i]=jArray.getJSONObject(i).getString("Destination");
                }
                generateButtons(destinations);
                progressBarWrapper.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e("log_tag", "Error Parsing Data " + e.toString());
            }
        }
    }

    public void generateButtons (String[] destinations) {
        // Generate path buttons
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int displayHeight = dm.heightPixels;

        LinearLayout ll = (LinearLayout)findViewById(R.id.favourite_buttons_layout);
        //LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (displayHeight/4)-2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (displayHeight/4));

        //float d = getApplicationContext().getResources().getDisplayMetrics().density;
        //lp.setMargins(0, 0, 0, 2);
        lp.setMargins(0, 0, 0, 0);

        Typeface tf = Typeface.createFromAsset(getAssets(), Constants.FONT_PATH);

        int i = 1;
        for(String destination : destinations) {
            Button pathButton = new Button(this);
            pathButton.setText(destination);
            //pathButton.setText(i + " " + destination);
            if (i%4 == 1) {
                pathButton.setBackgroundResource(R.drawable.button_custom_1);
                pathButton.setTextColor(getResources().getColor(R.color.txt_color));
            }
            else if (i%4 == 2){
                pathButton.setBackgroundResource(R.drawable.button_custom_2);
                pathButton.setTextColor(getResources().getColor(R.color.txt_color_2));
            }
            else if (i%4 == 3) {
                pathButton.setBackgroundResource(R.drawable.button_custom_3);
                pathButton.setTextColor(getResources().getColor(R.color.txt_color));
            }
            else {
                pathButton.setBackgroundResource(R.drawable.button_custom_4);
                pathButton.setTextColor(getResources().getColor(R.color.txt_color_2));
            }

            i++;

            pathButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.small_font_size));
            pathButton.setTypeface(tf);
            pathButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPathClick(v);
                }
            });
            pathButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    onAddressLongClick(v);
                    return true;
                }
            });
            ll.addView(pathButton, lp);
        }
    }
}