package itel.transsion.systemui.ChargingLeonids;

import java.util.List;
import java.util.Random;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
public class ChargeCircleAnimator extends View{

	//public static final int CHARGE_CIRCLE_TRANSLATE = 0;
	public static final int CHARGE_TEXT_SPACE= 4;
	public static final int OUT_LIGHT_NUMBER= 5;
	public static final int INNER_LIGHT_NUMBER= 3;
	private Paint mPaintText;
	private Paint mPaintSign;
	private Bitmap mCircle ;
	private Bitmap mLightBitmap;
	private Bitmap mFlashBitmap;
	private float angel;
	private int[] mImageList;
	private Random mRandom;
	private int pullInScaleState;
	private int mPullOutState;
	private Matrix matrix;
	private float pullInScaleValue;
	private String mText = "0";
	private int supportsRtl ;
	private LinearInterpolator mLinearInterpolator;
	private int number = 0;
	private int lightNumber = 0;
	private boolean isUpdate = true;
	private Typeface typeFace;
	private Rect boundsText;
	private Rect boundsSign;
	private int chargeState;
	private float mCircleRotateValue;
	private int lightRotateValue;
	private int circleRotateState;
	Paint circlePaint;
	private int textAlphaState;
	private int textUpdateAlphaState;
	private float scaleOutValue;
	private int alpha;
	private boolean isLightUpdate;
	private int lightAlphaValue;
	private boolean isTextScale;
	private float textScaleValue;
	private AnimatorSet circleAnimatorSet;
	private int lightChangeCount;
	private int lightChangeState;
	private float angelCount;
	private AnimatorSet lightChangeAnimSet;
	private float textSize;
	private float signSize;
	private AnimationCircleListener mCircleListener;
	AnimatorSet leaneAminatorSet;
	AnimatorSet textAnimatorSet;
	AnimatorSet textAlphaAnimatorSet;
	AnimatorSet scareAnimatorSet;
	public ChargeCircleAnimator(Context context) {
		this(context, null);
	}

	public ChargeCircleAnimator(Context context, AttributeSet attrs) {
		super(context, attrs);
		initData();
		initAlphaCharAnimator();
		initCircleRotateAnumator();
		initFlashCharAnimator();
		initLeaveAnimator();
		initLightChangeAnimator();
		initScaleAnimator();

	}

	private void initState(){
		pullInScaleState = CircleState.DEFAULT_STATE;
		pullInScaleValue = 0;
		circleRotateState = CircleState.DEFAULT_STATE;
		mCircleRotateValue = 0;
		lightRotateValue = 0;
		angel = 0;
		lightAlphaValue = 0;
		textAlphaState = CircleState.DEFAULT_STATE;
		textUpdateAlphaState = CircleState.DEFAULT_STATE;
		mPullOutState = CircleState.DEFAULT_STATE;
		textScaleValue = 0;
		lightChangeCount = -1;
		angelCount = 0;
		lightChangeState = CircleState.DEFAULT_STATE;
		textSize = getResources().getDimension(R.dimen.keyguard_charging_text_size);
		signSize = getResources().getDimension(R.dimen.keyguard_charging_sign_size);
		mPaintText.setTextSize(textSize/*60*/);
		mPaintSign.setTextSize(signSize/*32*/);
		mPaintSign.getTextBounds("%", 0, "%".length(), boundsSign);
	}


	private void initData(){
		mLinearInterpolator = new LinearInterpolator();
		typeFace = Typeface.createFromAsset(mContext.getAssets(),"fonts/Akrobat-Regular.otf");
		mRandom = new Random();
		mPaintText = new Paint();
		mPaintText.setAntiAlias(true);
		mPaintText.setTypeface(typeFace);
		mPaintText.setTextSize(textSize);
		mPaintText.setColor(Color.WHITE);
		mPaintText.setTextAlign(Align.LEFT);
		boundsText = new Rect();
		mPaintSign = new Paint();
		mPaintSign.setAntiAlias(true);
		mPaintSign.setTextSize(signSize);
		mPaintSign.setColor(Color.WHITE);
		mPaintSign.setTextAlign(Align.LEFT);
		boundsSign = new Rect();
		mPaintSign.getTextBounds("%", 0, "%".length(), boundsSign);
		mCircle = BitmapFactory.decodeResource(getResources(), R.drawable.charging);
		
		mImageList = new int[]{R.drawable.charging_light_1,
				R.drawable.charging_light_2,
				R.drawable.charging_light_3,
				R.drawable.charging_light_4,
				R.drawable.charging_light_5,
				R.drawable.charging_light_flash_1,
				R.drawable.charging_light_flash_2,
				R.drawable.charging_light_flash_3};
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		pullIn(canvas);
		pullOut(canvas);

	}

