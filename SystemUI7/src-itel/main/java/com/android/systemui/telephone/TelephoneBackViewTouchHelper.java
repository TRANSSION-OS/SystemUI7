package com.android.systemui.telephone;

import android.content.Context;
import android.telecom.TelecomManager;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.android.systemui.R;

import itel.transsion.settingslib.utils.LogUtil;

/**
 * Created by wujia.lin on 2017/4/15.
 */

public class TelephoneBackViewTouchHelper {
    private Context mContext;

    private float mInitialTouchX, mInitialTouchY;
    private float mCurrentTouchX, mCurrentTouchY;
    private float mTouchSlop;
    private boolean mIsClick;

    private int mStatusBarHeight;

    public TelephoneBackViewTouchHelper(Context context) {
        mContext = context;

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mIsClick = true;

        mStatusBarHeight = context.getResources().getDimensionPixelSize(R.dimen.status_bar_height);
    }

    public void onMotionEventActionDown(MotionEvent event) {
        LogUtil.d("linwujia onMotionEventActionDown");
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mInitialTouchX = event.getX();
            mInitialTouchY = event.getY();
            LogUtil.d("linwujia mInitialTouchX:" + mInitialTouchX + ", mInitialTouchY:" + mInitialTouchY);
            mCurrentTouchX = mInitialTouchX;
            mCurrentTouchY = mInitialTouchY;
            if(mInitialTouchY > 0.1f && mInitialTouchY < mStatusBarHeight) {
                mIsClick = true;
            } else {
                mIsClick = false;
            }
        }
    }


    public void onMotionEventActionMove(MotionEvent event) {
        LogUtil.d("linwujia onMotionEventActionMove");
        if(event.getActionMasked() == MotionEvent.ACTION_MOVE && mIsClick) {
            mCurrentTouchX = event.getX();
            mCurrentTouchY = event.getY();
            LogUtil.d("linwujia mCurrentTouchX:" + mCurrentTouchX + ", mCurrentTouchY:" + mCurrentTouchY);
            float distance = getDistance(mInitialTouchX, mInitialTouchY, mCurrentTouchX, mInitialTouchY);
            if(mCurrentTouchY > mStatusBarHeight || distance > mTouchSlop) {
                mIsClick = false;
            } else {
                mIsClick = true;
            }
        }
    }

    public boolean onMotionEventActionUp(MotionEvent event) {
        LogUtil.d("linwujia onMotionEventActionUp");
        if(event.getActionMasked() == MotionEvent.ACTION_UP && mIsClick) {
            mCurrentTouchX = event.getX();
            mCurrentTouchY = event.getY();
            LogUtil.d("linwujia mCurrentTouchX:" + mCurrentTouchX + ", mCurrentTouchY:" + mCurrentTouchY);
            float distance = getDistance(mInitialTouchX, mInitialTouchY, mCurrentTouchX, mInitialTouchY);
            if(mCurrentTouchY > mStatusBarHeight || distance > mTouchSlop) {
                mIsClick = false;
            } else {
                mIsClick = true;
            }
            LogUtil.d("linwujia mIsClick:" + mIsClick);
            if(mIsClick) {
                resumeCall();
                return true;
            }

        }
        mIsClick = true;
        return false;
    }

    private float getDistance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private TelecomManager getTelecommManager() {
        return (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);
    }

    private void resumeCall() {
        getTelecommManager().showInCallScreen(false);
    }
}
