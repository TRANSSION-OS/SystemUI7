package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherMenuWindowView extends FrameLayout implements OnClickListener {

	public final static int LEATHER_MENU_PROFILES = 0;
	public final static int LEATHER_MENU_TOPIC = 1;

	private Button mTopicBtn, mProfilesBtn;
	private View mDownBtn;
	private TextView mTopicTx, mProfilesTx;
	private boolean mIsAttached;
	private View mContentView;
	private CallBack mCallBack;
	private float mDownX;
	private float mDownY;
	private final int mSlop = 150;
	private boolean mGotoHideMenuView;

	private AnimationSet mShowAnimationSet;
	private Animation mShowTranslateAnimation;
	private Animation mShowAlphaAnimation;
	
	private AnimationSet mHideAnimationSet;
	private Animation mHideTranslateAnimation;
	private Animation mHideAlphaAnimation;
	
	public LeatherMenuWindowView(Context context) {
		this(context, null);
	}

	public LeatherMenuWindowView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LeatherMenuWindowView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mGotoHideMenuView = true;
		
		mShowTranslateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_menu_translate_in);
		mShowAlphaAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_menu_fade_in);
		mShowAnimationSet = new AnimationSet(true);
		mShowAnimationSet.addAnimation(mShowTranslateAnimation);
		mShowAnimationSet.addAnimation(mShowAlphaAnimation);
	    
		mHideTranslateAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_menu_translate_out);
		mHideAlphaAnimation = AnimationUtils.loadAnimation(mContext, R.anim.leather_menu_fade_out);
		mHideAnimationSet = new AnimationSet(true);
		mHideAnimationSet.addAnimation(mHideTranslateAnimation);
		mHideAnimationSet.addAnimation(mHideAlphaAnimation);
		mHideAnimationSet.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				mGotoHideMenuView = false;
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				setVisibility(View.GONE);
				mGotoHideMenuView = true;
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
		});	
	}	
	
	public void setCallBack(CallBack callback) {
		mCallBack = callback;
	}
	
	public View getContentView() {
		return mContentView;
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(!mIsAttached) {
			mDownBtn = findViewById(R.id.leather_menu_down);
			mTopicBtn = (Button) findViewById(R.id.leather_menu_topic);
			mProfilesBtn = (Button) findViewById(R.id.leather_menu_profiles);
			mTopicTx = (TextView) findViewById(R.id.leather_menu_topic_text);
			mProfilesTx = (TextView) findViewById(R.id.leather_menu_profile_text);
			mContentView = findViewById(R.id.leather_menu_content);
			
			mDownBtn.setOnClickListener(this);
			mTopicBtn.setOnClickListener(this);
			mProfilesBtn.setOnClickListener(this);
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(mIsAttached) {
			mIsAttached = false;
		}
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		LogUtil.d("LeatherMenuWindowView:onConfigurationChanged");
		mTopicTx.setText(R.string.leather_theme);
		mProfilesTx.setText(R.string.audio_profile);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getActionMasked();
		float x = event.getX();
		float y = event.getY();
		LogUtil.d("onTouchEvent:action=" + action);
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			mDownX = x;
			mDownY = y;
			break;
		case MotionEvent.ACTION_UP:
			final float h = y - mDownY;
			final float w = Math.abs(x - mDownX);
			LogUtil.d("h:" + h + ", w:" + w);
			if(Math.abs(h) > mSlop && Math.abs(h) > w) {
				LogUtil.d("h > mSlop && h > w");
				if(h > 0) {
					hideMenuView();
				}
			}
			break;
		/*case MotionEvent.ACTION_UP:
			if(getVisibility() == View.VISIBLE && mDownY <= 189 && y <= 189) {
				hideMenuView();
			}
			break;*/
		default:
			break;
		}
		return true;//用于消费掉事件，这样ViewPager才不会有滑动的事件
	}

	@Override
	public void onClick(View view) {
		hideMenuView();
		if(mCallBack != null) {
			if(view.getId() == R.id.leather_menu_profiles) {
				mCallBack.OnClick(LEATHER_MENU_PROFILES);
			} else if(view.getId() == R.id.leather_menu_topic) {
				mCallBack.OnClick(LEATHER_MENU_TOPIC);
			}
		}
	}
	
	public void showMenuView() {
	    startAnimation(mShowAlphaAnimation);
	    mContentView.startAnimation(mShowAnimationSet);
	}
	
	public void hideMenuView() {
		if(getVisibility() == View.VISIBLE && mGotoHideMenuView) {						
		    startAnimation(mHideAlphaAnimation);
		    mContentView.startAnimation(mHideAnimationSet);
		}
	}
	
	public interface CallBack {
		void OnClick(int whichView);
		void LeatherChange(int index);
	}
}
