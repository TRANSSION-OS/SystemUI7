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
 * limitations under the License.
 */

package com.android.systemui;

import android.animation.ArgbEvaluator;
import android.annotation.Nullable;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;

import com.android.systemui.statusbar.policy.BatteryController;

public class ItelBatteryMeterDrawable extends Drawable implements
        BatteryController.BatteryStateChangeCallback {

    public static final String TAG = BatteryMeterDrawable.class.getSimpleName();

    // SPRD : Bug 474751 add charge animation of batteryView
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    private final int[] mColors;
    private final int mIntrinsicWidth;
    private final int mIntrinsicHeight;

    private final Paint mFramePaint, mBatteryPaint, mWarningTextPaint;
    private int mIconTint = Color.WHITE;
    private float mOldDarkIntensity = 0f;

    private int mHeight;
    private int mWidth;
    private String mWarningString;
    private final int mCriticalLevel;

    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mBoltFrame = new RectF();

    private BatteryController mBatteryController;
    private boolean mPowerSaveEnabled;

    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;

    private final Context mContext;
    private final Handler mHandler;

    private int mLevel = -1;
    private boolean mPluggedIn;
    // SPRD: Bug 601597 support battery animation for status bar
    private boolean mCharging;

    /* SPRD: Bug 474751 add charge animation of batteryView @{ */
    private Runnable mChargingAnimate;
    private static final int LEVEL_UPDATE = 1;
    private static final int ANIMATION_DURATION = 1000;
    private BatteryTracker mTracker = new BatteryTracker();

    /* @} */

    //linwujia add begin
    private float battaryWidth;
    private int mPaddingBatteryAndEdge ;
    private int mPaddingBoltAndFrame;
    private int mPaddingBatteryBodyAndBolt;
    private int mPaddingButtonFrameTopAndFrameTop;

    private boolean mLoadChargeBg;
    private float mPercentSize;

    private final Path mBatteryBgPath = new Path();
    private final Path mBatteryInnerPath = new Path();

    private boolean mIsReturnCall;
    private int mReturnCallColor;
    //linwujia add end
    protected static final boolean DEBUG = false;
    private int mPowerSaveColor;

    //linwujia add begin
    private int mChargingColor;
    //linwujia add end

    // SPRD: Bug 587470 set flag to decide how to draw
    public ItelBatteryMeterDrawable(Context context, Handler handler, int frameColor, boolean isReturnCall) {
        mContext = context;
        mHandler = handler;
        mLoadChargeBg = false;
        final Resources res = context.getResources();
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

        final int N = levels.length();
        mColors = new int[2*N];
        for (int i=0; i<N; i++) {
            mColors[2*i] = levels.getInt(i, 0);
            mColors[2*i+1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();
        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        mCriticalLevel = mContext.getResources().getInteger(
                Resources.getSystem().getIdentifier("config_criticalBatteryWarningLevel", "integer","android"));

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(frameColor);
        mFramePaint.setDither(true);
        mFramePaint.setStrokeWidth(0);
        mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setStrokeWidth(0);
        mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWarningTextPaint.setColor(mColors[1]);
        Typeface font = Typeface.create("sans-serif", Typeface.BOLD);
        mWarningTextPaint.setTypeface(font);
        mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

        mPowerSaveColor = context.getColor(R.color.itel_battery_powersave_color);
        mChargingColor = context.getColor(R.color.itel_battery_charging_color);

        mDarkModeBackgroundColor =
                context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        mLightModeBackgroundColor =
                context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);

        mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.itel_battery_width);
        mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.itel_battery_height);

        //linwujia add begin
        battaryWidth = 15f*2;//2 dp
        mPaddingBatteryAndEdge = 4 ;//电池图标内容区域外边空白部分
        mPaddingBoltAndFrame = 3; //电池图标线条宽度
        mPaddingBatteryBodyAndBolt = 2; //图标线条与内部填充区的间距
        mPaddingButtonFrameTopAndFrameTop = 6;
        mPercentSize = 9*2;//9sp
        mIsReturnCall = isReturnCall;
        mReturnCallColor = context.getColor(R.color.itel_batterymeter_returncall_color);
        //linwujia add end
    }

    public ItelBatteryMeterDrawable(Context context, Handler handler, int frameColor) {
        this(context, handler, frameColor, false);
    }

    @Override
    public int getIntrinsicHeight() {
        return mIntrinsicHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return mIntrinsicWidth;
    }

    public void startListening() {

        /* SPRD: Bug 474751 add charge animation of batteryView @{ */
        if(DEBUG)
        {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(ACTION_LEVEL_TEST);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            final Intent sticky = mContext.registerReceiver(mTracker, filter);
            if (sticky != null) {
                // preload the battery level
                mTracker.onReceive(mContext, sticky);
            }
        }
        /* @} */
        mBatteryController.addStateChangedCallback(this);
    }

    public void stopListening() {
        // SPRD : Bug 474751 add charge animation of batteryView
        if(DEBUG)
        {
            mContext.unregisterReceiver(mTracker);
        }
        mBatteryController.removeStateChangedCallback(this);
    }

    private void postInvalidate() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                invalidateSelf();
            }
        });
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        mLevel = level;
        mPluggedIn = pluggedIn;
        // SPRD: Bug 601597 support battery animation for status bar
        mCharging = charging;
        postInvalidate();
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        mPowerSaveEnabled = isPowerSave;
        invalidateSelf();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mHeight = bottom - top;
        mWidth = right - left;
    }

    private int getColorForLevel(int percent) {

        if(mPluggedIn && mCharging) {
            if(mIsReturnCall) {
                return mReturnCallColor;
            } else {
                return mChargingColor;
            }
        }

        // If we are in power save mode, always use the orange color.
        if (mPowerSaveEnabled) {
            /// George:changed for power save
            return mPowerSaveColor;
        }
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            if (percent <= thresh) {

                // Respect tinting for "normal" level
                if (i == mColors.length-2) {
                    return mIconTint;
                } else {
                    return color;
                }
            }
        }
        return color;
    }

    public void setDarkIntensity(float darkIntensity) {
        if (darkIntensity == mOldDarkIntensity) {
            return;
        }
        int backgroundColor = getBackgroundColor(darkIntensity);
        int fillColor = getFillColor(darkIntensity);
        mIconTint = fillColor;
        mFramePaint.setColor(backgroundColor);
        mOldDarkIntensity = darkIntensity;
        invalidateSelf();
    }

    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeBackgroundColor, mDarkModeBackgroundColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    @Override
    public void draw(Canvas c) {
        drawStatusBar1(c);
    }

    private void drawStatusBar1(Canvas c) {
        final int level = mLevel;

        if (level == -1) return;

        final int height = mHeight;
        final int width = mWidth;

        final int buttonWidth = mPaddingBatteryAndEdge;
        mFrame.set(mPaddingBatteryAndEdge, mPaddingBatteryAndEdge, width-mPaddingBatteryAndEdge, height-mPaddingBatteryAndEdge); // 1

        // button-frame: area above the battery body
        mButtonFrame.set(
                mFrame.left,
                mFrame.top + mPaddingButtonFrameTopAndFrameTop,
                mFrame.left + buttonWidth,
                mFrame.bottom - mPaddingButtonFrameTopAndFrameTop); // right
        // frame: battery body area
        mFrame.left += buttonWidth;

        // define the bolt shape
        final float bl = mFrame.left + mPaddingBoltAndFrame;
        final float bt = mFrame.top + mPaddingBoltAndFrame;
        final float br = mFrame.right - mPaddingBoltAndFrame;
        final float bb = mFrame.bottom - mPaddingBoltAndFrame;
        if (mBoltFrame.left != bl || mBoltFrame.top != bt
                || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
            mBoltFrame.set(bl, bt, br, bb);
        }

        loadChargeBg();

        Log.d("StatusBar", "draw mCriticalLevel:" + mCriticalLevel);

        // draw the battery shape background
        c.drawPath(mBatteryBgPath, mFramePaint);

        if(level > mCriticalLevel) {

            drawRatioPercent(c, level);
        } else {

            if(mPluggedIn && mCharging) {
                drawRatioPercent(c, level);
            } else {
                drawLowLevelWarning(c, level);
            }
        }
    }

    /**
     * draw rate of rectangle battery
     * @param c
     * @param level battery level
     */
    private void drawRatioPercent(Canvas c, int level) {

        float drawFrac = level > 10 ? (float) level / 100f : 0.1f;
        int padding = mPaddingBoltAndFrame + mPaddingBatteryBodyAndBolt;
        float left = mFrame.right - padding - (int)(battaryWidth * drawFrac);
        float top = mFrame.top + padding;
        float right =  mFrame.right - padding;
        float bottom = mFrame.bottom - padding;

        mBatteryPaint.setColor(getColorForLevel(level));
        c.drawRect(left, top,  right, bottom, mBatteryPaint);
    }

    /**
     * draw the low level warning of battery
     * @param c
     */
    private void drawLowLevelWarning(Canvas c,int level) {
        float pctX = 0, pctY = 0;
        /// George:add for power save
        mWarningTextPaint.setColor(getColorForLevel(level));
        mWarningTextPaint.setTextSize(mPercentSize);

        pctX = mWidth - mPaddingBatteryAndEdge - mPaddingBoltAndFrame - mPaddingBatteryBodyAndBolt
                - battaryWidth / 2f;

        pctY =  ((mHeight / 2f) - ((mWarningTextPaint.descent() + mWarningTextPaint.ascent()) / 2f));
        // draw the warning text
        c.drawText(mWarningString, pctX, pctY, mWarningTextPaint);
    }

    private void loadChargeBg()
    {
        if(!mLoadChargeBg)
        {
            mLoadChargeBg = true;
            long time = System.currentTimeMillis();
            mBatteryBgPath.reset();
            mBatteryBgPath.moveTo(46f, 4f);
            mBatteryBgPath.lineTo(10f, 4f);

            mBatteryBgPath.cubicTo(8.9f,4f,8f,4.9f,8f,6f);

            mBatteryBgPath.lineTo(8f, 10f);
            mBatteryBgPath.lineTo(6f, 10f);

            mBatteryBgPath.cubicTo(4.9f,10f,4f,10.9f,4f,12f);

            mBatteryBgPath.lineTo(4f, 20f);

            mBatteryBgPath.cubicTo(4f,21.1f,4.9f,22f,6f,22f);

            mBatteryBgPath.lineTo(8f,22f );
            mBatteryBgPath.lineTo(8f, 26f);

            mBatteryBgPath.cubicTo(8f,27.1f,8.9f,28f,10f,28f);

            mBatteryBgPath.lineTo(46f,28f );

            mBatteryBgPath.cubicTo(47.1f,28f,48f,27.1f,48f,26f);

            mBatteryBgPath.lineTo(48f,6f );

            mBatteryBgPath.cubicTo(48f,4.9f,47.1f,4f,46f,4f );


            mBatteryInnerPath.moveTo(45f,24f);

            mBatteryInnerPath.cubicTo(45f,24.6f,44.6f,25f,44f,25f);

            mBatteryInnerPath.lineTo(12f,25f  );

            mBatteryInnerPath.cubicTo(11.4f,25f,11f,24.6f,11f,24f );

            mBatteryInnerPath.lineTo(11f,8f   );

            mBatteryInnerPath.cubicTo(11f,7.4f,11.4f,7f,12f,7f);


            mBatteryInnerPath.lineTo(44f,7f );

            mBatteryInnerPath.cubicTo(44.6f,7f,45f,7.4f,45f,8f );

            mBatteryInnerPath.lineTo(45f,24f );

            mBatteryBgPath.op(mBatteryInnerPath, Path.Op.DIFFERENCE);

            //c.drawPath(mBatteryInnerPath, mFrameInnerPaint);
            long timeend = System.currentTimeMillis();

            //Log.d("BatteryMeterDrawable","George-drawBatteryBg--time="+(timeend-time));
        }
    }

    // Some stuff required by Drawable.
    @Override
    public void setAlpha(int alpha) {
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    /* SPRD: Bug 474751 add charge animation of batteryView @{ */
    private final class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        int level = UNKNOWN_LEVEL;
        String percentStr;
        int plugType;
        boolean plugged;
        int health;
        int status;
        String technology;
        int voltage;
        int temperature;
        boolean testmode = false;
        int tempLevel = -1;
        int ChargeLevel = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (testmode && ! intent.getBooleanExtra("testmode", false)) return;

                level = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));

                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                plugged = plugType != 0;
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

                BatteryMeterViewAnimationShow(intent);
                /* @} */
            } else if (action.equals(ACTION_LEVEL_TEST)) {
                testmode = true;
                mHandler.post(new Runnable() {
                    int curLevel = 0;
                    int incr = 1;
                    int saveLevel = level;
                    int savePlugged = plugType;
                    Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                    @Override
                    public void run() {
                        if (curLevel < 0) {
                            testmode = false;
                            dummy.putExtra("level", saveLevel);
                            dummy.putExtra("plugged", savePlugged);
                            dummy.putExtra("testmode", false);
                        } else {
                            dummy.putExtra("level", curLevel);
                            dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC
                                    : 0);
                            dummy.putExtra("testmode", true);
                        }
                        mContext.sendBroadcast(dummy);

                        if (!testmode) return;

                        curLevel += incr;
                        if (curLevel == 100) {
                            incr *= -1;
                        }
                        mHandler.postDelayed(this, 200);
                    }
                });
            }

            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                cleanTimerTask();
            }

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    if (mChargingAnimate == null && level < 100) {
                        mChargingAnimate = new Runnable() {
                            @Override
                            public void run() {
                                ChargeLevel += 20;
                                level = ChargeLevel;
                                postInvalidate();
                                if (ChargeLevel > 90) {
                                    if (tempLevel > 20) {
                                        ChargeLevel = tempLevel - 20;
                                    } else {
                                        ChargeLevel = tempLevel;
                                    }
                                }
                                mHandler.postDelayed(this, ANIMATION_DURATION);
                            }
                        };
                        mHandler.postDelayed(mChargingAnimate, ANIMATION_DURATION);
                    }
                }
            }
        }

        private void BatteryMeterViewAnimationShow(Intent intent) {
            //Log.d(TAG, "level should be =" + level);
            if (plugged && level < 100 && status == BatteryManager.BATTERY_STATUS_CHARGING) {
                if (0 < level && level < 20) {
                    tempLevel = -1;
                } else if (20 < level && level < 40) {
                    tempLevel = 19;
                } else if (40 < level && level < 60) {
                    tempLevel = 39;
                } else if (60 < level && level < 80) {
                    tempLevel = 59;
                } else if (80 < level && level < 100) {
                    tempLevel = 79;
                }
                ChargeLevel = tempLevel;

                if (mChargingAnimate == null) {
                    mChargingAnimate = new Runnable() {
                        @Override
                        public void run() {
                            ChargeLevel += 20;
                            level = ChargeLevel;
                            postInvalidate();
                            if (ChargeLevel > 90) {
                                if (tempLevel > 20) {
                                    ChargeLevel = tempLevel - 20;
                                } else {
                                    ChargeLevel = tempLevel;
                                }
                            }
                            mHandler.postDelayed(this, ANIMATION_DURATION);
                        }
                    };
                    mHandler.postDelayed(mChargingAnimate, ANIMATION_DURATION);
                }
            } else {
                level = (int) (100f * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) / intent
                        .getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                //Log.d(TAG, " non-charge: level =" + level);
                postInvalidate();
                cleanTimerTask();
            }
        }
    }
    /* @} */

    /* SPRD : Bug 474751 add charge animation of batteryView @{ */
    private void cleanTimerTask() {
        if (mChargingAnimate != null){
            mHandler.removeCallbacks(mChargingAnimate);
            mChargingAnimate = null;
        }
    }
    /* @} */
}
