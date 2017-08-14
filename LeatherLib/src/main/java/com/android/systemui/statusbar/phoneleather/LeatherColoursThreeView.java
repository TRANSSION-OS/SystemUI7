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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LeatherColoursThreeView extends View implements LeatherBase {
	
	private BitmapFactory.Options mOptions;
	
	private Bitmap mBmpOne, mBmpTwo, mBmpThree;
	private Bitmap mBmpBg, mBmpFg;
	
	private int mWidth, mHeight;
	private int mCenterX, mCenterY;
	
	private Paint mPaint;
	private Paint mtextPaint;
	private Paint msmalltextPaint;
	private Paint mampmPaint;
	
	private String mTime;
	private String mMonthAndWeek;

	private ArrayList<String> mTimeList = new ArrayList<String>();
	private String mAmPmValues = "";
	
	private Calendar mCalendar;
	private String mClockFormatString;
	private SimpleDateFormat mClockFormat;
	private Locale mLocale;
	private int mTextDirection;
	
	private boolean mAttached;
	
	private int mOverlayAlpha;
	private float mHoleScale;
	
	private float mBgOneStartX;
	private float mBgOneStartY;
	private float mBgOneTranslateX;
	private float mBgOneTranslateY;
	private float mBgOneTranslateRate;
	
	private float mBgTwoStartX;
	private float mBgTwoStartY;
	private float mBgTwoTranslateX;
	private float mBgTwoTranslateY;
	private float mBgTwoTranslateRate;
	
	private float mBgThreeStartX;
	private float mBgThreeStartY;
	private float mBgThreeTranslateX;
	private float mBgThreeTranslateY;
	private float mBgThreeTranslateRate;
	
	
	private float mBgOneRunTranslate;
	private float mBgTwoRunTranslate;
	private float mBgThreeRunTranslate;
	
	// ʱ���ÿһ���ַ��ƶ��ľ���
	private float TextMoveDistance = 20;// 10dp
	private float mPaintTextDesent;
	private float mPaintTextHeight;
	private float mSmallPaintTextHeight;
	private float mSmallPaintDesent;
	
	private Matrix mMatrix;
	
	private float xOne, yOne;
	private float xTwo, yTwo;
	private float xThree, yThree;
	private float xFour, yFour;
	private float xFive, yFive; 
	private int alphaOne, alphaTwo, alphaThree, alphaFour, alphaFive;
	private float translateOne, translateTwo, translateThree, translateFour, translateFive;
	
	
	private float mWeekAndDayX;
	private float mWeekAndDayY;
	private int mWeekAndDayAlpha;
	
	private boolean mAnimatorEnd;
	
	
	private Animator mStartAnimator;
	
	private PhoneAndSmsDraw mPhoneAndSmsDraw;
	
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_TIME_TICK)) {

			} else if (action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
				if (mClockFormat != null) {
					mClockFormat.setTimeZone(mCalendar.getTimeZone());
				}
			} else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
				final Locale newLocale = getResources().getConfiguration().locale;
				if (!newLocale.equals(mLocale)) {
					mLocale = newLocale;
					mClockFormatString = ""; // force refresh
				}
			}
			initTime();
			invalidate();
		}
	};

	public LeatherColoursThreeView(Context context) {
		this(context, null);
	}

	public LeatherColoursThreeView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public LeatherColoursThreeView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		mMatrix = new Matrix();
		
		mTextDirection = getResources().getConfiguration().getLayoutDirection();
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
		
		mPhoneAndSmsDraw = new PhoneAndSmsDraw(context, attrs);
		
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
		
		mPaintTextDesent = mtextPaint.descent();
		mPaintTextHeight = mtextPaint.descent() - mtextPaint.ascent();
		mSmallPaintTextHeight = msmalltextPaint.descent() - msmalltextPaint.ascent();
		mSmallPaintDesent = msmalltextPaint.descent();
		mPhoneAndSmsDraw.measure(widthMeasureSpec, heightMeasureSpec);
		
		setMeasuredDimension(mWidth, mHeight);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		mPaint.setAlpha(255);
		canvas.drawBitmap(mBmpBg, 0, 0, null);
		
		mMatrix.reset();
		mMatrix.setTranslate(mBgOneStartX + mBgOneTranslateX * mBgOneTranslateRate + mBgOneRunTranslate, mBgOneStartY + mBgOneTranslateY * mBgOneTranslateRate + mBgOneRunTranslate);
		canvas.drawBitmap(mBmpOne, mMatrix, null);
		//canvas.translate(mBgOneStartX + mBgOneTranslateX * mBgOneTranslateRate + mBgOneRunTranslate, mBgOneStartY + mBgOneTranslateY * mBgOneTranslateRate + mBgOneRunTranslate);
		//canvas.drawBitmap(mBmpOne, 0, 0, null);
		//canvas.translate(-(mBgOneStartX + mBgOneTranslateX * mBgOneTranslateRate + mBgOneRunTranslate), -(mBgOneStartY + mBgOneTranslateY * mBgOneTranslateRate + mBgOneRunTranslate));
		
		mMatrix.reset();
		mMatrix.setTranslate(mBgTwoStartX + mBgTwoTranslateX * mBgTwoTranslateRate + mBgTwoRunTranslate, mBgTwoStartY + mBgTwoTranslateY * mBgTwoTranslateRate + mBgTwoRunTranslate);
		canvas.drawBitmap(mBmpTwo, mMatrix, null);
		/*canvas.translate(mBgTwoStartX + mBgTwoTranslateX * mBgTwoTranslateRate + mBgTwoRunTranslate, mBgTwoStartY + mBgTwoTranslateY * mBgTwoTranslateRate + mBgTwoRunTranslate);
		canvas.drawBitmap(mBmpTwo, 0, 0, null);
		canvas.translate(-(mBgTwoStartX + mBgTwoTranslateX * mBgTwoTranslateRate + mBgTwoRunTranslate), -(mBgTwoStartY + mBgTwoTranslateY * mBgTwoTranslateRate + mBgTwoRunTranslate));*/
		
		mMatrix.reset();
		mMatrix.setTranslate(mBgThreeStartX + mBgThreeTranslateX * mBgThreeTranslateRate + mBgThreeRunTranslate, mBgThreeStartY + mBgThreeTranslateY * mBgThreeTranslateRate + mBgThreeRunTranslate);
		canvas.drawBitmap(mBmpThree, mMatrix, null);
		/*canvas.translate(mBgThreeStartX + mBgThreeTranslateX * mBgThreeTranslateRate + mBgThreeRunTranslate, mBgThreeStartY + mBgThreeTranslateY * mBgThreeTranslateRate + mBgThreeRunTranslate);
		canvas.drawBitmap(mBmpThree, 0, 0, null);
		canvas.translate(-(mBgThreeStartX + mBgThreeTranslateX * mBgThreeTranslateRate + mBgThreeRunTranslate), -(mBgThreeStartY + mBgThreeTranslateY * mBgThreeTranslateRate + mBgThreeRunTranslate));*/
		
		canvas.drawBitmap(mBmpFg, 0, 0, null);
		
		float basesmallX = (mCenterX - msmalltextPaint.measureText(mMonthAndWeek) / 2);
		mWeekAndDayX = basesmallX;
		mWeekAndDayY = (mCenterX + (mSmallPaintTextHeight + mPaintTextHeight) / 2.0f - mSmallPaintDesent);
		msmalltextPaint.setAlpha(mWeekAndDayAlpha);
		canvas.drawText(mMonthAndWeek, mWeekAndDayX, mWeekAndDayY,
				msmalltextPaint);
		if(mTextDirection == View.LAYOUT_DIRECTION_LTR) {
			float baseX = (mCenterX - mtextPaint.measureText(mTime) / 2f);
			float baseY = (mCenterY - (mSmallPaintTextHeight + mPaintTextHeight) / 2.0f + mPaintTextHeight - mPaintTextDesent);
	
			float first = mtextPaint.measureText(mTimeList.get(0));
			float second = mtextPaint.measureText(mTimeList.get(1));
			float third = mtextPaint.measureText(mTimeList.get(2));
			float four = mtextPaint.measureText(mTimeList.get(3));
			float five = mtextPaint.measureText(mTimeList.get(4));
	
			xOne = baseX;
			yOne = baseY - TextMoveDistance + TextMoveDistance * translateOne;		
			mtextPaint.setAlpha(alphaOne);
			canvas.drawText(mTimeList.get(0), xOne, yOne, mtextPaint);
			
			xTwo = baseX + first;
			yTwo = baseY - TextMoveDistance + TextMoveDistance * translateTwo;
			mtextPaint.setAlpha(alphaTwo);
			canvas.drawText(mTimeList.get(1), xTwo, yTwo, mtextPaint);
			
			xThree = baseX + first + second;
			yThree = baseY - TextMoveDistance + TextMoveDistance * translateThree;
			mtextPaint.setAlpha(alphaThree);
			canvas.drawText(mTimeList.get(2), xThree, yThree, mtextPaint);
			
			xFour = baseX + first + second + third;
			yFour = baseY - TextMoveDistance + TextMoveDistance * translateFour;
			mtextPaint.setAlpha(alphaFour);
			canvas.drawText(mTimeList.get(3), xFour, yFour, mtextPaint);
	
			xFive = baseX + first + second + third + four;
			yFive = baseY - TextMoveDistance + TextMoveDistance * translateFive;
			mtextPaint.setAlpha(alphaFive);
			canvas.drawText(mTimeList.get(4), xFive, yFive, mtextPaint);
			
			if (!mAmPmValues.equals("") && alphaFive == 255) {
				canvas.drawText(mAmPmValues, baseX + first + second + third + four
						+ five, yFive, mampmPaint);
			}
		} else {
			
			float first = mtextPaint.measureText(mTimeList.get(4));
			float second = mtextPaint.measureText(mTimeList.get(3));
			float third = mtextPaint.measureText(mTimeList.get(2));
			float four = mtextPaint.measureText(mTimeList.get(1));
			float five = mtextPaint.measureText(mTimeList.get(0));
			
			float baseX = (mCenterX + mtextPaint.measureText(mTime) / 2f) - first;
			float baseY = (mCenterY - (mSmallPaintTextHeight + mPaintTextHeight) / 2.0f + mPaintTextHeight - mPaintTextDesent);

			xOne = baseX;
			yOne = baseY - TextMoveDistance + TextMoveDistance * translateOne;		
			mtextPaint.setAlpha(alphaOne);
			canvas.drawText(mTimeList.get(4), xOne, yOne, mtextPaint);
			
			xTwo = baseX - second;
			yTwo = baseY - TextMoveDistance + TextMoveDistance * translateTwo;
			mtextPaint.setAlpha(alphaTwo);
			canvas.drawText(mTimeList.get(3), xTwo, yTwo, mtextPaint);
			
			xThree = baseX - second - third;
			yThree = baseY - TextMoveDistance + TextMoveDistance * translateThree;
			mtextPaint.setAlpha(alphaThree);
			canvas.drawText(mTimeList.get(2), xThree, yThree, mtextPaint);
			
			xFour = baseX - second - third - four;
			yFour = baseY - TextMoveDistance + TextMoveDistance * translateFour;
			mtextPaint.setAlpha(alphaFour);
			canvas.drawText(mTimeList.get(1), xFour, yFour, mtextPaint);

			xFive = baseX - second - third - four - five;
			yFive = baseY - TextMoveDistance + TextMoveDistance * translateFive;
			mtextPaint.setAlpha(alphaFive);
			canvas.drawText(mTimeList.get(0), xFive, yFive, mtextPaint);
			
			if (!mAmPmValues.equals("") && alphaFive == 255) {
				canvas.drawText(mAmPmValues, baseX - second - third - four
						- five - mampmPaint.measureText(mAmPmValues), yFive, mampmPaint);
			}
			
		}
		
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
			if(mBmpFg != null && !mBmpFg.isRecycled()) {
				mBmpFg.recycle();
				mBmpFg = null;
			}
			
			if(mStartAnimator != null && mStartAnimator.isRunning()) {
				mStartAnimator.cancel();
				mStartAnimator = null;
			}
			
			mContext.unregisterReceiver(mIntentReceiver);
		}
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//获取文字方向ltr or rtl
		mTextDirection = newConfig.getLayoutDirection();
	}
	
	@Override
	public void reDraw() {
		if(mStartAnimator.isRunning()) {
			mStartAnimator.cancel();
		}
		mPhoneAndSmsDraw.resetState();
		initData();
		initPaint();
		mStartAnimator.start();
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
	}
	
	private void initData() {
		mOverlayAlpha = 255;
		mHoleScale = 1.0f;
		
		mBgOneStartX = getResources().getDimension(R.dimen.leather_color_three_one_startX);
		mBgOneStartY = getResources().getDimension(R.dimen.leather_color_three_one_startY);
		mBgOneTranslateX = getResources().getDimension(R.dimen.leather_color_three_one_translateX);
		mBgOneTranslateY = getResources().getDimension(R.dimen.leather_color_three_one_translateY);
		mBgOneTranslateRate = 0.0f;
		
		mBgTwoStartX = getResources().getDimension(R.dimen.leather_color_three_two_startX);
		mBgTwoStartY = getResources().getDimension(R.dimen.leather_color_three_two_startY);
		mBgTwoTranslateX = getResources().getDimension(R.dimen.leather_color_three_two_translateX);
		mBgTwoTranslateY = getResources().getDimension(R.dimen.leather_color_three_two_translateY);
		mBgTwoTranslateRate = 0.0f;
		
		
		mBgThreeStartX = getResources().getDimension(R.dimen.leather_color_three_three_startX);
		mBgThreeStartY = getResources().getDimension(R.dimen.leather_color_three_three_startY);
		mBgThreeTranslateX = getResources().getDimension(R.dimen.leather_color_three_three_translateX);
		mBgThreeTranslateY = getResources().getDimension(R.dimen.leather_color_three_three_translateY);
		mBgThreeTranslateRate = 0.0f;
		
		mBgOneRunTranslate = 0f;
		mBgTwoRunTranslate = 0f;
		mBgThreeRunTranslate = 0f;
		
		mAnimatorEnd = false;
		
		mWeekAndDayAlpha = 0;
		alphaOne = 0;
		alphaTwo = 0;
		alphaThree = 0;
		alphaFour = 0;
		alphaFive = 0;
		translateOne = 0f;
		translateTwo = 0f;
		translateThree = 0f;
		translateFour = 0f;
		translateFive = 0f;
	}
	
	private void initTime() {
		mCalendar = Calendar.getInstance(TimeZone.getDefault());
		mCalendar.setTimeInMillis(System.currentTimeMillis());

		mTimeList.clear();
		
		boolean is24 = DateFormat.is24HourFormat(mContext, ActivityManager.getCurrentUser());
        SimpleDateFormat sdf;
        final Locale l = Locale.getDefault();
        String format = is24 ? "HH:mm" : "hh:mm";// is24 ?"HH:mm":"hh:mm";
        if (!format.equals(mClockFormatString)) {
            mClockFormat = sdf = new SimpleDateFormat(format, l);
            mClockFormatString = format;
        } else {
            sdf = mClockFormat;
        }
        
        mTime = sdf.format(mCalendar.getTime());
		for(int i = 0; i < mTime.length(); i++) {
			mTimeList.add(mTime.charAt(i) + "");
		}
		
		if(!is24) {
			format = " a";
			sdf = new SimpleDateFormat(format, l);
			mAmPmValues = sdf.format(mCalendar.getTime());
		} else {
			mAmPmValues = "";
		}
		
		Date now = new Date();
		
		String fmt = DateFormat.getBestDateTimePattern(l, "EEEMMMMd");
		sdf = new SimpleDateFormat(fmt, l);
		mMonthAndWeek = sdf.format(now);
	}
	
	private void initBitmap() {
		Resources res = getResources();
		mBmpOne = BitmapFactory.decodeResource(res, R.drawable.leather_colours_three_one, mOptions);
		mBmpTwo = BitmapFactory.decodeResource(res, R.drawable.leather_colours_three_two, mOptions);
		mBmpThree = BitmapFactory.decodeResource(res, R.drawable.leather_colours_three_three, mOptions);
		mBmpBg = BitmapFactory.decodeResource(res, R.drawable.leather_colours_three_bg, mOptions);
		mBmpFg = BitmapFactory.decodeResource(res, R.drawable.leather_topic_front, mOptions);
		
	}
	
	private void initPaint() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setStyle(Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setStrokeWidth(0);
		mPaint.setColor(Color.WHITE);
		mPaint.setTextSize(mContext.getResources().getDimension(R.dimen.leather_color_textSize));
	
		mtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mtextPaint.setColor(Color.WHITE);
		mtextPaint.setStyle(Style.STROKE);
		mtextPaint.setTextSize(140);
		mtextPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Akrobat-Regular.otf"));
		
		mampmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mampmPaint.setColor(Color.WHITE);
		mampmPaint.setStyle(Style.STROKE);
		mampmPaint.setTextSize(36.0f);// 18sp
		mampmPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Roboto-Regular.ttf"));
		
		msmalltextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		msmalltextPaint.setColor(Color.WHITE);
		msmalltextPaint.setStyle(Style.FILL);
		msmalltextPaint.setTextSize(36);
		msmalltextPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Roboto-Regular.ttf"));
	}
	
	private void initAnimator() {
		mStartAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_colours_three_anim);
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

	public float getBgOneTranslateRate() {
		return mBgOneTranslateRate;
	}

	public void setBgOneTranslateRate(float mBgOneTranslateRate) {
		this.mBgOneTranslateRate = mBgOneTranslateRate;
		invalidate();
	}

	public float getBgTwoTranslateRate() {
		return mBgTwoTranslateRate;
	}

	public void setBgTwoTranslateRate(float mBgTwoTranslateRate) {
		this.mBgTwoTranslateRate = mBgTwoTranslateRate;
		invalidate();
	}

	public float getBgThreeTranslateRate() {
		return mBgThreeTranslateRate;
	}

	public void setBgThreeTranslateRate(float mBgThreeTranslateRate) {
		this.mBgThreeTranslateRate = mBgThreeTranslateRate;
		invalidate();
	}

	public float getBgOneRunTranslate() {
		return mBgOneRunTranslate;
	}

	public void setBgOneRunTranslate(float mBgOneRunTranslate) {
		this.mBgOneRunTranslate = mBgOneRunTranslate;
		invalidate();
	}

	public float getBgTwoRunTranslate() {
		return mBgTwoRunTranslate;
	}

	public void setBgTwoRunTranslate(float mBgTwoRunTranslate) {
		this.mBgTwoRunTranslate = mBgTwoRunTranslate;
		invalidate();
	}

	public float getBgThreeRunTranslate() {
		return mBgThreeRunTranslate;
	}

	public void setBgThreeRunTranslate(float mBgThreeRunTranslate) {
		this.mBgThreeRunTranslate = mBgThreeRunTranslate;
		invalidate();
	}

	public int getWeekAndDayAlpha() {
		return mWeekAndDayAlpha;
	}

	public void setWeekAndDayAlpha(int mWeekAndDayAlpha) {
		this.mWeekAndDayAlpha = mWeekAndDayAlpha;
		invalidate();
	}

	public int getAlphaOne() {
		return alphaOne;
	}

	public void setAlphaOne(int alphaOne) {
		this.alphaOne = alphaOne;
		invalidate();
	}

	public int getAlphaTwo() {
		return alphaTwo;
	}

	public void setAlphaTwo(int alphaTwo) {
		this.alphaTwo = alphaTwo;
		invalidate();
	}

	public int getAlphaThree() {
		return alphaThree;
	}

	public void setAlphaThree(int alphaThree) {
		this.alphaThree = alphaThree;
		invalidate();
	}

	public int getAlphaFour() {
		return alphaFour;
	}

	public void setAlphaFour(int alphaFour) {
		this.alphaFour = alphaFour;
		invalidate();
	}

	public int getAlphaFive() {
		return alphaFive;
	}

	public void setAlphaFive(int alphaFive) {
		this.alphaFive = alphaFive;
		invalidate();
	}

	public float getTranslateOne() {
		return translateOne;
	}

	public void setTranslateOne(float translateOne) {
		this.translateOne = translateOne;
		invalidate();
	}

	public float getTranslateTwo() {
		return translateTwo;
	}

	public void setTranslateTwo(float translateTwo) {
		this.translateTwo = translateTwo;
		invalidate();
	}

	public float getTranslateThree() {
		return translateThree;
	}

	public void setTranslateThree(float translateThree) {
		this.translateThree = translateThree;
		invalidate();
	}

	public float getTranslateFour() {
		return translateFour;
	}

	public void setTranslateFour(float translateFour) {
		this.translateFour = translateFour;
		invalidate();
	}

	public float getTranslateFive() {
		return translateFive;
	}

	public void setTranslateFive(float translateFive) {
		this.translateFive = translateFive;
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
