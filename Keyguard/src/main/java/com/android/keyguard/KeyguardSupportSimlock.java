
package com.android.keyguard;

import android.app.AddonManager;
import android.content.Intent;
import android.util.Log;

import com.android.internal.telephony.IccCardConstantsEx.State;

import itel.transsion.settingslib.utils.TalpaUtils;

public class KeyguardSupportSimlock {
    static KeyguardSupportSimlock sInstance;
    public static final String TAG = "KeyguardSupportSimlock";

    public KeyguardSupportSimlock() {
    }

    public static KeyguardSupportSimlock getInstance() {
        if (sInstance != null)
            return sInstance;

        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (KeyguardSupportSimlock) AddonManager.getDefault().getAddon(
                    R.string.plugin_keyguard_simlock, KeyguardSupportSimlock.class);
        }
        else {
            sInstance = new KeyguardSupportSimlock();
        }
        // @}

        return sInstance;
    }

    public String getSimLockStatusString(int slotId){
        Log.d(TAG, "unimplement!" );
        return null;
    }

    public boolean isSimlockStatusChange (Intent intent) {
        Log.d(TAG, "unimplement!" );
        return false;
    }

    public State getSimStateEx(int subId) {
        Log.d(TAG, "unimplement!" );
        return State.UNKNOWN;
    }
}
