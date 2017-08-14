package plugin.sprd.systemuifeatures.qstile;


import java.util.List;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.internal.telephony.TelephonyIntents;
import com.android.settingslib.net.DataUsageController;
import com.android.settingslib.net.DataUsageController.DataUsageInfo;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.tiles.CellularTile;
import com.android.systemui.qs.tiles.DataUsageDetailView;
import com.android.systemui.statusbar.policy.NetworkController;
//import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
//import com.android.systemui.statusbar.policy.NetworkController.MobileDataController.DataUsageInfo;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import com.android.systemui.statusbar.policy.TelephonyIconsEx;
import com.android.internal.logging.MetricsLogger;

import itel.transsion.settingslib.utils.LogUtil;
import itel.transsion.settingslib.utils.SIMHelper;

/** Quick settings tile: DataConnection **/
public class DataConnectionTile extends QSTile<QSTile.BooleanState> {
    // Talpa:PeterHuang Modified for use talpa net manager 代码从6.0移植
    // 7.0上是否我们的流量管理是否还存在待确认  comment 20170409@{
    // talpa@andy 2017/4/19 15:31 delete @{
/*    private static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));*/
    //
    /* private static final Intent CELLULAR_SETTINGS = new Intent("com.android.netmannager.datausage.action")
             .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);*/
    // @}
    // talpa@andy 2017/4/19 15:31 add @{
    private static final Intent SIM_SETTINGS = new Intent(Intent.ACTION_MAIN).setComponent(new
            ComponentName("com.android.settings","com.android.settings.Settings$SimSettingsActivity"));
    // @}
    // talpa@andy 2017/5/4 17:44 add @{
    private static final Intent CELLULAR_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
    // @}

    private final NetworkController mController;
    private final DataUsageController mDataController;
    private final CellularDetailAdapter mDetailAdapter;

    private final GlobalSetting mDataSetting;
    private TelephonyManager mTelephonyManager;
    private SubscriptionManager mSubscriptionManager;
    public static final int QS_DATACONNECTION = 411;
    public static final int QS_DATACONNECTION_DETAILS = 412;
    private boolean mListening;

    public DataConnectionTile(Host host) {
        super(host);
        mController = host.getNetworkController();
        mDataController = mController.getMobileDataController();
        mDetailAdapter = new CellularDetailAdapter();

        mTelephonyManager = TelephonyManager.from(mContext);
        mSubscriptionManager = SubscriptionManager.from(mContext);

        mDataSetting = new GlobalSetting(mContext, mHandler, Global.MOBILE_DATA) {
            @Override
            protected void handleValueChanged(int value) {
                mState.value = mTelephonyManager.getDataEnabled();
                handleRefreshState(value);
                mDetailAdapter.setMobileDataEnabled(mState.value);
            }
        };
    }

