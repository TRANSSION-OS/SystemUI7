package com.sprd.systemui;

import com.android.systemui.R;
import android.app.AddonManager;
import android.content.Context;

import itel.transsion.settingslib.utils.TalpaUtils;

public class SystemUIClockUtil {
    static SystemUIClockUtil sInstance;
    public static Context context;

    public static SystemUIClockUtil getInstance() {
        if (sInstance != null) {
            return sInstance;
                    }
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUIClockUtil) AddonManager.getDefault().getAddon(
                    R.string.feature_clock_addon, SystemUIClockUtil.class);
        }
        else {
            sInstance = new SystemUIClockUtil();
        }
        // @}
        return sInstance;
    }

    public boolean isAllDay(boolean is24) {
        /// George:return true when set 24 hour format
        return is24;
    }
}
