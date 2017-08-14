/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.SysUIToast;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.ZenModePanel;

import itel.transsion.settingslib.utils.ConfigService;

/**
 * Quick settings tile: Do not disturb
 **/
public class DndTile extends QSTile<QSTile.BooleanState> {

    private static final Intent ZEN_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);

    private static final Intent ZEN_PRIORITY_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS);

    private static final String ACTION_SET_VISIBLE = "com.android.systemui.dndtile.SET_VISIBLE";
    private static final String EXTRA_VISIBLE = "visible";

    private static final QSTile.Icon TOTAL_SILENCE =
            ResourceIcon.get(R.drawable.itel_ic_qs_dnd_on_total_silence/*talpa zhw ic_qs_dnd_on_total_silence*/);

    //      talpa zhw modify
//    private final AnimationIcon mDisable =
//            new AnimationIcon(R.drawable.ic_dnd_disable_animation,
//                    R.drawable.ic_qs_dnd_off);
//    private final AnimationIcon mDisableTotalSilence =
//            new AnimationIcon(R.drawable.ic_dnd_total_silence_disable_animation,
//                    R.drawable.ic_qs_dnd_off);
    //talpa zhw add
    private final Icon mDisable = ResourceIcon.get(R.drawable.itel_ic_qs_dnd_off);
    private final Icon mDisableTotalSilence = ResourceIcon.get(R.drawable.itel_ic_qs_dnd_off);
    //tlapa zhw add end

    private final ZenModeController mController;
    private final DndDetailAdapter mDetailAdapter;

    private boolean mListening;
    private boolean mShowingDetail;
    private ConfigService mConfigService;
    private static final String FIRST_SETTING = "first_setting";

    public DndTile(Host host) {
        super(host);
        mController = host.getZenModeController();
        mDetailAdapter = new DndDetailAdapter();
        mContext.registerReceiver(mReceiver, new IntentFilter(ACTION_SET_VISIBLE));
        mConfigService = new ConfigService(mContext);
    }

    public static void setVisible(Context context, boolean visible) {
        Prefs.putBoolean(context, Prefs.Key.DND_TILE_VISIBLE, visible);
    }

    public static boolean isVisible(Context context) {
        return Prefs.getBoolean(context, Prefs.Key.DND_TILE_VISIBLE, false /* defaultValue */);
    }

    public static void setCombinedIcon(Context context, boolean combined) {
        Prefs.putBoolean(context, Prefs.Key.DND_TILE_COMBINED_ICON, combined);
    }

    public static boolean isCombinedIcon(Context context) {
        return Prefs.getBoolean(context, Prefs.Key.DND_TILE_COMBINED_ICON,
                false /* defaultValue */);
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
    public Intent getLongClickIntent() {
        // talpa@andy 2017/6/6 14:50 add:长按进入勿扰模式，并打开当前保存的勿扰模式 @{
        showDetail(true);
        int zen = Prefs.getInt(mContext, Prefs.Key.DND_FAVORITE_ZEN, Global.ZEN_MODE_ALARMS);
        mController.setZen(zen, null, TAG);
        // @}
        //itel@andy modify @{
        /* return ZEN_SETTINGS;*/
        return null;
        //@}
    }

    @Override
    public void handleClick() {
        if (mController.isVolumeRestricted()) {
            // Collapse the panels, so the user can see the toast.
            mHost.collapsePanels();
            SysUIToast.makeText(mContext, mContext.getString(
                    Resources.getSystem().getIdentifier("error_message_change_not_allowed", "string", "android")),
                    Toast.LENGTH_LONG).show();
            return;
        }
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        if (mState.value) {
            mController.setZen(Global.ZEN_MODE_OFF, null, TAG);
        } else {
            int zen = Prefs.getInt(mContext, Prefs.Key.DND_FAVORITE_ZEN, Global.ZEN_MODE_ALARMS);
            mController.setZen(zen, null, TAG);
            // talpa@andy 2017/6/6 14:37 add:首次使用快捷按钮时实现点击进入，后续点击统一实现开/关 @{
            if (mConfigService.getBoolean(FIRST_SETTING, true)) {
                showDetail(true);
                mConfigService.setBoolean(FIRST_SETTING, false);
            }
            // @}
        }
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_dnd_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int zen = arg instanceof Integer ? (Integer) arg : mController.getZen();
        final boolean newValue = zen != Global.ZEN_MODE_OFF;
        final boolean valueChanged = state.value != newValue;
        state.value = newValue;
        checkIfRestrictionEnforcedByAdminOnly(state, UserManager.DISALLOW_ADJUST_VOLUME);
        switch (zen) {
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                state.icon = ResourceIcon.get(R.drawable.itel_ic_qs_dnd_on/*talpa zhw ic_qs_dnd_on*/);
                state.label = mContext.getString(R.string.quick_settings_dnd_priority_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_priority_on);
                break;
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                state.icon = TOTAL_SILENCE;
                state.label = mContext.getString(R.string.quick_settings_dnd_none_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_none_on);
                break;
            case Global.ZEN_MODE_ALARMS:
                state.icon = ResourceIcon.get(R.drawable.itel_ic_qs_dnd_on/*talpa zhw ic_qs_dnd_on*/);
                state.label = mContext.getString(R.string.quick_settings_dnd_alarms_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_alarms_on);
                break;
            default:
                state.icon = TOTAL_SILENCE.equals(state.icon) ? mDisableTotalSilence : mDisable;
                state.label = mContext.getString(R.string.quick_settings_dnd_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd);
                break;
        }

        //talpa zhw add
        state.bottomIcon = ResourceIcon.get(R.drawable.itel_ic_qs_dnd_bottom);
        //talpa zhw add end
        if (mShowingDetail && !state.value) {
            showDetail(false);
        }
        if (valueChanged) {
            fireToggleStateChanged(state.value);
        }
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();

    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_DND;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_dnd_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_dnd_changed_off);
        }
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (mListening) {
            mController.addCallback(mZenCallback);
            Prefs.registerListener(mContext, mPrefListener);
        } else {
            mController.removeCallback(mZenCallback);
            Prefs.unregisterListener(mContext, mPrefListener);
        }
    }

    @Override
    public boolean isAvailable() {
        return isVisible(mContext);
    }

    private final OnSharedPreferenceChangeListener mPrefListener
            = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                              @Prefs.Key String key) {
            if (Prefs.Key.DND_TILE_COMBINED_ICON.equals(key) ||
                    Prefs.Key.DND_TILE_VISIBLE.equals(key)) {
                refreshState();
            }
        }
    };

    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        public void onZenChanged(int zen) {
            refreshState(zen);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean visible = intent.getBooleanExtra(EXTRA_VISIBLE, false);
            setVisible(mContext, visible);
            refreshState();
        }
    };

    private final class DndDetailAdapter implements DetailAdapter, OnAttachStateChangeListener {

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_dnd_label);
        }

        @Override
        public Boolean getToggleState() {
            return mState.value;
        }

        @Override
        public Intent getSettingsIntent() {
            return ZEN_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(mContext, MetricsEvent.QS_DND_TOGGLE, state);
            if (!state) {
                mController.setZen(Global.ZEN_MODE_OFF, null, TAG);
                showDetail(false);
            }
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.QS_DND_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final ZenModePanel zmp = convertView != null ? (ZenModePanel) convertView
                    : (ZenModePanel) LayoutInflater.from(context).inflate(
                    R.layout.zen_mode_panel, parent, false);
            if (convertView == null) {
                zmp.init(mController);
                zmp.addOnAttachStateChangeListener(this);
                zmp.setCallback(mZenModePanelCallback);
            }
            return zmp;
        }

        @Override
        public void onViewAttachedToWindow(View v) {
            mShowingDetail = true;
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            mShowingDetail = false;
        }
    }

    private final ZenModePanel.Callback mZenModePanelCallback = new ZenModePanel.Callback() {
        @Override
        public void onPrioritySettings() {
            mHost.startActivityDismissingKeyguard(ZEN_PRIORITY_SETTINGS);
        }

        @Override
        public void onInteraction() {
            // noop
        }

        @Override
        public void onExpanded(boolean expanded) {
            // noop
        }
    };

    @Override
    public void addCallback(Callback callback) {
        super.addCallback(callback);
    }
}
