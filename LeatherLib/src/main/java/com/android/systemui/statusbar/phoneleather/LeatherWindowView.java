package com.android.systemui.statusbar.phoneleather;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.systemui.statusbar.phoneleather.LeatherMenuWindowView.CallBack;

import java.util.ArrayList;
import java.util.List;

public class LeatherWindowView extends RelativeLayout {
	private int mScreenWidth, mScreenHeight;
	private int mWidth, mHeight;
	// 外圆环距离顶部的距离
	private int mPaddingTop = 38;// px
	// 外圆距离左右的距离
	private int mCirclePadding = 30;// 20dp 人工计算得来，(480-280*1.5)/2 = 30;
	
	private LeatherBase mLeatherView;
	private int mCurrentViewIndex;
	//private LeatherMenuWindowView mMenuView;
	//private AudioProfilesWindowView mAudioProfilesView;
	private LeatherTopicView mTopicView;
	private LeatherViewPager mViewPager;
	private LeatherPagerAdapter mPagerAdapter;
	private LinearLayout mIndicationView;
    private List<View> mListViews = new ArrayList<View>();      
    
    private CallBack mCallBack;
	
	private boolean mIsAttached;
	private SharedPreferences mPreferences;
	private int MissPhoneCallCount, MissedSmsCount;

	//private LeatherAudioProfilesController mLeatherAudioProfilesController;

	private Callback mCallback;

	private CameraView mCameraView;
	private LeatherMusicView mLeatherMusicView;
	
	/**
     * M:
     * Broadcast Action: action used for launcher unread number feature.
     * The broadcat is sent when the unread number of application changes.
     * New
     * @hide
     * @internal
     */
    private final String ACTION_UNREAD_CHANGED = "com.mediatek.action.UNREAD_CHANGED";
    /**
     * M:
     * Extra used to indicate the unread number of which component changes.
     * New
     * @hide
     * @internal
     */
    private final String EXTRA_UNREAD_COMPONENT = "com.mediatek.intent.extra.UNREAD_COMPONENT";

    /**
     * M:
     * The number of unread messages.
     * New
     * @hide
     * @internal
     */
    private final String EXTRA_UNREAD_NUMBER = "com.mediatek.intent.extra.UNREAD_NUMBER";
    
    private final String DIALER_PACKAGE_NAME = "com.android.dialer";
    private final String DIALER_CLS_NAME = "com.android.dialer.DialtactsActivity";
    