	private void pullOut(Canvas canvas) {
		if (chargeState != ChargeAnimator.CHARGE_PULL_OUT
				|| mPullOutState != CircleState.PULL_OUT_STATE)
			return;

		canvas.save();
		if (matrix == null) {
			matrix = new Matrix();
		}
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setAlpha(alpha);
		matrix.setScale(scaleOutValue - 0.3f, scaleOutValue - 0.3f,
				mCircle.getWidth() / 2, mCircle.getHeight() / 2);
		canvas.drawBitmap(mCircle, matrix, paint);
		if(mLightBitmap == null){
			mLightBitmap = BitmapFactory.decodeResource(getResources(), mImageList[number]);
		}
		if(mLightBitmap == null) return;
		int lX = mCircle.getWidth() / 2 - mLightBitmap.getWidth() / 2;
		int ly = mCircle.getHeight() / 2 - mLightBitmap.getHeight() / 2;
		matrix.setScale(scaleOutValue, scaleOutValue, mLightBitmap.getWidth() / 2, mLightBitmap.getHeight() / 2);
		matrix.postTranslate(lX, ly);
		canvas.drawBitmap(mLightBitmap, matrix, paint);
		canvas.restore();
		canvas.save();
		float text = textSize * textScaleValue;
		float sign = signSize * textScaleValue;
		mText = mText.trim();
		mPaintText.setTextSize(text);
		mPaintSign.setTextSize(sign);
		mPaintText.setAlpha(alpha);
		mPaintSign.setAlpha(alpha);
		mPaintText.getTextBounds(mText, 0, mText.length(), boundsText);
		mPaintSign.getTextBounds("%", 0, "%".length(), boundsSign);
//		float x1 = getWidth() / 2 - (boundsText.width() + CHARGE_TEXT_SPACE + boundsSign.width()) / 2;
//		float x2 = getWidth() / 2 + (boundsText.width() + CHARGE_TEXT_SPACE - boundsSign.width()) / 2;
//		float y = getHeight() / 2 + boundsText.height() / 2;
		float x1 = 0, x2 = 0, y = 0;
		if(supportsRtl == View.LAYOUT_DIRECTION_LTR){
			x1 = getWidth()/2 - (boundsText.width() + CHARGE_TEXT_SPACE + boundsSign.width()) / 2;
			x2 = getWidth()/2 + (boundsText.width() + CHARGE_TEXT_SPACE - boundsSign.width()) / 2;
		}else {
			x2 = getWidth()/2 - (boundsText.width() /*+ CHARGE_TEXT_SPACE*/ + boundsSign.width()) / 2;
			x1 = getWidth()/2 - (boundsText.width() /*- CHARGE_TEXT_SPACE*/ - boundsSign.width()) / 2;
		}
		y = getHeight()/2 + boundsText.height()/2;
		canvas.drawText(mText, x1, y, mPaintText);
		canvas.drawText("%", x2, y, mPaintSign);
		canvas.restore();

	}

	private void leaveAnimator(){

		mPullOutState = CircleState.PULL_OUT_STATE;
		//pullOutTextScaleState = CircleState.PULL_OUT_TEXT_SCALE_STATE;
		leaneAminatorSet.start();

	}


	private void alphaCharAnimator(){

		textAlphaState = CircleState.TEXT_ALPHA_STATE;
		textUpdateAlphaState = CircleState.TEXT_UPDATE_ALPHE_STATE;
		if(textAnimatorSet != null){
			textAnimatorSet.start();
		}
	}


	private void startFlashChar(){
		textUpdateAlphaState = CircleState.TEXT_UPDATE_ALPHE_STATE;
		if(textAlphaAnimatorSet != null){
			textAlphaAnimatorSet.start();
		}
	}

	public void stopFlashChar(){
		if(textAlphaAnimatorSet != null && textAlphaAnimatorSet.isStarted()){
			textAlphaAnimatorSet.cancel();
		}

	}

