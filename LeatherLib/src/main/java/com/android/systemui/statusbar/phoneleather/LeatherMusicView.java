package com.android.systemui.statusbar.phoneleather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.afmobi.boomplayer.aidl.PitaoCallback;
import com.afmobi.boomplayer.aidl.PitaoService;
import com.android.cache.DoubleCache;
import com.android.cache.ImageLoader;
import com.android.systemui.statusbar.phoneleather.model.LeatherPlayerInfo;
import com.google.gson.Gson;

import java.util.Random;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherMusicView extends FrameLayout implements OnClickListener{

	private static final String HOST_NAME = "http://source.boomplaymusic.com/";
	
	private static final int DO_PLAY = 1;
	private static final int DO_NEXT = 2;
	private static final int DO_PREVIOUS = 3;
	
	private ImageButton mPlayBtn, mPreviousBtn, mNextBtn;
	private CircleImageView mRecordView;
	private ImageView mLoadView;
	private TextView mNameTv, mArtistTv;
	private LeatherMusicProgressBar mProgressBar;
	
	private Animator mAnimator;
	private Animator mLoadingAnimator;
	private Animator mShowAnimator;
	private Animator mPreAnimator;
	private Animator mNextAnimator;
	private boolean mIsPreBtnPress;
	
	private AnimatorListenerAdapter mAnimatorListenerAdapter;

	private int[] mRecordImgIds = { R.drawable.leather_music_album_cover_one,
			R.drawable.leather_music_album_cover_two, R.drawable.leather_music_album_cover_three };
	
	private PitaoService mLeatherService;
	private ServiceConnection conn = null;
	private LeatherPlayerInfo mPlayerInfo;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			int what = msg.what;
			handleRefreshView(what == 1);
		};
	};

	private PitaoCallback.Stub mLeatherCallback;

	public LeatherMusicView(Context context) {
		this(context, null);
	}

	public LeatherMusicView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LeatherMusicView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		ImageLoader.getInstance(mContext).setImageCache(new DoubleCache(context));
	}
	
	private void initAnimator() {
		mAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_music_anim);
		mAnimator.setTarget(mRecordView);

		mLoadingAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_music_loading_anim);
		mLoadingAnimator.setTarget(mLoadView);

		mShowAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_music_show_anim);
		mShowAnimator.setTarget(this);
		
		mAnimatorListenerAdapter = new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				mRecordView.setImageBitmap(null);
				mRecordView.setTag(null);
			}
		};
		
		mPreAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_music_pre_anim);
		mPreAnimator.addListener(mAnimatorListenerAdapter);
		mPreAnimator.setTarget(this);
		
		mNextAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_music_next_anim);
		mNextAnimator.addListener(mAnimatorListenerAdapter);
		mNextAnimator.setTarget(this);
	}

	private void bindConnection(final int action) {
		LogUtil.d("invoke bindConnection");
		if (conn != null) {
			return;
		}
		LogUtil.d("bindConnection");
		conn = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				LogUtil.d("onServiceConnected invoke");
				mLeatherService = PitaoService.Stub.asInterface(service);
				try {
					mLeatherCallback = new LeatherCallback();
					mLeatherService.registerCallback(mLeatherCallback);
					initPlayerInfo();
				} catch (RemoteException e1) {
					LogUtil.d("registerCallback RemoteException", e1);
					unbindConnection();
					return;
				} catch (Exception e) {
					LogUtil.d("registerCallback Exception", e);
					unbindConnection();
					return;
				}
				if (action > 0) {
					try {
						onDoAction(action);
					} catch (RemoteException e) {
						LogUtil.d("onDoAction RemoteException", e);
						unbindConnection();
					} catch (Exception e) {
						LogUtil.d("onDoAction Exception", e);
						unbindConnection();
					}
				}

				LogUtil.d("onServiceConnected");
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				// This is called when the connection with the service has been
				// unexpectedly disconnected,
				// that is, its process crashed. Because it is running in our
				// same
				// process, we should never see this happen.
				LogUtil.d("onServiceDisconnected invoke");

				unbindConnection();

				LogUtil.d("onServiceDisconnected");
			}
		};

		bindConnection();
	}

	private void bindConnection() {
		if(mLeatherService == null) {
			Intent intent = new Intent("boom.player.play.pitaoservice");
			intent.putExtra("pitao", true);
			intent.setPackage("com.afmobi.boomplayer");

			mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE);
		}
	}
	
	private void unbindConnection() {
		if (conn == null) {
			return;
		}
		LogUtil.d("unbindConnection");
		if (mLeatherCallback != null) {
			try {
				mLeatherService.unregisterCallback(mLeatherCallback);
			} catch (Exception e) {
				LogUtil.d("unregisterCallback", e);
			}
		}
		try {
			mContext.unbindService(conn);
		} catch (Exception e) {
			LogUtil.d("unbindService", e);
		}
		mLeatherCallback = null;
		mLeatherService = null;
		conn = null;
		mPlayerInfo = null;

		cleanState();
	}
	
	private void initPlayerInfo() throws RemoteException {
		LogUtil.d("initPlayerInfo");

		if(mLeatherCallback == null) {
			bindConnection();
			return;
		}
		String playerInfoRes = mLeatherService.getPlayerInfo();

		if (TextUtils.isEmpty(playerInfoRes)) {
			return;
		}
		LogUtil.d(playerInfoRes);
		Gson gson = new Gson();
		mPlayerInfo = gson.fromJson(playerInfoRes, LeatherPlayerInfo.class);

		refreshView(true);
	}

	/**
	 *
	 * @param refreshIcon 是否刷新封面
     */
	private void refreshView(boolean refreshIcon) {
		if(refreshIcon) {
			mHandler.sendEmptyMessage(1);
		} else {
			mHandler.sendEmptyMessage(2);
		}
	}
	
	private void handleRefreshView(boolean refreshIcon) {
		if(mPlayerInfo != null) {
			mNameTv.setText(mPlayerInfo.getMusicName());
			mArtistTv.setText(mPlayerInfo.getArtist());
			mProgressBar.setMax(mPlayerInfo.getDuration());
			mProgressBar.setProgress(mPlayerInfo.getPosition());
			LogUtil.d(mPlayerInfo.getIconUrl());
			if(refreshIcon) {
				if (!TextUtils.isEmpty(mPlayerInfo.getIconUrl())) {
					ImageLoader.getInstance(mContext).displayImage(HOST_NAME + mPlayerInfo.getIconUrl(), mRecordView, getRandomImageResId());
				} else {
					mRecordView.setImageResource(getRandomImageResId());
					mRecordView.setTag(null);
				}
			}

			if(mPlayerInfo.isPlaying()) {
				mPlayBtn.setVisibility(View.INVISIBLE);
				if(mPlayerInfo.isLoading()) {
					mLoadView.setVisibility(View.VISIBLE);
					mRecordView.setDrawForeground(true);
					if(!mLoadingAnimator.isStarted()) {
						mLoadingAnimator.start();
					}
					if(mAnimator.isStarted()) {
						mAnimator.pause();
					}

				} else {
					mLoadView.setVisibility(View.GONE);
					mRecordView.setDrawForeground(false);
					if(mLoadingAnimator.isRunning()) {
						mLoadingAnimator.cancel();
					}
					if (!mAnimator.isStarted()) {
						mAnimator.start();
					} else {
						mAnimator.resume();
					}
				}
			} else {
				mLoadView.setVisibility(View.GONE);
				mRecordView.setDrawForeground(true);
				mPlayBtn.setVisibility(View.VISIBLE);
				if(mAnimator.isStarted()) {
					mAnimator.pause();
				}
				if(mLoadingAnimator.isRunning()) {
					mLoadingAnimator.end();
				}
			}
		} else {
			cleanState();
		}
	}

	private void cleanState() {
		mNameTv.setText("");
		mArtistTv.setText("");
		mRecordView.setImageBitmap(null);
		mRecordView.setTag(null);
		mRecordView.setRotation(0);
		mProgressBar.setMax(0f);
		mProgressBar.setProgress(0f);

		mAnimator.cancel();
		mLoadingAnimator.cancel();
		mPreAnimator.cancel();
		mNextAnimator.cancel();
	}

	private int getRandomImageResId() {
		Random random = new Random();
		int index = random.nextInt(mRecordImgIds.length);
		return mRecordImgIds[index];
	}

	public void doAction(int action) throws RemoteException {
		if (conn == null) {
			bindConnection(action);
			return;
		}
		if (mPlayerInfo == null) {
			initPlayerInfo();
		}
		onDoAction(action);
	}
	
	private void onDoAction(int action) throws RemoteException {
		try {
			switch (action) {
			case DO_PLAY:
				mLeatherService.play();
				break;
			case DO_NEXT:
				mLeatherService.next();
				break;
			case DO_PREVIOUS:
				mLeatherService.prev();
				break;
			default:
				break;
			}
		} catch (Exception e) {
			LogUtil.d("onDoAction", e);
		}
	}
	
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		
		mPreviousBtn = (ImageButton) findViewById(R.id.leather_music_previous);
		mPlayBtn = (ImageButton) findViewById(R.id.leather_music_play);
		mNextBtn = (ImageButton) findViewById(R.id.leather_music_next);
		
		mRecordView = (CircleImageView) findViewById(R.id.record);
		mLoadView = (ImageView) findViewById(R.id.leather_music_loading);
		
		mNameTv = (TextView) findViewById(R.id.leather_music_name);
		mArtistTv = (TextView) findViewById(R.id.leather_music_artist);

		mProgressBar = (LeatherMusicProgressBar) findViewById(R.id.progressbar);
		
		mPlayBtn.setOnClickListener(this);
		mPreviousBtn.setOnClickListener(this);
		mNextBtn.setOnClickListener(this);
		mRecordView.setOnClickListener(this);
		
		initAnimator();
		
		//bindConnection(-1);

		LogUtil.d("onAttachedToWindow end");
	};
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		//unbindConnection();
	}

	public void bindService() {
		LogUtil.d("mLeatherCallback == null : " + (mLeatherCallback == null) + ", conn == null: " + (conn == null));
		if(mLeatherCallback == null) {
			if(conn == null) {
				bindConnection(-1);
			} else {
				bindConnection();
			}
		}
	}

	public void unbindService() {
		unbindConnection();
	}

	@Override
	public void onClick(View v) {
		try {
			if(v.getId() == R.id.leather_music_previous) {
				if(!mPreAnimator.isStarted()) {
					mIsPreBtnPress = true;
					mPreAnimator.start();
				}
				doAction(DO_PREVIOUS);
			} else if(v.getId() == R.id.leather_music_play || v.getId() == R.id.record) {
				doAction(DO_PLAY);
			} else if(v.getId() == R.id.leather_music_next) {
				if(!mNextAnimator.isStarted()) {
					mIsPreBtnPress = false;
					mNextAnimator.start();
				}
				doAction(DO_NEXT);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void cancelAnimator() {
		if(mPreAnimator != null && mPreAnimator.isRunning()) {
			mPreAnimator.end();
		}

		if(mNextAnimator != null && mNextAnimator.isRunning()) {
			mNextAnimator.end();
		}

		if(mAnimator != null && mAnimator.isRunning()) {
			mAnimator.pause();
		}
	}

	public void continueAnimator() {
		refreshView(true);
	}

	public boolean isPlaying() {
		return (mPlayerInfo != null && mPlayerInfo.isPlaying());
	}

	public void showWithAnimator() {
		if(mShowAnimator.isRunning()) {
			mShowAnimator.end();
		}
		mShowAnimator.start();
	}

	public void setMusicAlpha(float alpha) {
		mPreviousBtn.setAlpha(alpha);
		mPlayBtn.setAlpha(alpha);
		mNextBtn.setAlpha(alpha);
		mNameTv.setAlpha(alpha);
		mArtistTv.setAlpha(alpha);
	}
	
	@Override
	public void setAlpha(float alpha) {
		LogUtil.d("setAlpha:" + alpha);
		super.setAlpha(alpha);
		mRecordView.setAlpha(alpha);
	}
	
	public void setRemoveRotation(float rotation) {
		setPivotX(getWidth() / 2);
		setPivotY(getHeight() + 101 * 2.0f);
		super.setRotation(rotation);
	}
	
	public void setAddRotation(float rotation) {
		setPivotX(getWidth() / 2);
		setPivotY(getHeight() + 101 * 2.0f);
		if(mIsPreBtnPress) {
			super.setRotation(-30 + rotation);
		} else {
			super.setRotation(30 + rotation);
		}
	}

	private class LeatherCallback extends PitaoCallback.Stub {
		@Override
		public void onTrackStopOrPause() throws RemoteException {
			LogUtil.d("LeatherMusicView onTrackStopOrPause");
			if(mPlayerInfo != null) {
				mPlayerInfo.setPlaying(false);
				refreshView(false);
			}
		}

		@Override
		public void onStart(String playerInfo) throws RemoteException {
			LogUtil.d("LeatherMusicView onStart:" + playerInfo);
			Gson gson = new Gson();
			mPlayerInfo = gson.fromJson(playerInfo, LeatherPlayerInfo.class);
			mPlayerInfo.setPlaying(true);
			mPlayerInfo.setLoading(true);
			mPlayerInfo.setPosition(0);
			refreshView(true);
		}

		@Override
		public void onResume() throws RemoteException {
			LogUtil.d("LeatherMusicView onResume:");
			if(mPlayerInfo != null) {
				mPlayerInfo.setPlaying(true);
				refreshView(false);
			}
		}

		@Override
		public void onProgress(int seconds) throws RemoteException {
			LogUtil.d("LeatherMusicView onProgress:" + seconds);
			if (mPlayerInfo != null && mPlayerInfo.getPosition() != seconds) {
				mPlayerInfo.setPosition(seconds);
				mPlayerInfo.setPlaying(true);
				refreshView(false);
			}
		}

		@Override
		public void onPrepare(String playerInfo) throws RemoteException {
			LogUtil.d("LeatherMusicView onPrepare:" + playerInfo);
			Gson gson = new Gson();
			mPlayerInfo = gson.fromJson(playerInfo, LeatherPlayerInfo.class);
			mPlayerInfo.setLoading(false);
			refreshView(false);
		}
	}
}
