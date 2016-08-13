package com.kucode.oriound;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    protected ResultReceiver addressReceiver;
    private static final String TAG = "AddressService";

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        addressReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(
                Constants.LOCATION_DATA_EXTRA);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> matches = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (matches.isEmpty()) {
                Log.w(TAG, "Address not found");
                deliverResultToReceiver(Constants.FAILURE_RESULT, "Address not found");
            }
            else {
                deliverResultToReceiver(Constants.SUCCESS_RESULT, matches.get(0).getAddressLine(0));
            }
        } catch (IOException e) {
            // Catch network or other I/O problems.
            Log.e(TAG, e.getMessage());
            deliverResultToReceiver(Constants.FAILURE_RESULT, "IOException");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, illegalArgumentException.getMessage());
            deliverResultToReceiver(Constants.FAILURE_RESULT, "IllegalArgumentException");
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        addressReceiver.send(resultCode, bundle);
    }
}
