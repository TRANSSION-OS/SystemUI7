/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.annotation.ColorInt;
import android.annotation.DrawableRes;
import android.annotation.LayoutRes;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.InputQueue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.internal.policy.IKeyguardService;
import com.android.internal.view.FloatingActionMode;
import com.android.internal.widget.FloatingToolbar;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.DragDownHelper;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.android.systemui.telephone.TelephoneBackView;

public class StatusBarWindowView extends FrameLayout {
    public static final String TAG = "StatusBarWindowView";
    public static final boolean DEBUG = BaseStatusBar.DEBUG;

    private DragDownHelper mDragDownHelper;
    private NotificationStackScrollLayout mStackScrollLayout;
    private NotificationPanelView mNotificationPanel;
    private View mBrightnessMirror;

    public IKeyguardService.Stub mbinder = null;

    private int mRightInset = 0;

    //linwujia add begin
    public boolean isBounce = false;
    public int maxBounceHeight;
    public int mTouchX = 0;
    public int mTouchY = 0;
    private TelephoneBackView telephoneBack;
    private TelephoneBackView keyguard_call_back;
    private Chronometer chronometer, chronometerKeyguard;
    private long time = -1;
    private TelephonyManager mTelephonyManager;
    private TelephonyStateListener mPhoneStateListener = new TelephonyStateListener();
    private final String RETURN_CALL_BROADCAST = "com.android.dialer.remind_statusbar";
    //linwujia add end

    private PhoneStatusBar mService;
    private final Paint mTransparentSrcPaint = new Paint();
    private FalsingManager mFalsingManager;

    // Implements the floating action mode for TextView's Cut/Copy/Past menu. Normally provided by
    // DecorView, but since this is a special window we have to roll our own.
    private View mFloatingActionModeOriginatingView;
    private ActionMode mFloatingActionMode;
    private FloatingToolbar mFloatingToolbar;
    private ViewTreeObserver.OnPreDrawListener mFloatingToolbarPreDrawListener;