	private void circleRotateAnimator(){

		circleRotateState = CircleState.CIRCLE_ROTATE_STATE;
		if(circleAnimatorSet != null){
			circleAnimatorSet.start();
		}
	}

	private void lightChangeAnimator(){
		if(lightChangeAnimSet != null){
			lightChangeAnimSet.start();
		}

	}

	private boolean isStartAnimator;
	public void start(){
		isStartAnimator = true;
		if(chargeState == ChargeAnimator.CHARGE_PULL_IN){
			setVisibility(VISIBLE);
			initState();
			scaleStart();
			alphaCharAnimator();
		}else {
			initState();
			ChargeCircleAnimator.this.invalidate();
			((ViewGroup) ChargeCircleAnimator.this.getParent()).setVisibility(GONE);
		}

	}

	public void startScreenON(){
		isStartAnimator = true;
		initState();
		if(chargeState == ChargeAnimator.CHARGE_PULL_IN){
			setVisibility(VISIBLE);
			circleRotateAnimator();
			alphaCharAnimator();
		}else{
			recycleBitamp();
			ChargeCircleAnimator.this.invalidate();
		}

	}

	public void stop(){
		isStartAnimator = false;
		if(lightChangeAnimSet != null && lightChangeAnimSet.isStarted()){
			lightChangeAnimSet.cancel();
		}
		if(textAnimatorSet != null && textAnimatorSet.isStarted()){
			textAnimatorSet.cancel();
		}
		if(circleAnimatorSet != null && circleAnimatorSet.isStarted()){
				circleAnimatorSet.cancel();
		}

	}

	public boolean isStartAnimator(){

		return isStartAnimator;
	}
	int mLever = 0;
	public void setText(int lever){
		mLever = lever;
		supportsRtl = mContext.getResources().getConfiguration().getLayoutDirection();
		mText = mContext.getResources().getString(R.string.battery_level_charging, lever);
	}
	public void setChargeState(int state){
		chargeState = state;
	}


	private void scaleStart(){
		pullInScaleState = CircleState.PULL_IN_STATE;
		if(scareAnimatorSet != null){
			scareAnimatorSet.start();
		}
	}

