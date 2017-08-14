/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.android.systemui.DejankUtils;
import com.android.systemui.EventLogTags;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;
import com.android.systemui.telephone.BatterySettingObserver;

import java.util.Locale;

public class PhoneStatusBarView extends PanelBar implements
    BatteryController.BatteryStateChangeCallback, BatterySettingObserver.BatteryCallBack {
    private static final String TAG = "PhoneStatusBarView";
    private static final boolean DEBUG = PhoneStatusBar.DEBUG;
    private static final boolean DEBUG_GESTURES = false;

    PhoneStatusBar mBar;

    boolean mIsFullyOpenedPanel = false;
    private final PhoneStatusBarTransitions mBarTransitions;
    private ScrimController mScrimController;

    /* SPRD: Bug 474745 Add battery level percent feature @{ */
    private TextView mBatteryLevel;
    private boolean mIsShowLevel = false;
    private BatteryController mBatteryController;

    //linwujia edit begin
    private BatterySettingObserver mSettingObserver;

    private int mLevel;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // talpa@andy 2017/5/6 17:08 modify @{
//            mBatteryLevel.setText(getResources().getString(R.string.battery_level_template, mLevel));
            String percentage = "";
            if(mContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR){
                percentage = mContext.getResources().getString(R.string.battery_level_template, mLevel);
            } else {
                percentage = mContext.getResources().getString(R.string.battery_level_template_rtl, mLevel);
            }
            mBatteryLevel.setText(percentage);
            // @}
            mIsShowLevel = BatterySettingObserver.getState(mContext);
            mBatteryLevel.setVisibility(mIsShowLevel ? View.VISIBLE : View.GONE);
        }
    };
    //linwujia edit end
    /* @} */

    //linwujia add show charging icon begin
    private View mBatteryCharging;
    //linwujia add show charging icon end

    private float mMinFraction;
    private float mPanelFraction;
    private Runnable mHideExpandedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPanelFraction == 0.0f) {
                mBar.makeExpandedInvisible();
            }
        }
    };

    public PhoneStatusBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mBarTransitions = new PhoneStatusBarTransitions(this);

        // SPRD: Bug 474745 Add battery level percent features
        mBatteryController = new BatteryControllerImpl(context);

        layoutDirectionChanged(); // add by lych
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setBar(PhoneStatusBar bar) {
        mBar = bar;
    }

    public void setBatterySettingObserver(BatterySettingObserver batterySettingObserver) {
        mSettingObserver = batterySettingObserver;
        if(mSettingObserver != null) {
            mSettingObserver.addBatteryCallBack(this);
        }
    }

    public void setScrimController(ScrimController scrimController) {
        mScrimController = scrimController;
    }

    @Override
    public void onFinishInflate() {
        mBarTransitions.init();
        /* SPRD: Bug 474745 Add battery level percent feature @{ */
        mBatteryLevel = (TextView) findViewById(R.id.battery_level_sprd);
        updateVisibilities();
        /* @} */

        //linwujia add begin
        mBatteryCharging = findViewById(R.id.itel_battery_charging);
        //linwujia add end
    }

    @Override
    public boolean panelEnabled() {
        return mBar.panelsEnabled();
    }

    @Override
    public boolean onRequestSendAccessibilityEventInternal(View child, AccessibilityEvent event) {
        if (super.onRequestSendAccessibilityEventInternal(child, event)) {
            // The status bar is very small so augment the view that the user is touching
            // with the content of the status bar a whole. This way an accessibility service
            // may announce the current item as well as the entire content if appropriate.
            AccessibilityEvent record = AccessibilityEvent.obtain();
            onInitializeAccessibilityEvent(record);
            dispatchPopulateAccessibilityEvent(record);
            event.appendRecord(record);
            return true;
        }
        return false;
    }

    @Override
    public void onPanelPeeked() {
        super.onPanelPeeked();
        mBar.makeExpandedVisible(false);
    }

    @Override
    public void onPanelCollapsed() {
        super.onPanelCollapsed();
        // Close the status bar in the next frame so we can show the end of the animation.
        DejankUtils.postAfterTraversal(mHideExpandedRunnable);
        mIsFullyOpenedPanel = false;
    }

    public void removePendingHideExpandedRunnables() {
        DejankUtils.removeCallbacks(mHideExpandedRunnable);
    }

    @Override
    public void onPanelFullyOpened() {
        super.onPanelFullyOpened();
        if (!mIsFullyOpenedPanel) {
            mPanel.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        }
        mIsFullyOpenedPanel = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean barConsumedEvent = mBar.interceptTouchEvent(event);

        if (DEBUG_GESTURES) {
            if (event.getActionMasked() != MotionEvent.ACTION_MOVE) {
                EventLog.writeEvent(EventLogTags.SYSUI_PANELBAR_TOUCH,
                        event.getActionMasked(), (int) event.getX(), (int) event.getY(),
                        barConsumedEvent ? 1 : 0);
            }
        }

        return barConsumedEvent || super.onTouchEvent(event);
    }

    @Override
    public void onTrackingStarted() {
        super.onTrackingStarted();
        mBar.onTrackingStarted();
        mScrimController.onTrackingStarted();
        removePendingHideExpandedRunnables();
    }

    @Override
    public void onClosingFinished() {
        super.onClosingFinished();
        mBar.onClosingFinished();
    }

    @Override
    public void onTrackingStopped(boolean expand) {
        super.onTrackingStopped(expand);
        mBar.onTrackingStopped(expand);
    }

    @Override
    public void onExpandingFinished() {
        super.onExpandingFinished();
        mScrimController.onExpandingFinished();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mBar.interceptTouchEvent(event) || super.onInterceptTouchEvent(event);
    }

    @Override
    public void panelScrimMinFractionChanged(float minFraction) {
        if (mMinFraction != minFraction) {
            mMinFraction = minFraction;
            if (minFraction != 0.0f) {
                mScrimController.animateNextChange();
            }
            updateScrimFraction();
        }
    }

    @Override
    public void panelExpansionChanged(float frac, boolean expanded) {
        super.panelExpansionChanged(frac, expanded);
        mPanelFraction = frac;
        updateScrimFraction();
    }

    private void updateScrimFraction() {
        float scrimFraction = Math.max(mPanelFraction, mMinFraction);
        mScrimController.setPanelExpansion(scrimFraction);
    }

    public void onDensityOrFontScaleChanged() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = getResources().getDimensionPixelSize(
                R.dimen.status_bar_height);
        setLayoutParams(layoutParams);
    }

    /* SPRD: Bug 474745 Add battery level percent feature @{ */
    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        // TODO Auto-generated method stub
        mLevel = level;
        // talpa@andy 2017/5/6 17:08 modify @{
        // mBatteryLevel.setText(getResources().getString(R.string.battery_level_template, level));
        String percentage = "";
        if(mContext.getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR){
            percentage = mContext.getResources().getString(R.string.battery_level_template, level);
        } else {
            percentage = mContext.getResources().getString(R.string.battery_level_template_rtl, level);
        }
        mBatteryLevel.setText(percentage);
        // @}
        updateVisibilities();

        /// George: fix bug when level equal 100 ,dissmiss charging image
        if(/*(level!=100)&&*/pluggedIn&& charging)
        {
            mBatteryCharging.setVisibility(View.VISIBLE);
        }else
        {
            mBatteryCharging.setVisibility( View.GONE);
        }
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        // TODO Auto-generated method stub
    }

    private void updateVisibilities() {
        mIsShowLevel = BatterySettingObserver.getState(mContext);
        if (mIsShowLevel) {
            mBatteryLevel.setVisibility(View.VISIBLE);
        } else {
            mBatteryLevel.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if(mSettingObserver != null) {
            mSettingObserver.registerContentObserver();
            mSettingObserver.addBatteryCallBack(this);
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);

        mBatteryController.addStateChangedCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mSettingObserver != null) {
            mSettingObserver.removeBatteryCallBack(this);
        }
        mContext.unregisterReceiver(mReceiver);

        mBatteryController.removeStateChangedCallback(this);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
        mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            /*getResources().getDimensionPixelSize(R.dimen.battery_level_text_size)*/
                getResources().getDimensionPixelSize(R.dimen.itel_battery_level_text_size));
        layoutDirectionChanged(); // add by lych for fix bug cdm#22884
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
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mBar.mIconController.onConfigurationChanged();
    }

    @Override
    public void onChange(boolean isOpen) {
        mIsShowLevel = isOpen;
        mBatteryLevel.setVisibility(isOpen ? View.VISIBLE : View.GONE);
        postInvalidate();
    }
}
