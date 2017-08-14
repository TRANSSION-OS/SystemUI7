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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.IccCardConstants.State;

import itel.transsion.settingslib.utils.TalpaUtils;

/**
 * Displays a PIN pad for entering a PUK (Pin Unlock Kode) provided by a
 * carrier.
 */
public class KeyguardSimPukView extends KeyguardPinBasedInputView {
    private static final String LOG_TAG = "KeyguardSimPukView";
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    public static final String TAG = "KeyguardSimPukView";

    private ProgressDialog mSimUnlockProgressDialog = null;
    private CheckSimPuk mCheckSimPukThread;
    private String mPukText;
    private String mPinText;
    private StateMachine mStateMachine = new StateMachine();
    private AlertDialog mRemainingAttemptsDialog;
    private int mSubId;
    private ImageView mSimImageView;
    // SPRD: FEATURE_SHOW_PIN/PUK_HINT_WITH_REMAIN_TIMES
    private int mRemainTimes;
    private int pukResultSuccess = -1;// add by wangying

    KeyguardUpdateMonitorCallback mUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onSimStateChanged(int subId, int slotId, State simState) {
            if (DEBUG)
                Log.v(TAG, "onSimStateChanged(subId=" + subId + ",state="
                        + simState + ")");
            resetState();
        };
    };

    public KeyguardSimPukView(Context context) {
        this(context, null);
    }

    public KeyguardSimPukView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private class StateMachine {
        final int ENTER_PUK = 0;
        final int ENTER_PIN = 1;
        final int CONFIRM_PIN = 2;
        final int DONE = 3;
        private int state = ENTER_PUK;

        public void next() {
            int msg = 0;
            if (state == ENTER_PUK) {
                if (checkPuk()) {
                    state = ENTER_PIN;
                    msg = R.string.kg_puk_enter_pin_hint;
                } else {
                    msg = R.string.kg_invalid_sim_puk_hint;
                }
            } else if (state == ENTER_PIN) {
                if (checkPin()) {
                    state = CONFIRM_PIN;
                    msg = R.string.kg_enter_confirm_pin_hint;
                } else {
                    msg = R.string.kg_invalid_sim_pin_hint;
                }
            } else if (state == CONFIRM_PIN) {
                if (confirmPin()) {
                    state = DONE;
                    msg = R.string.keyguard_sim_unlock_progress_dialog_message;
                    updateSim();
                } else {
                    state = ENTER_PIN; // try again?
                    msg = R.string.kg_invalid_confirm_pin_hint;
                }
            }
            resetPasswordText(true /* animate */, true /* announce */);
            if (msg != 0) {
                mSecurityMessageDisplay.setMessage(msg, true);
            }
        }

        void reset() {
            mPinText = "";
            mPukText = "";
            state = ENTER_PUK;
            KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor
                    .getInstance(mContext);
            mSubId = monitor
                    .getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED);

            if (SubscriptionManager.isValidSubscriptionId(mSubId)) {
                int count = TelephonyManager.getDefault().getSimCount();
                Resources rez = getResources();
                final String msg;
                int color = Color.WHITE;
                if (count < 2) {
                    msg = rez.getString(R.string.kg_puk_enter_puk_hint);
                } else {
                    SubscriptionInfo info = monitor
                            .getSubscriptionInfoForSubId(mSubId);
                    /* SPRD :Show pin/puk remaining times. @{ */
                    CharSequence displayName = info != null ? "SIM"
                            + Integer.toString(SubscriptionManager
                            .getPhoneId(mSubId) + 1) : ""; // don't
                    // crash
                    // SPRD: It need subId here, See bug #494466.
                    //add by wangying begin fix tfs 15026
                    if (TalpaUtils.isMTKPlatform()) {
                        try {
                            int[] result = ITelephony.Stub.asInterface(
                                    ServiceManager.checkService("phone"))
                                    .supplyPukReportResultForSubscriber(mSubId, mPukText, mPinText);
                            mRemainTimes = result[1];
                        } catch (Throwable e) {

                        }
                        // add by wangying end
                    } else {
                        mRemainTimes = Integer.valueOf(TelephonyManager
                                .getTelephonyProperty(info.getSimSlotIndex(),
                                        "gsm.sim.puk.remaintimes", "0"));
                    }
                    msg = rez.getString(R.string.kg_puk_enter_puk_hint_multi,
                            displayName, mRemainTimes);
                    /* @} */
                    if (info != null) {
                        color = info.getIconTint();
                    }
                }
                mSecurityMessageDisplay.setMessage(msg, true);
                mSimImageView.setImageTintList(ColorStateList.valueOf(color));
            }

            mPasswordEntry.requestFocus();
        }
    }

    @Override
    protected int getPromtReasonStringRes(int reason) {
        // No message on SIM Puk
        return 0;
    }

    private String getPukPasswordErrorMessage(int attemptsRemaining) {
        String displayMessage;

        if (attemptsRemaining == 0) {
            displayMessage = getContext().getString(
                    R.string.kg_password_wrong_puk_code_dead);
        } else if (attemptsRemaining > 0) {
            displayMessage = getContext().getResources().getQuantityString(
                    R.plurals.kg_password_wrong_puk_code, attemptsRemaining,
                    attemptsRemaining);
        } else {
            displayMessage = getContext().getString(
                    R.string.kg_password_puk_failed);
        }
        if (DEBUG)
            Log.d(LOG_TAG, "getPukPasswordErrorMessage:"
                    + " attemptsRemaining=" + attemptsRemaining
                    + " displayMessage=" + displayMessage);
        return displayMessage;
    }

    @Override
    public void resetState() {
        super.resetState();
        mStateMachine.reset();
    }

    @Override
    protected boolean shouldLockout(long deadline) {
        // SIM PUK doesn't have a timed lockout
        return false;
    }

    @Override
    protected int getPasswordTextViewId() {
        return R.id.pukEntry;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSecurityMessageDisplay.setTimeout(0); // don't show ownerinfo/charging
                                                // status by default
        if (mEcaView instanceof EmergencyCarrierArea) {
            ((EmergencyCarrierArea) mEcaView).setCarrierTextVisible(true);
        }
        mSimImageView = (ImageView) findViewById(R.id.keyguard_sim);
        // SPRD:add for bug 597832 for limit 8 numbers
        mPasswordEntry.setLimit(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(
                mUpdateMonitorCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(
                mUpdateMonitorCallback);
    }

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void onPause() {
        // dismiss the dialog.
        // SPRD:add PUK verify success prompt.
        dismissProgressDialog();
    }

    /**
     * Since the IPC can block, we want to run the request in a separate thread
     * with a callback.
     */
    private abstract class CheckSimPuk extends Thread {

        private final String mPin, mPuk;
        private final int mSubId;

        protected CheckSimPuk(String puk, String pin, int subId) {
            mPuk = puk;
            mPin = pin;
            mSubId = subId;
        }

        abstract void onSimLockChangedResponse(final int result,
                final int attemptsRemaining);

        @Override
        public void run() {
            try {
                if (DEBUG)
                    Log.v(TAG, "call supplyPukReportResult()");
                final int[] result = ITelephony.Stub.asInterface(
                        ServiceManager.checkService("phone"))
                        .supplyPukReportResultForSubscriber(mSubId, mPuk, mPin);
                if (DEBUG) {
                    Log.v(TAG, "supplyPukReportResult returned: " + result[0]
                            + " " + result[1]);
                }
                post(new Runnable() {
                    @Override
                    public void run() {
                        //add by wangying begin
                        pukResultSuccess = result[0];
                        mRemainTimes = result[1];
                        //add by wangying end
                        onSimLockChangedResponse(result[0], result[1]);
                    }
                });
            } catch (Throwable/*RemoteException*/ e) { //add by wangying tfs Bug 13983
                Log.e(TAG, "RemoteException for supplyPukReportResult:", e);
                post(new Runnable() {
                    @Override
                    public void run() {
                        onSimLockChangedResponse(
                                PhoneConstants.PIN_GENERAL_FAILURE, -1);
                    }
                });
            }
        }
    }

    private Dialog getSimUnlockProgressDialog() {
        if (mSimUnlockProgressDialog == null) {
            mSimUnlockProgressDialog = new ProgressDialog(mContext);
            mSimUnlockProgressDialog.setMessage(mContext
                    .getString(R.string.kg_sim_unlock_progress_dialog_message));
            mSimUnlockProgressDialog.setIndeterminate(true);
            mSimUnlockProgressDialog.setCancelable(false);
            if (!(mContext instanceof Activity)) {
                mSimUnlockProgressDialog.getWindow().setType(
                        WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            }
        }
        return mSimUnlockProgressDialog;
    }

    private Dialog getPukRemainingAttemptsDialog(int remaining) {
        String msg = getPukPasswordErrorMessage(remaining);
        if (mRemainingAttemptsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage(msg);
            builder.setCancelable(false);
            builder.setNeutralButton(R.string.ok, null);
            mRemainingAttemptsDialog = builder.create();
            mRemainingAttemptsDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        } else {
            mRemainingAttemptsDialog.setMessage(msg);
        }
        return mRemainingAttemptsDialog;
    }

    /* SPRD:add PUK verify success prompt @{ */
    private void dismissProgressDialog(){
        postDelayed(new Runnable() {
            public void run() {
                Log.d(TAG, "onPause-postDelayed : mSimUnlockProgressDialog ="
                        + mSimUnlockProgressDialog);
                if (mSimUnlockProgressDialog != null) {
                    mSimUnlockProgressDialog.dismiss();
                    mSimUnlockProgressDialog = null;
                    Log.d(TAG, "onPause(); "
                            + "prompt subscribers PUK unlock success for one second");
                }
            }
        }, 500);
    }
    /* @} */

    private boolean checkPuk() {
        // make sure the puk is at least 8 digits long.
        if (mPasswordEntry.getText().length() == 8) {
            mPukText = mPasswordEntry.getText();
            return true;
        }
        return false;
    }

    private boolean checkPin() {
        // make sure the PIN is between 4 and 8 digits
        int length = mPasswordEntry.getText().length();
        if (length >= 4 && length <= 8) {
            mPinText = mPasswordEntry.getText();
            return true;
        }
        return false;
    }

    public boolean confirmPin() {
        return mPinText.equals(mPasswordEntry.getText());
    }

    private void updateSim() {
        getSimUnlockProgressDialog().show();

        if (mCheckSimPukThread == null) {
            mCheckSimPukThread = new CheckSimPuk(mPukText, mPinText, mSubId) {
                @Override
                void onSimLockChangedResponse(final int result,
                        final int attemptsRemaining) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            resetPasswordText(true /* animate */,
                                    result != PhoneConstants.PIN_RESULT_SUCCESS /* announce */);
                            if (result == PhoneConstants.PIN_RESULT_SUCCESS) {
                                /* SPRD:add PUK verify success prompt @{ */
                                if (mSimUnlockProgressDialog != null) {
                                    mSimUnlockProgressDialog.setMessage(
                                            mContext.getString(R.string.kg_sim_puk_verify_success));
                                }
                                KeyguardUpdateMonitor.getInstance(getContext())
                                        .reportSimUnlocked(mSubId);
                                mCallback.dismiss(true);
                                dismissProgressDialog();
                                /* @} */
                            } else {
                                /* SPRD:add PUK verify success prompt @{ */
                                if (mSimUnlockProgressDialog != null) {
                                    mSimUnlockProgressDialog.hide();
                                }
                                /* @} */
                                if (result == PhoneConstants.PIN_PASSWORD_INCORRECT) {
                                    if (attemptsRemaining <= 2) {
                                        // this is getting critical - show
                                        // dialog
                                        getPukRemainingAttemptsDialog(
                                                attemptsRemaining).show();
                                    } else {
                                        // show message
                                        mSecurityMessageDisplay
                                                .setMessage(
                                                        getPukPasswordErrorMessage(attemptsRemaining),
                                                        true);
                                    }
                                } else {
                                    mSecurityMessageDisplay
                                            .setMessage(
                                                    getContext()
                                                            .getString(
                                                                    R.string.kg_password_puk_failed),
                                                    true);
                                }
                                if (DEBUG)
                                    Log.d(LOG_TAG, "verifyPasswordAndUnlock "
                                            + " UpdateSim.onSimCheckResponse: "
                                            + " attemptsRemaining="
                                            + attemptsRemaining);
                                mStateMachine.reset();
                            }
                            mCheckSimPukThread = null;
                        }
                    });
                }
            };
            mCheckSimPukThread.start();
        }
    }

    @Override
    protected void verifyPasswordAndUnlock() {
        mStateMachine.next();
    }

    @Override
    public void startAppearAnimation() {
        // noop.
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return false;
    }
}
