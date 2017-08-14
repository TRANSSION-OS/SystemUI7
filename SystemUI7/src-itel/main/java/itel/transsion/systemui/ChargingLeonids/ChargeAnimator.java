package itel.transsion.systemui.ChargingLeonids;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.android.internal.content.NativeLibraryHelper;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryControllerImpl;

import java.util.ArrayList;
import java.util.List;

import itel.transsion.settingslib.utils.TalpaUtils;
import itel.transsion.systemui.ChargingLeonids.leonids.ParticleSystem;
import itel.transsion.systemui.ChargingLeonids.leonids.modifiers.AlphaModifier;
import itel.transsion.systemui.ChargingLeonids.leonids.modifiers.ScaleModifier;

public class ChargeAnimator extends FrameLayout implements ChargeFrameAnimator.AnimationFrameListener, BatteryController.BatteryStateChangeCallback{

	private static final int  IMAGE_CIRCLE_PADDING_BOTTOM = 0;
	public static final boolean DEBUG = false;
	public static final int REGISTER_SUCCESS = 2;
	private static final int TIEM_DELAY = 500;
	public static final int  CHARGE_PULL_IN = 0;
	public static final int  CHARGE_PULL_OUT = 1;
	private int  imageFrameWidth;
	private int  imageFrameHeight;
	private int  imageCircleWidth;
	private int  imageCircleHeight;
	private WindowManager mWindowManager;
	private int mScreenWidth;
	private int mScreenHeight;
	private boolean isStartChargeAnim;
	private ChargeFrameAnimator mFrameAnimator;
	private ChargeCircleAnimator mCircleAnimator;
	private int chargeState;
	private CircleHandler mCircleHandler;
	private View view;
	private BatteryController mBatteryController;
	private boolean isFrameAnimatorCancle;
	private PhoneStatusBar mStatusBar;
	//private LoadFinishHandler mLoadingHandler;
	public ChargeAnimator(Context context, AttributeSet attrs) {
		super(context, attrs);

		mFrameAnimator = new ChargeFrameAnimator(context);
		mFrameAnimator.setFrameListener(this);
		mCircleAnimator = new ChargeCircleAnimator(context);
		chargeState = CHARGE_PULL_OUT;
		mCircleHandler = new CircleHandler();
		view = new View(context);
		addView(mFrameAnimator);
		addView(mCircleAnimator);
		addView(view);
		mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		mScreenWidth = mWindowManager.getDefaultDisplay().getWidth();
		mScreenHeight = mWindowManager.getDefaultDisplay().getHeight();
		imageFrameWidth = getResources().getInteger(R.integer.keyguard_charging_frame_width);
		imageFrameHeight = getResources().getInteger(R.integer.keyguard_charging_frame_height);
		imageCircleWidth = getResources().getInteger(R.integer.keyguard_charging_circle_width);
		imageCircleHeight = getResources().getInteger(R.integer.keyguard_charging_circle_Height);
		//mBatteryController = new BatteryControllerImpl(mContext);
	}

	@Override
	public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
		if(mCircleAnimator != null){
			mCircleAnimator.setText(level);
		}

