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
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryMeterDrawable extends Drawable implements
        BatteryController.BatteryStateChangeCallback {

    private static final float ASPECT_RATIO = 9.5f / 14.5f;
    public static final String TAG = BatteryMeterDrawable.class.getSimpleName();
    //linwujia edit begin
    public static final String SHOW_PERCENT_SETTING = "status_bar_show_battery_percent";
   // public static final String SHOW_PERCENT_SETTING = "battery_percentage_enabled";
    //linwujia edit end

    // SPRD : Bug 474751 add charge animation of batteryView
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";
    private static final boolean SINGLE_DIGIT_PERCENT = false;

    private static final int FULL = 96;

    private static final float BOLT_LEVEL_THRESHOLD = 0.3f;  // opaque bolt below this fraction

    private final int[] mColors;
    private final int mIntrinsicWidth;
    private final int mIntrinsicHeight;

    private boolean mShowPercent;
    private float mButtonHeightFraction;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private final Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextPaint, mBoltPaint,
            mPlusPaint;
    private float mTextHeight, mWarningTextHeight;
    private int mIconTint = Color.WHITE;
    private float mOldDarkIntensity = 0f;

    private int mHeight;
    private int mWidth;
    private String mWarningString;
    private final int mCriticalLevel;
    private int mChargeColor;
    private final float[] mBoltPoints;
    private final Path mBoltPath = new Path();
    private final float[] mPlusPoints;
    private final Path mPlusPath = new Path();

    private final RectF mFrame = new RectF();
    private final RectF mButtonFrame = new RectF();
    private final RectF mBoltFrame = new RectF();
    private final RectF mPlusFrame = new RectF();

    private final Path mShapePath = new Path();
    private final Path mClipPath = new Path();
    private final Path mTextPath = new Path();

    private BatteryController mBatteryController;
    private boolean mPowerSaveEnabled;

    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;

    private final SettingObserver mSettingObserver = new SettingObserver();

    private final Context mContext;
    private final Handler mHandler;

    private int mLevel = -1;
    private boolean mPluggedIn;
    // SPRD: Bug 601597 support battery animation for status bar
    private boolean mCharging;

    private boolean mListening;

    /* SPRD: Bug 474751 add charge animation of batteryView @{ */
    private Runnable mChargingAnimate;
    private static final int LEVEL_UPDATE = 1;
    private static final int ANIMATION_DURATION = 1000;
    private BatteryTracker mTracker = new BatteryTracker();
    // SPRD : Bug 587470 set flag to decide how to draw
    private final boolean mIsBatteryTile;
    /* @} */

    //linwujia add begin
    private final float[] mBoltChargePoints;
    private float battaryWidth;
    private int mPaddingBatteryAndEdge ;
    private int mPaddingBoltAndFrame;
    private int mPaddingBatteryBodyAndBolt;
    private int mPaddingButtonFrameTopAndFrameTop;

    private boolean mLoadChargeBg;
    private float mPercentSize;

    private final Path mBatteryBgPath = new Path();
    private final Path mBatteryInnerPath = new Path();
    private final Path mChargePath = new Path();

    private boolean mIsReturnCall;
    private int mReturnCallColor;
    //linwujia add end
	 protected static final boolean DEBUG = false;
    /// George:add for power save
    private int mPowerSaveColor;
    //talpa zhw add
    boolean isQsBattery = false;
    public void setQsBattery(boolean qsBattery)
    {
        isQsBattery = qsBattery;
        if(isQsBattery)
        {
            mFramePaint.setColor(0xFF646464);
            mBoltPaint.setColor(0xFF3e3e3e);
            mBoltPaint.setDither(true);
            mBoltPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }
    }
    //talpa zhw add end

    // SPRD: Bug 587470 set flag to decide how to draw
    public BatteryMeterDrawable(Context context, Handler handler, int frameColor,boolean isBatteryTilte, boolean isReturnCall) {
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
        updateShowPercent();
        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        mCriticalLevel = mContext.getResources().getInteger(
                Resources.getSystem().getIdentifier("config_criticalBatteryWarningLevel", "integer","android"));
        mButtonHeightFraction = context.getResources().getFraction(
                R.fraction.battery_button_height_fraction, 1, 1);
        mSubpixelSmoothingLeft = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_left, 1, 1);
        mSubpixelSmoothingRight = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_right, 1, 1);

        mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePaint.setColor(frameColor);
        mFramePaint.setDither(true);
        mFramePaint.setStrokeWidth(0);
        mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBatteryPaint.setDither(true);
        mBatteryPaint.setStrokeWidth(0);
        mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        mTextPaint.setTypeface(font);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWarningTextPaint.setColor(mColors[1]);
        font = Typeface.create("sans-serif", Typeface.BOLD);
        mWarningTextPaint.setTypeface(font);
        mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

        //mChargeColor = context.getColor(R.color.batterymeter_charge_color);
        mChargeColor = context.getColor(R.color.itel_batterymeter_charge_color);

        mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBoltPaint.setColor(context.getColor(R.color.batterymeter_bolt_color));
        mBoltPoints = loadBoltPoints(res);
        /// George:add for power save
        mPowerSaveColor = context.getColor(R.color.battery_saver_mode_color);
        mPlusPaint = new Paint(mBoltPaint);
        mPlusPoints = loadPlusPoints(res);
        //linwujia add begin
        mBoltChargePoints = loadChargePoints(res);
        //linwujia add end

        mDarkModeBackgroundColor =
                context.getColor(R.color.dark_mode_icon_color_dual_tone_background);
        mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
        mLightModeBackgroundColor =
                context.getColor(R.color.light_mode_icon_color_dual_tone_background);
        mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);

        /*mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.battery_width);
        mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.battery_height);*/

        mIntrinsicWidth = context.getResources().getDimensionPixelSize(R.dimen.itel_battery_width);
        mIntrinsicHeight = context.getResources().getDimensionPixelSize(R.dimen.itel_battery_height);


        // SPRD: Bug 587470 set flag to decide how to draw
        mIsBatteryTile = isBatteryTilte;

        //linwujia add begin
        battaryWidth = 15f*2;//2 dp
        mPaddingBatteryAndEdge = 4 ;
        mPaddingBoltAndFrame = 3;
        mPaddingBatteryBodyAndBolt = 2;
        mPaddingButtonFrameTopAndFrameTop = 6;
        mPercentSize = 9*2;//9sp
        mIsReturnCall = isReturnCall;
        mReturnCallColor = context.getColor(R.color.itel_batterymeter_returncall_color);
        //linwujia add end
    }

    public BatteryMeterDrawable(Context context, Handler handler, int frameColor,boolean isBatteryTilte) {
        this(context, handler, frameColor, isBatteryTilte, false);
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
        mListening = true;
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(SHOW_PERCENT_SETTING), false, mSettingObserver);
        updateShowPercent();

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
        mListening = false;
        // SPRD : Bug 474751 add charge animation of batteryView
        if(DEBUG)
        {
        	mContext.unregisterReceiver(mTracker);
        }
        mContext.getContentResolver().unregisterContentObserver(mSettingObserver);
        mBatteryController.removeStateChangedCallback(this);
    }

    public void disableShowPercent() {
        mShowPercent = false;
        postInvalidate();
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

    private static float[] loadBoltPoints(Resources res) {
        final int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
        int maxX = 0, maxY = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        final float[] ptsF = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            ptsF[i] = (float)pts[i] / maxX;
            ptsF[i + 1] = (float)pts[i + 1] / maxY;
        }
        return ptsF;
    }

    private static float[] loadPlusPoints(Resources res) {
        final int[] pts = res.getIntArray(R.array.batterymeter_plus_points);
        int maxX = 0, maxY = 0;
        for (int i = 0; i < pts.length; i += 2) {
            maxX = Math.max(maxX, pts[i]);
            maxY = Math.max(maxY, pts[i + 1]);
        }
        final float[] ptsF = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            ptsF[i] = (float)pts[i] / maxX;
            ptsF[i + 1] = (float)pts[i + 1] / maxY;
        }
        return ptsF;
    }

    private static float[] loadChargePoints(Resources res) {
        final int[] pts = res.getIntArray(R.array.batterymeter_charge_points);

        final float[] ptsF = new float[pts.length];
        for (int i = 0; i < pts.length; i += 2) {
            ptsF[i] = pts[i];
            ptsF[i + 1] = pts[i + 1];
        }
        return ptsF;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        mHeight = bottom - top;
        mWidth = right - left;
        //mWarningTextPaint.setTextSize(mHeight * 0.75f);
        //mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
    }

    private void updateShowPercent() {
        mShowPercent = 0 != Settings.System.getInt(mContext.getContentResolver(),
                SHOW_PERCENT_SETTING, 1);
     //   mShowPercent = false;
    }

    private int getColorForLevel(int percent) {

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
        mBoltPaint.setColor(fillColor);
        mChargeColor = fillColor;
        //linwujia add begin
       // mWarningTextPaint.setColor(backgroundColor);
        //linwujia add end
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
    /* talpa zhw add
    @Override
    public void draw(Canvas c) {
    */
    public void drawV(Canvas c) {
        /* SPRD: Bug 587470 draw according the flag @{ */
        /* SPRD: Bug 474751 add charge animation of batteryView @{ */
        //final int level = mLevel;
        final int level = mIsBatteryTile ?  mLevel : mTracker.level;
        final boolean plugged = mIsBatteryTile ?  mPluggedIn : mTracker.plugged;
        /* @} */
        /* @} */

        if (level == -1) return;

        float drawFrac = (float) level / 100f;
        final int height = mHeight /3;
        final int width = (int) (ASPECT_RATIO * mHeight /3);

        int px = (mWidth - width) / 2;
        final int buttonHeight = (int) (height * mButtonHeightFraction);

        mFrame.set(0, 0, width, height);
        //c.drawRect(mFrame, mFramePaint);//zhw test
        mFrame.offset(px, 0);
        //talpa zhw add
        int py = (mHeight - height ) /2;
        mFrame.offset(0, py);
        //talpa zhw add
        // button-frame: area above the battery body
        mButtonFrame.set(
                mFrame.left + Math.round(width * 0.25f),
                mFrame.top,
                mFrame.right - Math.round(width * 0.25f),
                mFrame.top + buttonHeight);

        mButtonFrame.top += mSubpixelSmoothingLeft;
        mButtonFrame.left += mSubpixelSmoothingLeft;
        mButtonFrame.right -= mSubpixelSmoothingRight;

        // frame: battery body area
        mFrame.top += buttonHeight;
        mFrame.left += mSubpixelSmoothingLeft;
        mFrame.top += mSubpixelSmoothingLeft;
        mFrame.right -= mSubpixelSmoothingRight;
        mFrame.bottom -= mSubpixelSmoothingRight;

        // SPRD : Bug 474751 add charge animation of batteryView
        // set the battery charging color
        if(isQsBattery)
        {
            mBatteryPaint.setColor(0xFFa9a9a9);
        }
        else
        mBatteryPaint.setColor(plugged ? mChargeColor : getColorForLevel(level));
        if (level >= FULL) {
            drawFrac = 1f;
        } else if (level <= mCriticalLevel) {
            drawFrac = 0f;
        }

        final float levelTop = drawFrac == 1f ? mButtonFrame.top
                : (mFrame.top + (mFrame.height() * (1f - drawFrac)));

        // define the battery shape
        mShapePath.reset();
        mShapePath.moveTo(mButtonFrame.left, mButtonFrame.top);
        mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
        mShapePath.lineTo(mButtonFrame.right, mFrame.top);
        mShapePath.lineTo(mFrame.right, mFrame.top);
        mShapePath.lineTo(mFrame.right, mFrame.bottom);
        mShapePath.lineTo(mFrame.left, mFrame.bottom);
        mShapePath.lineTo(mFrame.left, mFrame.top);
        mShapePath.lineTo(mButtonFrame.left, mFrame.top);
        mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);

        /* SPRD: Bug 601597 support battery animation for status bar @{ */
        // SPRD : Bug 474751 add charge animation of batteryView
        if (plugged && level != 100
                && (mTracker.status == BatteryManager.BATTERY_STATUS_CHARGING || mCharging)) {
            // define the bolt shape
            /* @} */
            final float bl = mFrame.left + mFrame.width() / 4f;
            final float bt = mFrame.top + mFrame.height() / 6f;
            final float br = mFrame.right - mFrame.width() / 4f;
            final float bb = mFrame.bottom - mFrame.height() / 10f;
            if (mBoltFrame.left != bl || mBoltFrame.top != bt
                    || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                mBoltFrame.set(bl, bt, br, bb);
                mBoltPath.reset();
                mBoltPath.moveTo(
                        mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                        mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                for (int i = 2; i < mBoltPoints.length; i += 2) {
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                }
                mBoltPath.lineTo(
                        mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                        mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
            }

            float boltPct = (mBoltFrame.bottom - levelTop) / (mBoltFrame.bottom - mBoltFrame.top);
            boltPct = Math.min(Math.max(boltPct, 0), 1);
            if (boltPct <= BOLT_LEVEL_THRESHOLD) {
                // draw the bolt if opaque
                c.drawPath(mBoltPath, mBoltPaint);
            } else {
                // otherwise cut the bolt out of the overall shape
                //tlapa zhw add
                if(!isQsBattery)
                //talpa zhw add end
                mShapePath.op(mBoltPath, Path.Op.DIFFERENCE);
            }
        } else if (mPowerSaveEnabled) {
            // define the plus shape
            final float pw = mFrame.width() * 2 / 3;
            final float pl = mFrame.left + (mFrame.width() - pw) / 2;
            final float pt = mFrame.top + (mFrame.height() - pw) / 2;
            final float pr = mFrame.right - (mFrame.width() - pw) / 2;
            final float pb = mFrame.bottom - (mFrame.height() - pw) / 2;
            if (mPlusFrame.left != pl || mPlusFrame.top != pt
                    || mPlusFrame.right != pr || mPlusFrame.bottom != pb) {
                mPlusFrame.set(pl, pt, pr, pb);
                mPlusPath.reset();
                mPlusPath.moveTo(
                        mPlusFrame.left + mPlusPoints[0] * mPlusFrame.width(),
                        mPlusFrame.top + mPlusPoints[1] * mPlusFrame.height());
                for (int i = 2; i < mPlusPoints.length; i += 2) {
                    mPlusPath.lineTo(
                            mPlusFrame.left + mPlusPoints[i] * mPlusFrame.width(),
                            mPlusFrame.top + mPlusPoints[i + 1] * mPlusFrame.height());
                }
                mPlusPath.lineTo(
                        mPlusFrame.left + mPlusPoints[0] * mPlusFrame.width(),
                        mPlusFrame.top + mPlusPoints[1] * mPlusFrame.height());
            }

            float boltPct = (mPlusFrame.bottom - levelTop) / (mPlusFrame.bottom - mPlusFrame.top);
            boltPct = Math.min(Math.max(boltPct, 0), 1);
            if (boltPct <= BOLT_LEVEL_THRESHOLD) {
                // draw the bolt if opaque
                c.drawPath(mPlusPath, mPlusPaint);
            } else {
                // otherwise cut the bolt out of the overall shape
                mShapePath.op(mPlusPath, Path.Op.DIFFERENCE);
            }
        }

        // compute percentage text
        boolean pctOpaque = false;
        float pctX = 0, pctY = 0;
        String pctText = null;
        /* SPRD: Bug 474751 add charge animation of batteryView @{ */
        mShowPercent = true;
        if (!plugged && !mPowerSaveEnabled && level > mCriticalLevel && mShowPercent) {
            mTextPaint.setColor(getColorForLevel(level));
            mTextPaint.setTextSize(height *
                    (SINGLE_DIGIT_PERCENT ? 0.75f
                            : (mTracker.level == 100 ? 0.38f : 0.5f)));
            /* @} */
            mTextHeight = -mTextPaint.getFontMetrics().ascent;
            pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
            pctX = mWidth * 0.5f;
            pctY = (mHeight + mTextHeight) * 0.47f;
            pctOpaque = levelTop > pctY;
            if (!pctOpaque) {
                mTextPath.reset();
                mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, mTextPath);
                // cut the percentage text out of the overall shape
                if(!isQsBattery) //talpa zhw add
                mShapePath.op(mTextPath, Path.Op.DIFFERENCE);
            }
        }

        // draw the battery shape background
        c.drawPath(mShapePath, mFramePaint);

        // draw the battery shape, clipped to charging level
        mFrame.top = levelTop;
        mClipPath.reset();
        mClipPath.addRect(mFrame,  Path.Direction.CCW);
        mShapePath.op(mClipPath, Path.Op.INTERSECT);
        c.drawPath(mShapePath, mBatteryPaint);

        if(isQsBattery) {
            if(plugged) {
                mBoltPaint.setColor(0xFF3e3e3e);
                c.drawPath(mBoltPath, mBoltPaint);
            }
        }

        // SPRD 474751: change the date when charging, the charge animation of batteryView was stop
        if (!plugged && !mPowerSaveEnabled) {
            if (level <= mCriticalLevel) {
                // draw the warning text
                final float x = mWidth * 0.5f;
                final float y = (mHeight + mWarningTextHeight) * 0.48f;

                if(!isQsBattery)//talpa zhw add
                c.drawText(mWarningString, x, y, mWarningTextPaint);
            } else if (pctOpaque) {
                // draw the percentage text
                if(!isQsBattery)//talpa zhw add
                c.drawText(pctText, pctX, pctY, mTextPaint);
            }
        }
    }

    @Override
    public void draw(Canvas c) {
        if(isQsBattery)
        {
            c.save();
            drawV(c);
            c.restore();
            return;
        }//zhw
        drawStatusBar1(c);
    }

    private void drawStatusBar(Canvas c) {
        final int level = mLevel;

        if (level == -1) return;

        float drawFrac = (float) level / 100f;
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

        // set the battery charging color
        if(mPluggedIn && mCharging && (level!=100)) {
            if(mIsReturnCall) {
                mBatteryPaint.setColor(mReturnCallColor);
            } else {
                mBatteryPaint.setColor(mChargeColor);
            }
        } else {
            mBatteryPaint.setColor(getColorForLevel(level));
        }

        //mBatteryPaint.setColor((mPluggedIn&&mCharging&&(level!=100)) ? mChargeColor : getColorForLevel(level));

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
        if(level > mCriticalLevel)
        {
            if(mShowPercent )
            {   
            	drawPercent(c,level);
            }else
            {                     
            	// battery level battery shape rect
            	drawRatioPercent(c,level);         
            }
        }else
        {
            if (mPluggedIn && mCharging)
            {
                // battery level battery shape rect
            	 if(mShowPercent )
                 {   
                 	drawPercent(c,level);
                 }else
                 {                     
                 	// battery level battery shape rect
                 	drawRatioPercent(c,level);         
                 }             
            }else
            {
                // draw the battery shape background
            	if(mShowPercent)
            	{
            		drawPercent(c,level);
            	}else
            	{
            		drawLowLevelWarning(c,level);
            	}
            }
        }
        if (mPluggedIn && mCharging)
        {
            //set Visibility 充电图标
        }
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


        mBatteryPaint.setColor(getColorForLevel(level));

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

        if(level > mCriticalLevel) {
            if(mShowPercent) {
                drawPercent(c, level);
            } else {
                drawRatioPercent(c, level);
            }
        } else {
            if(mShowPercent) {
                drawPercent(c, level);
            } else {
                drawLowLevelWarning(c,level);
            }
        }
    }

    /**
     * draw percent of battery
     */
    private void drawPercent(Canvas c, int level) {
        c.drawPath(mBatteryBgPath, mFramePaint);

        float pctX = 0, pctY = 0;
        String pctText = null;
        mTextPaint.setColor(getColorForLevel(level));
        mTextPaint.setTextSize(mPercentSize);
        mTextHeight = -mTextPaint.getFontMetrics().ascent;
        //compute percentage text
        pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
        pctX = mWidth - mPaddingBatteryAndEdge - mPaddingBoltAndFrame - mPaddingBatteryBodyAndBolt
                - battaryWidth / 2f;
        pctY =  ((mHeight / 2f) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f));

        //draw percent
        c.drawText(pctText, pctX, pctY, mTextPaint);
    }

    /**
     * draw rate of rectangle battery
     * @param c
     * @param level battery level
     */
    private void drawRatioPercent(Canvas c, int level) {
        // battery level battery shape rect
        float drawFrac = (float) level / 100f;

        int padding = mPaddingBoltAndFrame + mPaddingBatteryBodyAndBolt;
        float left = mFrame.right - padding - (int)(battaryWidth * drawFrac);
        float top = mFrame.top + padding;
        float right =  mFrame.right - padding;
        float bottom = mFrame.bottom - padding;
        c.drawRect(left, top,  right, bottom, mBatteryPaint);
        c.drawPath(mBatteryBgPath, mFramePaint);
    }

    /**
     * draw the low level warning of battery
     * @param c
     */
    private void drawLowLevelWarning(Canvas c,int level) {
        // draw the battery shape background
        c.drawPath(mBatteryBgPath, mFramePaint);
        float pctX = 0, pctY = 0;
        /// George:add for power save
        mWarningTextPaint.setColor(getColorForLevel(level));
        mWarningTextPaint.setTextSize(mPercentSize);

        //linwujia edit begin
                /*float warningWidth = mWarningTextPaint.measureText(mWarningString);
                final float left = (mFrame.width()-warningWidth) / 2;
                pctX = mFrame.left +left+4 ;*/
        pctX = mWidth - mPaddingBatteryAndEdge - mPaddingBoltAndFrame - mPaddingBatteryBodyAndBolt
                - battaryWidth / 2f;
        //linwujia edit end

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
    //linwujia add end

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

    private final class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            updateShowPercent();
            postInvalidate();
            /* SPRD : Bug 474751 add charge animation of batteryView @{ */
            if(DEBUG){
	            BatteryTracker tracker =  mTracker;
	            if (!tracker.plugged) {
	                postInvalidate();
	                cleanTimerTask();
	            }
            }
            /* @} */
        }
    }

    /* SPRD : Bug 474751 add charge animation of batteryView @{ */
    private void cleanTimerTask() {
        if (mChargingAnimate != null){
            mHandler.removeCallbacks(mChargingAnimate);
            mChargingAnimate = null;
        }
    }
    /* @} */
}
