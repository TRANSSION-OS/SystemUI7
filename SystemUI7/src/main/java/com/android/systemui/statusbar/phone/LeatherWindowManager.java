package com.android.systemui.statusbar.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UEventObserver;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.statusbar.phoneleather.LeatherAudioProfilesController;
import com.android.systemui.statusbar.phoneleather.LeatherWindowView;
import com.android.systemui.statusbar.phoneleather.util.LeatherUtil;

/**
 * Created by wujia.lin on 2017/2/14.
 */

public class LeatherWindowManager implements LeatherWindowView.Callback {
    public static final String ClockUevent = "com.android.systemui.clockuevent";

    public static final String ALARM_ALERT = "com.android.deskclock.ALARM_ALERT";
    public static final String ALARM_DONE = "com.android.deskclock.ALARM_DONE";

    public static final boolean DEBUG = false;

    private Context mContext;
    private PhoneStatusBar mPhoneStatusBar;

    private LeatherAudioProfilesController mLeatherAudioProfilesController;

    private WindowManager mWindowManager;
    private LeatherWindowView mLeatherView;
    private WindowManager.LayoutParams mLp;

    private TelephonyManager mTelephonyManager;
    private TelephonyStateListener mPhoneStateListener = new TelephonyStateListener();

    private int mHallState= -1;
    private int isAlarmStart = -1;
    private int isPhoneCome = -1;
    private boolean incomingFlag  = false;
    private Handler mHandler;

    private PowerManager mPM;
    private PowerManager.WakeLock mShowKeyguardWakeLock = null;

    private Runnable mRunable = new Runnable() {

        @Override
        public void run() {
            Log.d("", "-line=206------ClockView--mRunable-Run--");
            showOrHideClockView();
            WakeScreen();
        }
    };

    private Runnable mClockViewAnimateRunable = new Runnable() {

        @Override
        public void run() {
            int callstate = mTelephonyManager.getCallState();
            Log.d("Leather", "mClockViewAnimateRunable===callstate=" + callstate);
            if(callstate != TelephonyManager.CALL_STATE_RINGING  && callstate != TelephonyManager.CALL_STATE_OFFHOOK) {
                mLeatherView.setViewPagerVisibility(View.VISIBLE);
                mLeatherView.setColockViewVisibility(View.VISIBLE);
            } else {
                mLeatherView.setViewPagerVisibility(View.VISIBLE);
                mLeatherView.setVisibility(View.GONE);
            }
        }
    };

    private Runnable mShowClockViewWithoutAnimateRunable = new Runnable() {

        @Override
        public void run() {
            if(isAlarmStart != 1 && isPhoneCome != 1) {
                mLeatherView.setVisibility(View.VISIBLE);
            }
            WakeScreen();
        }
    };

    private Runnable mRemoveClockViewRunable = new Runnable() {

        @Override
        public void run() {
            mLeatherView.setVisibility(View.GONE);
        }
    };

    private Runnable mShowClockViewRunable = new Runnable() {

        @Override
        public void run() {
            if(mLeatherView != null && mLeatherView.getVisibility() != View.VISIBLE) {
                if(mPM.isScreenOn()) {
                    mLeatherView.setVisibility(View.VISIBLE);
                    mLeatherView.reDrawClockView();
                } else {
                    mLeatherView.setVisible();
                }
            }
        }
    };

