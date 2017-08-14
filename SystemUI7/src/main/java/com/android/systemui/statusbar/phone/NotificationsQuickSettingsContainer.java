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
 * limitations under the License
 */

package com.android.systemui.statusbar.phone;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.qs.QSContainer;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.statusbar.StatusBarState;

import itel.transsion.systemui.View.NotificationClearAllViewButton;

/**
 * The container with notification stack scroller and quick settings inside.
 */
public class NotificationsQuickSettingsContainer extends FrameLayout
        implements ViewStub.OnInflateListener, AutoReinflateContainer.InflateListener {


    private AutoReinflateContainer mQsContainer;
    private View mUserSwitcher;
    private View mStackScroller;
    private View mKeyguardStatusBar;
    private boolean mInflated;
    private boolean mQsExpanded;
    private boolean mCustomizerAnimating;

    private int mBottomPadding;
    private int mStackScrollerMargin;

    // add begin by lych
    private PhoneStatusBar mStatusBar;
    private int mTransparentColor;
    private int mNotificationBackgroundColor;
    private int mStackScrollerMarginBottom;
    private QSContainer mQSContainer;
    private float mFraction;
    private int mAnimatorDuration;
    private ValueAnimator mStackScrollerHeightChangeAnimator;
    private ValueAnimator mClearAllChangeAnimator;
    private RelativeLayout mNotiClearAll;
    private NotificationClearAllViewButton mClearAllButton;
    private LinearLayout mSplitLine;
    // add end by lych

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        //init(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mQsContainer = (AutoReinflateContainer) findViewById(R.id.qs_auto_reinflate_container);
        mQsContainer.addInflateListener(this);
        mStackScroller = findViewById(R.id.notification_stack_scroller);
        mStackScrollerMargin = ((LayoutParams) mStackScroller.getLayoutParams()).bottomMargin;
        mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        ViewStub userSwitcher = (ViewStub) findViewById(R.id.keyguard_user_switcher);
        userSwitcher.setOnInflateListener(this);
        mUserSwitcher = userSwitcher;
        init(mContext);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reloadWidth(mQsContainer);
        reloadWidth(mStackScroller);
    }

    private void reloadWidth(View view) {
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        params.width = getContext().getResources().getDimensionPixelSize(
                R.dimen.notification_panel_width);
        view.setLayoutParams(params);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mBottomPadding = insets.getStableInsetBottom();
        setPadding(0, 0, 0, mBottomPadding);
        return insets;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean userSwitcherVisible = mInflated && mUserSwitcher.getVisibility() == View.VISIBLE;
        boolean statusBarVisible = mKeyguardStatusBar.getVisibility() == View.VISIBLE;

        final boolean qsBottom = mQsExpanded && !mCustomizerAnimating;
        View stackQsTop = qsBottom ? mStackScroller : mQsContainer;
        View stackQsBottom = !qsBottom ? mStackScroller : mQsContainer;
        // Invert the order of the scroll view and user switcher such that the notifications receive
        // touches first but the panel gets drawn above.
        if (child == mQsContainer) {
            return super.drawChild(canvas, userSwitcherVisible && statusBarVisible ? mUserSwitcher
                    : statusBarVisible ? mKeyguardStatusBar
                    : userSwitcherVisible ? mUserSwitcher
                    : stackQsBottom, drawingTime);
        } else if (child == mStackScroller) {
            return super.drawChild(canvas,
                    userSwitcherVisible && statusBarVisible ? mKeyguardStatusBar
                    : statusBarVisible || userSwitcherVisible ? stackQsBottom
                    : stackQsTop,
                    drawingTime);
        } else if (child == mUserSwitcher) {
            return super.drawChild(canvas,
                    userSwitcherVisible && statusBarVisible ? stackQsBottom
                    : stackQsTop,
                    drawingTime);
        } else if (child == mKeyguardStatusBar) {
            return super.drawChild(canvas,
                    stackQsTop,
                    drawingTime);
        } else {
            return super.drawChild(canvas, child, drawingTime);
        }
    }

    @Override
    public void onInflate(ViewStub stub, View inflated) {
        if (stub == mUserSwitcher) {
            mUserSwitcher = inflated;
            mInflated = true;
        }
    }

    @Override
    public void onInflated(View v) {
        QSCustomizer customizer = ((QSContainer) v).getCustomizer();
        customizer.setContainer(this);
    }

    public void setQsExpanded(boolean expanded) {
        if (mQsExpanded != expanded) {
            mQsExpanded = expanded;
            invalidate();
        }
    }

    public void setCustomizerAnimating(boolean isAnimating) {
        if (mCustomizerAnimating != isAnimating) {
            mCustomizerAnimating = isAnimating;
            invalidate();
        }
    }

    public void setCustomizerShowing(boolean isShowing) {
        if (isShowing) {
            // Clear out bottom paddings/margins so the qs customization can be full height.
            setPadding(0, 0, 0, 0);
            setBottomMargin(mStackScroller, 0);
        } else {
            setPadding(0, 0, 0, mBottomPadding);
            setBottomMargin(mStackScroller,0 /*mStackScrollerMargin*/);
        }

    }

    private void setBottomMargin(View v, int bottomMargin) {
        LayoutParams params = (LayoutParams) v.getLayoutParams();
        params.bottomMargin = bottomMargin;
        v.setLayoutParams(params);
    }

    // add begin by lych
    private void init(Context ctx){
        mTransparentColor=ctx.getColor(android.R.color.transparent);
        mNotificationBackgroundColor=ctx.getColor(R.color.notification_shade_background_color);
        mStackScrollerMarginBottom=getResources().getDimensionPixelSize(R.dimen.notification_stack_layout_margin_bottom);
        mAnimatorDuration = getResources().getInteger(R.integer.notification_clear_all_button_duration);
        mSplitLine = (LinearLayout) findViewById(R.id.clear_all_bottom_split);
        mNotiClearAll = (RelativeLayout) findViewById(R.id.notification_clear_all);
        mClearAllButton = (NotificationClearAllViewButton) findViewById(R.id.ib_clear_all);
        mClearAllButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MetricsLogger.action(mContext, MetricsProto.MetricsEvent.ACTION_DISMISS_ALL_NOTES);
                mStatusBar.clearAllNotifications();
            }
        });
    }
    public void setStatusBar(PhoneStatusBar bar) {
        mStatusBar = bar;
    }
    public void setQsContainer(QSContainer qsContainer){
        mQSContainer = qsContainer;
    }
    public void setExpandFraction(float fraction , float mExpandedHeight){
        mFraction = fraction;
        updateBackgroundColor();
        boolean hideImmediately = !shouldShowClearAllView() && mExpandedHeight< mQSContainer.getHeader().getHeight()/4;
        if(hideImmediately){
            if(isClearButtonVisibled()){
                setClearAllViewHide();
            }
        }else {
            /// George: when fraction lower than 0.3 and higher than 0.6 we  do animation,else not
            if(fraction < 0.3 || fraction > 0.6) {
                setClearAllButtonShowing(shouldShowClearAllView() && fraction < 0.5 );
            }
        }
    }
    public void updateBackgroundColor(){
        boolean transparent=mStatusBar.getBarState()== StatusBarState.KEYGUARD||mStatusBar.isPanelFullyCollapsed();
        setBackgroundColor(transparent?mTransparentColor:mNotificationBackgroundColor);
    }
    public void setClearAllButtonShowing(boolean isShowing) {
        if(isShowing==isClearButtonVisibled()) return;
        if(mNotiClearAll!=null && mClearAllButton!=null){
            mClearAllButton.setVisibility(isShowing?VISIBLE:GONE);
        }
        if (isShowing) {
            // Clear all button margins bottom.
            /// George:changed for performance, we dont need to request so many times when playing animation
            setBottomMargin(mStackScroller,mStackScrollerMarginBottom);
            startClearAllViewAnimation(mStackScrollerMarginBottom,0);
            /// end
        } else {
            /// George:changed for performance, we dont need to request so many times when playing animation
            setBottomMargin(mStackScroller,0);
            startClearAllViewAnimation(0,mStackScrollerMarginBottom);
            /// end
        }
    }
    public void setClearAllViewHide(){
        if(getStackScrollerBottomMargin()==0) return;
        if(mNotiClearAll!=null && mClearAllButton!=null){
            /// George: comment it for performance
            setNotiClearAllViewHeight(mStackScrollerMarginBottom);
            //mClearAllButton.setVisibility(GONE);
            /// end
        }
    }
    private void startStackScrollerHeightChangeAnimation(int oldHeight,int newHeight){
        mStackScrollerHeightChangeAnimator = ValueAnimator.ofInt(oldHeight,newHeight);
        mStackScrollerHeightChangeAnimator.setDuration(mAnimatorDuration);
        //mStackScrollerHeightChangeAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mStackScrollerHeightChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = (int)mStackScrollerHeightChangeAnimator.getAnimatedValue();
                setBottomMargin(mStackScroller, height);
            }
        });
        mStackScrollerHeightChangeAnimator.start();
    }
    private void startClearAllViewAnimation(int oldHeight,int newHeight){
        mClearAllChangeAnimator = ValueAnimator.ofInt(oldHeight,newHeight);
        mClearAllChangeAnimator.setDuration(mAnimatorDuration);
        mClearAllChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int height = (int)mClearAllChangeAnimator.getAnimatedValue();
                setNotiClearAllViewHeight(height);
            }
        });
        mClearAllChangeAnimator.start();
    }
    private void setNotiClearAllViewHeight(int height){
        /// George:comment it for we dont need to request so many times,use setTranslationY instead
       /* LayoutParams layoutParams = (LayoutParams) mNotiClearAll.getLayoutParams();
        layoutParams.height = height;
        mNotiClearAll.setLayoutParams(layoutParams);*/
       /// end
        mNotiClearAll.setTranslationY(height);
    }

    private boolean isClearButtonVisibled(){
        return mClearAllButton.isButtonVisibled();
    }
    private boolean shouldShowClearAllView(){
        boolean showClearAll = mStatusBar!=null
                    && mStatusBar.getBarState()!=StatusBarState.KEYGUARD
                    && !mStatusBar.isCollapsing()
                    && !mStatusBar.isPanelFullyCollapsed()
                    && !isQsCustomizering()
                    && mStatusBar.hasActiveNotifications();
        return showClearAll;
    }
    private boolean isQsCustomizering(){
        return mQSContainer.getCustomizer()!=null && mQSContainer.getCustomizer().isCustomizing();
    }
    private int getStackScrollerBottomMargin(){
        return ((LayoutParams) mStackScroller.getLayoutParams()).bottomMargin;
    }
    // add end by lych
}
