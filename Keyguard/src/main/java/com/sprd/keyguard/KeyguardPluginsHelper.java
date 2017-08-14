package com.sprd.keyguard;

import android.app.AddonManager;
import android.content.Context;
import android.content.res.Resources;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.keyguard.R;
import com.mediatek.keyguard.TeleUtils;

import itel.transsion.settingslib.utils.TalpaUtils;

public class KeyguardPluginsHelper {
    static KeyguardPluginsHelper mInstance;
    public static final String TAG = "KeyguardPluginsHelper";
    /* SPRD: modify by BUG 540847 @{ */
    protected String RAT_4G = "4G";
    protected String RAT_3G = "3G";
    protected String RAT_2G = "2G";
    /* @} */
    public KeyguardPluginsHelper() {
    }

    public static KeyguardPluginsHelper getInstance() {
        if (mInstance != null)
            return mInstance;
        // Tapla DepingHuang Modified @{
        if (TalpaUtils.isSPRDPlatform()) {
            mInstance = (KeyguardPluginsHelper) AddonManager.getDefault()
                    .getAddon(R.string.plugin_keyguard_operator, KeyguardPluginsHelper.class);
        }
        else {
            mInstance = new KeyguardPluginsHelper();
        }
        // @}
        return mInstance;
    }

    public boolean makeEmergencyInvisible() {
        return false;
    }

    public CharSequence parseOperatorName(Context context, ServiceState state, CharSequence operator) {
        StringBuilder relOperatorName = new StringBuilder();
        String separator = context.getResources().getString(
                Resources.getSystem().getIdentifier("kg_text_message_separator", "string","android"));
        if (operator.toString().contains(separator)) {
            String[] operators = operator.toString().split(separator);
            for (int i = 0; i < operators.length; i++) {
                relOperatorName.append(appendRatToNetworkName(context,
                        state, operators[i])).append(separator);
            }
            return relOperatorName.toString().subSequence(0, relOperatorName.length()-2);
        } else {
            relOperatorName.append(appendRatToNetworkName(context,
                    state, operator));
            return relOperatorName.toString();
        }
    }
    public CharSequence appendRatToNetworkName(Context context, ServiceState state,
            CharSequence operator) {
        CharSequence operatorName = TeleUtils.updateOperator(operator.toString(),
                "operator");
        /* SPRD: modify by BUG 601753 @{ */
        String emergencyCall = Resources.getSystem()
                .getText(Resources.getSystem().getIdentifier("emergency_calls_only", "string","android")).toString();
        String noService = Resources.getSystem()
                .getText(Resources.getSystem().getIdentifier("lockscreen_carrier_default", "string","android")).toString();

        if (context == null || state == null
            || operatorName.equals(emergencyCall)
            || operatorName.equals(noService) ) return operatorName;
        /* @} */

        boolean boolAppendRat = context.getResources().getBoolean(
                R.bool.config_show_rat_append_operator);

        if (!boolAppendRat) {
            return operatorName;
        }

        /* SPRD: add for BUG 536878 @{ */
        if (operatorName != null && operatorName.toString().matches(".*[2-4]G$")) {
            return operatorName;
        }
        /* @} */

        if (state.getDataRegState() == ServiceState.STATE_IN_SERVICE
                || state.getVoiceRegState() == ServiceState.STATE_IN_SERVICE) {
            int voiceNetType = state.getVoiceNetworkType();
            int dataNetType = state.getDataNetworkType();
            int chosenNetType = ((dataNetType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                    ? voiceNetType : dataNetType);
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            int ratInt = tm.getNetworkClass(chosenNetType);
            String networktypeString = getNetworkTypeToString(context, ratInt, state);
            operatorName = new StringBuilder().append(operatorName).append(" ")
                    .append(networktypeString);
            return operatorName;
        }
        return operatorName;
    }

    protected String getNetworkTypeToString(Context context, int ratInt, ServiceState state) {
        String ratClassName = "";
        switch (ratInt) {
            case TelephonyManager.NETWORK_CLASS_2_G:
                boolean showRat2G = context.getResources().getBoolean(
                        R.bool.config_show_2g);
                Log.d(TAG, "showRat2G : " + showRat2G);
                ratClassName = showRat2G ? RAT_2G : "";
                break;
            case TelephonyManager.NETWORK_CLASS_3_G:
                Log.d(TAG, "showRat3G : " + show3G(state));
                ratClassName = show3G(state) ? RAT_3G : "";
                break;
            case TelephonyManager.NETWORK_CLASS_4_G:
                boolean showRat4g = context.getResources().getBoolean(
                        R.bool.config_show_4g);
                Log.d(TAG, "showRat4g : " + showRat4g);
                ratClassName = showRat4g ? RAT_4G : "";
                break;
        }
        return ratClassName;
    }

    /* SPRD: modify by BUG 522715 @{ */
    protected boolean show3G(ServiceState state) {
        return true;
    }
    /* @} */
}
