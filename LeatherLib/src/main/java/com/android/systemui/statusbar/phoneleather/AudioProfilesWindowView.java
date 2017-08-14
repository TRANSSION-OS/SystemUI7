package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class AudioProfilesWindowView extends FrameLayout implements OnClickListener {
	
	private boolean mAttached;
	private Button mGeneralBtn, mSilentBtn, mMeetingBtn, mOutDoorBtn;
	private TextView mGeneralTx, mSilentTx, mMeetingTx, mOutDoorTx;
	private Button mBackBtn;
	private LeatherAudioProfilesController mAudioProfilesController;
	private Animation mShowAnimation, mHideAnimation;
	
	private boolean mGotoHideAudioProfilesWindowView;
	
	public AudioProfilesWindowView(Context context) {
		this(context, null);
	}
	
	public AudioProfilesWindowView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AudioProfilesWindowView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		mGotoHideAudioProfilesWindowView = true;
		
		mShowAnimation = AnimationUtils.loadAnimation(mContext, R.anim.audioprofiles_fade_in);
		mHideAnimation = AnimationUtils.loadAnimation(mContext, R.anim.audioprofiles_fade_out);
		mHideAnimation.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				mGotoHideAudioProfilesWindowView = false;
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				setVisibility(View.GONE);
				mGotoHideAudioProfilesWindowView = true;
			}
		});
	}

	public void setLeatherAudioProfilesController(LeatherAudioProfilesController leatherAudioProfilesController) {
		mAudioProfilesController = leatherAudioProfilesController;
		if(mAudioProfilesController != null) {
			mAudioProfilesController.setAudioProfileCallback(new LeatherAudioProfilesController.AudioProfileCallback() {

				@Override
				public void onChangeState(String profileKey) {
					refreshState(profileKey);
				}
			});
		}
	}
	
	public void refreshState() {
		if(mAudioProfilesController != null) {
			String activeProfileKey = mAudioProfilesController.getActiveProfileKey();
			refreshState(activeProfileKey);
		}
	}
	
	private void refreshState(String activeProfileKey) {
		if(!TextUtils.isEmpty(activeProfileKey)) {
			cleanState();
			if(LeatherAudioProfilesController.TALPA_AUDIOPROFILE_GENERAL.equals(activeProfileKey)) {
				mGeneralBtn.setSelected(true);
			} else if(LeatherAudioProfilesController.TALPA_AUDIOPROFILE_SILENT.equals(activeProfileKey)) {
				mSilentBtn.setSelected(true);	
			} else if(LeatherAudioProfilesController.TALPA_AUDIOPROFILE_OUTDOOR.equals(activeProfileKey)) {
				mOutDoorBtn.setSelected(true);
			} else if(LeatherAudioProfilesController.TALPA_AUDIOPROFILE_MEETING.equals(activeProfileKey)) {
				mMeetingBtn.setSelected(true);
			} else {
				//mGeneralBtn.setSelected(true);
			}
		}
	}
	
	private void cleanState() {
		mGeneralBtn.setSelected(false);
		mSilentBtn.setSelected(false);
		mOutDoorBtn.setSelected(false);
		mMeetingBtn.setSelected(false);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(!mAttached) {
			mAttached = true;
			mGeneralBtn = (Button) findViewById(R.id.audio_general);
			mSilentBtn = (Button) findViewById(R.id.audio_silent);
			mMeetingBtn = (Button) findViewById(R.id.audio_meeting);
			mOutDoorBtn = (Button) findViewById(R.id.audio_outdoor);
			
			mGeneralTx = (TextView) findViewById(R.id.leather_audioprofiles_normal_text);
			mSilentTx = (TextView) findViewById(R.id.leather_audioprofiles_mute_text);
			mMeetingTx = (TextView) findViewById(R.id.leather_audioprofiles_meeting_text);
			mOutDoorTx = (TextView) findViewById(R.id.leather_audioprofiles_outdoor_text);
			
			mBackBtn = (Button) findViewById(R.id.audio_back);
			
			mGeneralBtn.setOnClickListener(this);
			mSilentBtn.setOnClickListener(this);
			mMeetingBtn.setOnClickListener(this);
			mOutDoorBtn.setOnClickListener(this);
			mBackBtn.setOnClickListener(this);
			refreshState();
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(mAttached)
    	{
    		mAttached = false;
    	}
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mGeneralTx.setText(R.string.normal);
		mSilentTx.setText(R.string.mute);
		mMeetingTx.setText(R.string.meeting);
		mOutDoorTx.setText(R.string.outdoor);
	}

	@Override
	public void onClick(View view) {
		String nextKey = null;
		if(view.getId() == R.id.audio_general) {
			nextKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_GENERAL;
		} else if(view.getId() == R.id.audio_silent) {
			nextKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_SILENT;
		} else if(view.getId() == R.id.audio_meeting) {
			nextKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_MEETING;
		} else if(view.getId() == R.id.audio_outdoor) {
			nextKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_OUTDOOR;
		} else if(view.getId() == R.id.audio_back) {
			hideWithAnimator();
			return;
		} else {
			nextKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_GENERAL;
		}
		if(mAudioProfilesController != null) {
			mAudioProfilesController.setAudio(nextKey);
			refreshState(nextKey);
		}
	}
	
	public void showWithAnimator() {
		refreshState();
		setVisibility(View.VISIBLE);	
		startAnimation(mShowAnimation);
	}
	
	public void hideWithAnimator() {
		if(getVisibility() == View.VISIBLE && mGotoHideAudioProfilesWindowView) {		
			startAnimation(mHideAnimation);
		}
	}
}