    private final String MMS_PACKAGE_NAME = "com.android.mms";
	//linwujia edit begin
	//private final String MMS_CLS_NAME = "com.android.mms.ui.BootActivity";
	private final String MMS_CLS_NAME = "com.android.messaging.ui.conversationlist.ConversationListActivity";
	//linwujia edit end
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(ACTION_UNREAD_CHANGED.equals(action)) {
	        	   final Bundle bundle = intent.getExtras();
	               final ComponentName componentName = (ComponentName) bundle.get(EXTRA_UNREAD_COMPONENT);
	               int unreadNum = intent.getIntExtra(EXTRA_UNREAD_NUMBER, -1);
	               if(DIALER_PACKAGE_NAME.equals(componentName.getPackageName()) && DIALER_CLS_NAME.equals(componentName.getClassName())) {
	            	   if(unreadNum != MissPhoneCallCount) {
	            		   mPreferences.edit().putInt("missedcall", unreadNum).commit();
	            		   MissPhoneCallCount = unreadNum;
	            		   mLeatherView.changePhoneOrSms(MissPhoneCallCount, MissedSmsCount);
	            	   }
	               } else if(MMS_PACKAGE_NAME.equals(componentName.getPackageName()) && MMS_CLS_NAME.equals(componentName.getClassName())) {
	            	   if(unreadNum != MissedSmsCount) {
	            		   mPreferences.edit().putInt("unreadsms", unreadNum).commit();
	            		   MissedSmsCount = unreadNum;
	            		   mLeatherView.changePhoneOrSms(MissPhoneCallCount, MissedSmsCount);
	            	   }	            	   
	               }	               
			}
		}
	};
    
	public LeatherWindowView(Context context) {
		this(context, null);
	}

	public LeatherWindowView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LeatherWindowView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		WindowManager wm = (WindowManager) (context
				.getSystemService(Context.WINDOW_SERVICE));
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		mScreenHeight = dm.heightPixels;
		mScreenWidth = dm.widthPixels;

		if (mScreenWidth < mScreenHeight) {
			mWidth = mScreenWidth - mCirclePadding * 2;// 280dp * 1.5;
		} else {
			mWidth = mScreenHeight - mCirclePadding * 2;// 280dp * 1.5;		
		}
		mHeight = mWidth;		
		mPreferences = context.getSharedPreferences("data", 0);
		mCurrentViewIndex =  mPreferences.getInt("topic", 0);
		MissPhoneCallCount = mPreferences.getInt("missedcall", 0);
        MissedSmsCount = mPreferences.getInt("unreadsms", 0);

		mCallBack = new CallBack() {
			
			@Override
			public void OnClick(int whicView) {
				/*switch (whicView) {
				case LeatherMenuWindowView.LEATHER_MENU_PROFILES:
					mAudioProfilesView.showWithAnimator();
					break;
				case LeatherMenuWindowView.LEATHER_MENU_TOPIC:
					mTopicView.showWithAnimator();
					break;
				default:
					break;
				}*/
			}

			@Override
			public void LeatherChange(int index) {
				if(mCurrentViewIndex == index) {
					return;
				}
				mCurrentViewIndex = index;
				View view = getLeatherViewByIndex(index);
				if(view != null) {
					mLeatherView = (LeatherBase) view.findViewById(R.id.topic_view);
					mLeatherView.setPhoneAndSms(MissPhoneCallCount, MissedSmsCount);
					/*mListViews.clear();
					mListViews.add(view);*/
					mListViews.set(1, view);
					mPagerAdapter.setChangedViewIndex(1);
					mPagerAdapter.notifyDataSetChanged();
				}
			}
		};
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(!mIsAttached) {
			mIsAttached = true;
			/*mMenuView = (LeatherMenuWindowView) findViewById(R.id.menu);
			mMenuView.setCallBack(mCallBack);

			mAudioProfilesView = (AudioProfilesWindowView) findViewById(R.id.footer);
			mAudioProfilesView.setLeatherAudioProfilesController(mLeatherAudioProfilesController);*/

			mTopicView = (LeatherTopicView) findViewById(R.id.leather_topic);
			mTopicView.setCallBack(mCallBack);

			mViewPager = (LeatherViewPager) findViewById(R.id.viewPager);
			//mViewPager.setLeatherMenu(mMenuView);
			mViewPager.setLeatherTopicView(mTopicView);
			mIndicationView = (LinearLayout) findViewById(R.id.dotLayout);
			initListViews();
			
			IntentFilter filter = new IntentFilter();
			filter.addAction(ACTION_UNREAD_CHANGED);
			mContext.registerReceiver(mReceiver, filter);
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(mIsAttached) {
			mIsAttached = false;
			mContext.unregisterReceiver(mReceiver);
			//mMenuView.setCallBack(null);
			mTopicView.setCallBack(null);
			mViewPager.setLeatherMenu(null);
			/*mCallBack = null;
			mMenuView = null;
			mAudioProfilesView = null;*/
			mTopicView = null;
			mViewPager = null;
		}
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

	private void initListViews() {
		if(mViewPager != null) {
			mLeatherMusicView = (LeatherMusicView) View.inflate(mContext, R.layout.leather_music, null);
			mListViews.add(mLeatherMusicView);

			View view = getLeatherViewByIndex(mCurrentViewIndex);
			mLeatherView = (LeatherBase) view.findViewById(R.id.topic_view);
			mLeatherView.setPhoneAndSms(MissPhoneCallCount, MissedSmsCount);
			mListViews.add(view);

			final View cameraView = View.inflate(mContext, R.layout.leather_camera, null);
			ViewGroup viewGroup = (ViewGroup)cameraView.findViewById(R.id.camera_view);
			mCameraView = new CameraView(mContext, viewGroup);
			mListViews.add(cameraView);

			mPagerAdapter = new LeatherPagerAdapter(mListViews);
			mViewPager.setAdapter(mPagerAdapter);
			mViewPager.addOnPageChangeListener(new OnPageChangeListener() {
				
				@Override
				public void onPageSelected(int position) {
					cleanState();
					((ImageView)mIndicationView.getChildAt(position)).setImageResource(R.drawable.leather_viewpager_selected);
					if(position == 0) {
						((LeatherMusicView)mListViews.get(0)).bindService();
						//((LeatherMusicView)mListViews.get(0)).continueAnimator();
					}/* else {
						((LeatherMusicView)mListViews.get(0)).unbindService();
						((LeatherMusicView)mListViews.get(0)).cancelAnimator();

					}*/
					if(position == 2) {
						mCallback.cleanGoToSleep();
						mCameraView.show();
					} else {
						mCallback.userActivity();
						mCameraView.hide();
					}
				}
				
				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					
				}
				
				@Override
				public void onPageScrollStateChanged(int arg0) {
					
				}
			});			
		}
		initIndication();
	}
	
	private void initIndication() {
		if(mIndicationView != null) {
			mIndicationView.removeAllViews();
			int currentItem = mLeatherMusicView.isPlaying() ? 0 : 1;
			for(int i = 0; i < mListViews.size(); i++) {
				ImageView imgView = new ImageView(mContext);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				if(i == currentItem) {
					imgView.setImageResource(R.drawable.leather_viewpager_selected);
				} else {
					imgView.setImageResource(R.drawable.leather_viewpager_normal);
				}
				if(i != (mListViews.size() - 1)) {
					//linwujia edit begin
					//lp.rightMargin = 9;
					lp.setMarginEnd(9);
					//linwujia edit end
				}
				lp.weight = 1.0f;
				mIndicationView.addView(imgView, lp);
			}
			mViewPager.setCurrentItem(currentItem);
		}
	}
	
	private void cleanState() {
		for(int i = 0; i < mIndicationView.getChildCount(); i++) {
			((ImageView)mIndicationView.getChildAt(i)).setImageResource(R.drawable.leather_viewpager_normal);
		}
	}
	
	private void resetState(boolean resetView) {
		int currentItem = mLeatherMusicView.isPlaying() ? 0 : 1;
		mViewPager.setCurrentItem(currentItem);

		for(int i = 0; i < mIndicationView.getChildCount(); i++) {
			if(i == currentItem) {
				((ImageView)mIndicationView.getChildAt(i)).setImageResource(R.drawable.leather_viewpager_selected);
			} else {
				((ImageView)mIndicationView.getChildAt(i)).setImageResource(R.drawable.leather_viewpager_normal);
			}
		}

		if(!resetView) {
			return;
		}
		
		/*if(mMenuView != null && mMenuView.getVisibility() == View.VISIBLE) {
			mMenuView.setVisibility(View.GONE);
		}
		
		if(mAudioProfilesView != null && mAudioProfilesView.getVisibility() == View.VISIBLE) {
			mAudioProfilesView.setVisibility(View.GONE);
		}*/

		if(mTopicView != null && mTopicView.getVisibility() == View.VISIBLE) {
			mTopicView.setVisibility(View.GONE);
		}
	}
	
	public void reDrawClockView() {
		if(mLeatherView != null) {
			
			mLeatherView.reDraw();
		}
	}
	
	public void setColockViewVisibility(int visibility) {
		if(mViewPager != null) {
			resetState(visibility == View.GONE);
		}
		if(mLeatherView != null) {
			if(visibility == View.VISIBLE) {
				mLeatherView.reDraw();
			}
			mLeatherView.setVisibility(visibility);
		}
	}
	
	public void setViewPagerVisibility(int visibility) {
		if(mViewPager != null) {
			mViewPager.setVisibility(visibility);
		}
	}
	
	@Override
	public void setVisibility(int visibility) {
		if(mViewPager != null) {
			resetState(visibility == View.GONE);
		}
		if(visibility == View.VISIBLE) {
			if(mViewPager.getCurrentItem() == 0) {
				mLeatherMusicView.showWithAnimator();
			} else {
				mLeatherView.setVisibility(View.VISIBLE);
			}
		}
		super.setVisibility(visibility);
	}
	
	public void setVisible() {
		super.setVisibility(View.VISIBLE);
	}
	
	private View getLeatherViewByIndex(int index) {
		View view = null;
		int id = 0;
		switch (index) {
		case 0:
			view = LayoutInflater.from(mContext).inflate(R.layout.leather_view_three, null);
			id = R.drawable.leather_menu_bg_one;
			break;
		case 1:
			view = LayoutInflater.from(mContext).inflate(R.layout.leather_view_two, null);
			id = R.drawable.leather_menu_bg_two;
			break;
		case 2:
			view = LayoutInflater.from(mContext).inflate(R.layout.leather_view_six, null);
			id = R.drawable.leather_menu_bg_six;
			break;
		case 3:
			view = LayoutInflater.from(mContext).inflate(R.layout.leather_view_four, null);
			id = R.drawable.leather_menu_bg_four;
			break;
		case 4:
			view = LayoutInflater.from(mContext).inflate(R.layout.leather_view_five, null);
			id = R.drawable.leather_menu_bg_five;
			break;
		case 5:
			view = LayoutInflater.from(mContext).inflate(R.layout.leather_view_one, null);
			id = R.drawable.leather_menu_bg_three;
			break;
		default:
			break;
		}
		/*Drawable[] layers = new Drawable[2];
		layers[0] = mContext.getDrawable(id);
		layers[1] = mContext.getDrawable(R.drawable.leather_menu_bg);
		LayerDrawable layerDrawable = new LayerDrawable(layers);
		mMenuView.setBackground(layerDrawable);*/
		return view;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if(mCallback != null && mViewPager.getCurrentItem() != (mListViews.size() - 1)) {
			mCallback.userActivity();
		}
		float rawX = event.getRawX();
		float rawY = event.getRawY();
		int action = event.getAction();
		if(action == MotionEvent.ACTION_DOWN && (rawX < mCirclePadding || rawX > (mCirclePadding + mWidth) || rawY < mPaddingTop || rawY > (mPaddingTop + mHeight))) {
			return true;
		}
		return false;
	}

	public void setLeatherAudioProfilesController(LeatherAudioProfilesController leatherAudioProfilesController) {
		/*mLeatherAudioProfilesController = leatherAudioProfilesController;
		if(mAudioProfilesView != null) {
			mAudioProfilesView.setLeatherAudioProfilesController(mLeatherAudioProfilesController);
		}*/
	}

	public void release(boolean delay) {
		if(mCameraView != null) {
			if(delay) {
				mCameraView.hide();
			} else {
				mCameraView.postHide();
			}
		}
		/*if(mLeatherMusicView != null) {
			mLeatherMusicView.unbindService();
		}*/
	}

	public void setCallback(Callback callback) {
		mCallback = callback;
	}

	public interface Callback {
		void userActivity();
		void cleanGoToSleep();
	}
	
}
