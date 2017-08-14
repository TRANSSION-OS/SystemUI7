package com.android.music;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SprdSensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ShakeListener implements SensorEventListener {
    private static String TAG = "ShakeListener";
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private OnShakeListener mOnShakeListener;
    private Context mContext;

    public ShakeListener(Context c) {
        mContext = c;
    }

    public void start() {
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(SprdSensor.TYPE_SPRDHUB_SHAKE);
        }
        if (mSensor != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        if(mSensorManager != null){
            mSensorManager.unregisterListener(this);
        }
    }

    public interface OnShakeListener {
        public void onShakeLeft();
        public void onShakeRight();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        mOnShakeListener = listener;
    }

    public void onSensorChanged(SensorEvent event) {
        if (mContext != null) {
            int direction = (int)event.values[2];
            Log.d(TAG, "onSensorChanged " + direction);
            if(direction == 1){
                mOnShakeListener.onShakeLeft();
            }else if(direction == 2){
                mOnShakeListener.onShakeRight();
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