		if(!pluggedIn && isStartChargeAnim){
			setChargeState(CHARGE_PULL_OUT);
		}if(pluggedIn && !isStartChargeAnim){
			setChargeState(CHARGE_PULL_IN);
		}
	}

	@Override
	public void onPowerSaveChanged(boolean isPowerSave) {
		if(isPowerSave && isStartChargeAnim){
			setChargeState(CHARGE_PULL_OUT);
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		int l = (getWidth() - imageFrameWidth) / 2;
		int r = (getWidth() + imageFrameWidth) / 2;
		int t = getHeight() - imageFrameHeight;
		int b = getHeight();
		mFrameAnimator.layout(l, t, r, b);
		l = (getWidth() - imageCircleWidth) / 2;
		r = (getWidth()  + imageCircleWidth) / 2;
		t = getHeight() - imageCircleHeight - IMAGE_CIRCLE_PADDING_BOTTOM;
		b = getHeight() - IMAGE_CIRCLE_PADDING_BOTTOM;
		mCircleAnimator.layout(l, t, r, b);
		view.layout((getWidth() - imageCircleWidth) / 2, getHeight() - 1, (getWidth()  + imageCircleWidth) / 2 , getHeight());
	}


	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		setBackgroundColor(Color.BLACK);
		setAlpha(0.65f);
	}



	@Override
	public void onFrameAnimationEnd() {
		if(chargeState == CHARGE_PULL_IN && !mCircleAnimator.isStartAnimator()){
			mCircleAnimator.start();
			particleEffect();
		}else {
			setVisibility(View.GONE);
		}
	}

	private void startAnim(){
//		if(mBatteryController != null && mBatteryController.isPowerSave())
//			return;
		if(TalpaUtils.isSuperPowerSaveMode(mContext) || mStatusBar == null){
			return;
		}
		if(mStatusBar.getBarState() != StatusBarState.KEYGUARD){
			return;
		}
		setVisibility(View.VISIBLE);
		if(mFrameAnimator == null){
			return;
		}
		isStartChargeAnim = true;
		mFrameAnimator.start();

	}

	private void stopAnim(){
		isStartChargeAnim = false;
		if(mFrameAnimator != null){
			mFrameAnimator.stop();
		}
		if(mCircleAnimator != null ){
			if(mCircleAnimator.isStartAnimator()){
				mCircleAnimator.stop();
			}else {
				setVisibility(GONE);
			}
		}
		stopParticle();
		if(mStatusBar.getBarState() != StatusBarState.KEYGUARD){
			setVisibility(GONE);
		}

	}

	public void stopParticle(){
		if(mParticleSystem != null){
			mParticleSystem.stopEmitting();
			mParticleSystem.cancel();
			mParticleSystem = null;
		}

		if(mParticleSystem1 != null){
			mParticleSystem1.stopEmitting();
			mParticleSystem1.cancel();
			mParticleSystem1 = null;
		}
	}

	class CircleHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what){
				case CHARGE_PULL_IN:
					startAnim();
					break;
				case CHARGE_PULL_OUT:
					stopAnim();
					break;
				case REGISTER_SUCCESS:
					if(mBatteryController != null){
						mBatteryController.addStateChangedCallback(ChargeAnimator.this);
					}

					break;
			}
		}
	}

	public ChargeCircleAnimator getCircleAnimator(){

		return mCircleAnimator;
	}

	public ChargeFrameAnimator getFrameAnimator(){

		return mFrameAnimator;
	}

	public boolean isStartAnim(){

		return isStartChargeAnim;
	}

	public void setChargeState(int state){
		if(DEBUG) return;
		mCircleAnimator.setChargeState(state);
		chargeState = state;
		Message msg = new Message();
		msg.what = state;
		mCircleHandler.sendMessage(msg);
	}

	public void setBatteryController(BatteryController batteryController){
		mBatteryController = batteryController;
		if(mCircleHandler != null){
			Message msg = new Message();
			msg.what = REGISTER_SUCCESS;
			mCircleHandler.sendMessageDelayed(msg, TIEM_DELAY);
		}
	}
	public void setStatusBar(PhoneStatusBar statusBar){
		mStatusBar = statusBar;
	}

	ParticleSystem mParticleSystem, mParticleSystem1;
	public void particleEffect(){
		stopParticle();
		if(mParticleSystem == null){
			mParticleSystem = new ParticleSystem(this, 2, getResources().getDrawable(R.drawable.charging_particle), 2000);
			mParticleSystem.setSpeedByComponentsRange(-0.015f, 0.015f, -0.045f, -0.055f)
			.setAcceleration(0, 0)
			.setInitialRotationRange(0, 360)
			.addModifier(new AlphaModifier(255, 255, 0, 3000))
			.addModifier(new ScaleModifier(1.0f, 0.5f, 0, 3000))
			.emitWithGravity(view,Gravity.TOP, 1);
		}
		if(mParticleSystem1 == null){
			mParticleSystem1 = new ParticleSystem(this, 10, getResources().getDrawable(R.drawable.charging_particle), 2000);
			mParticleSystem1.setSpeedByComponentsRange(-0.015f, 0.015f, -0.035f, -0.045f)
					.setAcceleration(0, 0)
					.setInitialRotationRange(0, 360)
					.addModifier(new AlphaModifier(153, 153, 0, 3000))
					.addModifier(new ScaleModifier(0.5f, 0.5f, 0, 3000))
					.emitWithGravity(view,Gravity.TOP, 5);

		}

	}
}
