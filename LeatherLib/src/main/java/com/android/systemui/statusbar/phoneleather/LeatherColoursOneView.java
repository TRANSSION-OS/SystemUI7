package com.android.systemui.statusbar.phoneleather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherColoursOneView extends View implements LeatherBase {
	
	private BitmapFactory.Options mOptions;
	private Bitmap mBmpOne, mBmpTwo, mBmpThree, mBmpFour;
	private Bitmap mBmpBg, mBmpFg;
	private int[] mNumIds = { R.drawable.leather_colours_num0, R.drawable.leather_colours_num1, R.drawable.leather_colours_num2,
			R.drawable.leather_colours_num3, R.drawable.leather_colours_num4, R.drawable.leather_colours_num5, R.drawable.leather_colours_num6,
			R.drawable.leather_colours_num7, R.drawable.leather_colours_num8, R.drawable.leather_colours_num9 };
	private List<Bitmap> mNumBitmaps;
	private int mWidth, mHeight;
	private int mCenterX, mCenterY;
	
	private float mFirstLineX, mFirstLineY;
	private float mSecondLineX, mSecondLineY;
	private float mFirstLineTextSize, mSecondLineTextSize;
	
	private Paint mPaint;
	
	private Calendar mCalendar;
	private int mHour, mMinute;
	
	private String mLocalHour, mLocalMinute;
	
	private boolean mAttached;
	
	private int mOverlayAlpha;
	private float mHoleScale;
	private float mBgOneTranslate;
	private float mBgTwoTranslate;
	private float mBgThreeTranslate;
	private float mBgFourTranslate;
	private float mFirstLineTextScale;
	private int mFirstLineTextAlpha;
	private boolean mDrawFirstLine;
	private float mSecondLineTextScale;
	private int mSecondLineTextAlpha;
	private boolean mDrawSecondLine;
	
	private float mBgOneRunTranslate;
	private float mBgThreeRunTranslate;
	private float mBgFourRunTranslate;
	
	private Matrix mMatrix;
	
	//是否是支持翻译的语言，在config.xml中配置
	private boolean mSupportLang;
	
	private boolean mAnimatorEnd;
	
	
	private Animator mStartAnimator;
	private Animator mRunAnimator;
	
	private PhoneAndSmsDraw mPhoneAndSmsDraw;
	
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			initTime();
			invalidate();
		}
	};

	public LeatherColoursOneView(Context context) {
		this(context, null);
	}

	public LeatherColoursOneView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public LeatherColoursOneView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mPhoneAndSmsDraw = new PhoneAndSmsDraw(context, attrs);
		
		List<String> supportLangs = Arrays.asList(mContext.getResources().getString(R.string.leather_color_view_support_language).split(","));
		if(supportLangs != null && supportLangs.size() > 0) {
			mSupportLang = supportLangs.contains(Locale.getDefault().getLanguage());
		}
		
		mMatrix = new Matrix();
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
		
		initData();
		initTime();
		initBitmap();
		initPaint();
		initAnimator();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		
		if(widthMode == MeasureSpec.EXACTLY) {
			mWidth = MeasureSpec.getSize(widthMeasureSpec);
		} else {
			mWidth = mBmpBg.getWidth();
		}
		
		if(heightMode == MeasureSpec.EXACTLY) {
			mHeight = MeasureSpec.getSize(heightMeasureSpec);
		} else {
			mHeight = mBmpBg.getHeight();
		}
		mCenterX = mWidth / 2;
		mCenterY = mHeight / 2;
		
		mPhoneAndSmsDraw.measure(widthMeasureSpec, heightMeasureSpec);

		setMeasuredDimension(mWidth, mHeight);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		mPaint.setAlpha(255);
		canvas.drawBitmap(mBmpBg, 0, 0, null);

		canvas.translate(-mBgTwoTranslate - mBgOneRunTranslate, -mBgTwoTranslate + mBgOneRunTranslate);
		canvas.drawBitmap(mBmpTwo, 0, 0, null);
		canvas.translate(mBgTwoTranslate + mBgOneRunTranslate, mBgTwoTranslate - mBgOneRunTranslate);
		
		canvas.translate(mBgOneTranslate + mBgOneRunTranslate, mBgOneTranslate - mBgOneRunTranslate);
		canvas.drawBitmap(mBmpOne, 0, 0, null);
		canvas.translate(-(mBgOneTranslate + mBgOneRunTranslate), -(mBgOneTranslate - mBgOneRunTranslate));
		
		canvas.translate(mBgThreeTranslate + mBgThreeRunTranslate, mBgThreeTranslate - mBgThreeRunTranslate);
		canvas.drawBitmap(mBmpThree, 0, 0, null);
		canvas.translate(-(mBgThreeTranslate + mBgThreeRunTranslate), -(mBgThreeTranslate - mBgThreeRunTranslate));
		
		canvas.translate(-mBgFourTranslate - mBgFourRunTranslate, -mBgFourTranslate + mBgFourRunTranslate);
		canvas.drawBitmap(mBmpFour, 0, 0, null);
		canvas.translate(mBgFourTranslate + mBgFourRunTranslate, mBgFourTranslate - mBgFourRunTranslate);
		
		canvas.drawBitmap(mBmpFg, 0, 0, null);
		
		drawTime(canvas);
		
		mPhoneAndSmsDraw.draw(canvas);
		
		if(!mAnimatorEnd) {
			mPaint.setColor(Color.BLACK);
			mPaint.setStyle(Style.STROKE);
			mPaint.setStrokeWidth(mWidth / 4.0f * mHoleScale);
			canvas.drawCircle(mCenterX, mCenterY, mWidth / 2.0f - mWidth / 4.0f * mHoleScale / 2.0f + 0.5f, mPaint);
			
			
			mPaint.setAlpha(mOverlayAlpha);
			mPaint.setStyle(Style.FILL);
			canvas.drawRect(0, 0, mWidth, mHeight, mPaint);
		}		
	}
	
	private void drawTime(Canvas canvas) {
		LogUtil.d("drawTime---mSupportLang:" + mSupportLang);
		if(mSupportLang) {
			if(mDrawFirstLine) {
				mPaint.setTextSize(mFirstLineTextSize * mFirstLineTextScale);
				LogUtil.d("drawTime---mFirstLineTextSize:" + mFirstLineTextSize + ", mFirstLineTextScale:" + mFirstLineTextScale);
				mFirstLineX = mCenterX - mPaint.measureText(mLocalHour) / 2;
				mFirstLineY = mCenterY - mPaint.descent() / 2;
				mPaint.setColor(Color.WHITE);
				mPaint.setAlpha(mFirstLineTextAlpha);
				LogUtil.d("drawTime---mSupportLang:" + mSupportLang + ", mDrawFirstLine:" + mDrawFirstLine + ", TextSize:" + (mFirstLineTextSize * mFirstLineTextScale)
						+ ", mFirstLineX:" + mFirstLineX + ", mFirstLineY:" + mFirstLineY + ", mFirstLineTextAlpha:" + mFirstLineTextAlpha + ", mLocalHour:" + mLocalHour);
				canvas.drawText(mLocalHour, mFirstLineX, mFirstLineY, mPaint);
			}
			
			if(mDrawSecondLine) {
				mPaint.setTextSize(mSecondLineTextSize * mSecondLineTextScale);
				mSecondLineX = mCenterX - mPaint.measureText(mLocalMinute) / 2;
				mSecondLineY = mCenterY - mPaint.ascent()- mPaint.descent() / 2;
				mPaint.setAlpha(mSecondLineTextAlpha);
				canvas.drawText(mLocalMinute, mSecondLineX, mSecondLineY, mPaint);
			}
		} else {
			Bitmap bmp = null;
			if(mDrawFirstLine) {
				bmp = getFirstTextBitmap(mHour);
				mMatrix.reset();
				mMatrix.setScale(mFirstLineTextScale, mFirstLineTextScale, bmp.getWidth(), bmp.getHeight());
				mPaint.setAlpha(mFirstLineTextAlpha);
				canvas.translate(mCenterX - bmp.getWidth(), mCenterY - bmp.getHeight() - 6);
				canvas.drawBitmap(bmp, mMatrix, mPaint);
				canvas.translate(-(mCenterX - bmp.getWidth()), -(mCenterY - bmp.getHeight() - 6));
				
				bmp = getSecondTextBitmap(mHour);
				mMatrix.reset();
				mMatrix.setScale(mFirstLineTextScale, mFirstLineTextScale, 0, bmp.getHeight());
				canvas.translate(mCenterX, mCenterY - bmp.getHeight() - 6);
				canvas.drawBitmap(bmp, mMatrix, mPaint);
				canvas.translate(-mCenterX, -(mCenterY - bmp.getHeight() - 6));
				mPaint.setAlpha(255);
			}
			
			if(mDrawSecondLine) {
				bmp = getFirstTextBitmap(mMinute);
				mMatrix.reset();
				mMatrix.setScale(mSecondLineTextScale, mSecondLineTextScale, bmp.getWidth(), 0);
				mPaint.setAlpha(mSecondLineTextAlpha);
				canvas.translate(mCenterX - bmp.getWidth(), mCenterY + 6);
				canvas.drawBitmap(bmp, mMatrix, mPaint);
				canvas.translate(-(mCenterX - bmp.getWidth()), -(mCenterY + 6));
				
				bmp = getSecondTextBitmap(mMinute);
				mMatrix.reset();
				mMatrix.setScale(mSecondLineTextScale, mSecondLineTextScale, 0, 0);
				canvas.translate(mCenterX, mCenterY + 6);
				canvas.drawBitmap(bmp, mMatrix, mPaint);
				canvas.translate(-mCenterX, -(mCenterY + 6));
			}
		}
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(!mAttached) {
			mAttached = true;
			anim();
			mPhoneAndSmsDraw.onAttachedToWindow();
			
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);//监听时区变化
			filter.addAction(Intent.ACTION_DATE_CHANGED);//监听日期变化
			filter.addAction(Intent.ACTION_LOCALE_CHANGED);
			filter.addAction(Intent.ACTION_TIME_CHANGED);//监听时间变化
			mContext.registerReceiver(mIntentReceiver, filter);
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(mAttached) {
			mAttached = false;
			mOptions = null;
			mPhoneAndSmsDraw.onDetachedFromWindow();
			if(mBmpBg != null && !mBmpBg.isRecycled()) {
				mBmpBg.recycle();
				mBmpBg = null;
			}
			if(mBmpTwo != null && !mBmpTwo.isRecycled()) {
				mBmpTwo.recycle();
				mBmpTwo = null;
			}
			if(mBmpOne != null && !mBmpOne.isRecycled()) {
				mBmpOne.recycle();
				mBmpOne = null;
			}
			if(mBmpThree != null && !mBmpThree.isRecycled()) {
				mBmpThree.recycle();
				mBmpThree = null;
			}
			if(mBmpFour != null && !mBmpFour.isRecycled()) {
				mBmpFour.recycle();
				mBmpFour = null;
			}
			if(mBmpFg != null && !mBmpFg.isRecycled()) {
				mBmpFg.recycle();
				mBmpFg = null;
			}
			for(Bitmap bmp : mNumBitmaps) {
				bmp.recycle();
				bmp = null;
			}
			
			mNumBitmaps.clear();
			mNumBitmaps = null;
			
			if(mStartAnimator != null && mStartAnimator.isRunning()) {
				mStartAnimator.cancel();
				mStartAnimator = null;
			}
			
			if(mRunAnimator != null && mRunAnimator.isRunning()) {
				mRunAnimator.cancel();
				mRunAnimator = null;
			}
			
			mContext.unregisterReceiver(mIntentReceiver);
		}
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		List<String> supportLangs = Arrays.asList(mContext.getResources().getString(R.string.leather_color_view_support_language).split(","));
		if(supportLangs != null && supportLangs.size() > 0) {
			mSupportLang = supportLangs.contains(Locale.getDefault().getLanguage());
		}
	}
	
	@Override
	public void reDraw() {
		if(mStartAnimator.isRunning()) {
			mStartAnimator.cancel();
		}
		if(mRunAnimator.isRunning()) {
			mRunAnimator.cancel();
		}
		mPhoneAndSmsDraw.resetState();
		initData();
		initPaint();
		initTime();
		anim();
	}
	
	@Override
	public void anim() {
		if(mStartAnimator.isRunning()) {
			mStartAnimator.cancel();
		}
		mStartAnimator.start();
	}
	
	@Override
	public void setPhoneAndSms(int phoneNum, int smsNum) {
		mPhoneAndSmsDraw.setPhoneAndSms(phoneNum, smsNum);
	}

	@Override
	public void changePhoneOrSms(int phoneNum, int smsNum) {
		mPhoneAndSmsDraw.setPhoneAndSms(phoneNum, smsNum);
		invalidate();
	}	
	
	private void run() {
		mRunAnimator.start();
	}
	
	private void initData() {
		mOverlayAlpha = 255;
		mHoleScale = 1.0f;
		mBgOneTranslate = getResources().getDimension(R.dimen.leather_color_one_translate);
		mBgTwoTranslate = getResources().getDimension(R.dimen.leather_color_two_translate_start);
		mBgThreeTranslate = getResources().getDimension(R.dimen.leather_color_one_translate);
		mBgFourTranslate = getResources().getDimension(R.dimen.leather_color_three_translate);
		mFirstLineTextSize = getResources().getDimension(R.dimen.leather_color_textSize);
		mSecondLineTextSize = mFirstLineTextSize;
		
		mFirstLineTextScale = 0.9f;
		mDrawFirstLine = false;
		mSecondLineTextScale = 0.9f;
		mDrawSecondLine = false;
		
		mBgOneRunTranslate = 0f;
		mBgThreeRunTranslate = 0f;
		mBgFourRunTranslate = 0f;
		
		mAnimatorEnd = false;
	}
	
	private void initTime() {
		mCalendar = Calendar.getInstance(TimeZone.getDefault());
		mCalendar.setTimeInMillis(System.currentTimeMillis());
		
		boolean is24 = DateFormat.is24HourFormat(mContext, ActivityManager.getCurrentUser());
        final Locale l = Locale.getDefault();
		
        mHour = is24 ? mCalendar.get(Calendar.HOUR_OF_DAY) : mCalendar.get(Calendar.HOUR);
		mMinute = mCalendar.get(Calendar.MINUTE);
        
        String hourFormat = is24 ? "HH" : "hh";// is24 ?"HH":"hh";        
        String minuteFormat = "mm";      
        
        SimpleDateFormat hourSdf = new SimpleDateFormat(hourFormat, l);
        SimpleDateFormat minuteSdf = new SimpleDateFormat(minuteFormat, l);
        mLocalHour = hourSdf.format(mCalendar.getTime());
        mLocalMinute = minuteSdf.format(mCalendar.getTime());
	}
	
	private void initBitmap() {
		Resources res = getResources();
		mBmpOne = BitmapFactory.decodeResource(res, R.drawable.leather_colours_one, mOptions);
		mBmpTwo = BitmapFactory.decodeResource(res, R.drawable.leather_colours_two, mOptions);
		mBmpThree = BitmapFactory.decodeResource(res, R.drawable.leather_colours_three, mOptions);
		mBmpFour = BitmapFactory.decodeResource(res, R.drawable.leather_colours_four, mOptions);
		mBmpBg = BitmapFactory.decodeResource(res, R.drawable.leather_colours_bg, mOptions);
		mBmpFg = BitmapFactory.decodeResource(res, R.drawable.leather_topic_front, mOptions);
		
		mNumBitmaps = new ArrayList<Bitmap>();
		for(int i = 0; i < mNumIds.length; i++) {
			Bitmap bmp = BitmapFactory.decodeResource(res, mNumIds[i], mOptions);
			mNumBitmaps.add(bmp);
		}
		
	}
	
	private void initPaint() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setStyle(Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setStrokeWidth(0);
		mPaint.setColor(Color.WHITE);
		mPaint.setTextSize(mFirstLineTextSize);
		mPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Akrobat-Regular.otf"));
	}
	
	private void initAnimator() {
		mStartAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_colours_anim);
		mStartAnimator.addListener(new AnimatorListenerAdapter() {
			
			@Override
			public void onAnimationStart(Animator animation) {
				mAnimatorEnd = false;
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				mAnimatorEnd = true;
				run();
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				mAnimatorEnd = true;
			}
		});
		mStartAnimator.setTarget(this);
		mRunAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_colours_run_anim);
		mRunAnimator.setTarget(this);
	}
	
	private Bitmap getFirstTextBitmap(int num) {
		if(num < 10) {
			return mNumBitmaps.get(0);
		} else {
			return mNumBitmaps.get(num / 10);
		}
	}
	
	private Bitmap getSecondTextBitmap(int num) {
		if(num < 10) {
			return mNumBitmaps.get(num);
		} else {
			return mNumBitmaps.get(num % 10);
		}
	}

	public int getOverlayAlpha() {
		return mOverlayAlpha;
	}

	public void setOverlayAlpha(int mOverlayAlpha) {
		this.mOverlayAlpha = mOverlayAlpha;
		invalidate();
	}

	public float getHoleScale() {
		return mHoleScale;
	}

	public void setHoleScale(float mHoleScale) {
		this.mHoleScale = mHoleScale;
		invalidate();
	}

	public float getBgOneTranslate() {
		return mBgOneTranslate;
	}

	public void setBgOneTranslate(float mBgOneTranslate) {
		this.mBgOneTranslate = mBgOneTranslate;
		invalidate();
	}

	public float getBgTwoTranslate() {
		return mBgTwoTranslate;
	}

	public void setBgTwoTranslate(float mBgTwoTranslate) {
		this.mBgTwoTranslate = mBgTwoTranslate;
		invalidate();
	}

	public float getBgThreeTranslate() {
		return mBgThreeTranslate;
	}

	public void setBgThreeTranslate(float mBgThreeTranslate) {
		this.mBgThreeTranslate = mBgThreeTranslate;
		invalidate();
	}

	public float getBgFourTranslate() {
		return mBgFourTranslate;
	}

	public void setBgFourTranslate(float mBgFourTranslate) {
		this.mBgFourTranslate = mBgFourTranslate;
		invalidate();
	}

	public float getFirstLineTextScale() {
		return mFirstLineTextScale;
	}

	public void setFirstLineTextScale(float mFirstLineTextScale) {
		this.mFirstLineTextScale = mFirstLineTextScale;
		mDrawFirstLine = true;
		invalidate();
	}

	public float getSecondtLineTextScale() {
		return mSecondLineTextScale;
	}

	public void setSecondLineTextScale(float mSecondLineTextScale) {
		this.mSecondLineTextScale = mSecondLineTextScale;
		mDrawSecondLine = true;
		invalidate();
	}

	public int getFirstLineTextAlpha() {
		return mFirstLineTextAlpha;
	}

	public void setFirstLineTextAlpha(int mFirstLineTextAlpha) {
		this.mFirstLineTextAlpha = mFirstLineTextAlpha;
		invalidate();
	}

	public int getSecondLineTextAlpha() {
		return mSecondLineTextAlpha;
	}

	public void setSecondLineTextAlpha(int mSecondLineTextAlpha) {
		this.mSecondLineTextAlpha = mSecondLineTextAlpha;
		invalidate();
	}

	public float getBgOneRunTranslate() {
		return mBgOneRunTranslate;
	}

	public void setBgOneRunTranslate(float mBgOneRunTranslate) {
		this.mBgOneRunTranslate = mBgOneRunTranslate;
		invalidate();
	}

	public float getBgThreeRunTranslate() {
		return mBgThreeRunTranslate;
	}

	public void setBgThreeRunTranslate(float mBgThreeRunTranslate) {
		this.mBgThreeRunTranslate = mBgThreeRunTranslate;
		invalidate();
	}

	public float getBgFourRunTranslate() {
		return mBgFourRunTranslate;
	}

	public void setBgFourRunTranslate(float mBgFourRunTranslate) {
		this.mBgFourRunTranslate = mBgFourRunTranslate;
		invalidate();
	}
	
	public float getPhoneAndSmsScale() {
		return mPhoneAndSmsDraw.getPhoneAndSmsScale();
	}

	public void setPhoneAndSmsScale(float mPhoneAndSmsScale) {
		mPhoneAndSmsDraw.setPhoneAndSmsScale(mPhoneAndSmsScale);
		invalidate();
	}

	public float getPhoneAndSmsNumberScale() {
		return mPhoneAndSmsDraw.getPhoneAndSmsNumberScale();
	}

	public void setPhoneAndSmsNumberScale(float mPhoneAndSmsNumberScale) {
		mPhoneAndSmsDraw.setPhoneAndSmsNumberScale(mPhoneAndSmsNumberScale);
		invalidate();
	}
	
	public int getUpTranslate() {
		return mPhoneAndSmsDraw.getUpTranslate();
	}

	public void setUpTranslate(int upTranslate) {
		mPhoneAndSmsDraw.setUpTranslate(upTranslate);
		invalidate();
	}

	public float getUpAlpha() {
		return mPhoneAndSmsDraw.getUpAlpha();
	}

	public void setUpAlpha(float upAlpha) {
		mPhoneAndSmsDraw.setUpAlpha(upAlpha);
		invalidate();
	}

}
