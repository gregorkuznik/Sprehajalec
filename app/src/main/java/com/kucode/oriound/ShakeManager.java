package com.kucode.oriound;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeManager implements SensorEventListener {

    private static final float GRAVITY_THRESHOLD = 3F;
    private static final int MIN_ELAPSED_TIME = 500;

    private ShakeListener mListener;
    private long mTimestamp;

    public void setListener(ShakeListener listener) {
        this.mListener = listener;
    }

    public interface ShakeListener {

        void onShake();

    }

    @Override
    public void onSensorChanged(SensorEvent e) {

        if (mListener != null) {
            float x = e.values[0];
            float y = e.values[1];
            float z = e.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            // Gravity force is near 1 when no movement
            float gravityForce = (float)Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gravityForce > GRAVITY_THRESHOLD) {
                final long currentTime = System.currentTimeMillis();

                // Ignoring close shake events
                if (mTimestamp + MIN_ELAPSED_TIME > currentTime) {
                    return;
                }

                mTimestamp = currentTime;

                mListener.onShake();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
