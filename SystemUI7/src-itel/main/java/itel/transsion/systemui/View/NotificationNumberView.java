package itel.transsion.systemui.View;/*
 * Copyright (C) 2014 The Android Open Source Project
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


import com.android.systemui.R;
import com.android.systemui.statusbar.phone.IconMerger;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

/**
 * A view show number of notification which canot be show on statusbar
 */
public class NotificationNumberView extends View {
    private Paint mTextPaint;
    private int mNumber;
    private int mTextSize;
    private boolean mDark;
    private int mDarkModeFillColor;
    private int mLightModeFillColor;
    private int mFillColor;
    private int mIconHPadding;


    //private PorterDuffXfermode pdXfermode;
    public NotificationNumberView(Context context) {
        this(context, null);
    }

    public NotificationNumberView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationNumberView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationNumberView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mTextSize = context.getResources().getDimensionPixelSize(R.dimen.itel_text_size_notification_count);
        mDarkModeFillColor = context.getColor(R.color.dark_mode_icon_color_single_tone);
        mLightModeFillColor = context.getColor(R.color.light_mode_icon_color_dual_tone_fill);
        mFillColor = mLightModeFillColor;
        initPaint();
        reloadDimens();
    }

    private void reloadDimens() {
        Resources res = mContext.getResources();
        mIconHPadding = res.getDimensionPixelSize(R.dimen.itel_padding_for_notification_number_view);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadDimens();
    }

    private void initPaint() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);//10sp
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setStrokeWidth(0);
        //pdXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    }


    @Override
    protected void onDraw(Canvas canvas) {

        if (mNumber > 0) {
            // 计算Baseline绘制的起点X轴坐标
            String notificationNumbers = mNumber + "";
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            int baseX = (int) (canvasWidth / 2f - mTextPaint.measureText(notificationNumbers) / 2f);
            // 计算Baseline绘制的Y坐标
            int baseY = (int) ((canvasHeight / 2f) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f));
            mTextPaint.setColor(mFillColor);
            float paintWidth = 1f;
            /// George:calculate the radius of circle
            float radius = canvasWidth / 2f - mIconHPadding - paintWidth;
            //mTextPaint.setXfermode(pdXfermode);
            canvas.drawCircle(canvasWidth / 2f, canvasHeight / 2f, radius, mTextPaint);
            canvas.drawText(notificationNumbers, baseX, baseY, mTextPaint);
            //mTextPaint.setXfermode(null);
        }

    }

    public void setNumber(int number) {
        mNumber = number;
        invalidate();
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }

    public void setDark(int darkIntensity) {
        /// George:the parameter has assigned a value ,so we dont need to get from getFillColor function
        mFillColor = darkIntensity;//getFillColor(darkIntensity);
        invalidate();
    }


}