    //linwujia add begin
    private BroadcastReceiver mCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("PhoneReturnCall", "action:" + action);
            if (action.equals(RETURN_CALL_BROADCAST)) {
                int showcall = intent.getIntExtra("showcall", 0); // 1 显示通话   2 后台通话
                int hold = intent.getIntExtra("hold", 0); // 1 hold  2 unhold
                long tempTime = intent.getLongExtra("time", -1); // 1451610339449
                Log.d("PhoneReturnCall", "action:" + action + ", showcall:" + showcall + ", hold:" + hold + ", tempTime:" + tempTime);
                if(tempTime != -1 && getTelecommManager().isInCall()) { // 有值的时候读取。
                    time = tempTime;
                }
                if (showcall == 2) {
                    /*if(!getTelecommManager().isInCall()) {
                        time = -1;
                        return;
                    }*/
                    setTelephoneBack(View.VISIBLE);
                    mService.setReturnCall(true);
                    startChronometer();
                } else if (showcall == 3) {
                    setTelephoneBack(View.VISIBLE);
                    mService.setReturnCall(true);
                    startChronometer();
                } else if (showcall == 1) {
                    setTelephoneBack(View.INVISIBLE);
                    mService.setReturnCall(false);
                }

                startChronometer();

                if (hold ==1) {
                    // TODO
                } else if (hold == 2) {
                    // TODO
                }
            }
        }
    };

    public void showKeyguardView()
    {
        if(mbinder!=null)
        {
            try {
                mbinder.onSystemReady();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else
        {
            ConntectKeyguard();
            Log.d("line=350", "line=350-mRunable-ClockView-1");
        }
    }

    public void ConntectKeyguard()
    {
        ComponentName cn = new ComponentName("com.android.systemui",
                "com.android.systemui.keyguard.KeyguardService");
        Intent intent = new Intent();
        intent.setComponent(cn);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mbinder = (IKeyguardService.Stub)service;
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {

            }

        };
        if (mContext.bindServiceAsUser(intent, conn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT))
        {

        }

    }

    // / M: in order to register for separate listener, use extends @{
    private class TelephonyStateListener extends PhoneStateListener {

        TelephonyStateListener() { super(); }

        @Override
        public void onCallStateChanged(int state, String ignored) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                mService.setReturnCall(false);
                delayRemoveCall();
            } else {
                if (mService.getBarState() == StatusBarState.KEYGUARD && state == TelephonyManager.CALL_STATE_OFFHOOK) {
                    keyguard_call_back.setVisibility(View.VISIBLE);
                } else {
                    keyguard_call_back.setVisibility(View.INVISIBLE);
                }
            }
        }
    }
    //linwujia add end

    public StatusBarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMotionEventSplittingEnabled(false);
        mTransparentSrcPaint.setColor(0);
        mTransparentSrcPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mFalsingManager = FalsingManager.getInstance(context);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (getFitsSystemWindows()) {
            boolean paddingChanged = insets.left != getPaddingLeft()
                    || insets.top != getPaddingTop()
                    || insets.bottom != getPaddingBottom();

            // Super-special right inset handling, because scrims and backdrop need to ignore it.
            if (insets.right != mRightInset) {
                mRightInset = insets.right;
                applyMargins();
            }
            // Drop top inset, apply left inset and pass through bottom inset.
            if (paddingChanged) {
                setPadding(insets.left, 0, 0, 0);
            }
            insets.left = 0;
            insets.top = 0;
            insets.right = 0;
        } else {
            if (mRightInset != 0) {
                mRightInset = 0;
                applyMargins();
            }
            boolean changed = getPaddingLeft() != 0
                    || getPaddingRight() != 0
                    || getPaddingTop() != 0
                    || getPaddingBottom() != 0;
            if (changed) {
                setPadding(0, 0, 0, 0);
            }
            insets.top = 0;
        }
        return false;
    }

    private void applyMargins() {
        final int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View child = getChildAt(i);
            if (child.getLayoutParams() instanceof LayoutParams) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.ignoreRightInset && lp.rightMargin != mRightInset) {
                    lp.rightMargin = mRightInset;
                    child.requestLayout();
                }
            }
        }
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mStackScrollLayout = (NotificationStackScrollLayout) findViewById(
                R.id.notification_stack_scroller);
        mNotificationPanel = (NotificationPanelView) findViewById(R.id.notification_panel);
        mBrightnessMirror = findViewById(R.id.brightness_mirror);
    }

    public void setService(PhoneStatusBar service) {
        mService = service;
        mDragDownHelper = new DragDownHelper(getContext(), this, mStackScrollLayout, mService);
    }

    @Override
    protected void onAttachedToWindow () {
        super.onAttachedToWindow();

        telephoneBack = (TelephoneBackView) findViewById(R.id.telephone_back_layout);
        keyguard_call_back = (TelephoneBackView) findViewById(R.id.keyguard_call_back);
        keyguard_call_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                resumeCall();
            }
        });

        // We need to ensure that our window doesn't suffer from overdraw which would normally
        // occur if our window is translucent. Since we are drawing the whole window anyway with
        // the scrim, we don't need the window to be cleared in the beginning.
        if (mService.isScrimSrcModeEnabled()) {
            IBinder windowToken = getWindowToken();
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
            lp.token = windowToken;
            setLayoutParams(lp);
            WindowManagerGlobal.getInstance().changeCanvasOpacity(windowToken, true);
            setWillNotDraw(false);
        } else {
            setWillNotDraw(!DEBUG);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(RETURN_CALL_BROADCAST);
        mContext.registerReceiver(mCallReceiver, filter);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mCallReceiver != null) {
            getContext().unregisterReceiver(mCallReceiver);
        }
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                // Talpa:PeterHuang add 同返回键处理 @{
            case KeyEvent.KEYCODE_HOME:
                // @}
                if (!down) {
                    mService.onBackPressed();
                }
                if(mService.getBarState() == StatusBarState.SHADE_LOCKED) {
                    mService.takeToKeyguard(); // wangying add 9047 锁屏页下拉通知栏，点击物理back键无法返回至锁屏页
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                if (!down) {
                    return mService.onMenuPressed();
                }
            case KeyEvent.KEYCODE_SPACE:
                if (!down) {
                    return mService.onSpacePressed();
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                //add by wangying begin
                if(mService.mAccidentWindowManager != null){
                    if(mService.mAccidentWindowManager.isOpen()){
                        if(mService.mAccidentWindowManager.volumeKeyDown()){
                            return true;
                        }
                    }

                }
                //add by wangying end
            case KeyEvent.KEYCODE_VOLUME_UP:
                //add by wangying begin
                if(mService.mAccidentWindowManager != null){
                    if(mService.mAccidentWindowManager.isOpen()){
                        if(mService.mAccidentWindowManager.volumeKeyUp()){
                            return true;
                        }
                    }
                }
                //add by wangying end
                if (mService.isDozing()) {
                    MediaSessionLegacyHelper.getHelper(mContext).sendVolumeKeyEvent(event, true);
                    return true;
                }
                break;
        }
        if (mService.interceptMediaKey(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        mFalsingManager.onTouchEvent(ev, getWidth(), getHeight());
        if (mBrightnessMirror != null && mBrightnessMirror.getVisibility() == VISIBLE) {
            // Disallow new pointers while the brightness mirror is visible. This is so that you
            // can't touch anything other than the brightness slider while the mirror is showing
            // and the rest of the panel is transparent.
            if (ev.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                return false;
            }
        }
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mStackScrollLayout.closeControlsIfOutsideTouch(ev);
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        if (mNotificationPanel.isFullyExpanded()
                && mStackScrollLayout.getVisibility() == View.VISIBLE
                && mService.getBarState() == StatusBarState.KEYGUARD
                && !mService.isBouncerShowing()) {
            intercept = mDragDownHelper.onInterceptTouchEvent(ev);
            // wake up on a touch down event, if dozing
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mService.wakeUpIfDozing(ev.getEventTime(), ev);
            }
        }
        if (!intercept) {
            super.onInterceptTouchEvent(ev);
        }
        if (intercept) {
            MotionEvent cancellation = MotionEvent.obtain(ev);
            cancellation.setAction(MotionEvent.ACTION_CANCEL);
            mStackScrollLayout.onInterceptTouchEvent(cancellation);
            mNotificationPanel.onInterceptTouchEvent(cancellation);
            cancellation.recycle();
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;

        // linwujia add for fix tfs bug 11956 begin
        final float y = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(getTelecommManager().isInCall() && mService.returnCallShowing()) {
                    int statusHeight = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
                    if(y > 0.1f && y < statusHeight) { // 点击返回通话
                        resumeCall();
                        if(mService.getBarState() == StatusBarState.KEYGUARD)
                            return true; // 防止屏幕抖动
                    }
                }
        }
        // linwujia add for fix tfs bug 11956 end

        if (mService.getBarState() == StatusBarState.KEYGUARD) {
            handled = mDragDownHelper.onTouchEvent(ev);
        }
        if (!handled) {
            handled = super.onTouchEvent(ev);
        }
        final int action = ev.getAction();
        if (!handled && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)) {
            mService.setInteracting(StatusBarManager.WINDOW_STATUS_BAR, false);
        }
        return handled;
    }

    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mService.isScrimSrcModeEnabled()) {
            // We need to ensure that our window is always drawn fully even when we have paddings,
            // since we simulate it to be opaque.
            int paddedBottom = getHeight() - getPaddingBottom();
            int paddedRight = getWidth() - getPaddingRight();
            if (getPaddingTop() != 0) {
                canvas.drawRect(0, 0, getWidth(), getPaddingTop(), mTransparentSrcPaint);
            }
            if (getPaddingBottom() != 0) {
                canvas.drawRect(0, paddedBottom, getWidth(), getHeight(), mTransparentSrcPaint);
            }
            if (getPaddingLeft() != 0) {
                canvas.drawRect(0, getPaddingTop(), getPaddingLeft(), paddedBottom,
                        mTransparentSrcPaint);
            }
            if (getPaddingRight() != 0) {
                canvas.drawRect(paddedRight, getPaddingTop(), getWidth(), paddedBottom,
                        mTransparentSrcPaint);
            }
        }
        if (DEBUG) {
            Paint pt = new Paint();
            pt.setColor(0x80FFFF00);
            pt.setStrokeWidth(12.0f);
            pt.setStyle(Paint.Style.STROKE);
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), pt);
        }
    }

    public void cancelExpandHelper() {
        if (mStackScrollLayout != null) {
            mStackScrollLayout.cancelExpandHelper();
        }
    }

    public class LayoutParams extends FrameLayout.LayoutParams {

        public boolean ignoreRightInset;

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.StatusBarWindowView_Layout);
            ignoreRightInset = a.getBoolean(
                    R.styleable.StatusBarWindowView_Layout_ignoreRightInset, false);
            a.recycle();
        }
    }

    @Override
    public ActionMode startActionModeForChild(View originalView, ActionMode.Callback callback,
            int type) {
        if (type == ActionMode.TYPE_FLOATING) {
            return startActionMode(originalView, callback, type);
        }
        return super.startActionModeForChild(originalView, callback, type);
    }

    private ActionMode createFloatingActionMode(
            View originatingView, ActionMode.Callback2 callback) {
        if (mFloatingActionMode != null) {
            mFloatingActionMode.finish();
        }
        cleanupFloatingActionModeViews();
        final FloatingActionMode mode =
                new FloatingActionMode(mContext, callback, originatingView);
        mFloatingActionModeOriginatingView = originatingView;
        mFloatingToolbarPreDrawListener =
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mode.updateViewLocationInWindow();
                        return true;
                    }
                };
        return mode;
    }

    private void setHandledFloatingActionMode(ActionMode mode) {
        mFloatingActionMode = mode;
        mFloatingToolbar = new FloatingToolbar(mContext, mFakeWindow);
        ((FloatingActionMode) mFloatingActionMode).setFloatingToolbar(mFloatingToolbar);
        mFloatingActionMode.invalidate();  // Will show the floating toolbar if necessary.
        mFloatingActionModeOriginatingView.getViewTreeObserver()
                .addOnPreDrawListener(mFloatingToolbarPreDrawListener);
    }

    private void cleanupFloatingActionModeViews() {
        if (mFloatingToolbar != null) {
            mFloatingToolbar.dismiss();
            mFloatingToolbar = null;
        }
        if (mFloatingActionModeOriginatingView != null) {
            if (mFloatingToolbarPreDrawListener != null) {
                mFloatingActionModeOriginatingView.getViewTreeObserver()
                        .removeOnPreDrawListener(mFloatingToolbarPreDrawListener);
                mFloatingToolbarPreDrawListener = null;
            }
            mFloatingActionModeOriginatingView = null;
        }
    }

    private ActionMode startActionMode(
            View originatingView, ActionMode.Callback callback, int type) {
        ActionMode.Callback2 wrappedCallback = new ActionModeCallback2Wrapper(callback);
        ActionMode mode = createFloatingActionMode(originatingView, wrappedCallback);
        if (mode != null && wrappedCallback.onCreateActionMode(mode, mode.getMenu())) {
            setHandledFloatingActionMode(mode);
        } else {
            mode = null;
        }
        return mode;
    }

    //linwujia add begin
    private TelecomManager getTelecommManager() {
        return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    private void setTelephoneBack(int visibility) {
        if(visibility == View.VISIBLE) {
            telephoneBack.setVisibility(visibility);
            if (mService.getBarState() == StatusBarState.KEYGUARD) {
                keyguard_call_back.setVisibility(visibility);
            }
        } else {
            telephoneBack.setVisibility(visibility);
            keyguard_call_back.setVisibility(visibility);
        }
    }

    private void startChronometer(){
        if(time != -1) { // 计时开始时间
            long duration;
            if(SystemProperties.get("ro.call.showtime").equals("1")){
                duration = SystemClock.elapsedRealtime() - time;
            }else{
                duration = System.currentTimeMillis() - time;
            }
            startChronometer(duration);
        }else {
            telephoneBack.findViewById(R.id.telephone_chronometer).setVisibility(View.INVISIBLE);
            keyguard_call_back.findViewById(R.id.telephone_chronometer).setVisibility(View.INVISIBLE);
        }
    }

    private void startChronometer(long duration) {
        //获取计时器组件
        chronometer = (Chronometer) telephoneBack.findViewById(R.id.telephone_chronometer);
        chronometerKeyguard = (Chronometer) keyguard_call_back.findViewById(R.id.telephone_chronometer);
        chronometer.setVisibility(View.VISIBLE); //设置起始时间(elapsedRealtime是从开机到现在的毫秒数)
        chronometerKeyguard.setVisibility(View.VISIBLE);
        chronometer.setBase(SystemClock.elapsedRealtime() - duration);
        chronometerKeyguard.setBase(SystemClock.elapsedRealtime() - duration);
        chronometer.setFormat("%s"); // 设置时间显示格式   ch.setFormat("通话：%s")
        chronometerKeyguard.setFormat("%s");
        chronometer.start(); // 计时器启动
        chronometerKeyguard.start();
        //添加监听器
//		chronometer.setOnChronometerTickListener(new OnChronometerTickListener() {
//	    	@Override public void onChronometerTick(Chronometer chronometer) {
//	    		if(SystemClock.elapsedRealtime() - chronometer.getBase() >= 10000) chronometer.stop();//停止计时器
//			}
//		});
        telephoneBack.chronometerAnimation();
        keyguard_call_back.chronometerAnimation();
    }

    private void delayRemoveCall() {
        try {
            if (time != -1) {
                onRecordPause();
                setCallText(R.string.call_end);
            } else
                setCallText(R.string.call_fail);
        } catch (Exception e) {
            Log.e("StatusBarCall", "StatusBar_CallBack_Error");
        }
        time = -1;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                removeSmallWindow(mContext);
            }
        }, 600);
    }

    private void onRecordPause() {
        if(chronometer != null) {
            chronometer.stop(); // 停止计时
        }
        if(chronometerKeyguard != null) {
            chronometerKeyguard.stop(); // 停止计时
        }
    }

    private void setCallText(int resId) {
        TextView callTextView = (TextView) telephoneBack.findViewById(R.id.telephone_back_text);
        TextView callTextKeyguard = (TextView) keyguard_call_back.findViewById(R.id.telephone_back_text);
        callTextView.setText(telephoneBack.getContext().getText(resId));
        callTextKeyguard.setText(keyguard_call_back.getContext().getText(resId));
    }

    /** * 将小悬浮窗从屏幕上移除。
     * @param context 必须为应用程序的Context. */
    private void removeSmallWindow(Context context) {
        if(chronometer != null) {
            chronometer.setText("");
            chronometer.setVisibility(View.INVISIBLE);
            chronometer.stop();//停止计时器
//			chronometer = null;
        }
        if(chronometerKeyguard != null) {
            chronometerKeyguard.setText("");
            chronometerKeyguard.setVisibility(View.INVISIBLE);
            chronometerKeyguard.stop();//停止计时器
//			chronometerKeyguard = null;
        }
        setCallText(R.string.click_return_telephone);
        setTelephoneBack(View.INVISIBLE);
    }

    public void keyguardTelephoneBack(int visibility) {
        if(keyguard_call_back != null) {
            keyguard_call_back.setVisibility(visibility);
        }
    }

    //linwujia add end

    private class ActionModeCallback2Wrapper extends ActionMode.Callback2 {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallback2Wrapper(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            requestFitSystemWindows();
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            if (mode == mFloatingActionMode) {
                cleanupFloatingActionModeViews();
                mFloatingActionMode = null;
            }
            requestFitSystemWindows();
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
            if (mWrapped instanceof ActionMode.Callback2) {
                ((ActionMode.Callback2) mWrapped).onGetContentRect(mode, view, outRect);
            } else {
                super.onGetContentRect(mode, view, outRect);
            }
        }
    }

    /**
     * Minimal window to satisfy FloatingToolbar.
     */
    private Window mFakeWindow = new Window(mContext) {
        @Override
        public void takeSurface(SurfaceHolder.Callback2 callback) {
        }

        @Override
        public void takeInputQueue(InputQueue.Callback callback) {
        }

        @Override
        public boolean isFloating() {
            return false;
        }

        @Override
        public void alwaysReadCloseOnTouchAttr() {
        }

        @Override
        public void setContentView(@LayoutRes int layoutResID) {
        }

        @Override
        public void setContentView(View view) {
        }

        @Override
        public void setContentView(View view, ViewGroup.LayoutParams params) {
        }

        @Override
        public void addContentView(View view, ViewGroup.LayoutParams params) {
        }

        @Override
        public void clearContentView() {
        }

        @Override
        public View getCurrentFocus() {
            return null;
        }

        @Override
        public LayoutInflater getLayoutInflater() {
            return null;
        }

        @Override
        public void setTitle(CharSequence title) {
        }

        @Override
        public void setTitleColor(@ColorInt int textColor) {
        }

        @Override
        public void openPanel(int featureId, KeyEvent event) {
        }

        @Override
        public void closePanel(int featureId) {
        }

        @Override
        public void togglePanel(int featureId, KeyEvent event) {
        }

        @Override
        public void invalidatePanelMenu(int featureId) {
        }

        @Override
        public boolean performPanelShortcut(int featureId, int keyCode, KeyEvent event, int flags) {
            return false;
        }

        @Override
        public boolean performPanelIdentifierAction(int featureId, int id, int flags) {
            return false;
        }

        @Override
        public void closeAllPanels() {
        }

        @Override
        public boolean performContextMenuIdentifierAction(int id, int flags) {
            return false;
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
        }

        @Override
        public void setBackgroundDrawable(Drawable drawable) {
        }

        @Override
        public void setFeatureDrawableResource(int featureId, @DrawableRes int resId) {
        }

        @Override
        public void setFeatureDrawableUri(int featureId, Uri uri) {
        }

        @Override
        public void setFeatureDrawable(int featureId, Drawable drawable) {
        }

        @Override
        public void setFeatureDrawableAlpha(int featureId, int alpha) {
        }

        @Override
        public void setFeatureInt(int featureId, int value) {
        }

        @Override
        public void takeKeyEvents(boolean get) {
        }

        @Override
        public boolean superDispatchKeyEvent(KeyEvent event) {
            return false;
        }

        @Override
        public boolean superDispatchKeyShortcutEvent(KeyEvent event) {
            return false;
        }

        @Override
        public boolean superDispatchTouchEvent(MotionEvent event) {
            return false;
        }

        @Override
        public boolean superDispatchTrackballEvent(MotionEvent event) {
            return false;
        }

        @Override
        public boolean superDispatchGenericMotionEvent(MotionEvent event) {
            return false;
        }

        @Override
        public View getDecorView() {
            return StatusBarWindowView.this;
        }

        @Override
        public View peekDecorView() {
            return null;
        }

        @Override
        public Bundle saveHierarchyState() {
            return null;
        }

        @Override
        public void restoreHierarchyState(Bundle savedInstanceState) {
        }

        @Override
        protected void onActive() {
        }

        @Override
        public void setChildDrawable(int featureId, Drawable drawable) {
        }

        @Override
        public void setChildInt(int featureId, int value) {
        }

        @Override
        public boolean isShortcutKey(int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public void setVolumeControlStream(int streamType) {
        }

        @Override
        public int getVolumeControlStream() {
            return 0;
        }

        @Override
        public int getStatusBarColor() {
            return 0;
        }

        @Override
        public void setStatusBarColor(@ColorInt int color) {
        }

        @Override
        public int getNavigationBarColor() {
            return 0;
        }

        @Override
        public void setNavigationBarColor(@ColorInt int color) {
        }

        @Override
        public void setDecorCaptionShade(int decorCaptionShade) {
        }

        @Override
        public void setResizingCaptionDrawable(Drawable drawable) {
        }

        @Override
        public void onMultiWindowModeChanged() {
        }

        @Override
        public void reportActivityRelaunched() {
        }
    };

}

