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

package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.BatteryMeterView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import com.android.systemui.telephone.BatterySettingObserver;

import java.util.Locale;

/**
 * The header group on Keyguard.
 */
public class KeyguardStatusBarView extends RelativeLayout
        implements BatteryController.BatteryStateChangeCallback, BatterySettingObserver.BatteryCallBack {

    private boolean mBatteryCharging;
    private boolean mKeyguardUserSwitcherShowing;
    private boolean mBatteryListening;

    //private TextView mCarrierLabel;
    private View mSystemIconsSuperContainer;
    private MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;
    private TextView mBatteryLevel;

    //linwujia add for show battery charging icon begin
    private View mBatteryChargingView;
    //linwujia add for show battery charging icon end

    //linwujia add begin
    private BatterySettingObserver mSettingObserver;

    private int mLevel;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatteryLevel(mLevel);
        }
    };
    //linwujia add end

    private BatteryController mBatteryController;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;

    private int mSystemIconsSwitcherHiddenExpandedMargin;
    private View mSystemIconsContainer;

    // SPRD: Bug 474745 Add battery level percent feature
    private boolean mIsShowLevel = false;

    public KeyguardStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSystemIconsSuperContainer = findViewById(R.id.system_icons_super_container);
        mSystemIconsContainer = findViewById(R.id.system_icons_container);
        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) findViewById(R.id.multi_user_avatar);
        mBatteryLevel = (TextView) findViewById(R.id.battery_level_sprd);
        //linwujia edit begin
        //mBatteryLevel = (TextView) findViewById(R.id.battery_level);
        //mCarrierLabel = (TextView) findViewById(R.id.keyguard_carrier_text);
        //linwujia edit end

        //linwujia add begin
        mBatteryChargingView = findViewById(R.id.itel_keyguard_battery_charging);
        //linwujia add end
        loadDimens();
        updateUserSwitcher();
        layoutDirectionChanged(); // add by lych
    }

    //add begin by lych for fix bug cdm#22884
    private void layoutDirectionChanged(){
        String language= Locale.getDefault().getLanguage();
        //Log.i("lych", "PhoneStatusBarView  language="+language);
        //if("ar".equals(language)){
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        //}
    }
    // add end by lych for fix bug cdm#22884

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutDirectionChanged(); // add by lych

        MarginLayoutParams lp = (MarginLayoutParams) mMultiUserAvatar.getLayoutParams();
        lp.width = lp.height = getResources().getDimensionPixelSize(
                R.dimen.multi_user_avatar_keyguard_size);
        mMultiUserAvatar.setLayoutParams(lp);

        lp = (MarginLayoutParams) mMultiUserSwitch.getLayoutParams();
        lp.width = getResources().getDimensionPixelSize(
                R.dimen.multi_user_switch_width_keyguard);
        lp.setMarginEnd(getResources().getDimensionPixelSize(
                R.dimen.multi_user_switch_keyguard_margin));
        mMultiUserSwitch.setLayoutParams(lp);

        lp = (MarginLayoutParams) mSystemIconsSuperContainer.getLayoutParams();
        //lp.height = getResources().getDimensionPixelSize(
                //R.dimen.status_bar_header_height);
        lp.setMarginStart(getResources().getDimensionPixelSize(
                R.dimen.system_icons_super_container_margin_start));
        mSystemIconsSuperContainer.setLayoutParams(lp);
        mSystemIconsSuperContainer.setPaddingRelative(mSystemIconsSuperContainer.getPaddingStart(),
                mSystemIconsSuperContainer.getPaddingTop(),
                getResources().getDimensionPixelSize(R.dimen.system_icons_keyguard_padding_end),
                mSystemIconsSuperContainer.getPaddingBottom());

        lp = (MarginLayoutParams) mSystemIconsContainer.getLayoutParams();
        lp.height = getResources().getDimensionPixelSize(
                R.dimen.status_bar_height);
        mSystemIconsContainer.setLayoutParams(lp);

        lp = (MarginLayoutParams) mBatteryLevel.getLayoutParams();
        //linwujia edit begin
        /*lp.setMarginStart(
                getResources().getDimensionPixelSize(R.dimen.header_battery_margin_keyguard));
        mBatteryLevel.setLayoutParams(lp);
        mBatteryLevel.setPaddingRelative(mBatteryLevel.getPaddingStart(),
                mBatteryLevel.getPaddingTop(),
                getResources().getDimensionPixelSize(R.dimen.battery_level_padding_end),
                mBatteryLevel.getPaddingBottom());*/

        lp.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.itel_keyguard_battery_level_margin_end));
        mBatteryLevel.setLayoutParams(lp);
        mBatteryLevel.setPaddingRelative(mBatteryLevel.getPaddingStart(),
                mBatteryLevel.getPaddingTop(),
                mBatteryLevel.getPaddingEnd(),
                mBatteryLevel.getPaddingBottom());
        //linwujia edit end
        mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                /*getResources().getDimensionPixelSize(R.dimen.battery_level_text_size)*/
                getResources().getDimensionPixelSize(R.dimen.itel_battery_level_text_size));

        // Respect font size setting.
        /*mCarrierLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(
                        com.android.internal.R.dimen.text_size_small_material));
        lp = (MarginLayoutParams) mCarrierLabel.getLayoutParams();
        lp.setMarginStart(
                getResources().getDimensionPixelSize(R.dimen.keyguard_carrier_text_margin));
        mCarrierLabel.setLayoutParams(lp);*/

        lp = (MarginLayoutParams) getLayoutParams();
        //lp.height =  getResources().getDimensionPixelSize(
                //R.dimen.status_bar_header_height_keyguard);
        setLayoutParams(lp);
    }

    private void loadDimens() {
        mSystemIconsSwitcherHiddenExpandedMargin = getResources().getDimensionPixelSize(
                R.dimen.system_icons_switcher_hidden_expanded_margin);
    }

    private void updateVisibilities() {
        if (mMultiUserSwitch.getParent() != this && !mKeyguardUserSwitcherShowing) {
            if (mMultiUserSwitch.getParent() != null) {
                getOverlay().remove(mMultiUserSwitch);
            }
            addView(mMultiUserSwitch, 0);
        } else if (mMultiUserSwitch.getParent() == this && mKeyguardUserSwitcherShowing) {
            removeView(mMultiUserSwitch);
        }
        /* @} */
        //SPRD: Bug 474745 Add battery level percent feature
        //mBatteryLevel.setVisibility(mBatteryCharging ? View.VISIBLE : View.GONE);
    }

    private void updateSystemIconsLayoutParams() {
        RelativeLayout.LayoutParams lp =
                (LayoutParams) mSystemIconsSuperContainer.getLayoutParams();
        int marginEnd = mKeyguardUserSwitcherShowing ? mSystemIconsSwitcherHiddenExpandedMargin : 0;
        if (marginEnd != lp.getMarginEnd()) {
            lp.setMarginEnd(marginEnd);
            mSystemIconsSuperContainer.setLayoutParams(lp);
        }
    }

    public void setListening(boolean listening) {
        if (listening == mBatteryListening) {
            return;
        }
        mBatteryListening = listening;
        if (mBatteryListening) {
            mBatteryController.addStateChangedCallback(this);
        } else {
            mBatteryController.removeStateChangedCallback(this);
        }
    }

    private void updateUserSwitcher() {
        boolean keyguardSwitcherAvailable = mKeyguardUserSwitcher != null;
        mMultiUserSwitch.setClickable(keyguardSwitcherAvailable);
        mMultiUserSwitch.setFocusable(keyguardSwitcherAvailable);
        mMultiUserSwitch.setKeyguardMode(keyguardSwitcherAvailable);
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        ((BatteryMeterView) findViewById(R.id.battery)).setBatteryController(batteryController);
    }

    public void setBatterySettingObserver(BatterySettingObserver settingObserver) {
        mSettingObserver = settingObserver;
        if(mSettingObserver != null) {
            mSettingObserver.addBatteryCallBack(this);
        }
    }

    public void setUserSwitcherController(UserSwitcherController controller) {
        mMultiUserSwitch.setUserSwitcherController(controller);
    }

    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(new UserInfoController.OnUserInfoChangedListener() {
            @Override
            public void onUserInfoChanged(String name, Drawable picture) {
                mMultiUserAvatar.setImageDrawable(picture);
            }
        });
    }

    public void setQSPanel(QSPanel qsp) {
        mMultiUserSwitch.setQsPanel(qsp);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        /* SPRD: Bug 474745 Add battery level percent feature @{ */
        //String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        //mBatteryLevel.setText(percentage);
        /* @} */
        boolean changed = mBatteryCharging != charging;
        mBatteryCharging = charging;
        if (changed) {
            updateVisibilities();
        }

        // SPRD: Bug 474745 Add battery level percent feature
        updateBatteryLevel(level);

        //linwujia add begin
        mBatteryChargingView.setVisibility(pluggedIn && charging ? View.VISIBLE : View.GONE);
        //linwujia add end
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        // could not care less
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        mKeyguardUserSwitcher = keyguardUserSwitcher;
        mMultiUserSwitch.setKeyguardUserSwitcher(keyguardUserSwitcher);
        updateUserSwitcher();
    }

    public void setKeyguardUserSwitcherShowing(boolean showing, boolean animate) {
        mKeyguardUserSwitcherShowing = showing;
        if (animate) {
            // talpa@andy 2017/3/31 15:32 delete @{
           /* animateNextLayoutChange();*/
            // @}
        }
        // talpa@andy 2017/3/31 15:13 delete @{
//        updateVisibilities();
        // @}
        updateSystemIconsLayoutParams();
    }

    private void animateNextLayoutChange() {
        final int systemIconsCurrentX = mSystemIconsSuperContainer.getLeft();
        final boolean userSwitcherVisible = mMultiUserSwitch.getParent() == this;
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                boolean userSwitcherHiding = userSwitcherVisible
                        && mMultiUserSwitch.getParent() != KeyguardStatusBarView.this;
                mSystemIconsSuperContainer.setX(systemIconsCurrentX);
                mSystemIconsSuperContainer.animate()
                        .translationX(0)
                        .setDuration(400)
                        .setStartDelay(userSwitcherHiding ? 300 : 0)
                        .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                        .start();
                if (userSwitcherHiding) {
                    getOverlay().add(mMultiUserSwitch);
                    mMultiUserSwitch.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .setStartDelay(0)
                            .setInterpolator(Interpolators.ALPHA_OUT)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    mMultiUserSwitch.setAlpha(1f);
                                    getOverlay().remove(mMultiUserSwitch);
                                }
                            })
                            .start();

                } else {
                    mMultiUserSwitch.setAlpha(0f);
                    mMultiUserSwitch.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .setStartDelay(200)
                            .setInterpolator(Interpolators.ALPHA_IN);
                }
                return true;
            }
        });

    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != View.VISIBLE) {
            mSystemIconsSuperContainer.animate().cancel();
            // talpa@andy 2017/3/31 15:30 delete @{
//            mMultiUserSwitch.animate().cancel();
//            mMultiUserSwitch.setAlpha(1f);
        }
    }
            // @}
    //linwujia add begin
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mSettingObserver != null) {
            mSettingObserver.registerContentObserver();
            mSettingObserver.addBatteryCallBack(this);
        }
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mSettingObserver != null) {
            mSettingObserver.removeBatteryCallBack(this);
        }
        mContext.unregisterReceiver(mReceiver);
    }
    //linwujia add end

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /* SPRD: Bug 474745 Add battery level percent feature @{ */
    private void updateBatteryLevel(int level) {
        mLevel = level;
        // talpa@andy 2017/5/6 17:08 modify @{
//        mBatteryLevel.setText(getResources().getString(R.string.battery_level_template, level));
        String percentage = "";
        if(mContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR){
            percentage = mContext.getResources().getString(R.string.battery_level_template, level);
        } else {
            percentage = mContext.getResources().getString(R.string.battery_level_template_rtl, level);
        }
        mBatteryLevel.setText(percentage);
        // @}
        mIsShowLevel = BatterySettingObserver.getState(mContext);
        // SPRD:ADD fixbug442959 The battery percentege's display in statusbar dues to the battery
        // percentege is checked.
        // mBatteryLevel.setVisibility(mBatteryCharging || mIsShowLevel ? View.VISIBLE : View.GONE);
        mBatteryLevel.setVisibility(mIsShowLevel ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onChange(boolean isOpen) {
        mIsShowLevel = isOpen;
        mBatteryLevel.setVisibility(isOpen ? View.VISIBLE : View.GONE);
        postInvalidate();
    }
    /* @} */
}
