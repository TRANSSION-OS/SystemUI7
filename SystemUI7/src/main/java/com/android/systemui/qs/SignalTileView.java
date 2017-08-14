/*
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
 * limitations under the License.
 */

package com.android.systemui.qs;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile.SignalState;

/** View that represents a custom quick settings tile for displaying signal info (wifi/cell). **/
public final class SignalTileView extends QSIconView {
    private static final long DEFAULT_DURATION = new ValueAnimator().getDuration();
    private static final long SHORT_DURATION = DEFAULT_DURATION / 3;

    private FrameLayout mIconFrame;
    private ImageView mSignal;
    private ImageView mOverlay;
    // talpa@andy 2017/4/24 17:14 delete @{
//    private ImageView mIn;
//    private ImageView mOut;
    // @}

    private int mWideOverlayIconStartPadding;

    public SignalTileView(Context context) {
        super(context);
     // talpa@andy 2017/3/30 22:08 delete @{
 /*       if(false) {
            mIn = addTrafficView(R.drawable.ic_qs_signal_in);
            mOut = addTrafficView(R.drawable.ic_qs_signal_out);
        }*/
//        mIn = addTrafficView(R.drawable.itel_ic_qs_signal_in);
//        mOut = addTrafficView(R.drawable.itel_ic_qs_signal_out);
        // @}
        mWideOverlayIconStartPadding = context.getResources().getDimensionPixelSize(
                R.dimen.wide_type_icon_start_padding_qs);
    }

    private ImageView addTrafficView(int icon) {
        final ImageView traffic = new ImageView(mContext);
        traffic.setImageResource(icon);
        traffic.setAlpha(0f);
        addView(traffic);
        return traffic;
    }

    @Override
    protected View createIcon() {
        mIconFrame = new FrameLayout(mContext);

        //talpa zhw add
        mBgView = new ImageView(mContext);
        mBgView.setScaleType(ImageView.ScaleType.CENTER);
        mBgView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_bg));
        mIconFrame.addView(mBgView);
        mBottomView = new ImageView(mContext);
        mBottomView.setScaleType(ImageView.ScaleType.CENTER);
        mIconFrame.addView(mBottomView);
        //talpa zhw add en

        mSignal = new ImageView(mContext);
        mIconFrame.addView(mSignal);
        mOverlay = new ImageView(mContext);
        mIconFrame.addView(mOverlay, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        //talpa zhw add
        mMaskView = new ImageView(mContext);
        mMaskView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mMaskView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_mask));
        mIconFrame.addView(mMaskView);
        mTopView = mSignal;
        //talpa zhw add end
        return mIconFrame;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // talpa@andy 2017/4/24 17:16 delete @{
