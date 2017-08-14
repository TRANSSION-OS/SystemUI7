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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.keyguard.KeyguardStatusView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSPanel.Callback;
import com.android.systemui.qs.QuickQSPanel;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.android.systemui.tuner.TunerService;

import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;

public class QuickStatusBarHeader extends BaseStatusBarHeader implements
        NextAlarmChangeCallback, OnClickListener, OnUserInfoChangedListener {

    private static final String TAG = "QuickStatusBarHeader";

    private static final float EXPAND_INDICATOR_THRESHOLD = .93f;

    private ActivityStarter mActivityStarter;
    private NextAlarmController mNextAlarmController;
    private SettingsButton mSettingsButton;
//    protected View mSettingsContainer;

//    private TextView mAlarmStatus;
//    private View mAlarmStatusCollapsed;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mAlarmShowing;

//    private ViewGroup mDateTimeGroup;
    private ViewGroup mDateTimeAlarmGroup;
    private TextView mEmergencyOnly;

//    protected ExpandableIndicator mExpandIndicator;

    private boolean mListening;
    private AlarmManager.AlarmClockInfo mNextAlarm;

    private QuickQSPanel mHeaderQsPanel;
    private boolean mShowEmergencyCallsOnly;
    protected MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;

    private float mDateTimeTranslation;
    private float mDateTimeAlarmTranslation;
    private float mDateScaleFactor;
    protected float mGearTranslation;

    private TouchAnimator mSecondHalfAnimator;
    private TouchAnimator mFirstHalfAnimator;
    private TouchAnimator mDateSizeAnimator;
    private TouchAnimator mAlarmTranslation;
    protected TouchAnimator mSettingsAlpha;
    private float mExpansionAmount;
    private QSTileHost mHost;
    private boolean mShowFullAlarm;
    private boolean mAirplaneMode = false;

    //talpa zhw add
    private View mTalpaQsEdit;
    //talpa zhw add end
    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mEmergencyOnly = (TextView) findViewById(R.id.header_emergency_calls_only);

        mDateTimeAlarmGroup = (ViewGroup) findViewById(R.id.date_time_alarm_group);
//        mDateTimeAlarmGroup.findViewById(R.id.empty_time_view).setVisibility(View.GONE);
//        mDateTimeGroup = (ViewGroup) findViewById(R.id.date_time_group);
//        mDateTimeGroup.setPivotX(0);
//        mDateTimeGroup.setPivotY(0);
        mShowFullAlarm = getResources().getBoolean(R.bool.quick_settings_show_full_alarm);

//        mExpandIndicator = (ExpandableIndicator) findViewById(R.id.expand_indicator);

        mHeaderQsPanel = (QuickQSPanel) findViewById(R.id.quick_qs_panel);

        mSettingsButton = (SettingsButton) findViewById(R.id.settings_button);
//        mSettingsContainer = findViewById(R.id.settings_button_container);
        mSettingsButton.setOnClickListener(this);

//        mAlarmStatusCollapsed = findViewById(R.id.alarm_status_collapsed);
//        mAlarmStatus = (TextView) findViewById(R.id.alarm_status);
//        mAlarmStatus.setOnClickListener(this);

        mMultiUserSwitch = (MultiUserSwitch) findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = (ImageView) mMultiUserSwitch.findViewById(R.id.multi_user_avatar);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view
        ((RippleDrawable) mSettingsButton.getBackground()).setForceSoftware(true);
//        ((RippleDrawable) mExpandIndicator.getBackground()).setForceSoftware(true);

        updateResources();

        //talpa zhw add
        mTalpaQsEdit = findViewById(R.id.qs_edit);
        mTalpaQsEdit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mQsPanel != null)
                {
                    mQsPanel.handleeEditClickEvent(view);
                }
            }
        });
        //talpa zhw add end
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
//        FontSizeUtils.updateFontSize(mAlarmStatus, R.dimen.qs_date_collapsed_size);
        FontSizeUtils.updateFontSize(mEmergencyOnly, R.dimen.qs_emergency_calls_only_text_size);
        mGearTranslation = mContext.getResources().getDimension(R.dimen.qs_header_gear_translation);
        //added by chenzhengjun start
        mDateTimeTranslation = 0;;//mContext.getResources().getDimension(
//                R.dimen.qs_date_anim_translation);
        mDateTimeAlarmTranslation = 0;//mContext.getResources().getDimension(
//                R.dimen.qs_date_alarm_anim_translation);
        //added by chenzhengjun end


        float dateCollapsedSize = mContext.getResources().getDimension(
                R.dimen.qs_date_collapsed_text_size);
        float dateExpandedSize = mContext.getResources().getDimension(
                R.dimen.qs_date_text_size);
        mDateScaleFactor = dateExpandedSize / dateCollapsedSize;
        updateDateTimePosition();

        mSecondHalfAnimator = new TouchAnimator.Builder()
				//added by chenzhengjun start
