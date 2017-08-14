package com.afmobi.boomplayer.aidl;
import com.afmobi.boomplayer.aidl.PitaoCallback;
interface PitaoService {
    String getPlayerInfo();
    void play();
    void next();
    void prev();
    void registerCallback(PitaoCallback cb);
    void unregisterCallback(PitaoCallback cb);
}