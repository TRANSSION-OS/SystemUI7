package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherViewPager extends ViewPager {
	private float mDownX;
	private float mDownY;
	private final int mSlop = 100;
	private final int mMinSlop = 15;
	private final long mMinTime = 300;
	private long mDownTime;
	private LeatherMenuWindowView mMenuView;
	private LeatherTopicView mTopicView;
	
	public LeatherViewPager(Context context) {
		this(context, null);
	}

	public LeatherViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void setLeatherMenu(LeatherMenuWindowView menuView) {
		mMenuView = menuView;
	}

	public void setLeatherTopicView(LeatherTopicView topicView) {
		mTopicView = topicView;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		float x = event.getX();
		float y = event.getY();
		long time = event.getEventTime();
		LogUtil.d("dispatchTouchEvent:action=" + action);
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mDownX = x;
			mDownY = y;
			mDownTime = time;
			break;
		case MotionEvent.ACTION_MOVE:
			final float h = y - mDownY;
			final float w = Math.abs(x - mDownX);
			final long t = time - mDownTime;
			LogUtil.d("h:" + h + ", w:" + w + ", t:" + t);
			if(getCurrentItem() == 1 && Math.abs(h) > mSlop && Math.abs(h) > w) {
				LogUtil.d("h > mSlop && h > w");
				if(h < 0) {
					//showMenuView();
					showTopicView();
				}
				return true;
			} else if(getCurrentItem() == 1 && Math.abs(h) < mMinSlop && w < mMinSlop && t >= mMinTime) {
				//showMenuView();
				showTopicView();
			}
			break;
		default:
			break;
		}
		return super.dispatchTouchEvent(event);
	}

	private void showMenuView() {
		if(mMenuView != null && mMenuView.getVisibility() == View.GONE) {
			mMenuView.setVisibility(View.VISIBLE);
			mMenuView.showMenuView();
		}
	}

	private void showTopicView() {
		if(mTopicView != null && mTopicView.getVisibility() == View.GONE) {
			mTopicView.showWithAnimator();
		}
	}
	
	/*@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		float x = event.getX();
		float y = event.getY();
		Log.d("ClockView", "onInterceptTouchEvent:action=" + action);		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mDownX = x;
			mDownY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			final float h = Math.abs(y - mDownY);
			final float w = Math.abs(x - mDownX);
			Log.d("ClockView", "h:" + h + ", w:" + w);
			if(h > mSlop && h > w) {
				Log.d("ClockView", "h > mSlop && h > w");
			}
			break;
		default:
			break;
		}
		return false;
	}*/
	
}
