package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.Display;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.systemui.statusbar.phoneleather.LeatherMenuWindowView.CallBack;

import java.util.ArrayList;
import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherTopicView extends RelativeLayout {
	private ViewPager mViewPager;
	private LeatherPagerAdapter mPagerAdapter;
	private ImageButton mImageButton;
	private LinearLayout mLinearLayout;
	private SharedPreferences mPreferences;
	private List<View> mListViews;
	private int[] mDrawableIds = {R.drawable.leather_topic_three_bg, R.drawable.leather_topic_two_bg, R.drawable.leather_topic_six_bg, R.drawable.leather_topic_four_bg, R.drawable.leather_topic_five_bg, R.drawable.leather_topic_one_bg};
	private int mIndicationMarginStart;
	private boolean mAttached = false;
	private Animation mShowTranslateAnimation;
	private Animation mShowAlphaAnimation;
	private AnimationSet mShowAnimationSet;
	private Animation mHideTranslateAnimation;
	private Animation mHideAlphaAnimation;
	private AnimationSet mHideAnimationSet;
	
	private boolean mGotoHideTopicView;
	
	private CallBack mCallBack;
	private OnClickListener mOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			hideWithAnimator();
			if(v.getId() == R.id.leather_topic_back) {
				
			} else {
				cleanSelectedState();
				v.setSelected(true);
				int index = mListViews.indexOf(v);
				mPreferences.edit().putInt("topic", index).commit();
				if(mCallBack != null) {
					mCallBack.LeatherChange(index);
				}
			}
		}
	};

	public LeatherTopicView(Context context) {
		this(context, null);
	}
	
	public LeatherTopicView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public LeatherTopicView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		mPreferences = context.getSharedPreferences("data", 0);
		mListViews = new ArrayList<View>();
		mIndicationMarginStart = (int) getResources().getDimension(R.dimen.leather_topic_indication_margin_end);
		
		mGotoHideTopicView = true;
		
		mShowTranslateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_topic_translate_in);
		mShowAlphaAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_topic_fade_in);
		mShowAnimationSet = new AnimationSet(true);
		mShowAnimationSet.addAnimation(mShowTranslateAnimation);
		mShowAnimationSet.addAnimation(mShowAlphaAnimation);
		
		mHideTranslateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_topic_translate_out);
		mHideAlphaAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_topic_fade_out);
		mHideAnimationSet = new AnimationSet(true);
		mHideAnimationSet.addAnimation(mHideTranslateAnimation);
		mHideAnimationSet.addAnimation(mHideAlphaAnimation);
		mHideAnimationSet.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation arg0) {
				mGotoHideTopicView = false;
			}
			
			@Override
			public void onAnimationRepeat(Animation arg0) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation arg0) {
				setVisibility(View.GONE);
				mGotoHideTopicView = true;
			}
		});
	}
	
	public void setCallBack(CallBack callback) {
		mCallBack = callback;
	}
	
	public void showWithAnimator() {
		if(getVisibility() != View.VISIBLE) {
			setVisibility(View.VISIBLE);
			mViewPager.startAnimation(mShowAnimationSet);
			startAnimation(mShowAlphaAnimation);
		}
	}
	
	public void hideWithAnimator() {
		if(getVisibility() == View.VISIBLE && mGotoHideTopicView) {
			mViewPager.startAnimation(mHideAnimationSet);
			startAnimation(mHideAlphaAnimation);
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		if(!mAttached) {
			mAttached = true;
			mViewPager = (ViewPager) findViewById(R.id.leather_topic_viewPager);
			mImageButton = (ImageButton) findViewById(R.id.leather_topic_back);
			mLinearLayout = (LinearLayout) findViewById(R.id.dotLayout);
			
			initViews();
			initIndication();
			mImageButton.setOnClickListener(mOnClickListener);
			mViewPager.setOffscreenPageLimit(mListViews.size() > 3 ? 3 : mListViews.size());
			mViewPager.setPageTransformer(true, new LeatherTopicPageTransformer());
			mPagerAdapter = new LeatherPagerAdapter(mListViews);
			mViewPager.setAdapter(mPagerAdapter);
			mViewPager.addOnPageChangeListener(new OnPageChangeListener() {
				
				@Override
				public void onPageSelected(int position) {
					cleanState();
					((ImageView)mLinearLayout.getChildAt(position)).setImageResource(R.drawable.leather_viewpager_selected);
				}
				
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					
				}
				
				@Override
				public void onPageScrollStateChanged(int arg0) {
					
				}
			});
		}
	}
	
	@Override
	public void setVisibility(int visibility) {
		if(visibility == View.VISIBLE && mViewPager != null) {
			int selected = mPreferences.getInt("topic", 0);
			mViewPager.setCurrentItem(selected);
		}
		super.setVisibility(visibility);
	}

	@Override
	public Resources getResources() {
		Resources res = super.getResources();
		Configuration config = res.getConfiguration();
		try {
			IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.checkService(
					Context.WINDOW_SERVICE));
			if(wm != null) {
				int densityDpi = wm.getInitialDisplayDensity(Display.DEFAULT_DISPLAY);
				config.densityDpi = densityDpi;
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		res.updateConfiguration(config, res.getDisplayMetrics());
		return res;
	}
	
	private void initViews() {
		ImageView img;
		int selected = mPreferences.getInt("topic", 0);
		for(int i = 0; i < mDrawableIds.length; i++) {
			img = new ImageView(mContext);
			img.setOnClickListener(mOnClickListener);
			img.setImageResource(mDrawableIds[i]);
			if(selected == i) {
				img.setSelected(true);
			}
			mListViews.add(img);
		}
	}
	
	private void initIndication() {
		if(mLinearLayout != null) {
			mLinearLayout.removeAllViews();
			for(int i = 0; i < mDrawableIds.length; i++) {
				ImageView imgView = new ImageView(mContext);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				if(i == 0) {
					imgView.setImageResource(R.drawable.leather_viewpager_selected);
				} else {
					imgView.setImageResource(R.drawable.leather_viewpager_normal);
				}
				if(i != (mListViews.size() - 1)) {
					lp.setMarginEnd(mIndicationMarginStart);
				}
				lp.weight = 1.0f;
				mLinearLayout.addView(imgView, lp);
			}
		}
	}
	
	private void cleanSelectedState() {
		for(int i = 0; i < mListViews.size(); i++) {
			mListViews.get(i).setSelected(false);
		}
	}
	
	private void cleanState() {
		for(int i = 0; i < mLinearLayout.getChildCount(); i++) {
			((ImageView)mLinearLayout.getChildAt(i)).setImageResource(R.drawable.leather_viewpager_normal);
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_UP && isClickOnButton(event)) {
			return mImageButton.performClick();
		}
		return mViewPager.dispatchTouchEvent(event);
	}

	private boolean isClickOnButton(MotionEvent event) {
		int[] location = new int[2];
		mImageButton.getLocationOnScreen(location);
		int minX = location[0] - 30;
		int minY = mImageButton.getTop();
		int maxX = minX + mImageButton.getWidth() + 20;
		int maxY = minY + mImageButton.getHeight();
		float x = event.getX();
		float y = event.getY();
		LogUtil.d("minX:" + minX + ", minY:" + minY + ", maxX:" + maxX + ", maxY:" + maxY);
		LogUtil.d("x:" + x + ", y:" + y);
		if((x > minX && x < maxX) && (y > minY && x < maxY)) {
			return true;
		}
		return false;
	}
	
}
