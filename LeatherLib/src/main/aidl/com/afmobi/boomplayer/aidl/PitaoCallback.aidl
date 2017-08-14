// PitaoCallback.aidl
package com.afmobi.boomplayer.aidl;

interface PitaoCallback {
    void onStart(String playerInfo);
    void onPrepare(String playerInfo);
    void onProgress(int seconds);
    void onResume();
    void onTrackStopOrPause();
}