	private void pullIn(Canvas canvas){

		if(chargeState != ChargeAnimator.CHARGE_PULL_IN
				&& !isStartAnimator) return;
		if(matrix == null){
			matrix = new Matrix();
		}
		if(pullInScaleState == CircleState.PULL_IN_STATE){
			canvas.save();
			matrix.setScale(pullInScaleValue, pullInScaleValue, mCircle.getWidth() / 2, mCircle.getHeight() / 2);
			canvas.drawBitmap(mCircle, matrix, null);
			canvas.restore();
		}

		if(circleRotateState == CircleState.CIRCLE_ROTATE_STATE){
			canvas.save();
			if(circlePaint == null){
				circlePaint = new Paint();
				circlePaint.setAntiAlias(true);
			}
			//circlePaint.reset();
			circlePaint.setAlpha(lightAlphaValue);
			matrix.setScale(0.75f, 0.75f, mCircle.getWidth() / 2, mCircle.getHeight() / 2);
			matrix.postRotate(-mCircleRotateValue + 0.0f, mCircle.getWidth() / 2,  mCircle.getHeight() / 2);
			canvas.drawBitmap(mCircle, matrix, null);

			if(isUpdate){
				isUpdate = false;
				number = mRandom.nextInt(OUT_LIGHT_NUMBER);
				if(mLightBitmap != null){
					mLightBitmap.recycle();
					mLightBitmap = null;
				}
				mLightBitmap = BitmapFactory.decodeResource(getResources(), mImageList[number]);
			}
			if(mLightBitmap != null){
				int lX = mCircle.getWidth() / 2 - mLightBitmap.getWidth() / 2;
				int ly = mCircle.getHeight() / 2 - mLightBitmap.getHeight() / 2;
				matrix.reset();
				//matrix.setScale(1.0f, 1.0f, mList.get(number).getWidth() / 2, mList.get(number).getHeight() / 2);
				matrix.postTranslate(lX, ly);
				angel = lightRotateValue + angelCount;
				matrix.postRotate(-angel, mCircle.getWidth() / 2,  mCircle.getHeight() / 2);
				canvas.drawBitmap(mLightBitmap, matrix, circlePaint);
			}
			canvas.restore();

			if(isLightUpdate){
				isLightUpdate = false;
				canvas.save();
				matrix.reset();
				lightNumber = mRandom.nextInt(INNER_LIGHT_NUMBER) + OUT_LIGHT_NUMBER;
				if(mFlashBitmap != null){
					mFlashBitmap.recycle();
					mFlashBitmap = null;
				}
				mFlashBitmap = BitmapFactory.decodeResource(getResources(), mImageList[lightNumber]);
				if(mFlashBitmap != null){
					matrix.setTranslate(getWidth()/2 - mFlashBitmap.getWidth()/2, getHeight()/2 - mFlashBitmap.getHeight()/2);
					canvas.drawBitmap(mFlashBitmap,matrix, null);
				}
				
				canvas.restore();
			}
		}

		if(textAlphaState == CircleState.TEXT_ALPHA_STATE){
			canvas.save();
			mText = mText.trim();
			mPaintText.getTextBounds(mText, 0, mText.length(), boundsText);
			mPaintSign.getTextBounds("%", 0, "%".length(), boundsSign);
			float x1 = 0, x2 = 0, y = 0;
			if(supportsRtl == View.LAYOUT_DIRECTION_LTR){
				x1 = getWidth()/2 - (boundsText.width() + CHARGE_TEXT_SPACE + boundsSign.width()) / 2;
				x2 = getWidth()/2 + (boundsText.width() + CHARGE_TEXT_SPACE - boundsSign.width()) / 2;
			}else {
				x2 = getWidth()/2 - (boundsText.width() /*+ CHARGE_TEXT_SPACE */+ boundsSign.width()) / 2;
				x1 = getWidth()/2 - (boundsText.width() /*- CHARGE_TEXT_SPACE */- boundsSign.width()) / 2;
			}
			y = getHeight()/2 + boundsText.height()/2;
			if(textUpdateAlphaState == CircleState.TEXT_UPDATE_ALPHE_STATE){
				mPaintText.setAlpha(alpha);
				mPaintSign.setAlpha(alpha);
			}else {
				mPaintText.setAlpha(255);
				mPaintSign.setAlpha(255);
			}
			canvas.drawText(mText, x1, y, mPaintText );
			canvas.drawText("%", x2, y, mPaintSign);
			canvas.restore();
		}

	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		supportsRtl = mContext.getResources().getConfiguration().getLayoutDirection();
		mText = getResources().getString(R.string.battery_level_charging, mLever);

	}

