package com.sprd.keyguard;

import android.app.AddonManager;
import android.widget.TextClock;
import android.content.Context;

import com.android.keyguard.R;

import itel.transsion.settingslib.utils.TalpaUtils;

public class KeyguardSupportAmPm {
    static KeyguardSupportAmPm sInstance;
    public static Context mContext;
    public static KeyguardSupportAmPm getInstance() {
        if (sInstance != null)
            return sInstance;
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (KeyguardSupportAmPm) AddonManager.getDefault().getAddon(
                    R.string.plugin_keyguard_ampm, KeyguardSupportAmPm.class);
        }
        else {
            sInstance = new KeyguardSupportAmPm();
        }
        // @}
        return sInstance;
    }

    public void setFormat12Hour(int textSize,TextClock clockView){
    }

    public boolean isEnabled(){
        return false;
    }
}
