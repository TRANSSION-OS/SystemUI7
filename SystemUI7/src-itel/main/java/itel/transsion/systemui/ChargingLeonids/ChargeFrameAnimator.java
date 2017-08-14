package itel.transsion.systemui.ChargingLeonids;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.systemui.R;

public class ChargeFrameAnimator extends View{

	public static final int INIT_PREVALUE = -1;
	private boolean isStartFrame;
	private AnimationFrameListener frameListener;
	private ValueAnimator anim;
	private int[] mImageList;
	int currentValue;
	private int mPreValue;
	private Bitmap mImageBitmap;

	public ChargeFrameAnimator(Context context){
		this(context, null);
	}

	public ChargeFrameAnimator(Context context, AttributeSet attrs) {
		super(context, attrs);
		mImageList = new int[]{R.drawable.charging_frame_00,
				R.drawable.charging_frame_01,
				R.drawable.charging_frame_02,
				R.drawable.charging_frame_03,
				R.drawable.charging_frame_04,
				R.drawable.charging_frame_05,
				R.drawable.charging_frame_06,
				R.drawable.charging_frame_07,
				R.drawable.charging_frame_08,
				R.drawable.charging_frame_09,
				R.drawable.charging_frame_10,
				R.drawable.charging_frame_11,
				R.drawable.charging_frame_12,
				R.drawable.charging_frame_13,
				R.drawable.charging_frame_14,
				R.drawable.charging_frame_15,
				R.drawable.charging_frame_16,
				R.drawable.charging_frame_17,
				R.drawable.charging_frame_18,
				R.drawable.charging_frame_19,
				R.drawable.charging_frame_20,
				R.drawable.charging_frame_21,
				R.drawable.charging_frame_22,
				R.drawable.charging_frame_23,
				R.drawable.charging_frame_24,
				R.drawable.charging_frame_25,
				R.drawable.charging_frame_26,
				R.drawable.charging_frame_27,
				R.drawable.charging_frame_28};

		mPreValue = INIT_PREVALUE;

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if(isStartFrame){

			if(currentValue > mPreValue){
				clear();
				mImageBitmap = BitmapFactory.decodeResource(getResources(),
						mImageList[currentValue]);
			}
			if(mImageBitmap != null){
				canvas.save();
				canvas.drawBitmap(mImageBitmap, 0, 0, null);
				canvas.restore();
			}
			mPreValue = currentValue;
		}
	}


	public void start(){
		setVisibility(VISIBLE);
		if(!isStartFrame){
			isStartFrame = true;
			if(anim == null){
				anim = ValueAnimator.ofInt(0,mImageList.length - 1);
				anim.setDuration(1600);
				anim.setInterpolator(new LinearInterpolator());
				anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						currentValue =  (Integer) animation.getAnimatedValue();
						ChargeFrameAnimator.this.invalidate();
					}
				});
				anim.addListener(new AnimatorListenerAdapter() {

					@Override
					public void onAnimationEnd(Animator animation) {
						super.onAnimationEnd(animation);
						resetState();
						if(frameListener != null){
							frameListener.onFrameAnimationEnd();
						}
					}

				});
			}
			anim.start();
		}
	}

	public void stop(){
		if(isStartFrame){
			if(anim.isRunning()) {
				anim.cancel();
			}
		}
	}
	private void resetState(){
		isStartFrame = false;
		currentValue = 0;
		mPreValue = INIT_PREVALUE;
		clear();
	}
	private void clear(){
		if(mImageBitmap != null){
			mImageBitmap.recycle();
			mImageBitmap = null;
		}
	}

	public void setFrameListener(AnimationFrameListener listener){

		frameListener = listener;
	}

	public boolean isStartFrameAnimator(){

		return isStartFrame;
	}
	public interface AnimationFrameListener{
		void onFrameAnimationEnd();
	}

}