//                .addFloat(mShowFullAlarm ? mAlarmStatus : findViewById(R.id.date), "alpha", 0, 1) //added by chenzhengjun
                //added by chenzhengjun end
				.addFloat(mEmergencyOnly, "alpha", 0, 1)
                .setStartDelay(.5f)
                .build();
      // talpa@andy 2017/3/31 18:25 delete @{
      /*  if (mShowFullAlarm) {
            mFirstHalfAnimator = new TouchAnimator.Builder()
                    .addFloat(mAlarmStatusCollapsed, "alpha", 1, 0)
                    .setEndDelay(.5f)
                    .build();
        }*/
       // @}
        //added by chenzhengjun start
        mDateSizeAnimator = new TouchAnimator.Builder()
                //added by chenzhengjun start
//                .addFloat(mDateTimeGroup, "scaleX", 1, mDateScaleFactor)
//                .addFloat(mDateTimeGroup, "scaleY", 1, mDateScaleFactor)
//                .setStartDelay(.36f)
                //added by chenzhengjun end
                .build();

        updateSettingsAnimator();
    }

    protected void updateSettingsAnimator() {
        mSettingsAlpha = new TouchAnimator.Builder()
                //added by chenzhengjun start
//                .addFloat(mMultiUserSwitch, "translationY", -mGearTranslation, 0)
//                .addFloat(mMultiUserSwitch, "alpha", 0, 1)
//                .addFloat(mSettingsContainer, "translationY", -mGearTranslation, 0)
//                .addFloat(mSettingsButton, "rotation", -90, 0)
//                .addFloat(mSettingsContainer, "alpha", 0, 1)
//                .setStartDelay(QSAnimator.EXPANDED_TILE_DELAY)
                //added by chenzhengjun end
                .build();

        final boolean isRtl = isLayoutRtl();
      /*  if (isRtl && mDateTimeGroup.getWidth() == 0) {
            mDateTimeGroup.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    mDateTimeGroup.setPivotX(getWidth());
                    mDateTimeGroup.removeOnLayoutChangeListener(this);
                }
            });
        } else {
            mDateTimeGroup.setPivotX(isRtl ? mDateTimeGroup.getWidth() : 0);
        }*/
    }

    @Override
    public int getCollapsedHeight() {
        return getHeight();
    }

    @Override
    public int getExpandedHeight() {
        return getHeight();
    }

    @Override
    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
        mHeaderQsPanel.setExpanded(expanded);
        updateEverything();
    }

    @Override
    public void onNextAlarmChanged(AlarmManager.AlarmClockInfo nextAlarm) {
        mNextAlarm = nextAlarm;
        if (nextAlarm != null) {
            String alarmString = KeyguardStatusView.formatNextAlarm(getContext(), nextAlarm);
//            mAlarmStatus.setText(alarmString);
//            mAlarmStatus.setContentDescription(mContext.getString(
//                    R.string.accessibility_quick_settings_alarm, alarmString));
            // talpa@andy 2017/3/31 18:25 delete @{
            /*mAlarmStatusCollapsed.setContentDescription(mContext.getString(
                    R.string.accessibility_quick_settings_alarm, alarmString));*/
            // @}
        }
        if (mAlarmShowing != (nextAlarm != null)) {
            mAlarmShowing = nextAlarm != null;
            updateEverything();
        }
    }

    @Override
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        //talpa zhw mSecondHalfAnimator.setPosition(headerExpansionFraction);
        if (mShowFullAlarm) {
            mFirstHalfAnimator.setPosition(headerExpansionFraction);
        }
        mDateSizeAnimator.setPosition(headerExpansionFraction);
        mAlarmTranslation.setPosition(headerExpansionFraction);
        mSettingsAlpha.setPosition(headerExpansionFraction);

        updateAlarmVisibilities();

