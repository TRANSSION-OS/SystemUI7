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
package com.android.keyguard;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import java.util.List;
import java.util.Locale;

public class KeyguardPatternView extends LinearLayout implements KeyguardSecurityView,
        AppearAnimationCreator<LockPatternView.CellState>,
        EmergencyButton.EmergencyButtonCallback {

    private static final String TAG = "SecurityPatternView";
    private static final boolean DEBUG = KeyguardConstants.DEBUG;

    // how long before we clear the wrong pattern
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;

    // how long we stay awake after each key beyond MIN_PATTERN_BEFORE_POKE_WAKELOCK
    private static final int UNLOCK_PATTERN_WAKE_INTERVAL_MS = 7000;

    // how many cells the user has to cross before we poke the wakelock
    private static final int MIN_PATTERN_BEFORE_POKE_WAKELOCK = 2;

    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final AppearAnimationUtils mAppearAnimationUtils;
    private final DisappearAnimationUtils mDisappearAnimationUtils;

    private CountDownTimer mCountdownTimer = null;
    private LockPatternUtils mLockPatternUtils;
    private AsyncTask<?, ?, ?> mPendingLockCheck;
    private LockPatternView mLockPatternView;
    private KeyguardSecurityCallback mCallback;

    //add by wangying begin
    private ViewGroup grid;
    private  View[] cellList = new View[9];
    private ImageView mLockIcon;
	//add by wangying end
    /**
     * Keeps track of the last time we poked the wake lock during dispatching of the touch event.
     * Initialized to something guaranteed to make us poke the wakelock when the user starts
     * drawing the pattern.
     * @see #dispatchTouchEvent(android.view.MotionEvent)
     */
    private long mLastPokeTime = -UNLOCK_PATTERN_WAKE_INTERVAL_MS;

    /**
     * Useful for clearing out the wrong pattern after a delay
     */
    private Runnable mCancelPatternRunnable = new Runnable() {
        @Override
        public void run() {
            mLockPatternView.clearPattern();
        }
    };
    private Rect mTempRect = new Rect();
    private KeyguardMessageArea mSecurityMessageDisplay;
    private View mEcaView;
    private ViewGroup mContainer;
    private int mDisappearYTranslation;

    enum FooterMode {
        Normal,
        ForgotLockPattern,
        VerifyUnlocked
    }

    public KeyguardPatternView(Context context) {
        this(context, null);
    }

    public KeyguardPatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(mContext);
        mAppearAnimationUtils = new AppearAnimationUtils(context,
                AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 1.5f /* translationScale */,
                2.0f /* delayScale */, AnimationUtils.loadInterpolator(
                        mContext, android.R.interpolator.linear_out_slow_in));
        mDisappearAnimationUtils = new DisappearAnimationUtils(context,
                125, 1.2f /* translationScale */,
                0.6f /* delayScale */, AnimationUtils.loadInterpolator(
                        mContext, android.R.interpolator.fast_out_linear_in));
        mDisappearYTranslation = getResources().getDimensionPixelSize(
                R.dimen.disappear_y_translation);
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mLockPatternUtils = mLockPatternUtils == null
                ? new LockPatternUtils(mContext) : mLockPatternUtils;
        mLockIcon = (ImageView) findViewById(R.id.keyguard_lock_icon);
        mLockPatternView = (LockPatternView) findViewById(R.id.lockPatternView);
        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());

        // vibrate mode will be the same for the life of this screen
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
		//add by wangying begin
        grid = (ViewGroup)findViewById(R.id.gridPanel);

        for(int i=0; i<grid.getChildCount(); i++) {
            cellList[i] = grid.getChildAt(i);
        }
		//add by wangying end
        mSecurityMessageDisplay =
                (KeyguardMessageArea) KeyguardMessageArea.findSecurityMessageDisplay(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        mContainer = (ViewGroup) findViewById(R.id.container);

        EmergencyButton button = (EmergencyButton) findViewById(R.id.emergency_call_button);
        if (button != null) {
            button.setCallback(this);
        }
    }

    private void cellTouch(List<LockPatternView.Cell> pattern){

        for(int i =0 ;i<pattern.size();i++){
            int j = pattern.get(i).getRow()*3 +pattern.get(i).getColumn();
            //linwujia add begin
            GradientDrawable drawable = (GradientDrawable) cellList[j].getBackground();
            drawable.setColor(0x7fffffff);
            //cellList[j].setBackgroundColor(0x7fffffff);
            cellList[j].setBackground(drawable);
            //linwujia add end
        }

    }

    private void clearBack(){
        for(int i=0;i<cellList.length;i++){
            //linwujia add begin
            GradientDrawable drawable = (GradientDrawable) cellList[i].getBackground();
            if( i%2 ==0){
                drawable.setColor(0x26ffffff);
                //cellList[i].setBackgroundColor( 0x26ffffff);//0x4cffffff // alpha 15% George change
            }else {
                drawable.setColor(0x19ffffff);
                //cellList[i].setBackgroundColor( 0x19ffffff); // alpha 10%
            }
            cellList[i].setBackground(drawable);
            //linwujia add end
        }

    }

    @Override
    public void onEmergencyButtonClickedWhenInCall() {
        mCallback.reset();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        // as long as the user is entering a pattern (i.e sending a touch event that was handled
        // by this screen), keep poking the wake lock so that the screen will stay on.
        final long elapsed = SystemClock.elapsedRealtime() - mLastPokeTime;
        if (result && (elapsed > (UNLOCK_PATTERN_WAKE_INTERVAL_MS - 100))) {
            mLastPokeTime = SystemClock.elapsedRealtime();
        }
        mTempRect.set(0, 0, 0, 0);
        offsetRectIntoDescendantCoords(mLockPatternView, mTempRect);
        ev.offsetLocation(mTempRect.left, mTempRect.top);
        result = mLockPatternView.dispatchTouchEvent(ev) || result;
        ev.offsetLocation(-mTempRect.left, -mTempRect.top);
        return result;
    }

    @Override
    public void reset() {
        // reset lock pattern
        mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(
                KeyguardUpdateMonitor.getCurrentUser()));
        mLockPatternView.enableInput();
        mLockPatternView.setEnabled(true);
        mLockPatternView.clearPattern();
        mSecurityMessageDisplay.setDefaultMessage(R.string.kg_pattern_instructions, true);
        // add by wangying begin
        showLockIcon(false);
        //add by wangying end
        // if the user is currently locked out, enforce it.
        long deadline = mLockPatternUtils.getLockoutAttemptDeadline(
                KeyguardUpdateMonitor.getCurrentUser());
        if (deadline != 0) {
            handleAttemptLockout(deadline);
        } else {
            displayDefaultSecurityMessage();
        }
    }

    private void displayDefaultSecurityMessage() {
        mSecurityMessageDisplay.setMessage(R.string.kg_pattern_instructions, true/*false*/);
    }

    @Override
    public void showUsabilityHint() {
    }

    /** TODO: hook this up */
    public void cleanUp() {
        if (DEBUG) Log.v(TAG, "Cleanup() called on " + this);
        mLockPatternUtils = null;
        mLockPatternView.setOnPatternListener(null);
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {

        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);
            mSecurityMessageDisplay.setMessage("", false);
        }

        @Override
        public void onPatternCleared() {
        }

        @Override
        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
            //add by wangying begin
            if(mLockPatternUtils.isVisiblePatternEnabled(ActivityManager.getCurrentUser())){
                cellTouch(pattern);
            }
            //add by wangying end
            mCallback.userActivity();
        }

        @Override
        public void onPatternDetected(final List<LockPatternView.Cell> pattern) {
            clearBack();
            mLockPatternView.disableInput();
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
            }

            final int userId = KeyguardUpdateMonitor.getCurrentUser();
            if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                mLockPatternView.enableInput();
                onPatternChecked(userId, false, 0, false /* not valid - too short */);
                return;
            }

            mPendingLockCheck = LockPatternChecker.checkPattern(
                    mLockPatternUtils,
                    pattern,
                    userId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onChecked(boolean matched, int timeoutMs) {
                            mLockPatternView.enableInput();
                            mPendingLockCheck = null;
                            onPatternChecked(userId, matched, timeoutMs, true);
                        }
                    });
            if (pattern.size() > MIN_PATTERN_BEFORE_POKE_WAKELOCK) {
                mCallback.userActivity();
            }
        }

        private void onPatternChecked(int userId, boolean matched, int timeoutMs,
                boolean isValidPattern) {
            boolean dismissKeyguard = KeyguardUpdateMonitor.getCurrentUser() == userId;
            if (matched) {
                mCallback.reportUnlockAttempt(userId, true, 0);
                if (dismissKeyguard) {
                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                    mCallback.dismiss(true);
                }
            } else {
                mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                if (isValidPattern) {
				//add by wangying begin
                    mCallback.startShakeAnim();
				//add by wangying end
                    mCallback.reportUnlockAttempt(userId, false, timeoutMs);
                    if (timeoutMs > 0) {
                        long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                                userId, timeoutMs);
                        handleAttemptLockout(deadline);
                    }
                }
                if (timeoutMs == 0) {
                    //add by wangying begin
                    String msg;
                    KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(mContext);
                    int attempts = monitor.getFailedUnlockAttempts(userId) % 5;
                    attempts = 5 - attempts;
                    msg = (attempts != 0 && attempts <= 2) ?
                            mContext.getString(R.string.kg_password_failed_attempts_message, attempts) :
                            mContext.getString(R.string.kg_wrong_pattern);
                    //add by wangying end
                    // add begin by lych for fix bug cdn#10194
                    if(getResources().getConfiguration().locale.getLanguage().contains(Locale.ENGLISH.getLanguage()) && attempts == 2) {
                        msg += "s";
                    }
                    // add end by lych for fix bug cdn#10194
                    mSecurityMessageDisplay.setMessage(msg, true);
                    mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
                }
            }
        }
    }

    private void handleAttemptLockout(long elapsedRealtimeDeadline) {
        mLockPatternView.clearPattern();
        mLockPatternView.setEnabled(false);
        //add by wangying begin
        showLockIcon(true);
        //add by wangying end
        final long elapsedRealtime = SystemClock.elapsedRealtime();
        mCountdownTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsRemaining = (int) (millisUntilFinished / 1000);
                // modified begin by lych for fixing bug zcd#29289: try again in 1 seconds
//                 mSecurityMessageDisplay.setMessage(
//                        R.string.kg_too_many_failed_attempts_countdown, true, secondsRemaining);
                if(getResources().getConfiguration().locale.getLanguage().contains(Locale.ENGLISH.getLanguage()) && secondsRemaining == 1) {
                    mSecurityMessageDisplay.setMessage(R.string.kg_too_many_failed_attempts_countdown_singular, true, secondsRemaining);
                }else {
                    mSecurityMessageDisplay.setMessage(R.string.kg_too_many_failed_attempts_countdown, true, secondsRemaining);
                }
                // modified end by lych for fixing bug zcd#29289: try again in 1 seconds
            }

            @Override
            public void onFinish() {
                mLockPatternView.setEnabled(true);
                displayDefaultSecurityMessage();
                //add by wangying begin
                showLockIcon(false);
                //add by wangying end
            }

        }.start();
    }

    //add by wangying begin
    private void showLockIcon(boolean isShow){
        if(isShow){
            mContainer.setVisibility(View.GONE);
            mLockIcon.setVisibility(View.VISIBLE);
            return;
        }
        mContainer.setVisibility(View.VISIBLE);
        mLockIcon.setVisibility(View.GONE);
    }
    //add by wangying end
	
    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
        }
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
            mPendingLockCheck = null;
        }
    }

    @Override
    public void onResume(int reason) {
        reset();
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void showPromptReason(int reason) {
        switch (reason) {
            case PROMPT_REASON_RESTART:
                mSecurityMessageDisplay.setMessage(R.string.kg_prompt_reason_restart_pattern,
                        true /* important */);
                break;
            case PROMPT_REASON_TIMEOUT:
                mSecurityMessageDisplay.setMessage(R.string.kg_prompt_reason_timeout_pattern,
                        true /* important */);
                break;
            case PROMPT_REASON_DEVICE_ADMIN:
                mSecurityMessageDisplay.setMessage(R.string.kg_prompt_reason_device_admin,
                        true /* important */);
                break;
            case PROMPT_REASON_USER_REQUEST:
                mSecurityMessageDisplay.setMessage(R.string.kg_prompt_reason_user_request,
                        true /* important */);
                break;
            case PROMPT_REASON_NONE:
                break;
            default:
                mSecurityMessageDisplay.setMessage(R.string.kg_prompt_reason_timeout_pattern,
                        true /* important */);
                break;
        }
    }

    @Override
    public void showMessage(String message, int color) {
        mSecurityMessageDisplay.setNextMessageColor(color);
        mSecurityMessageDisplay.setMessage(message, true /* important */);
    }

    @Override
    public void startAppearAnimation() {
        clearBack();
        enableClipping(false);
        setAlpha(1f);
        setTranslationY(mAppearAnimationUtils.getStartTranslation());
        AppearAnimationUtils.startTranslationYAnimation(this, 0 /* delay */, 500 /* duration */,
                0, mAppearAnimationUtils.getInterpolator());
        mAppearAnimationUtils.startAnimation2d(
                mLockPatternView.getCellStates(),
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                        mLockPatternView.invalidate();
                    }
                },
                this);
        if (!TextUtils.isEmpty(mSecurityMessageDisplay.getText())) {
            mAppearAnimationUtils.createAnimation(mSecurityMessageDisplay, 0,
                    AppearAnimationUtils.DEFAULT_APPEAR_DURATION,
                    mAppearAnimationUtils.getStartTranslation(),
                    true /* appearing */,
                    mAppearAnimationUtils.getInterpolator(),
                    null /* finishRunnable */);
        }
    }

    @Override
    public boolean startDisappearAnimation(final Runnable finishRunnable) {
        mLockPatternView.clearPattern();
        enableClipping(false);
        setTranslationY(0);
        AppearAnimationUtils.startTranslationYAnimation(this, 0 /* delay */, 300 /* duration */,
                -mDisappearAnimationUtils.getStartTranslation(),
                mDisappearAnimationUtils.getInterpolator());
        mDisappearAnimationUtils.startAnimation2d(mLockPatternView.getCellStates(),
                new Runnable() {
                    @Override
                    public void run() {
                        enableClipping(true);
                        if (finishRunnable != null) {
                            finishRunnable.run();
                        }
                    }
                }, KeyguardPatternView.this);
        if (!TextUtils.isEmpty(mSecurityMessageDisplay.getText())) {
            mDisappearAnimationUtils.createAnimation(mSecurityMessageDisplay, 0,
                    200,
                    - mDisappearAnimationUtils.getStartTranslation() * 3,
                    false /* appearing */,
                    mDisappearAnimationUtils.getInterpolator(),
                    null /* finishRunnable */);
        }

        AlphaAnimation an = new AlphaAnimation(1,0);
        an.setDuration(300);
        grid.startAnimation(an);

        return true;
    }

    private void enableClipping(boolean enable) {
        setClipChildren(enable);
        mContainer.setClipToPadding(enable);
        mContainer.setClipChildren(enable);
    }

    @Override
    public void createAnimation(final LockPatternView.CellState animatedCell, long delay,
            long duration, float translationY, final boolean appearing,
            Interpolator interpolator,
            final Runnable finishListener) {
        mLockPatternView.startCellStateAnimation(animatedCell,
                1f, appearing ? 1f : 0f, /* alpha */
                appearing ? translationY : 0f, appearing ? 0f : translationY, /* translation */
                appearing ? 0f : 1f, 1f /* scale */,
                delay, duration, interpolator, finishListener);
        if (finishListener != null) {
            // Also animate the Emergency call
            mAppearAnimationUtils.createAnimation(mEcaView, delay, duration, translationY,
                    appearing, interpolator, null);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
