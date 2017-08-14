package com.android.systemui.telephone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.BatteryMeterView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;

public class TelephoneBackView extends RelativeLayout implements BatteryController.BatteryStateChangeCallback, BatterySettingObserver.BatteryCallBack {

    private AlphaAnimation alpha;

    private BatterySettingObserver mBatterySettingObserver;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
            updateBatteryLevel(mLevel);
        }
    };

    public TelephoneBackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBatteryController = new BatteryControllerImpl(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //linwujia edit begin
        mBatteryLevel = (TextView) findViewById(R.id.battery_level_back);
        mChargingView = findViewById(R.id.itel_returnCall_battery_charging);
        //linwujia edit end
        telephoneBackText = (TextView) findViewById(R.id.telephone_back_text);
        startAnimation();
    }

    private void startAnimation() {
        alpha = new AlphaAnimation(0.2f, 1.0f);
        alpha.setDuration(1000);
        alpha.setRepeatCount(Animation.INFINITE);
        alpha.setRepeatMode(Animation.REVERSE);
        findViewById(R.id.telephone_back_text).setAnimation(alpha);
        alpha.start();
    }

    public void chronometerAnimation() {
        findViewById(R.id.telephone_chronometer).setAnimation(alpha);
    }

    private BatteryController mBatteryController;
    private boolean mBatteryListening;
    //linwujia edit begin
    private boolean mBatteryCharging;
    private TextView mBatteryLevel;
    //linwujia edit end
    private TextView telephoneBackText;
    //linwujia edit begin
    private View mChargingView;
    // SPRD: fixbug:421109 Add battery percentege visibility controller
    private boolean mIsShowLevel = false;
    private int mLevel;
    //linwujia edit end

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

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        ((BatteryMeterView) findViewById(R.id.telephone_back_battery)).setBatteryController(mBatteryController);
    }

    public void setBatterySettingObserver(BatterySettingObserver batterySettingObserver) {
        mBatterySettingObserver = batterySettingObserver;
        if(mBatterySettingObserver != null) {
            mBatterySettingObserver.addBatteryCallBack(this);
        }
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        /* SPRD: fixbug:421109 Add battery percentege visibility controller @{
        //String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        //mBatteryLevel.setText(percentage);
         @} */

        //linwujia edit begin
        boolean changed = mBatteryCharging != charging;
        mBatteryCharging = charging;
        if (changed) {
            updateVisibilities();
        }
        // SPRD: fixbug421109 Add battery percentege visibility controller
        updateBatteryLevel(level);
        if(pluggedIn) {
            mChargingView.setVisibility(View.VISIBLE);
        } else {
            mChargingView.setVisibility(View.GONE);
        }
        //linwujia edit end
    }

    @Override
    public void onPowerSaveChanged(boolean changed) {
        // could not care less
    }

    //linwujia edit begin
    private void updateVisibilities() {
		mIsShowLevel = BatterySettingObserver.getState(mContext);
		 if (mIsShowLevel) {
		    mBatteryLevel.setVisibility(View.VISIBLE);
		 } else {
		    mBatteryLevel.setVisibility(View.GONE);
		 }
    }

     //SPRD: fixbug421109 Add battery percentege visibility controller @{
	private void updateBatteryLevel(int level) {
        mLevel = level;
		mBatteryLevel.setText(getResources().getString(
				R.string.battery_level_template, level));
		mIsShowLevel = BatterySettingObserver.getState(mContext);
		// mBatteryLevel.setVisibility(mBatteryCharging || mIsShowLevel ?
		// View.VISIBLE : View.GONE);
		mBatteryLevel.setVisibility(mIsShowLevel ? View.VISIBLE : View.GONE);// change by george
	}
     //@}
    //linwujia edit end

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //linwujia edit begin
        mBatteryLevel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                /*getResources().getDimensionPixelSize(R.dimen.battery_level_text_size)*/
                getResources().getDimensionPixelSize(R.dimen.itel_battery_level_text_size));
        //linwujia edit end
        telephoneBackText.setText(R.string.click_return_telephone);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //linwujia add begin
        if(mBatterySettingObserver != null) {
            mBatterySettingObserver.registerContentObserver();
            mBatterySettingObserver.addBatteryCallBack(this);
        }
        IntentFilter intentFilter =  new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mReceiver, intentFilter);
        //linwujia add end

        mBatteryController.addStateChangedCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mBatterySettingObserver != null) {
            mBatterySettingObserver.removeBatteryCallBack(this);
        }
        mContext.unregisterReceiver(mReceiver);
        mBatteryController.removeStateChangedCallback(this);
    }

    @Override
    public void onChange(boolean isOpen) {
        mIsShowLevel = isOpen;
        mBatteryLevel.setVisibility(isOpen ? View.VISIBLE : View.GONE);
        postInvalidate();
    }
}