//        mExpandIndicator.setExpanded(headerExpansionFraction > EXPAND_INDICATOR_THRESHOLD);
    }


    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
            {
                TelephonyManager telmanger = (TelephonyManager)context.getSystemService(Service.TELEPHONY_SERVICE);
                int state = telmanger.getSimState();

            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //talpa zhw add
        this.registerServiceStateChangeReciver();
        //talpa zhw add end
    }

    @Override
    protected void onDetachedFromWindow() {
        setListening(false);
        mHost.getUserInfoController().remListener(this);
        mHost.getNetworkController().removeEmergencyListener(this);
        super.onDetachedFromWindow();

        //talpa zhw add
        this.unregisterServiceStateChangeReciver();
        //talpa zhw add end
    }

    private void updateAlarmVisibilities() {
//        mAlarmStatus.setVisibility(mAlarmShowing && mShowFullAlarm ? View.VISIBLE : View.INVISIBLE);
        /*talpa zhw
        mAlarmStatusCollapsed.setVisibility(mAlarmShowing ? View.VISIBLE : View.INVISIBLE);
        */
        //talpa zhw add

    }

    private void updateDateTimePosition() {
        // This one has its own because we have to rebuild it every time the alarm state changes.
        mAlarmTranslation = new TouchAnimator.Builder()
                .addFloat(mDateTimeAlarmGroup, "translationY", 0, mAlarmShowing
                        ? mDateTimeAlarmTranslation : mDateTimeTranslation)
                .build();
        mAlarmTranslation.setPosition(mExpansionAmount);
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mHeaderQsPanel.setListening(listening);
        mListening = listening;
        updateListeners();
    }

    @Override
    public void updateEverything() {
        updateDateTimePosition();
        updateVisibilities();

        setClickable(false);
    }

    protected void updateVisibilities() {
        // talpa@andy 2017/4/28 18:16 delete @{
       /* updateAlarmVisibilities();*/
        // @}
        /* talpa zhw pingbi
        mEmergencyOnly.setVisibility(mExpanded && mShowEmergencyCallsOnly
                ? View.VISIBLE : View.INVISIBLE);
                */
        //added by chenzhengjun start
//        mSettingsContainer.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
        //added by chenzhengjun end
//        mSettingsContainer.findViewById(R.id.tuner_icon).setVisibility(
//                TunerService.isTunerEnabled(mContext) ? View.VISIBLE : View.INVISIBLE);
        /* talpa zhw modify
        mMultiUserSwitch.setVisibility(mExpanded && mMultiUserSwitch.hasMultipleUsers()
                ? View.VISIBLE : View.INVISIBLE);
                */
        mMultiUserSwitch.setVisibility(mMultiUserSwitch.hasMultipleUsers()
                ? View.VISIBLE : View.GONE);
        //added by chenzhengjun start
        //mMultiUserSwitch.setVisibility(View.INVISIBLE);
        //added by chenzhengjun end
    }

    private void updateListeners() {
        if (mListening) {
            mNextAlarmController.addStateChangedCallback(this);
        } else {
            mNextAlarmController.removeStateChangedCallback(this);
        }
    }

    @Override
    public void setActivityStarter(ActivityStarter activityStarter) {
        mActivityStarter = activityStarter;
    }

    @Override
    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
        if (mQsPanel != null) {
            mMultiUserSwitch.setQsPanel(qsPanel);
        }
    }

    public void setupHost(final QSTileHost host) {
        mHost = host;
//        host.setHeaderView(mExpandIndicator);
        mHeaderQsPanel.setQSPanelAndHeader(mQsPanel, this);
        mHeaderQsPanel.setHost(host, null /* No customization in header */);
        setUserInfoController(host.getUserInfoController());
        setBatteryController(host.getBatteryController());
        setNextAlarmController(host.getNextAlarmController());

        final boolean isAPhone = mHost.getNetworkController().hasVoiceCallingFeature();
        if (isAPhone) {
            mHost.getNetworkController().addEmergencyListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        LogUtil.i("onClick");
        if (v == mSettingsButton) {
//            LogUtil.i("SettingsButton>onClick");
            MetricsLogger.action(mContext,
                    MetricsProto.MetricsEvent.ACTION_QS_EXPANDED_SETTINGS_LAUNCH);
            if (mSettingsButton.isTunerClick()) {
                mHost.startRunnableDismissingKeyguard(new Runnable() {
					@Override
					public void run() {
						post(new Runnable() {
							@Override
							public void run() {
							    if (TunerService.isTunerEnabled(mContext)) {
							        TunerService.showResetRequest(mContext, new Runnable() {
										@Override
										public void run() {
										    // Relaunch settings so that the tuner disappears.
										    startSettingsActivity();
										}
									});
							    } else {
							        Toast.makeText(getContext(), R.string.tuner_toast,
							                Toast.LENGTH_LONG).show();
							        TunerService.setTunerEnabled(mContext, true);
							    }
							    startSettingsActivity();

							}
						});
					}
				});
            } else {
                startSettingsActivity();
            }
        } else if (mNextAlarm != null) {
            PendingIntent showIntent = mNextAlarm.getShowIntent();
            if (showIntent != null && showIntent.isActivity()) {
                mActivityStarter.startActivity(showIntent.getIntent(), true /* dismissShade */);
            }
        }
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS),
                true /* dismissShade */);
    }

    @Override
    public void setNextAlarmController(NextAlarmController nextAlarmController) {
        mNextAlarmController = nextAlarmController;
    }

    @Override
    public void setBatteryController(BatteryController batteryController) {
        // Don't care
    }

    @Override
    public void setUserInfoController(UserInfoController userInfoController) {
        userInfoController.addListener(this);
    }

    @Override
    public void setCallback(Callback qsPanelCallback) {
        mHeaderQsPanel.setCallback(qsPanelCallback);
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
            }
        }
    }

    @Override
    public void onUserInfoChanged(String name, Drawable picture) {
        // talpa@andy 2017/3/31 15:05 delete @{
//      mMultiUserAvatar.setImageDrawable(picture);
        // @}
    }

    //talpa zhw add

    private static  final  int MSG_UPDATEEMERGENCYTEXT = 0xFF0F;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    // talpa@andy 2017/3/21 13:06 delete @{
  /*  Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MSG_UPDATEEMERGENCYTEXT:
                    updateEmergencyText();
                    break;
            }
        }
    };*/
    // @}
    private void updateEmergencyText() {
        StringBuilder content = new StringBuilder();
        SubscriptionManager mSubscriptionManager = SubscriptionManager.from(this.getContext());
        List<SubscriptionInfo> subs = mKeyguardUpdateMonitor.getSubscriptionInfo(false);
        int N = subs.size();
        if (N == 0) {
            content.append(getResources().getString(R.string.emergency_calls_only));
        }
        for (int i = 0; i < N; i++) {
            if (i >= 1) {
                content.append("|");
            }
            CharSequence carrierName = subs.get(i).getCarrierName();
            content.append(carrierName);
        }
        // add begin by lych for fix bug ncd#10329, set the 'airplane mode' again when layout direction changed
        if(isAirplaneModeOn()){
            content.setLength(0); // clear the content
            content.append(mContext.getString(R.string.airplane_mode));
        }
        // add end by lych for fix bug ncd#10329, set the 'airplane mode' again when layout direction changed
        mEmergencyOnly.setText(content);
    }
    public KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onRefreshCarrierInfo() {
            if (!mAirplaneMode) {
                updateEmergencyText();
            }
        }

        // talpa@andy 2017/3/20 22:23 add @{
        @Override
        public void onAirplaneModeChanged(boolean airplaneMode) {
            mAirplaneMode = airplaneMode;
            if (mAirplaneMode) {
                mEmergencyOnly.setText(getContext().getString(R.string.airplane_mode));
            } else {
                updateEmergencyText();
            }
        }// @}

        public void onFinishedGoingToSleep(int why) {};

        public void onStartedWakingUp() {};
    };

    private  void registerServiceStateChangeReciver()
    {
        mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.getContext());
        mKeyguardUpdateMonitor.registerCallback(mCallback);
        mKeyguardUpdateMonitor.setmQuickStatusBarHeaderCallback(mCallback);
        IntentFilter mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
        //this.getContext().registerReceiver(mServiceStateChangeReciver, mIntentFilter);
    }
    private  void unregisterServiceStateChangeReciver()
    {
        if (mKeyguardUpdateMonitor != null) {
            mKeyguardUpdateMonitor.removeCallback(mCallback);
        }
        //this.getContext().unregisterReceiver(mServiceStateChangeReciver);
    }
    private BroadcastReceiver mServiceStateChangeReciver = new  BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyIntents.ACTION_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
                int phoneId = intent.getIntExtra(PhoneConstants.SLOT_KEY,0);
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());

                //mHandler.sendEmptyMessage(MSG_UPDATEEMERGENCYTEXT);
            }
        }
    };
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        LogUtil.i("dispatchTouchEvent>ev"+ev.getActionMasked());
//        LogUtil.i("QuickStatusBarHeader>dispatchTouchEvent>y="+ev.getY());
        // talpa@andy 2017/4/14 17:51 fix bug：设置按钮旋转动画无法取消 @{
       /*if(mHeaderQsPanel.getVisibility() != View.VISIBLE)
        {
            if(ev.getY() > 109) {
                LogUtil.i("QuickStatusBarHeader>dispatchTouchEvent>false");
                return false;
            }
        }*/
        // @}
        return super.dispatchTouchEvent(ev);
    }

  /*  @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
        LogUtil.i("onInterceptHoverEvent>event"+event.getActionMasked());
        return super.onInterceptHoverEvent(event);
    }*/

   // add begin by lych for fix bug ncd#10329
    public boolean isAirplaneModeOn() {
      return Settings.Global.getInt(mContext.getContentResolver(),
              Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
    // add end by lych for fix bug ncd#10329

}