    @Override
    /*protected*/public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE);
            mContext.registerReceiver(mReceiver, filter);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
        mDataSetting.setListening(listening);
    }

    /* SPRD: Bug 594164 Add listening when back from Detail Setting. @{ */
    public void setDetailListening(boolean listening) {
        if (mListening == listening) return;
        refreshState();
    }
    /* @} */

    @Override
    public Intent getLongClickIntent() {
        /* SPRD: Bug 474780 CMCC version hidden data traffic interface. @{ */
        if (!mContext.getResources().getBoolean(R.bool.config_showDataUsageSummary)) {
            return new Intent();
        } else {
            return CELLULAR_SETTINGS;
        }
        /* @} */
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_data_connection_label);
    }

    @Override
    public int getMetricsCategory() {
        return QS_DATACONNECTION;
    }

    public boolean isAvailable() {
        return true;
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        if (!SIMHelper.isMobileSimInserted(mContext)) {
            return;
        }
        if (mDataController.isMobileDataSupported()) {
            if (isAirplaneModeOn()) {
                mHost.collapsePanels();
                Toast.makeText(mContext, R.string.toggle_data_error_airplane, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            // talpa@andy 2017/4/19 15:29 add @{
            if (!SIMHelper.isMobileSimEnable(mContext)){
                mHost.startActivityDismissingKeyguard(SIM_SETTINGS);
                return;
            }
            // @}
            if (isDefaultDataSimAvailable()) {
                boolean enabled = !mTelephonyManager.getDataEnabled();
                toggleDataConnectionToDesired(enabled);
                handleRefreshState(!mState.value);
            } else {
                // talpa@andy 2017/4/19 15:29 add @{
                mHost.startActivityDismissingKeyguard(SIM_SETTINGS);
                // @}
            }
        }
    }

    private void toggleDataConnectionToDesired(boolean enabled) {
        mState.value = enabled;
        mDataController.setMobileDataEnabled(enabled);
    }

   private boolean isDefaultDataSimAvailable() {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        int defaultDataPhoneId = SubscriptionManager.getSlotId(defaultDataSubId);
        boolean isDefaultDataSimReady = SubscriptionManager
                .getSimStateForSlotIdx(defaultDataPhoneId) == TelephonyManager.SIM_STATE_READY;
        boolean isDefaultDataValid = SubscriptionManager.isValidPhoneId(defaultDataPhoneId);
//        Log.d(TAG, "defaultDataSubId = " + defaultDataSubId + " isDefaultDataSimReady = "
//                + isDefaultDataSimReady + " isDefaultDataStandby = " + isDefaultDataValid);
        return isDefaultDataSimReady && isDefaultDataValid;
    }

   /* @Override
    protected void handleSecondaryClick() {
        Log.d(TAG, "handleSecondaryClick");
        if (mDataController.isMobileDataSupported() && isDefaultDataSimAvailable()) {
            showDetail(true);
        } else {
            mHost.startActivityDismissingKeyguard(CELLULAR_SETTINGS);
        }
    }*/

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean dataConnected = mTelephonyManager.getDataEnabled();
        state.value = dataConnected;
//      state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_data_connection_label);
//        Log.d(TAG, "dataConnected = " + dataConnected + "| isDefaultDataSimAvailable =" +
//                isDefaultDataSimAvailable() + "| isAirplaneModeOn = "+ isAirplaneModeOn());
        // talpa@andy 2017/5/5 22:27 modify @{
        if (dataConnected
                && isDefaultDataSimEnable() && !isAirplaneModeOn()) {
        // @}
            state.icon = ResourceIcon.get(R.drawable.itel_ic_qs_mobile_data_on/*talpa zhw ic_qs_mobile_data_on*/);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_data_on);
        } else {
            state.icon = ResourceIcon.get(R.drawable.itel_ic_qs_mobile_data_off/*ic_qs_mobile_data_off*/);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_data_off);
        }
        //talpa zhw add
        state.bottomIcon = ResourceIcon.get(R.drawable.itel_ic_qs_mobile_data_bottom);
        //talpa zhw add end
    }
    
    // talpa@andy 2017/5/5 22:27 add @{
    // /M: Change the label when default SIM isn't set @{
    public boolean isDefaultDataSimExist() {
        int[] subList = SubscriptionManager.from(mContext).getActiveSubscriptionIdList();
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
//        Log.d(TAG, "isDefaultDataSimExist, Default data sub id : " + defaultDataSubId);
        for (int subId : subList) {
            if (subId == defaultDataSubId) {
                return true;
            }
        }
        return false;
    }
    // @}
    // @}

    // talpa@andy 2017/5/16 15:54 fix bug：tfs#17499:【SIM卡】关闭sim卡后，
    // 移动数据网络仍显示为：开启状态 @{
    private boolean isDefaultDataSimEnable() {
        List<SubscriptionInfo> infos =
                SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
        int dataConnectionId = SubscriptionManager.getDefaultDataSubscriptionId();
        int dataSlotId = SubscriptionManager.getSlotId(dataConnectionId);
        boolean simEnable = SIMHelper.isSimEnable(infos, dataSlotId, mContext);
        return simEnable;
    }
    // @}

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_data_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_data_changed_off);
        }
    }

    private final class CellularDetailAdapter implements DetailAdapter {

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_cellular_detail_title);
        }

        @Override
        public int getMetricsCategory() {
            return QS_DATACONNECTION_DETAILS;
        }

        @Override
        public Boolean getToggleState() {
            return mDataController.isMobileDataSupported()
                    ? mDataController.isMobileDataEnabled()
                    : null;
        }

        @Override
        public Intent getSettingsIntent() {
            return SIM_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            toggleDataConnectionToDesired(state);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final DataUsageDetailView v = (DataUsageDetailView) (convertView != null
                    ? convertView
                    : LayoutInflater.from(mContext).inflate(R.layout.data_usage, parent, false));
            final DataUsageInfo info = mDataController.getDataUsageInfo();
            if (info == null) return v;
            v.bind(info);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            fireToggleStateChanged(enabled);
        }
    }

    public boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                refreshState();
            // talpa@andy 2017/5/16 15:58 fix bug:tfs#17499,cdn#10051@{
            } else if (intent.getAction().equals(TelephonyIntents.ACTION_SUBINFO_CONTENT_CHANGE)) {
                refreshState();
            }
            // @}
        }
    };
}
