/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settingslib.wifi.AccessPoint;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSDetailItems.Item;
import com.android.systemui.qs.QSIconView;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.HotspotController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.AccessPointController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;
import java.util.List;

import itel.transsion.settingslib.utils.ConfigService;

/** Quick settings tile: Wifi **/
public class WifiTile extends QSTile<QSTile.SignalState> {
    private static final Intent WIFI_SETTINGS = new Intent(Settings.ACTION_WIFI_SETTINGS);

    private final NetworkController mController;
    private final AccessPointController mWifiController;
    private final WifiDetailAdapter mDetailAdapter;
    private final QSTile.SignalState mStateBeforeClick = newTileState();

    protected final WifiSignalCallback mSignalCallback = new WifiSignalCallback();
    public static final String WIFI_CONNECT = "wifi_connect";
    private ConfigService mConfigService;

    public WifiTile(Host host) {
        super(host);
        mController = host.getNetworkController();
        mWifiController = mController.getAccessPointController();
        mDetailAdapter = new WifiDetailAdapter();
        mHotspotController = host.getHotspotController(); // add by lych
        mConfigService = new ConfigService(mContext);
    }

    @Override
    public SignalState newTileState() {
        return new SignalState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mController.addSignalCallback(mSignalCallback);
            mHotspotController.addCallback(mHotspotCallback); // add by lych
        } else {
            mController.removeSignalCallback(mSignalCallback);
            mHotspotController.removeCallback(mHotspotCallback); // add by lych
        }
    }

    @Override
    public void setDetailListening(boolean listening) {
        if (listening) {
            mWifiController.addAccessPointCallback(mDetailAdapter);
        } else {
            mWifiController.removeAccessPointCallback(mDetailAdapter);
        }
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public QSIconView createTileView(Context context) {
        return new SignalTileView(context);
    }

    @Override
    public Intent getLongClickIntent() {
        return WIFI_SETTINGS;
    }

    @Override
    protected void handleSecondaryClick() {
        // talpa@andy 2017/6/27 18:43 add:第一次（从未成功连接过WiFi）点击，
        // 实现直接进入设置-wifi界面 @{
        if (!mConfigService.getBoolean(WIFI_CONNECT, false) && !mState.value) {
            if (!mWifiController.canConfigWifi()) {
                mHost.startActivityDismissingKeyguard(new Intent(Settings.ACTION_WIFI_SETTINGS));
                return;
            }
            mController.setWifiEnabled(true);
            mState.value = true;
            mHost.startActivityDismissingKeyguard(new Intent(WIFI_SETTINGS));
            return;
        }
        // @}
        // Secondary clicks are header clicks, just toggle.
        mState.copyTo(mStateBeforeClick);
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        mController.setWifiEnabled(!mState.value);
        // talpa@andy 2017/6/1 19:20 add:添加过渡状态 @{
        if(!mState.value){
            mOpening = true;
            refreshState(mSignalCallback.mInfo);
        }
        // @}
    }

    @Override
    protected void handleClick() {
        // talpa@andy 2017/6/2 18:35 modify:快捷面板展开与收缩保持一致，统一实现开关按钮 @{
      /*  if (!mWifiController.canConfigWifi()) {
            mHost.startActivityDismissingKeyguard(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return;
        }
        if (!mState.value) {
            mController.setWifiEnabled(true);
            mState.value = true;
        }
        showDetail(true);*/
        handleSecondaryClick();
        // @}
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_wifi_label);
    }

    @Override
    protected void handleUpdateState(SignalState state, Object arg) {
        if (DEBUG) Log.d(TAG, "handleUpdateState arg=" + arg);
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = mSignalCallback.mInfo;
        }

        boolean wifiConnected = cb.enabled && (cb.wifiSignalIconId > 0) && (cb.enabledDesc != null);
        boolean wifiNotConnected = (cb.wifiSignalIconId > 0) && (cb.enabledDesc == null);
        boolean enabledChanging = state.value != cb.enabled;
        if (enabledChanging) {
            mDetailAdapter.setItemsVisible(cb.enabled);
            fireToggleStateChanged(cb.enabled);
        }
        state.value = cb.enabled;
        state.connected = wifiConnected;
        // talpa@andy 2017/6/2 10:24 delete @{
      /*state.activityIn = cb.enabled && cb.activityIn;
        state.activityOut = cb.enabled && cb.activityOut;*/
        // @}
        state.filter = true;
        final StringBuffer minimalContentDescription = new StringBuffer();
        final StringBuffer expandedContentDescription = new StringBuffer();
        final Resources r = mContext.getResources();
        state.switching = mOpening;
        // talpa@andy 2017/6/1 19:25 modify:添加过渡状态@{
        if (mOpening) {
            state.icon = ResourceIcon.get(R.drawable.itel_qs_progress_wifi_enable);
            state.label = r.getString(R.string.quick_settings_wifi_label);
        }  else if (state.value) {
            state.icon = ResourceIcon.get(R.drawable.itel_ic_qs_wifi_full_4);
            if (wifiConnected) {
                // talpa@andy 2017/6/27 18:54 add:wifi连接成功，更新状态@{
                mConfigService.setBoolean(WIFI_CONNECT, true);
                // @}
                state.label = removeDoubleQuotes(cb.enabledDesc);
            } else {
                state.label = r.getString(R.string.quick_settings_wifi_label);
            }
        } else {
            //modified by chenzhengjun start
            state.icon = ResourceIcon.get(R.drawable.ic_qs_wifi_itel_disabled);//ic_qs_wifi_disabled
            //modified by chenzhengjun end
            state.label = r.getString(R.string.quick_settings_wifi_label);
        }
        // @}
        minimalContentDescription.append(
                mContext.getString(R.string.quick_settings_wifi_label)).append(",");
        if (state.value) {
            expandedContentDescription.append(
                    r.getString(R.string.quick_settings_wifi_on_label)).append(",");
            if (wifiConnected) {
                minimalContentDescription.append(cb.wifiSignalContentDescription).append(",");
                minimalContentDescription.append(removeDoubleQuotes(cb.enabledDesc));
                expandedContentDescription.append(cb.wifiSignalContentDescription).append(",");
                expandedContentDescription.append(removeDoubleQuotes(cb.enabledDesc));
            }
        } else {
            expandedContentDescription.append(
                    r.getString(R.string.quick_settings_wifi_off_label));
        }
        state.minimalContentDescription =  minimalContentDescription;
        expandedContentDescription.append(",").append(
                r.getString(R.string.accessibility_quick_settings_open_settings, getTileLabel()));
        state.contentDescription = expandedContentDescription;
        CharSequence wifiName = state.label;
        if (state.connected) {
            wifiName = r.getString(R.string.accessibility_wifi_name, state.label);
        }
        state.dualLabelContentDescription = wifiName;
        state.expandedAccessibilityClassName = Button.class.getName();
        state.minimalAccessibilityClassName = Switch.class.getName();

        //talpa zhw add
        state.bottomIcon = ResourceIcon.get(R.drawable.ic_qs_wifi_itel_bottom);
        //talpa zhw add end
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_WIFI;
    }

    @Override
    protected boolean shouldAnnouncementBeDelayed() {
        return mStateBeforeClick.value == mState.value;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_wifi_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_wifi_changed_off);
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    private static String removeDoubleQuotes(String string) {
        if (string == null) return null;
        final int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"') && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    protected static final class CallbackInfo {
        boolean enabled;
        boolean connected;
        int wifiSignalIconId;
        String enabledDesc;
        boolean activityIn;
        boolean activityOut;
        String wifiSignalContentDescription;

        @Override
        public String toString() {
            return new StringBuilder("CallbackInfo[")
                    .append("enabled=").append(enabled)
                    .append(",connected=").append(connected)
                    .append(",wifiSignalIconId=").append(wifiSignalIconId)
                    .append(",enabledDesc=").append(enabledDesc)
                    .append(",activityIn=").append(activityIn)
                    .append(",activityOut=").append(activityOut)
                    .append(",wifiSignalContentDescription=").append(wifiSignalContentDescription)
                    .append(']').toString();
        }
    }

    protected final class WifiSignalCallback extends SignalCallbackAdapter {
        final CallbackInfo mInfo = new CallbackInfo();

        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                                      boolean activityIn, boolean activityOut, String description) {
            if (DEBUG) Log.d(TAG, "onWifiSignalChanged enabled=" + enabled);
            mInfo.enabled = enabled;
            // talpa@andy 2017/6/1 19:29 add:添加过渡状态 @{
            if (enabled) {
                mOpening = false;
            }
            // @}
            mInfo.connected = qsIcon.visible;
            mInfo.wifiSignalIconId = qsIcon.icon;
            mInfo.enabledDesc = description;
            mInfo.activityIn = activityIn;
            mInfo.activityOut = activityOut;
            mInfo.wifiSignalContentDescription = qsIcon.contentDescription;
            refreshState(mInfo);
        }
    };

    private final class WifiDetailAdapter implements DetailAdapter,
            NetworkController.AccessPointController.AccessPointCallback, QSDetailItems.Callback {

        private QSDetailItems mItems;
        private AccessPoint[] mAccessPoints;

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_wifi_label);
        }

        public Intent getSettingsIntent() {
            return WIFI_SETTINGS;
        }

        @Override
        public Boolean getToggleState() {
            return mState.value;
        }

        @Override
        public void setToggleState(boolean state) {
            if (DEBUG) Log.d(TAG, "setToggleState " + state);
            MetricsLogger.action(mContext, MetricsEvent.QS_WIFI_TOGGLE, state);
            mController.setWifiEnabled(state);
            showDetail(false);
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.QS_WIFI_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            if (DEBUG) Log.d(TAG, "createDetailView convertView=" + (convertView != null));
            mAccessPoints = null;
            mWifiController.scanForAccessPoints();
            fireScanStateChanged(true);
            mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            mItems.setTagSuffix("Wifi");
            mItems.setCallback(this);
            mItems.setEmptyState(R.drawable.ic_qs_wifi_detail_empty,
                    R.string.quick_settings_wifi_detail_empty_text);
            updateItems();
            setItemsVisible(mState.value);
            return mItems;
        }

        @Override
        public void onAccessPointsChanged(final List<AccessPoint> accessPoints) {
            mAccessPoints = accessPoints.toArray(new AccessPoint[accessPoints.size()]);
            updateItems();
            if (accessPoints != null && accessPoints.size() > 0) {
                fireScanStateChanged(false);
            }
        }

        @Override
        public void onSettingsActivityTriggered(Intent settingsIntent) {
            mHost.startActivityDismissingKeyguard(settingsIntent);
        }

        @Override
        public void onDetailItemClick(Item item) {
            if (item == null || item.tag == null) return;
            final AccessPoint ap = (AccessPoint) item.tag;
            if (!ap.isActive()) {
                if (mWifiController.connect(ap)) {
                    mHost.collapsePanels();
                }
            }
            showDetail(false);
        }

        @Override
        public void onDetailItemDisconnect(Item item) {
            // noop
        }

        public void setItemsVisible(boolean visible) {
            if (mItems == null) return;
            mItems.setItemsVisible(visible);
        }

        private void updateItems() {
            if (mItems == null) return;
            Item[] items = null;
            if (mAccessPoints != null) {
                items = new Item[mAccessPoints.length];
                for (int i = 0; i < mAccessPoints.length; i++) {
                    final AccessPoint ap = mAccessPoints[i];
                    final Item item = new Item();
                    item.tag = ap;
                    item.icon = mWifiController.getIcon(ap);
                    item.line1 = ap.getSsid();
                    item.line2 = ap.isActive() ? ap.getSummary() : null;
                    item.overlay = ap.getSecurity() != AccessPoint.SECURITY_NONE
                            ? mContext.getDrawable(R.drawable.qs_ic_wifi_lock)
                            : null;
                    items[i] = item;
                }
            }
            mItems.setItems(items);
        }
    };

    // add begin by lych
    private HotspotController mHotspotController;
    private final HotspotCallback mHotspotCallback = new HotspotCallback();
    private final class HotspotCallback implements HotspotController.Callback {
        @Override
        public void onHotspotChanged(boolean enabled, int state) {
            if (DEBUG) Log.d("lych","WifiTile onHotspotChanged");
            refreshState();
        }
    };
    // add end by lych
}
