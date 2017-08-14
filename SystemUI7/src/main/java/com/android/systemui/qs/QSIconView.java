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

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.systemui.R;

import java.util.Objects;

import itel.transsion.settingslib.utils.LogUtil;

public class QSIconView extends ViewGroup {

    public static final int  AnimationDuration = 250;
    protected final View mIcon;
    protected final int mIconSizePx;
    protected final int mTilePaddingBelowIconPx;
    private boolean mAnimationEnabled = true;
    //zhw add
    protected FrameLayout mIconFrame;
    protected ImageView mMaskView;
    protected ImageView mTopView;
    protected ImageView mBottomView;
    protected ImageView mBgView;

    protected boolean isFirst = true;

    //talpa zhw add
    QSPanel mPanel = null;
    //talpa zhw add end

    public void setQSPanel(QSPanel panel)
    {
        mPanel = panel;
    }
    //zhw add end

    public QSIconView(Context context) {
        super(context);

        final Resources res = context.getResources();
        mIconSizePx = res.getDimensionPixelSize(R.dimen.qs_tile_icon_size);
        mTilePaddingBelowIconPx =  res.getDimensionPixelSize(R.dimen.qs_tile_padding_below_icon);
        mIcon = createIcon();
        addView(mIcon);
    }

    public void disableAnimation() {
        mAnimationEnabled = false;
    }

    public View getIconView() {
        return mIcon;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int w = MeasureSpec.getSize(widthMeasureSpec);
        final int iconSpec = exactly(mIconSizePx);
        mIcon.measure(MeasureSpec.makeMeasureSpec(w, getIconMeasureMode()), iconSpec);

        setMeasuredDimension(w, mIcon.getMeasuredHeight() + mTilePaddingBelowIconPx);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int w = getMeasuredWidth();
        final int h = getMeasuredHeight();
        int top = 0;
        final int iconLeft = (w - mIcon.getMeasuredWidth()) / 2;
        layout(mIcon, iconLeft, top);
    }

    protected  boolean canAnimation() {
        if (isFirst || !mAnimationEnabled) {
            if (mBgView != null) {
                mBgView.setVisibility(View.INVISIBLE);
            }
            if (mMaskView != null) {
                mMaskView.setVisibility(View.GONE);
            }
            if (mBottomView != null) {
                mBottomView.setVisibility(View.INVISIBLE);
            }
            return false;
        }
        if (mPanel != null && mPanel.getVisibility() != View.VISIBLE) {
            return false;
        }
        return true;
    }

    boolean stateValue = false;
    public void setIcon(QSTile.State state) {
        //talpa zhw setIcon((ImageView) mIcon, state);
        /// George:fix bug when pull down quick setting panel ,some icon may do animation without state changed
        if (Objects.equals(state.icon, mTopView.getTag(R.id.qs_icon_tag))) {
//            Log.d("mTopView", "qs_icon_tag-same");
            return;
        }
        // talpa@andy 2017/5/22 15:11 add @{
//        LogUtil.d("switching="+state.switching);
        if (state.switching) {
            setIcon(mTopView, state);
            return;
        }
        if (mTopView.getDrawable() != null&&mTopView.getDrawable() instanceof Animatable) {
                ((Animatable) mTopView.getDrawable()).stop();
        }
        // @}
        QSTile.BooleanState tempState = null;
        if (state instanceof QSTile.BooleanState) {
            tempState = (QSTile.BooleanState) state;
            stateValue = tempState.value;
        }
        if(!canAnimation()) {
            setIcon(mTopView, state);
            isFirst = false;
            return;
        }
        final  QSTile.State mState = state;
        if (mMaskView != null) {
            mMaskView.setVisibility(View.VISIBLE);
        }
        if (mBottomView != null) {
            mBottomView.setVisibility(View.VISIBLE);
        }
        int topViewStart = 0;
        int topViewEnd = -mTopView.getMeasuredHeight();
        int bottomViewStart = 0;
        if (mBottomView != null) {
            bottomViewStart = mBottomView.getMeasuredHeight();
        }
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
        if (state.bottomIcon != null && mBottomView != null) {
            mBottomView.setImageDrawable(state.bottomIcon.getDrawable(mContext));
        }
        Animation ani2 = new TranslateAnimation(0, 0, bottomViewStart, bottomViewEnd);
        ani2.setDuration(AnimationDuration);
        if (mBottomView != null) {
            mBottomView.startAnimation(ani2);
        }
        Animation ani = new TranslateAnimation(0, 0, topViewStart, topViewEnd);
        ani.setDuration(AnimationDuration);
        ani.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (stateValue) {
                    setIcon(mTopView, mState);
                }
                if (mBgView != null) {
                    mBgView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (!stateValue) {
                    setIcon(mTopView, mState);
                }
                if (mBgView != null) {
                    mBgView.setVisibility(View.GONE);
                }
                if (mMaskView != null) {
                    mMaskView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mTopView.startAnimation(ani);
    }

    protected void setIcon(ImageView iv, QSTile.State state) {
        if (!Objects.equals(state.icon, iv.getTag(R.id.qs_icon_tag))) {
            Drawable d = state.icon != null
                    ? iv.isShown() && mAnimationEnabled ? state.icon.getDrawable(mContext)
                    : state.icon.getInvisibleDrawable(mContext) : null;
            int padding = state.icon != null ? state.icon.getPadding() : 0;
            if (d != null && state.autoMirrorDrawable) {
                d.setAutoMirrored(true);
            }

            iv.setImageDrawable(d);
            iv.setTag(R.id.qs_icon_tag, state.icon);
            iv.setPadding(0, padding, 0, padding);
            if (d instanceof Animatable && iv.isShown()) {
                Animatable a = (Animatable) d;
                a.start();
                if (!iv.isShown()) {
                    a.stop(); // skip directly to end state
                }
            }
        }
        if (state.disabledByPolicy) {
            iv.setColorFilter(getContext().getColor(R.color.qs_tile_disabled_color));
        } else {
            iv.clearColorFilter();
        }
    }

    protected int getIconMeasureMode() {
        return MeasureSpec.EXACTLY;
    }

    protected View createIcon() {
        //zhw add
        mIconFrame = new FrameLayout(mContext);
        mBgView = new ImageView(mContext);
        mBgView.setScaleType(ScaleType.CENTER);
        mBgView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_bg));
        mIconFrame.addView(mBgView);

        mBottomView = new ImageView(mContext);
        mBottomView.setScaleType(ScaleType.CENTER);
        mIconFrame.addView(mBottomView);

        mTopView = new ImageView(mContext);
        mTopView.setId(android.R.id.icon);
        mTopView.setScaleType(ScaleType.CENTER);
        mIconFrame.addView(mTopView);

        mMaskView = new ImageView(mContext);
        mMaskView.setScaleType(ScaleType.FIT_CENTER);
        mMaskView.setImageDrawable(mContext.getDrawable(R.drawable.itel_qs_mask));
        mIconFrame.addView(mMaskView);
        return mIconFrame;
    }

    //设置Tile过渡图标
    public void setTransitionIcon(int resID) {
         mTopView.setImageResource(resID);
    }

    protected final int exactly(int size) {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
    }

    protected final void layout(View child, int left, int top) {
        child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
    }

   /* @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        LogUtil.i("dispatchTouchEvent>ev"+ev.getActionMasked());
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {
        LogUtil.i("onInterceptHoverEvent>event"+event.getActionMasked());
        return super.onInterceptHoverEvent(event);
    }*/
}
