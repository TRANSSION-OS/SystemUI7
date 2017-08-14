package com.sprd.systemui;

import android.app.AddonManager;
import android.content.Context;
import com.android.systemui.R;

import itel.transsion.settingslib.utils.TalpaUtils;

public class SystemUILockAppUtils {
    static SystemUILockAppUtils sInstance;

    public SystemUILockAppUtils() {
    }

    public static SystemUILockAppUtils getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        // Tapla:PeterHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUILockAppUtils) AddonManager.getDefault().getAddon(
                    R.string.feature_lock_app_systemui, SystemUILockAppUtils.class);
        }
        else {
            sInstance = new SystemUILockAppUtils();
        }
        // @}
        return sInstance;
    }

    public static SystemUILockAppUtils getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        // Tapla:PeterHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUILockAppUtils) new AddonManager(context).getAddon(
                    R.string.feature_lock_app_systemui, SystemUILockAppUtils.class);
        }
        else {
            sInstance = new SystemUILockAppUtils();
        }
        //@ }
        return sInstance;
    }

    public boolean isSupportLockApp() {
        // Talpa:PeterHuang modify default support lock app @{
        return true;
        // @}
    }
}