//        int hs = MeasureSpec.makeMeasureSpec(mIconFrame.getMeasuredHeight(), MeasureSpec.EXACTLY);
//        int ws = MeasureSpec.makeMeasureSpec(mIconFrame.getMeasuredHeight(), MeasureSpec.AT_MOST);
//        mIn.measure(ws, hs);
//        mOut.measure(ws, hs);
        // @}
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        // talpa@andy 2017/4/24 17:16 delete @{
//        layoutIndicator(mIn);
//        layoutIndicator(mOut);
        // @}
    }

    @Override
    protected int getIconMeasureMode() {
        return MeasureSpec.AT_MOST;
    }

    private void layoutIndicator(View indicator) {
        boolean isRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int left, right;
        /* talpa zhw modify
        if (isRtl) {
            right = mIconFrame.getLeft();
            left = right - indicator.getMeasuredWidth();
        } else {
            left = mIconFrame.getRight();
            right = left + indicator.getMeasuredWidth();
        }
        */

        left = mIconFrame.getLeft();
        right = mIconFrame.getRight();
        if (isRtl) {
            indicator.setRotationY(180f);
        }
        indicator.layout(
                left,
                mIconFrame.getBottom() - indicator.getMeasuredHeight(),
                right,
                mIconFrame.getBottom());
    }

    //talpa zhw add
    boolean oldValue = false;
    protected boolean canAnimation(QSTile.State state)
    {
        QSTile.SignalState mState = (QSTile.SignalState)state;
        boolean can = super.canAnimation();
        if(can)
        {
            if(oldValue == mState.value)
            {
                can = false;
            }
        }
        oldValue = mState.value;
        return can;
    }

    private  void updateAnotherInfo(SignalState s)
    {
        if (s.overlayIconId > 0) {
            mOverlay.setVisibility(VISIBLE);
            mOverlay.setImageResource(s.overlayIconId);
        } else {
            mOverlay.setVisibility(GONE);
        }
        if (s.overlayIconId > 0 && s.isOverlayIconWide) {
            mSignal.setPaddingRelative(mWideOverlayIconStartPadding, 0, 0, 0);
        } else {
            mSignal.setPaddingRelative(0, 0, 0, 0);
        }
        Drawable drawable = mSignal.getDrawable();
        if (s.autoMirrorDrawable && drawable != null) {
            drawable.setAutoMirrored(true);
        }
        // talpa@andy 2017/4/24 17:33 delete @{
        /*final boolean shown = isShown();
        setVisibility(mIn, shown, s.activityIn);
        setVisibility(mOut, shown, s.activityOut);*/
        // @}
    }
    //talpa zhw add end
    @Override
    public void setIcon(QSTile.State state) {
        final SignalState s = (SignalState) state;
        //talpa zhw modify setIcon(mSignal, s);
        if (!canAnimation(state)) {
            setIcon(mSignal, s);
            isFirst = false;
            updateAnotherInfo(s);
            return;
        }
        QSTile.BooleanState tempState = null;
        if (state instanceof QSTile.BooleanState) {
            tempState = (QSTile.BooleanState) state;
            stateValue = tempState.value;
        }

        mMaskView.setVisibility(View.VISIBLE);
        mBottomView.setVisibility(View.VISIBLE);

        int topViewStart = 0;
        int topViewEnd = -mTopView.getMeasuredHeight();
        int bottomViewStart = mBottomView.getMeasuredHeight();
        int bottomViewEnd = 0;
        if (tempState != null && tempState.value == true) {
            int temp;
            temp = topViewStart;
            topViewStart = topViewEnd;
            topViewEnd = temp;

            temp = bottomViewStart;
            bottomViewStart = bottomViewEnd;
            bottomViewEnd = temp;
        }

        if (state.bottomIcon != null) {
            mBottomView.setImageDrawable(state.bottomIcon.getDrawable(mContext));
        }
        Animation ani2 = new TranslateAnimation(0, 0, bottomViewStart, bottomViewEnd);
        ani2.setDuration(AnimationDuration);
        mBottomView.startAnimation(ani2);

        Animation ani = new TranslateAnimation(0, 0, topViewStart, topViewEnd);
        ani.setDuration(AnimationDuration);
        ani.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (stateValue) {
                    setIcon((ImageView) mTopView, s);
                }

                mBgView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!stateValue) {

                    setIcon((ImageView) mTopView, s);
                }
                mBgView.setVisibility(View.GONE);
                mMaskView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mTopView.startAnimation(ani);
        //talpa zhw modify end
        // talpa@andy 2017/4/24 17:38 delete @{
       /* if (s.overlayIconId > 0) {
            mOverlay.setVisibility(VISIBLE);
            mOverlay.setImageResource(s.overlayIconId);
        } else {
            mOverlay.setVisibility(GONE);
        }
        if (s.overlayIconId > 0 && s.isOverlayIconWide) {
            mSignal.setPaddingRelative(mWideOverlayIconStartPadding, 0, 0, 0);
        } else {
            mSignal.setPaddingRelative(0, 0, 0, 0);
        }
        Drawable drawable = mSignal.getDrawable();
        if (state.autoMirrorDrawable && drawable != null) {
            drawable.setAutoMirrored(true);
        }*/
//        final boolean shown = isShown();
//        setVisibility(mIn, shown, s.activityIn);
//        setVisibility(mOut, shown, s.activityOut);
        // @}
    }

    private void setVisibility(View view, boolean shown, boolean visible) {
        final float newAlpha = shown && visible ? 1 : 0;
        if (view.getAlpha() == newAlpha) return;
        if (shown) {
            view.animate()
                .setDuration(visible ? SHORT_DURATION : DEFAULT_DURATION)
                .alpha(newAlpha)
                .start();
        } else {
            view.setAlpha(newAlpha);
        }
    }
}