	private void initCircleRotateAnumator(){
		ValueAnimator circleRotateAnim, lightUpdateAnim, lightAlphaAnim, lightRotateAnim;

		if(circleAnimatorSet == null){
			circleAnimatorSet = new AnimatorSet();
			circleRotateAnim = ValueAnimator.ofFloat(0.0f, 360.0f);
			circleRotateAnim.setDuration(1500);
			circleRotateAnim.setRepeatCount(ValueAnimator.INFINITE);
			circleRotateAnim.setRepeatMode(ValueAnimator.RESTART);
			circleRotateAnim.setInterpolator(mLinearInterpolator);
			circleRotateAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					mCircleRotateValue =  (Float) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});
			circleRotateAnim.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					circleRotateState = CircleState.DEFAULT_STATE;
					if(chargeState == ChargeAnimator.CHARGE_PULL_OUT){
						leaveAnimator();
					}else {
						initState();
						recycleBitamp();
						ChargeCircleAnimator.this.invalidate();
					}
					
				}

			});

			lightRotateAnim = ValueAnimator.ofInt(0, 36);
			lightRotateAnim.setDuration(100);
			lightRotateAnim.setRepeatCount(ValueAnimator.INFINITE);
			lightRotateAnim.setRepeatMode(ValueAnimator.RESTART);
			lightRotateAnim.setInterpolator(mLinearInterpolator);
			lightRotateAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					lightRotateValue =  (Integer) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});
			lightRotateAnim.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationRepeat(Animator animation) {
					super.onAnimationRepeat(animation);
					isUpdate = true;
					angelCount += 36.0;
					if(angelCount >= 360.0){
						angelCount = 0;
					}
					if(lightChangeState == CircleState.LIGHT_CHANGE_STATE){
						lightChangeCount++;
						if(lightChangeCount == 12){
							lightChangeState = CircleState.DEFAULT_STATE;
							lightChangeCount = 0;
							lightChangeAnimator();
						}
					}
				}

			});

			lightUpdateAnim = ValueAnimator.ofFloat(1.0f,0.73f);
			lightUpdateAnim.setDuration(1000);
			lightUpdateAnim.setInterpolator(mLinearInterpolator);
			lightUpdateAnim.setRepeatCount(ValueAnimator.INFINITE);
			lightUpdateAnim.setRepeatMode(ValueAnimator.RESTART);
			lightUpdateAnim.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationRepeat(Animator animation) {
					super.onAnimationRepeat(animation);
					isLightUpdate = true;
					ChargeCircleAnimator.this.invalidate();
				}

			});

			lightAlphaAnim = ValueAnimator.ofInt(0,255);
			lightAlphaAnim.setDuration(100);
			lightAlphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {

					lightAlphaValue = (Integer) animation.getAnimatedValue();
				}
			});
			lightAlphaAnim.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					lightAlphaValue = 255;
					lightChangeCount = 0;
					lightChangeState = CircleState.LIGHT_CHANGE_STATE;
				}

			});

			circleAnimatorSet.play(circleRotateAnim).with(lightUpdateAnim).with(lightAlphaAnim).with(lightRotateAnim);
		}
	}

	private void initLightChangeAnimator(){
		ValueAnimator firstLightChangeAnim, secondLightChangeAnim, thirdLightChangeAnim;
		if(lightChangeAnimSet == null) {

			lightChangeAnimSet = new AnimatorSet();

			firstLightChangeAnim = ValueAnimator.ofInt(255, 0);
			firstLightChangeAnim.setDuration(300);
			firstLightChangeAnim.setInterpolator(mLinearInterpolator);
			firstLightChangeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator valueAnimator) {
					lightAlphaValue = (Integer) valueAnimator.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();

				}
			});

			secondLightChangeAnim = ValueAnimator.ofInt(255, 0);
			secondLightChangeAnim.setDuration(160);
			secondLightChangeAnim.setInterpolator(mLinearInterpolator);
			secondLightChangeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator valueAnimator) {
					lightAlphaValue = 0;
					ChargeCircleAnimator.this.invalidate();
				}
			});

			thirdLightChangeAnim = ValueAnimator.ofInt(0, 255);
			thirdLightChangeAnim.setDuration(300);
			thirdLightChangeAnim.setInterpolator(mLinearInterpolator);
			thirdLightChangeAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator valueAnimator) {
					lightAlphaValue = (Integer) valueAnimator.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});

			thirdLightChangeAnim.addListener(new AnimatorListenerAdapter() {


				@Override
				public void onAnimationEnd(Animator animation) {
					lightChangeState = CircleState.LIGHT_CHANGE_STATE;
				}

			});

			lightChangeAnimSet.play(secondLightChangeAnim).before(thirdLightChangeAnim).after(firstLightChangeAnim);
		}
	}

	private void initFlashCharAnimator(){
		ValueAnimator textAlpha1, textAlpha2;
		if(textAlphaAnimatorSet == null) {
			textAlphaAnimatorSet = new AnimatorSet();
			textAlpha1 = ValueAnimator.ofInt(255,76);
			textAlpha1.setDuration(60);
			textAlpha1.setInterpolator(mLinearInterpolator);
			textAlpha1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {

					alpha = (Integer) animation.getAnimatedValue();
					//ChargeCircleAnimator.this.invalidate();
				}
			});

			textAlpha2 = ValueAnimator.ofInt(76,255);
			textAlpha2.setDuration(180);
			textAlpha2.setInterpolator(mLinearInterpolator);
			textAlpha2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					alpha =   (Integer) animation.getAnimatedValue();
					//ChargeCircleAnimator.this.invalidate();
				}
			});
			textAlpha2.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					textUpdateAlphaState = CircleState.DEFAULT_STATE;
					ChargeCircleAnimator.this.invalidate();
				}
			});

			textAlphaAnimatorSet.play(textAlpha1).before(textAlpha2);
		}
	}

	private void initAlphaCharAnimator(){
		ValueAnimator textAlphaAnim, textAlpha;
		if(textAnimatorSet == null){
			textAnimatorSet = new AnimatorSet();
			textAlpha = ValueAnimator.ofInt(0,255);
			textAlpha.setDuration(200);
			textAlpha.setInterpolator(mLinearInterpolator);
			textAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					alpha =   (Integer) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});
			textAlpha.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					textUpdateAlphaState = CircleState.DEFAULT_STATE;
				}
			});

			textAlphaAnim = ValueAnimator.ofInt(255,255);
			textAlphaAnim.setDuration(1240);
			textAlphaAnim.setRepeatCount(ValueAnimator.INFINITE);
			textAlphaAnim.setRepeatMode(ValueAnimator.RESTART);
			textAlphaAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {

					ChargeCircleAnimator.this.invalidate();
				}
			});
			textAlphaAnim.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationRepeat(Animator animation) {
					super.onAnimationRepeat(animation);
					startFlashChar();
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					textAlphaState = CircleState.DEFAULT_STATE;
					stopFlashChar();
				}
			});
			textAnimatorSet.play(textAlpha).before(textAlphaAnim);
		}
	}

	private void initLeaveAnimator(){
		if(leaneAminatorSet == null){
			leaneAminatorSet = new AnimatorSet();
			ValueAnimator ringScale, ringAlpha, textScale;
			ringScale = ValueAnimator.ofFloat(1.0f,2.0f);
			ringScale.setDuration(200);
			ringScale.setInterpolator(mLinearInterpolator);
			ringScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					scaleOutValue = (Float) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();

				}
			});
			ringScale.addListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					//mPullOutState = CircleState.DEFAULT_STATE;
					initState();
					recycleBitamp();
					ChargeCircleAnimator.this.invalidate();
					if(mCircleListener != null && chargeState == ChargeAnimator.CHARGE_PULL_OUT){
						mCircleListener.onCircleAnimationEnd();
					}
					((ViewGroup) ChargeCircleAnimator.this.getParent()).setVisibility(GONE);
				}
			});

			textScale = ValueAnimator.ofFloat(1.0f,1.5f);
			textScale.setDuration(200);
			textScale.setInterpolator(mLinearInterpolator);
			textScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {

					textScaleValue = (Float) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();

				}
			});

			ringAlpha = ValueAnimator.ofInt(100,0);
			ringAlpha.setDuration(200);
			ringAlpha.setInterpolator(mLinearInterpolator);
			ringAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					alpha =   (Integer) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});


			leaneAminatorSet.play(ringScale).with(ringAlpha).with(textScale);
		}
	}

	private void initScaleAnimator(){
		ValueAnimator animScacle1, animScacle2;
		if(scareAnimatorSet == null){
			scareAnimatorSet = new AnimatorSet();
			animScacle1 = ValueAnimator.ofFloat(1.0f,0.73f);
			animScacle1.setDuration(200);
			animScacle1.setInterpolator(mLinearInterpolator);
			animScacle1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					pullInScaleValue =  (Float) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});

			animScacle2 = ValueAnimator.ofFloat(0.73f, 0.75f);
			animScacle2.setDuration(100);
			animScacle2.setInterpolator(mLinearInterpolator);
			animScacle2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
				@Override
				public void onAnimationUpdate(ValueAnimator animation) {
					pullInScaleValue =  (Float) animation.getAnimatedValue();
					ChargeCircleAnimator.this.invalidate();
				}
			});
			animScacle2.addListener(new AnimatorListenerAdapter() {

				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					pullInScaleState = CircleState.DEFAULT_STATE;

					if(chargeState == ChargeAnimator.CHARGE_PULL_IN){
						circleRotateAnimator();

					}else {
						stop();
						initState();
						recycleBitamp();
						ChargeCircleAnimator.this.invalidate();
						if(mCircleListener != null){
							mCircleListener.onCircleAnimationEnd();
						}
						((ViewGroup) ChargeCircleAnimator.this.getParent()).setVisibility(GONE);
					}
				}
			});
			scareAnimatorSet.play(animScacle1).before(animScacle2);
		}
	}

	public void recycleBitamp(){
		if(mLightBitmap != null){
			mLightBitmap.recycle();
			mLightBitmap = null;
		}
		if(mFlashBitmap != null){
			mFlashBitmap.recycle();
			mFlashBitmap = null;
		}
	}
	public void setCircleListener(AnimationCircleListener listener){
		mCircleListener = listener;
	}
	public interface AnimationCircleListener{
		void onCircleAnimationEnd();
	}

}
