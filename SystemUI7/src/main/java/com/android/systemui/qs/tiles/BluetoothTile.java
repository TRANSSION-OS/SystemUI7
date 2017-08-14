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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItems;
import com.android.systemui.qs.QSDetailItems.Item;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.BluetoothController;

import java.util.ArrayList;
import java.util.Collection;

import itel.transsion.settingslib.utils.LogUtil;

/** Quick settings tile: Bluetooth **/
public class BluetoothTile extends QSTile<QSTile.BooleanState>  {
    private static final Intent BLUETOOTH_SETTINGS = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);

    private final BluetoothController mController;
    private final BluetoothDetailAdapter mDetailAdapter;

    //added by chenzhengjun start
    private final Icon mBluetoothStateEnable =
            ResourceIcon.get(R.drawable.itel_ic_qs_bluetooth_enable);
    private final Icon mBluetoothStateDisable =
            ResourceIcon.get(R.drawable.itel_ic_qs_bluetooth_disable);
    //added by chenzhengjun end
    // talpa@andy 2017/5/25 15:41 add:过渡动画 @{
    private final Icon mProgressEnable = ResourceIcon.get(
            R.drawable.itel_qs_progress_bluetooth_enable);
    // @}
    public BluetoothTile(Host host) {
        super(host);
        mController = host.getBluetoothController();
        mDetailAdapter = new BluetoothDetailAdapter();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mController.addStateChangedCallback(mCallback);
        } else {
            mController.removeStateChangedCallback(mCallback);
        }
    }

    @Override
    protected void handleSecondaryClick() {
        // talpa@andy 2017/5/25 20:28 add:防止重复点击 @{
        if (mOpening) {
            return;
        }
        // @}
        // Secondary clicks are header clicks, just toggle.
        final boolean isEnabled = (Boolean)mState.value;
        MetricsLogger.action(mContext, getMetricsCategory(), !isEnabled);
        mController.setBluetoothEnabled(!isEnabled);
        // talpa@andy 2017/5/25 20:28 add：添加过渡状态 @{
        if (!isEnabled) {
            mOpening = true;
            refreshState(null);
        }
        // @}
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
    }

    @Override
    protected void handleClick() {
        // talpa@andy 2017/6/8 14:53 modify：快捷面板展开状态下实现开启/关闭@{
        handleSecondaryClick();
      /*  if (!mController.canConfigBluetooth()) {
            mHost.startActivityDismissingKeyguard(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            return;
        }
        if (!mState.value) {
            mState.value = true;
            mController.setBluetoothEnabled(true);
        }
        showDetail(true);*/
        // @}
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_bluetooth_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final boolean enabled = mController.isBluetoothEnabled();
        final boolean connected = mController.isBluetoothConnected();
        final boolean connecting = mController.isBluetoothConnecting();
        state.value = enabled;
        state.autoMirrorDrawable = false;
        state.minimalContentDescription =
                mContext.getString(R.string.accessibility_quick_settings_bluetooth);
        // talpa@andy 2017/5/25 20:32 add:添加过渡状态 @{
        state.switching = mOpening;
        state.icon = mOpening ? mProgressEnable : enabled ? mBluetoothStateEnable :
                mBluetoothStateDisable;
        // @}
        if (enabled) {
            state.label = null;
            if (connected) {
                //deleted by chengzhengjun start
//                state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_connected);
                //deleted by chenzhengjun end
                state.label = mController.getLastDeviceName();
                state.contentDescription = mContext.getString(
                        R.string.accessibility_bluetooth_name, state.label);
                state.minimalContentDescription = state.minimalContentDescription + ","
                        + state.contentDescription;
            } else if (connecting) {
                //deleted by chengzhengjun start
//                state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_connecting);
                //deleted by chenzhengjun end
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_bluetooth_connecting);
                state.label = mContext.getString(R.string.quick_settings_bluetooth_label);
                state.minimalContentDescription = state.minimalContentDescription + ","
                        + state.contentDescription;
            } else {
                //deleted by chengzhengjun start
//                state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_on);
                //deleted by chenzhengjun end
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_bluetooth_on) + ","
                        + mContext.getString(R.string.accessibility_not_connected);
                state.minimalContentDescription = state.minimalContentDescription + ","
                        + mContext.getString(R.string.accessibility_not_connected);
            }
            if (TextUtils.isEmpty(state.label)) {
                state.label = mContext.getString(R.string.quick_settings_bluetooth_label);
            }
        } else {
            //deleted by chenzhenjgun start
//            state.icon = ResourceIcon.get(R.drawable.ic_qs_bluetooth_off);
            //deleted by chenzhengjun end
            state.label = mContext.getString(R.string.quick_settings_bluetooth_label);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_bluetooth_off);
        }
        CharSequence bluetoothName = state.label;
        if (connected) {
            bluetoothName = state.dualLabelContentDescription = mContext.getString(
                    R.string.accessibility_bluetooth_name, state.label);
        }
        state.dualLabelContentDescription = bluetoothName;
        state.contentDescription = state.contentDescription + "," + mContext.getString(
                R.string.accessibility_quick_settings_open_settings, getTileLabel());
        state.expandedAccessibilityClassName = Button.class.getName();
        state.minimalAccessibilityClassName = Switch.class.getName();
        //talpa zhw add
        state.bottomIcon = ResourceIcon.get(R.drawable.itel_ic_qs_bluetooth_bottom);
        //talpa zhw add end
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_BLUETOOTH;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_bluetooth_changed_off);
        }
    }

    @Override
    public boolean isAvailable() {
        return mController.isBluetoothSupported();
    }

    private final BluetoothController.Callback mCallback = new BluetoothController.Callback() {
        @Override
        public void onBluetoothStateChange(boolean enabled) {
            // talpa@andy 2017/6/2 10:18 add:结束过渡状态的条件@{
            if (enabled) {
                mOpening = false;
            }
            // @}
            refreshState();
        }

        @Override
        public void onBluetoothDevicesChanged() {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDetailAdapter.updateItems();
                }
            });
            refreshState();
        }
    };

    private final class BluetoothDetailAdapter implements DetailAdapter, QSDetailItems.Callback {
        private QSDetailItems mItems;

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_bluetooth_label);
        }

        @Override
        public Boolean getToggleState() {
            return mState.value;
        }

        @Override
        public Intent getSettingsIntent() {
            return BLUETOOTH_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(mContext, MetricsEvent.QS_BLUETOOTH_TOGGLE, state);
            mController.setBluetoothEnabled(state);
            showDetail(false);
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.QS_BLUETOOTH_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            mItems = QSDetailItems.convertOrInflate(context, convertView, parent);
            mItems.setTagSuffix("Bluetooth");
            mItems.setEmptyState(R.drawable.ic_qs_bluetooth_detail_empty,
                    R.string.quick_settings_bluetooth_detail_empty_text);
            mItems.setCallback(this);
            updateItems();
            setItemsVisible(mState.value);
            return mItems;
        }

        public void setItemsVisible(boolean visible) {
            if (mItems == null) return;
            mItems.setItemsVisible(visible);
        }

        private void updateItems() {
            if (mItems == null) return;
            ArrayList<Item> items = new ArrayList<Item>();
            final Collection<CachedBluetoothDevice> devices = mController.getDevices();
            if (devices != null) {
                for (CachedBluetoothDevice device : devices) {
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) continue;
                    final Item item = new Item();
                    item.icon = R.drawable.ic_qs_bluetooth_on;
                    item.line1 = device.getName();
                    int state = device.getMaxConnectionState();
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        item.icon = R.drawable.ic_qs_bluetooth_connected;
                        item.line2 = mContext.getString(R.string.quick_settings_connected);
                        item.canDisconnect = true;
                    } else if (state == BluetoothProfile.STATE_CONNECTING) {
                        item.icon = R.drawable.ic_qs_bluetooth_connecting;
                        item.line2 = mContext.getString(R.string.quick_settings_connecting);
                    }
                    item.tag = device;
                    items.add(item);
                }
            }
            mItems.setItems(items.toArray(new Item[items.size()]));
        }

        @Override
        public void onDetailItemClick(Item item) {
            if (item == null || item.tag == null) return;
            final CachedBluetoothDevice device = (CachedBluetoothDevice) item.tag;
            if (device != null && device.getMaxConnectionState()
                    == BluetoothProfile.STATE_DISCONNECTED) {
                mController.connect(device);
            }
        }

        @Override
        public void onDetailItemDisconnect(Item item) {
            if (item == null || item.tag == null) return;
            final CachedBluetoothDevice device = (CachedBluetoothDevice) item.tag;
            if (device != null) {
                mController.disconnect(device);
            }
        }
    }
}
