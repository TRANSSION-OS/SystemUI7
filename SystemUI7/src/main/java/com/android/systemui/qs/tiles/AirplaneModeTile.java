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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSTile;

import itel.transsion.settingslib.utils.LogUtil;

/** Quick settings tile: Airplane mode **/
public class AirplaneModeTile extends QSTile<QSTile.BooleanState> {
    //deleted by chenzhengjun start
/*    private final AnimationIcon mEnable =
            new AnimationIcon(R.drawable.ic_signal_airplane_enable_animation,
                    R.drawable.ic_signal_airplane_disable);

    private final AnimationIcon mDisable1 =
            new AnimationIcon(R.drawable.ic_signal_airplane_disable_animation,
                    R.drawable.ic_signal_airplane_enable);
    */
    //deleted by chenzhengjun end
    //added by chenzhengjun start
    private final Icon mEnable = ResourceIcon.get(R.drawable.itel_ic_qs_airplanemode_on);
    private final Icon mDisable = ResourceIcon.get(R.drawable.itel_ic_qs_airplanemode_off);
    //added by chenzhengjun end
    // talpa@andy 2017/5/25 11:45 add @{
    private final Icon mProgressEnable = ResourceIcon.get(R.drawable.itel_qs_progress_airplanemode_enable);
    private final Icon mProgressDisable = ResourceIcon.get(R.drawable.itel_qs_progress_airplanemode_disable);
    // @}
    private final GlobalSetting mSetting;
    private boolean mListening;
    // /M: Maybe airplane mode need more time to turn on/off @{
    private static final String INTENT_ACTION_AIRPLANE_CHANGE_DONE =
            "com.mediatek.intent.action.AIRPLANE_CHANGE_DONE";
    private static final String EXTRA_AIRPLANE_MODE = "airplaneMode";
    private boolean mSwitching;

    public AirplaneModeTile(Host host) {
        super(host);

        mSetting = new GlobalSetting(mContext, mHandler, Global.AIRPLANE_MODE_ON) {
            @Override
            protected void handleValueChanged(int value) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
//         Log.d(TAG, "handleClick() mSwitching= " + mSwitching);
        if (mSwitching) {
            return;
        }
        mSwitching = true;
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        setEnabled(!mState.value);
    }

    private void setEnabled(boolean enabled) {
        final ConnectivityManager mgr =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mgr.setAirplaneMode(enabled);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.airplane_mode);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = arg instanceof Integer ? (Integer)arg : mSetting.getValue();
        final boolean airplaneMode = value != 0;
        state.value = airplaneMode;
        state.label = mContext.getString(R.string.airplane_mode);
        state.switching = mSwitching;
//        LogUtil.d("airplaneMode=" + airplaneMode);
        // talpa@andy 2017/5/24 11:05 add @{
        state.icon = mSwitching ? airplaneMode ? mProgressEnable : mProgressDisable :
                airplaneMode ? mEnable : mDisable;
        // @}
        state.contentDescription = state.label;
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();
        // talpa@andy 2017/5/24 11:07 delete @{
        /*handleAnimationState(state, arg);*/
        // @}
        //talpa zhw add
        state.bottomIcon = ResourceIcon.get(R.drawable.itel_ic_qs_airplanemode_bottom);
        //talpa zhw add end
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_AIRPLANEMODE;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_airplane_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_airplane_changed_off);
        }
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
//        LogUtil.d("mListening="+mListening);
        if (listening) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            // /M: Maybe airplane mode need more time to turn on/off @{
            filter.addAction(INTENT_ACTION_AIRPLANE_CHANGE_DONE);
            // @}
            mContext.registerReceiver(mReceiver, filter);
        } else {
            mContext.unregisterReceiver(mReceiver);
        }
        mSwitching = false;
        mSetting.setListening(listening);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                // talpa@andy 2017/5/25 11:43 delete @{
                /* SPRD: airplane_mode_on reading from database may not consistent with it
                 * actually is. In this situation, QS can only refresh UI by receiving intent
                 * with 'state' parameter. see bug #603094. @{ */
                /*boolean airplane = intent.getBooleanExtra("state", false);
                LogUtil.i("received ACTION_AIRPLANE_MODE_CHANGED, state = " + airplane);
                int value = (airplane) ? 1 : 0;
                if (value != mSetting.getValue() && airplane == !mState.value) {
                    LogUtil.i("mSetting.getValue()="+mSetting.getValue());
                    refreshState(value);
                    return;
                }*/
                /* @} */
                // @}
//                boolean airplane = intent.getBooleanExtra("state", false);
//                LogUtil.i("received ACTION_AIRPLANE_MODE_CHANGED, state = " + airplane);
                refreshState();
            } else if (INTENT_ACTION_AIRPLANE_CHANGE_DONE.equals(intent.getAction())) {
                // talpa@andy 2017/5/25 11:43 add @{
//                boolean airplaneModeOn = intent.getBooleanExtra(EXTRA_AIRPLANE_MODE, false);
//                LogUtil.i("onReceive() AIRPLANE_CHANGE_DONE,  airplaneModeOn= " + airplaneModeOn);
                mSwitching = false;
                refreshState();
                // @}
            }
        }
    };
}

