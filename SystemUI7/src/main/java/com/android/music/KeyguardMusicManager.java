package com.android.music;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.os.UserHandle;
import com.android.music.ShakeListener.OnShakeListener;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;

public class KeyguardMusicManager implements OnShakeListener{
    private static String TAG = "KeyguardMusicManager";

    private static final Object MSYNC_OBJ = new Object();
    private static final String MUSIC_SERVICE = "com.android.music.MediaPlaybackService";
    private static final String PREVIOUS_ACTION = "com.android.music.musicservicecommand.previous";
    private static final String NEXT_ACTION = "com.android.music.musicservicecommand.next";

    private Context mContext = null;
    private ShakeListener mShakeListener = null;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager = null;

    private static final int DURATION_TIME = 1500;
    private long CURRENT_TIME = 0;
    private static KeyguardMusicManager mSingleKeyguardMusicManager = null;
    private AudioManager mAudioManager;

    private KeyguardMusicManager(Context context, StatusBarKeyguardViewManager sbvm) {
        Log.d(TAG, "init KeyguardMusicManager");
        if (mSingleKeyguardMusicManager == null) {
            synchronized (this) {
                this.mContext = context;
                this.mShakeListener = new ShakeListener(context);
                this.mShakeListener.setOnShakeListener(this);
                this.mStatusBarKeyguardViewManager = sbvm;
                this.mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            }
        }
    }

    public static KeyguardMusicManager getInstance(Context context, StatusBarKeyguardViewManager sbvm){
        if(mSingleKeyguardMusicManager == null){
            return new KeyguardMusicManager(context, sbvm);
        }else{
            return mSingleKeyguardMusicManager;
        }
    }

    public void start(){
        if(!isAllowShake()){
            return;
        }
        Log.d(TAG, "Start SPRD ShakeListener successful");
        mShakeListener.start();
    }

    public void stop() {
        if(!isAllowShake()){
            return;
        }
        Log.d(TAG, "on stop ...");
        mShakeListener.stop();
    }

    private boolean isAllowShake(){
        if(mShakeListener == null || mStatusBarKeyguardViewManager == null || mContext == null){
            Log.d(TAG, "Start SPRD ShakeListener fail");
            return false;
        }
        if(mAudioManager == null
                || (mAudioManager != null && !mAudioManager.isMusicActive())){
            Log.d(TAG, "Start SPRD ShakeListener fail, mAudioManager: " + mAudioManager);
            return false;
        }
        return true;
    }

    @Override
    public void onShakeLeft() {
        Log.d(TAG, "onShakeLeft ...");
        synchronized (MSYNC_OBJ) {
            if(!allowShake()){
                return;
            }
            Intent intent = new Intent(PREVIOUS_ACTION);
            mContext.sendBroadcast(intent);
        }
    }

    @Override
    public void onShakeRight() {
        Log.d(TAG, "onShakeRight ...");
        synchronized (MSYNC_OBJ) {
            if(!allowShake()){
                return;
            }
            Intent intent = new Intent(NEXT_ACTION);
            mContext.sendBroadcast(intent);
        }
    }

    private boolean allowShake(){
        if(!mStatusBarKeyguardViewManager.isShowing()){
            return false;
        }
        long curTimes = new Date().getTime();
        if(CURRENT_TIME > 0 && (curTimes - CURRENT_TIME) < DURATION_TIME){
            return false;
        }
        CURRENT_TIME = curTimes;
        return true;
    }
}
