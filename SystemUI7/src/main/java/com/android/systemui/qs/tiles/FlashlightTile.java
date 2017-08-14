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
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.FlashlightController;

import itel.transsion.settingslib.utils.LogUtil;

/** Quick settings tile: Control flashlight **/
public class FlashlightTile extends QSTile<QSTile.BooleanState> implements
        FlashlightController.FlashlightListener {

    //deleted by chenzhengjun start
//    private final AnimationIcon mEnable
//            = new AnimationIcon(R.drawable.ic_signal_flashlight_enable_animation,
//            R.drawable.ic_signal_flashlight_disable);
//    private final AnimationIcon mDisable
//            = new AnimationIcon(R.drawable.ic_signal_flashlight_disable_animation,
//            R.drawable.ic_signal_flashlight_enable);
    //deleted by chenzhengjun end
    //added by chenzhengjun start
    private final Icon mEnable = ResourceIcon.get(R.drawable.itel_ic_qs_flashlight_on);
    private final Icon mDisable = ResourceIcon.get(R.drawable.itel_ic_qs_flashlight_off);
    //added by chenzhengjun end
    private final FlashlightController mFlashlightController;

    public FlashlightTile(Host host) {
        super(host);
        mFlashlightController = host.getFlashlightController();
        mFlashlightController.addListener(this);
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();

        mFlashlightController.removeListener(this);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
    }

    @Override
    public boolean isAvailable() {
        return mFlashlightController.hasFlashlight();
    }

    @Override
    protected void handleClick() {
        if (ActivityManager.isUserAMonkey()) {
            return;
        }

        // Talpa:PeterHuang add for incoming call do not update state @{
        //modified begin by lych for fix bug tfs#18386
        //if (getTelecomManager().isInCall()){
        if (getTelecomManager().isRinging() && flashEnabledIncallRinging()){
        // modified end by lych for fix bug tfs#18386
            //LogUtil.d("is InCall");
            return;
        }
        // @}

        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        boolean newState = !mState.value;
        refreshState(newState);
        mFlashlightController.setFlashlight(newState);
        // Talpa:PeterHuang Add @{
        Settings.System.putInt(mContext.getContentResolver(), "open_flashlight_by_quicksetting_panel", newState ? 1 : 0); // 1 打开 0 关闭
        // @}
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_flashlight_label);
    }

    @Override
    protected void handleLongClick() {
        handleClick();
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {

        state.label = mHost.getContext().getString(R.string.quick_settings_flashlight_label);
        if (!mFlashlightController.isAvailable()) {
            Drawable icon = mHost.getContext().getDrawable(R.drawable.ic_signal_flashlight_disable)
                    .mutate();
            final int disabledColor = mHost.getContext().getColor(R.color.qs_tile_tint_unavailable);
            icon.setTint(disabledColor);
            state.icon = mDisable;//new DrawableIcon(icon);
            state.label = new SpannableStringBuilder().append(state.label,
                    new ForegroundColorSpan(disabledColor),
                    SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
            state.contentDescription = mContext.getString(
                    R.string.accessibility_quick_settings_flashlight_unavailable);
            // Talpa:peterHuang add @{
            state.value = false; // 因为现在开关一直显示，所以在不可用时，显示为关闭。
            // @}
            return;
        }
        if (arg instanceof Boolean) {
            boolean value = (Boolean) arg;
            if (value == state.value) {
                return;
            }
            state.value = value;
        } else {
            state.value = mFlashlightController.isEnabled();
        }

        // Talpa:peterHuang add @{
        if(!state.value) { // 被第三方关闭
            //state.visible = true;
            // 先读取值判断，减少写入SettingsProvider的操作
            int open_flashlight_by_power_panel = Settings.System.getInt(mContext.getContentResolver(), "open_flashlight_by_power_panel", 1);
            if (open_flashlight_by_power_panel != 0) {
                Settings.System.putInt(mContext.getContentResolver(), "open_flashlight_by_power_panel", 0);
            }
        }


        int flashlightOpen = 0;
        try {
            flashlightOpen = Settings.System.getInt(mContext.getContentResolver(), "flashlight_open");
        } catch (Settings.SettingNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        state.value = mFlashlightController.isAvailable() && (flashlightOpen == 1);
        // @}

        // Talpa:PeterHuang add for incoming call do not update state @{
        //modified begin by lych for fix bug tfs#18386
        //if (getTelecomManager().isInCall()){
        if (getTelecomManager().isRinging() && flashEnabledIncallRinging()){
        //modified end by lych for fix bug tfs#18386
            //LogUtil.d("is InCall");
            state.value = false;
        }
        // @}

        //added by chenzhengjun start
        final Icon icon = state.value ? mEnable : mDisable;
        //added by chenzhengjun end

        state.icon = icon;
        state.contentDescription = mContext.getString(R.string.quick_settings_flashlight_label);
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();

        //talpa zhw add
        state.bottomIcon = ResourceIcon.get(R.drawable.itel_ic_qs_flashlight_bottom);
        //talpa zhw add end
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_FLASHLIGHT;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_flashlight_changed_off);
        }
    }

    @Override
    public void onFlashlightChanged(boolean enabled) {
        // Talpa:PeterHuang add for check log ncd#10442 @{
        LogUtil.d("enabled: " + enabled);
        // @]
        refreshState(enabled);
    }

    @Override
    public void onFlashlightError() {
        refreshState(false);
    }

    @Override
    public void onFlashlightAvailabilityChanged(boolean available) {
        refreshState();
    }

    private TelecomManager getTelecomManager() {
        return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }
    // add begin by lychfor fix bug tfs#18386
    private static final String KEY_PROVIDER_CALL = "def_enable_phone_twinkle";
    private boolean flashEnabledIncallRinging(){
        int settingStates = android.provider.Settings.Secure.getInt(mContext.getContentResolver(),
                KEY_PROVIDER_CALL,0);
        return settingStates == 1;
    }
    // add end by lych for fix bug tfs#18386
}
