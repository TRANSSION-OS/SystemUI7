
package com.sprd.systemui;

import android.app.AddonManager;
import android.content.Context;
import com.android.systemui.R;

import itel.transsion.settingslib.utils.TalpaUtils;

public class SystemUIDynaNavigationBarUtils {
    static SystemUIDynaNavigationBarUtils sInstance;

    public SystemUIDynaNavigationBarUtils() {
    }

    public static SystemUIDynaNavigationBarUtils getInstance() {
        if (sInstance != null)
            return sInstance;
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUIDynaNavigationBarUtils) AddonManager.getDefault().getAddon(
                    R.string.feature_dynamic_navigationbat_systemui, SystemUIDynaNavigationBarUtils.class);
        }
        else {
            sInstance = new SystemUIDynaNavigationBarUtils();
        }
        // @}
        return sInstance;
    }

    public static SystemUIDynaNavigationBarUtils getInstance(Context context) {
        if (sInstance != null)
            return sInstance;
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUIDynaNavigationBarUtils) new AddonManager(context).getAddon(
                    R.string.feature_dynamic_navigationbat_systemui, SystemUIDynaNavigationBarUtils.class);
        }
        else {
            sInstance = new SystemUIDynaNavigationBarUtils();
        }
        // @}
        return sInstance;
    }

    public boolean isSupportDynaNaviBar() {
        return false;
    }
}
