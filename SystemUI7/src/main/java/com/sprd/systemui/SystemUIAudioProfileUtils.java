package com.sprd.systemui;

import android.app.AddonManager;
import android.content.Context;

import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTile.Host;
import com.android.systemui.qs.tiles.IntentTile;
import com.android.systemui.R;

import itel.transsion.settingslib.utils.TalpaUtils;

public class SystemUIAudioProfileUtils {
    static SystemUIAudioProfileUtils sInstance;

    public SystemUIAudioProfileUtils() {
    }

    public static SystemUIAudioProfileUtils getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUIAudioProfileUtils) AddonManager.getDefault().getAddon(
                    R.string.feature_audioprofile_systemui, SystemUIAudioProfileUtils.class);
        }
        else {
            sInstance = new SystemUIAudioProfileUtils();
        }
        // @}
        return sInstance;
    }

    public static SystemUIAudioProfileUtils getInstance(Context context) {
        if (sInstance != null) {
            return sInstance;
        }
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            sInstance = (SystemUIAudioProfileUtils) new AddonManager(context).getAddon(
                    R.string.feature_audioprofile_systemui, SystemUIAudioProfileUtils.class);
        }
        else {
            sInstance = new SystemUIAudioProfileUtils();
        }
        // @}
        return sInstance;
    }

    public boolean isSupportAudioProfileTile() {
        return false;
    }

    public QSTile<?> createAudioProfileTile(Host host, Context context) {
        return IntentTile.create(host, "");
    }

}