    private Runnable mShowKeyguardViewRunable = new Runnable() {

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            showKeyguardView();
            long end = System.currentTimeMillis();
            Log.d("Leather", "mShowKeyguardViewRunable wasted time :" + (end - start));
        }
    };

    private Runnable mScreenOffRunable = new Runnable() {

        @Override
        public void run() {
            mPM.goToSleep(SystemClock.uptimeMillis());
        }
    };

    private UEventObserver mHallObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            String hallString = event.get("SWITCH_STATE");
            int state = Integer.parseInt(hallString);

            updateHallState(state);
        }
    };

    public LeatherWindowManager(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mLeatherAudioProfilesController = SystemUIFactory.getInstance().createLeatherAudioProfilesController(mContext);

        mPM = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mShowKeyguardWakeLock = mPM.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "show keyguard");//change by george
        mShowKeyguardWakeLock.setReferenceCounted(false);

        mHallObserver.startObserving("DEVPATH=/devices/virtual/switch/hall");
        mHandler = new Handler();

        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ALARM_ALERT);
        intentFilter.addAction(ALARM_DONE);
        mContext.registerReceiver(mAlarmReceiver, intentFilter);

        if(DEBUG) {
            IntentFilter filter = new IntentFilter("com.android.systemui.phoneleather");
            mContext.registerReceiver(mDemoReceiver, filter);
        }
    }

    public void add(View leatherView) {
        mLp = new WindowManager.LayoutParams();
        mLp.gravity = Gravity.TOP;
        mLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        //mLpLeather.type = WindowManager.LayoutParams.TYPE_TOAST;
        mLp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mLp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        /*mLp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;*/
        mLp.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                & ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        mLp.format = PixelFormat.RGB_565;
        mLeatherView = (LeatherWindowView) leatherView;
        mWindowManager.addView(mLeatherView, mLp);
        mLeatherView.setVisibility(View.GONE);
        mLeatherView.setSystemUiVisibility(View.STATUS_BAR_DISABLE_HOME);

        mLeatherView.setCallback(this);
        mLeatherView.setLeatherAudioProfilesController(mLeatherAudioProfilesController);

        mHallState = LeatherUtil.readHallState();
        if(mHallState == 1 && !LeatherUtil.bootFromPoweroffAlarm()) {
            mHandler.removeCallbacks(mShowClockViewRunable);
            mHandler.postDelayed(mShowClockViewRunable, 2500);
        }
    }

    public void setPhoneStatusBar(PhoneStatusBar phoneStatusBar) {
        mPhoneStatusBar = phoneStatusBar;
    }

    public void onScreenTurnedOn() {
        if(mHallState == 1 && mLeatherView != null && isPhoneCome != 1 && isAlarmStart != 1/* && mLeatherView.getVisibility() == View.VISIBLE*/) {
            mLeatherView.setVisible();
            mLeatherView.setColockViewVisibility(View.VISIBLE);
            WakeScreen();
            return;
        }
    }

    public void onScreenTurnedOff() {
        if(mHallState == 1 && mLeatherView != null && mLeatherView.getVisibility() == View.VISIBLE) {
            mLeatherView.setColockViewVisibility(View.GONE);
            mLeatherView.release(false);

        }
    }

    /**
     *
     * @return return true if the leather. phone or alarm is shown, otherwise return false
     */
    public boolean isVisibility() {
        return (mHallState == 1 || isPhoneCome == 1 || isAlarmStart == 1);
    }

    private void showOrHideClockView()
    {
        /*if(mAccidentWindowManager != null && mAccidentWindowManager.isShowWindow()) {
            mAccidentWindowManager.accidentWindowDismiss();
        }*/
        if(mHallState == 1) {
            if(isPhoneCome == 1)//phone come
            {
                if(mLeatherView != null && mLeatherView.getVisibility() != View.GONE)
                {
                    mHandler.removeCallbacks(mRemoveClockViewRunable);
                    mHandler.postDelayed(mRemoveClockViewRunable, 1100);

                }
                Log.d("", "line=246--showOrHideClockView-phone-come--miss-ClockView");
            }else if(isAlarmStart == 1){//alarm come
                if(mLeatherView != null && mLeatherView.getVisibility() != View.GONE)
                {
                    mHandler.removeCallbacks(mRemoveClockViewRunable);
                    mHandler.postDelayed(mRemoveClockViewRunable, 80);
                }
                Log.d("", "line=420--showOrHideClockView-alarm-come--miss-ClockView");
            } else {
				/*if (mCurrentState.statusBarState != StatusBarState.KEYGUARD && mCurrentState.statusBarState != StatusBarState.SHADE_LOCKED) {*/

                Log.d("", "line=251-showOrHideClockView---normal--or--alarm-end-phone-end--show-ClockView");
                if(mLeatherView != null && mLeatherView.getVisibility() != View.VISIBLE  && mPM.isScreenOn()) {
                    int callstate = mTelephonyManager.getCallState();
                    Log.d("Leather", "showOrHideClockView---callstate=" + callstate);
                    if(callstate != TelephonyManager.CALL_STATE_RINGING  && callstate != TelephonyManager.CALL_STATE_OFFHOOK) {
                        mLeatherView.setVisibility(View.VISIBLE);
                        mLeatherView.setViewPagerVisibility(View.GONE);
                        mHandler.removeCallbacks(mClockViewAnimateRunable);
                        mHandler.postDelayed(mClockViewAnimateRunable, 300);
                        //mShowKeyguardViewRunable.run();
                    }
                } else {
                    mHandler.removeCallbacks(mShowClockViewRunable);
                    mHandler.post(mShowClockViewRunable);
                }

                mHandler.removeCallbacks(mShowKeyguardViewRunable);
                mHandler.post(mShowKeyguardViewRunable);
                //mShowKeyguardViewRunable.run();
				/*} else {
					mHandler.removeCallbacks(mShowClockViewRunable);
					mHandler.post(mShowClockViewRunable);
				}*/


            }
        } else{
            mHandler.removeCallbacks(mShowKeyguardViewRunable);
            if(mLeatherView != null && mLeatherView.getVisibility() != View.GONE)
            {
                mLeatherView.setColockViewVisibility(View.GONE);
                mLeatherView.setVisibility(View.GONE);
                mLeatherView.release(false);
            }

        }

    }

    private void updateHallState(int state) {
        if(mHallState == state) {
            return;
        }
        mHallState = state;

        Log.d("onUEvent", "---onUEvent----ClockView----start to send broadcast action:" + LeatherWindowManager.ClockUevent + " and state=" + state + ", time=" + System.currentTimeMillis());
        Intent timerIntent = new Intent();
        timerIntent.setAction(LeatherWindowManager.ClockUevent);
        timerIntent.putExtra("state", mHallState);
        mContext.sendBroadcast(timerIntent);
        Log.d("onUEvent", "---onUEvent----ClockView----send broadcast end action:" + LeatherWindowManager.ClockUevent + " and state=" + state + ", time=" + System.currentTimeMillis());

        //linwujia add begin
            /*if(!mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser())) {
                if(state == 1) {
                    mContext.sendBroadcast(mScreenOffIntent);
                } else if(state == 0) {
                    mContext.sendBroadcast(mScreenOnIntent);
                }
            }*/
        //linwujia add end

        Log.d("", "---onUEvent----ClockView----mHallState="+mHallState);
        mHandler.removeCallbacks(mRunable);
        mHandler.post(mRunable);
    }

    //点亮屏幕10s
    private void WakeScreen()
    {
        boolean isScreenOn = mPM.isScreenOn();
        Log.d("onUEvent", "WakeScreen isScreenOn:" + isScreenOn + ", mHallState:" + mHallState + ", isPhoneCome:" + isPhoneCome);
        if(mHallState == 0 || (mHallState == 1 && isScreenOn)) {
            Log.d("onUEvent", "start to wakescreen");
            mShowKeyguardWakeLock.acquire(10000);
            Log.d("onUEvent", "wakescreen end");
        }
        SleepScreen();
    }

    private void showKeyguardView() {
        mPhoneStatusBar.takeToKeyguard();
    }

    private void SleepScreen() {
        mHandler.removeCallbacks(mScreenOffRunable);
        if(mHallState == 1 && isAlarmStart != 1 && isPhoneCome != 1) {
            Log.d("mScreenOffRunable", "call mScreenOffRunable");
            mHandler.postDelayed(mScreenOffRunable, 10000);
        }
    }

    @Override
    public void userActivity() {
        WakeScreen();
    }

    @Override
    public void cleanGoToSleep() {
        mHandler.removeCallbacks(mScreenOffRunable);
    }

    // / M: in order to register for separate listener, use extends @{
    private class TelephonyStateListener extends PhoneStateListener {

        TelephonyStateListener() {
            super();
        }

        @Override
        public void onCallStateChanged(int state, String ignored) {

            if((state == TelephonyManager.CALL_STATE_RINGING)||(state ==TelephonyManager.CALL_STATE_OFFHOOK) )
            {
                incomingFlag = true;
                isPhoneCome = 1;// Phone ringing or on the phone
                mHandler.removeCallbacks(mRunable);
                mHandler.post(mRunable);
                Log.d("line=166", "-onCallStateChanged--state--come--ring-or--offhook--ClockView"+state);
            }else if(state == TelephonyManager.CALL_STATE_IDLE)
            {
                if(incomingFlag)
                {
                    isPhoneCome = -1;//Phone call end
                    incomingFlag = false;
                    Log.d("line=173", "onCallStateChanged---phonecall-end-ClockView");
                    //&&mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser())
                    if(mHallState==1)
                    {
                        mHandler.removeCallbacks(mShowClockViewWithoutAnimateRunable);
                        mHandler.postDelayed(mShowClockViewWithoutAnimateRunable, 60);
                    }
                }
            }

        }
    }

    private BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ALARM_ALERT))
            {
                isAlarmStart = 1; //alarm come
                mHandler.removeCallbacks(mRunable);
                mHandler.post(mRunable);
                Log.d("line=134", "-mAlarmReceiver--alarm-start--ClockView");
            } else if (action.equals(ALARM_DONE))
            {
                isAlarmStart = -1;//alarm end
                Log.d("line=137", "-mAlarmReceiver--alarm-end--ClockView");
                //&&mLockPatternUtils.isLockScreenDisabled(ActivityManager.getCurrentUser())
                if(mHallState==1)
                {
                    mHandler.removeCallbacks(mShowClockViewWithoutAnimateRunable);
                    mHandler.post(mShowClockViewWithoutAnimateRunable);
                }
            }
        }
    };

    private BroadcastReceiver mDemoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("hallstate", 0);
            updateHallState(state);
        }
    };
}
