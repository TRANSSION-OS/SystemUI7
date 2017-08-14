
package com.sprd.systemui;

import android.content.Context;

import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.phone.ActivityStarter;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import android.view.WindowManager;
import com.android.systemui.statusbar.KeyguardIndicationController;

import itel.transsion.settingslib.utils.TalpaUtils;

public class SystemuiFeatureUtil {
    static SystemuiFeatureUtil sInstance;

    public SystemuiFeatureUtil() {
    }

    public static SystemuiFeatureUtil getInstance() {
        if (sInstance != null)
            return sInstance;
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
        /*sInstance = (SystemuiFeatureUtil) AddonManager.getDefault().getAddon(
                R.string.feature_systemui, SystemuiFeatureUtil.class);*/
            sInstance = new SystemuiFeatureUtil();
        }
        else {
            sInstance = new SystemuiFeatureUtil();
        }
        return sInstance;
    }

    public boolean launchAudioProfile(ActivityStarter activityStarter, Context context) {
        /* SPRD: Bug 591038 Can't launch camera by pressing power button twice with screen locked @{ */
        return false;
        /* @} */
    }

    public void changeCameraToProfile(KeyguardAffordanceView keyguardAffordanceView) {
    }

    public void changeHeadsUpbelowStatusBar(WindowManager.LayoutParams lp, PhoneStatusBar bar) {
    }

    public boolean changeProfileHint(KeyguardIndicationController keyguardIndicationController) {
        /* SPRD: Bug 591038 Can't launch camera by pressing power button twice with screen locked @{ */
        return false;
        /* @} */
    }

    public boolean isCMCC() {
        return false;
    }
}
