package com.android.systemui.statusbar.phoneleather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import itel.transsion.settingslib.utils.LogUtil;

public class LeatherTimeView extends View implements LeatherBase {	
	private Paint mPaint;
	private Paint mDayPaint;

	private int mWidth, mHeight;

	private int mCenterX, mCenterY;
	
	private boolean mAttached;
	private boolean mIsAnimation;
	private Matrix mMatrix;
	
	private BitmapFactory.Options mOptions;
	private Bitmap mBitmapBg;
	private Bitmap mLightTwoBitmap;
	private Bitmap mLightThreeBitmap;
	private Bitmap mWeekBitmap;	
	private Bitmap mHourBitmap;
	private Bitmap mHourShadowBitmap;
	private Bitmap mMinuteBitmap;
	private Bitmap mMinuteShadowBitmap;
	private Bitmap mSecondShadowBitmap;
	private Bitmap mSecondOriginBitmap;
	private Bitmap mDayOfWeekShadowBitmap;
	private Bitmap mDayOfWeekOriginBitmap;
	private Bitmap mTrayBitmap;
	private int mDay;
	private int mHour;
	private int mMinute;
	private int mSecond;
	private int mMilliSecond;
	private int mDayOfWeek;
	private String mDayStr;
	private float mDayStrWidth;
	private float mDayStrBaseY;
	private PaintFlagsDrawFilter mDrawFilter;
	
	private Calendar mCalendar;
	
	private float mDateRightMargin;	
	private float mDateWidth;
	private float mDateTextSize;
	
	private float mWeekBottomMargin;
	private float mWeekSquareWidth;
	
	private float mSecondTopHeight;
	private float mSecondBottomHeight;
		
	private int BgAlpha;
	private float BgScale;
	private float TrayScale;
	private int LightTwoAlpha;
	private int LightThreeAlpha;
	private int WeekAlpha;
	private float DayOfWeekRotate;
	private boolean mIsDrawDayOfWeek;
	private int HourAlpha;
	private float HourRotate;
	private int MinuteAlpha;
	private float MinuteRotate;
	private int SecondAlpha;
	private float SecondRotate;
	
	private AnimatorSet mAnimatorSet;
	
	private PhoneAndSmsDraw mPhoneAndSmsDraw;
	
	private List<Animator> mAnimatorList;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			initTime();
			initAnimator();
		}
	};
	
	public LeatherTimeView(Context context) {
		this(context, null);
	}
	
	public LeatherTimeView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public LeatherTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);	

		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LeatherTimeView);
		mDateRightMargin = typedArray.getDimension(R.styleable.LeatherTimeView_date_right_margin, 68 * 1.5f);
		mDateWidth = typedArray.getDimension(R.styleable.LeatherTimeView_date_width, 20 * 1.5f);
		mDateTextSize = typedArray.getDimension(R.styleable.LeatherTimeView_date_textSize, 13 * 1.5f);
		mWeekBottomMargin = typedArray.getDimension(R.styleable.LeatherTimeView_week_bottom_margin, 64 * 1.5f);
		mWeekSquareWidth = typedArray.getDimension(R.styleable.LeatherTimeView_week_square_width, 48 * 1.5f);
		mSecondTopHeight = typedArray.getDimension(R.styleable.LeatherTimeView_second_top_height, 108 * 1.5f);
		mSecondBottomHeight = typedArray.getDimension(R.styleable.LeatherTimeView_second_bottom_height, 48 * 1.5f);
		typedArray.recycle();
		
		mAnimatorList = new ArrayList<Animator>();
		
		mPhoneAndSmsDraw = new PhoneAndSmsDraw(context, attrs);
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
		
		initData();
		initBitmap();
		mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
		
		initTime();
		initPaint();
		initAnimator();
		
		mMatrix = new Matrix();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {		
		long start = System.currentTimeMillis();
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heighMode = MeasureSpec.getMode(heightMeasureSpec);
		if(widthMode == MeasureSpec.EXACTLY) {
			mWidth = MeasureSpec.getSize(widthMeasureSpec);
		} else {
			mWidth = mBitmapBg.getWidth();
		}
		if(heighMode == MeasureSpec.EXACTLY) {
			mHeight = MeasureSpec.getSize(heightMeasureSpec);
		} else {
			mHeight = mBitmapBg.getHeight();
		}
		mCenterX = mWidth / 2;
		mCenterY = mHeight / 2;
		mDayStrBaseY = mCenterY + (Math.abs(mDayPaint.ascent()) - Math.abs(mDayPaint.descent())) / 2 ;
		
		mPhoneAndSmsDraw.measure(widthMeasureSpec, heightMeasureSpec);
		
		setMeasuredDimension(mWidth, mHeight);
		long end = System.currentTimeMillis();
		LogUtil.d("onMeasure wast time:" + (end - start));
	}
	
	private void initData() {
		mIsAnimation = true;
		BgAlpha = 0;
		BgScale = 0.8f;
		TrayScale = 0.0f;
		LightTwoAlpha = 0;
		LightThreeAlpha = 0;
		WeekAlpha = 0;
		DayOfWeekRotate = 0.0f;
		mIsDrawDayOfWeek = false;
		HourAlpha = 0;
		HourRotate = 0.0f;
		MinuteAlpha = 0;
		MinuteRotate = 0.0f;
		SecondAlpha = 0;
		SecondRotate = 0.0f;
	}
	
	private void initBitmap() {
		Resources res = getResources();
		mBitmapBg = BitmapFactory.decodeResource(res, R.drawable.leather_time_before_bg, mOptions);
		mLightTwoBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_light_two, mOptions);
		mLightThreeBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_light_three, mOptions);
		mWeekBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_week, mOptions);
		mHourBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_hour, mOptions);
		mHourShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_hour_shadow, mOptions);
		mMinuteBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_minute, mOptions);
		mMinuteShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_minute_shadow, mOptions);
		mSecondShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_second_shadow, mOptions);
		mSecondOriginBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_second_origin, mOptions);
		mTrayBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_tray, mOptions);
		mDayOfWeekShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_dayofweek_shadow, mOptions);
		mDayOfWeekOriginBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_time_dayofweek_origin, mOptions);
	}
	
	private void initTime() {
		long start = System.currentTimeMillis();
		mCalendar = Calendar.getInstance(TimeZone.getDefault());
		mDay = mCalendar.get(Calendar.DAY_OF_MONTH);		
		mDayStr = mDay < 10 ? "0" + mDay : String.valueOf(mDay);
		mHour = mCalendar.get(Calendar.HOUR);
		mMinute = mCalendar.get(Calendar.MINUTE);
		mSecond = mCalendar.get(Calendar.SECOND);
		mMilliSecond = mCalendar.get(Calendar.MILLISECOND);
		mDayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
		long end = System.currentTimeMillis();
		LogUtil.d("initTime() spend time: " + (end - start));
	}
	
	private void initPaint() {
		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setStrokeWidth(0);
		
		mDayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mDayPaint.setColor(0xff757575);
		mDayPaint.setStyle(Paint.Style.STROKE);
		mDayPaint.setTextSize(mDateTextSize);
		mDayPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("CORBERT_REGULAR_REGULAR.OTF"));
		
		mDayStrWidth = mDayPaint.measureText(mDayStr);
	}
	
	private void initAnimator() {
		if(mAnimatorSet != null) {
			mAnimatorList.add(mAnimatorSet);
		}
		mAnimatorSet = (AnimatorSet) AnimatorInflater.loadAnimator(mContext, R.animator.leather_time_anim);
		ValueAnimator animator = ObjectAnimator.ofInt(1, mDay);
		long duration = 0;
		if(mDay > 20) {
			duration = 10 * mDay;
		} else if(mDay > 10) {
			duration = 15 * mDay;
		} else {
			duration = 20 * mDay;
		}
		animator.setDuration(duration);
		animator.setInterpolator(new LinearInterpolator());
		animator.addUpdateListener(new AnimatorUpdateListener() {
			
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				mDay = (Integer) animation.getAnimatedValue();
			}
		});
		mAnimatorSet.play(animator).after(900);
		mAnimatorSet.setTarget(this);
		mAnimatorSet.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				mSecond += 2;
				if(mMilliSecond + 140 >= 1000) { 
					mSecond += 1;
				}
				mIsAnimation = true;
			}
			
			@Override
			public void onAnimationCancel(Animator animation) {
				mIsAnimation = false;
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				mIsAnimation = false;
			}
		});
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!mAttached) {
			mAttached = true;
			anim();
			
			mPhoneAndSmsDraw.onAttachedToWindow();
			
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);//监听时区变化
			filter.addAction(Intent.ACTION_DATE_CHANGED);//监听日期变化
			filter.addAction(Intent.ACTION_LOCALE_CHANGED);
			filter.addAction(Intent.ACTION_TIME_CHANGED);//监听时间变化
			filter.addAction(Intent.ACTION_TIME_TICK);
			
			mContext.registerReceiver(mReceiver, filter);
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			mAttached = false;
			mOptions = null;
			mPhoneAndSmsDraw.onDetachedFromWindow();
			
			if(mBitmapBg != null) {
				mBitmapBg = null;
			}
			if(mLightTwoBitmap != null) {
				mLightTwoBitmap = null;
			}
			if(mLightThreeBitmap != null) {
				mLightThreeBitmap = null;
			}
			if(mWeekBitmap != null) {
				mWeekBitmap = null;
			}
			if(mHourBitmap != null) {
				mHourBitmap = null;
			}
			if(mHourShadowBitmap != null) {
				mHourShadowBitmap = null;
			}
			if(mMinuteBitmap != null) {
				mMinuteBitmap = null;
			}
			if(mMinuteShadowBitmap != null) {
				mMinuteShadowBitmap = null;
			}
			if(mSecondShadowBitmap != null) {
				mSecondShadowBitmap = null;
			}
			if(mSecondOriginBitmap != null) {
				mSecondOriginBitmap = null;
			}
			if(mDayOfWeekShadowBitmap != null) {
				mDayOfWeekShadowBitmap = null;
			}
			if(mDayOfWeekOriginBitmap != null) {
				mDayOfWeekOriginBitmap = null;
			}
			if(mTrayBitmap != null) {
				mTrayBitmap = null;
			}
			
			if(mAnimatorSet != null && mAnimatorSet.isRunning()) {
				mAnimatorSet.cancel();
				mAnimatorSet = null;
			}
			
			if(mAnimatorList != null) {
				for(Animator animator : mAnimatorList) {
					animator.cancel();
				}
				mAnimatorList.clear();
				mAnimatorList = null;
			}
			
			mContext.unregisterReceiver(mReceiver);
		}
	}
	
	@Override
	public void reDraw() {
		for(Animator animator : mAnimatorList) {
			if(animator != null && animator.isRunning()) {
				animator.cancel();
			}
		}
		mAnimatorList.clear();
		if(mAnimatorSet.isRunning()) {
			mAnimatorSet.cancel();
		}
		mPhoneAndSmsDraw.resetState();
		initData();
		initPaint();
		initTime();
		anim();
	}
	
	@Override
	public void anim() {
		if(mAnimatorSet.isRunning()) {
			mAnimatorSet.cancel();
		}
		mAnimatorSet.start();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		drawAnimation(canvas);
		
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
	
	private void drawAnimation(Canvas canvas) {
		
		canvas.setDrawFilter(mDrawFilter);
		mMatrix = new Matrix();
		mMatrix.setScale(BgScale, BgScale, mBitmapBg.getWidth() / 2, mBitmapBg.getHeight() / 2);
		mPaint.setAlpha(BgAlpha);
		canvas.drawBitmap(mBitmapBg, mMatrix, mPaint);
		
		mPaint.setAlpha(LightTwoAlpha);
		canvas.drawBitmap(mLightTwoBitmap, 0, 0, mPaint);
		
		mPaint.setAlpha(LightThreeAlpha);
		canvas.drawBitmap(mLightThreeBitmap, 0, 0, mPaint);
		
		mPaint.setAlpha(WeekAlpha);
		canvas.drawBitmap(mWeekBitmap, 0, 0, mPaint);
		mPaint.setAlpha(255);
		
		if(mIsDrawDayOfWeek) {
			canvas.translate(mCenterX, mHeight - (mWeekSquareWidth / 2 + mWeekBottomMargin));		
			canvas.translate(3f, 3f);
			canvas.rotate((mDayOfWeek - 2) * 360f / 7f - 30.0f + DayOfWeekRotate);
			canvas.drawBitmap(mDayOfWeekShadowBitmap, - mDayOfWeekShadowBitmap.getWidth() / 2, - mDayOfWeekShadowBitmap.getHeight(), mPaint);
			canvas.rotate(-((mDayOfWeek - 2) * 360f / 7f- 30.0f + DayOfWeekRotate));
			canvas.translate(-3f, -3f);
			
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(0xff1065b7);
			mPaint.setStrokeWidth(2);
			canvas.rotate((mDayOfWeek - 2) * 360f / 7f - 30.0f + DayOfWeekRotate);		
			canvas.drawLine(0, 0, 0, - mWeekSquareWidth / 2, mPaint);
			canvas.rotate(-((mDayOfWeek - 2) * 360f / 7f- 30.0f + DayOfWeekRotate));
			
			mPaint.setStrokeWidth(0);
			canvas.drawBitmap(mDayOfWeekOriginBitmap, - mDayOfWeekOriginBitmap.getWidth() / 2, - mDayOfWeekOriginBitmap.getHeight() / 2, mPaint);
			canvas.translate(-mCenterX, -(mHeight - (mWeekSquareWidth / 2 + mWeekBottomMargin)));
			
			mDayStr = mDay < 10 ? "0" + mDay : String.valueOf(mDay);
			canvas.drawText(mDayStr, mWidth - mDateRightMargin - (mDateWidth - mDayStrWidth) / 2 - mDayStrWidth, mDayStrBaseY, mDayPaint);
		
		}
		
		mPhoneAndSmsDraw.draw(canvas);
		
		canvas.translate(mCenterX, mCenterY);
		
		canvas.translate(4f, 8f);
		mPaint.setAlpha(HourAlpha);
		float rotate = (mHour + mMinute / 60.0f) * 30.0f - 90.0f + HourRotate;
		LogUtil.d("rotate:" + rotate);
		canvas.rotate(rotate);		
		canvas.drawBitmap(mHourShadowBitmap, - mHourShadowBitmap.getWidth() / 2, - mHourShadowBitmap.getHeight(), mPaint);
		canvas.rotate(-rotate);
		canvas.translate(-4f, -8f);
		
		canvas.rotate(rotate);
		canvas.drawBitmap(mHourBitmap, - mHourBitmap.getWidth() / 2.0f, - mHourBitmap.getHeight() / 2.0f, mPaint);		
		canvas.rotate(-rotate);
		
		canvas.translate(4f, 8f);
		mPaint.setAlpha(MinuteAlpha);
		canvas.rotate((mMinute + mSecond / 60.0f) * 6.0f - 90.0f + MinuteRotate);
		canvas.drawBitmap(mMinuteShadowBitmap, - mMinuteShadowBitmap.getWidth() / 2, - mMinuteShadowBitmap.getHeight(), mPaint);
		canvas.rotate(-((mMinute + mSecond / 60.0f) * 6.0f - 90.0f + MinuteRotate));
		canvas.translate(-4f, -8f);
		
		canvas.rotate((mMinute + mSecond / 60.0f) * 6.0f - 90.0f + MinuteRotate);
		canvas.drawBitmap(mMinuteBitmap, - mMinuteBitmap.getWidth() / 2, - mMinuteBitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(-((mMinute + mSecond / 60.0f) * 6.0f - 90.0f + MinuteRotate));
		mPaint.setAlpha(255);
		
		mMatrix = new Matrix();
		mMatrix.setScale(TrayScale, TrayScale, mTrayBitmap.getWidth() / 2.0f, mTrayBitmap.getHeight() / 2.0f);
		canvas.translate(-mTrayBitmap.getWidth() / 2, -mTrayBitmap.getHeight() / 2);
		canvas.drawBitmap(mTrayBitmap, mMatrix, mPaint);
		canvas.translate(mTrayBitmap.getWidth() / 2, mTrayBitmap.getHeight() / 2);
		
		if(mIsAnimation) {
			mPaint.setAlpha(SecondAlpha);
			canvas.translate(4f, 8f);
			canvas.rotate(mSecond * 6.0f - 90.0f + SecondRotate);
			canvas.drawBitmap(mSecondShadowBitmap, - mSecondShadowBitmap.getWidth() / 2, - mSecondTopHeight, mPaint);
			canvas.rotate(-(mSecond * 6.0f - 90.0f + SecondRotate));
			canvas.translate(-4f, -8f);
			
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(0xff1065b7);
			mPaint.setAlpha(SecondAlpha);
			mPaint.setStrokeWidth(0);
			canvas.rotate(mSecond * 6.0f - 90.0f + SecondRotate);
			canvas.drawRoundRect(-2 , - mSecondTopHeight , 2 , mSecondBottomHeight , 2f, 2f, mPaint);
			canvas.rotate(-(mSecond * 6.0f - 90.0f + SecondRotate));
			
		} else {
			mPaint.setAlpha(SecondAlpha);
			mPaint.setStyle(Paint.Style.FILL);
			mPaint.setColor(0xff1065b7);
			if(mMilliSecond <= 925) {
				
				canvas.translate(4f, 8f);
				canvas.rotate(mSecond * 6.0f);
				canvas.drawBitmap(mSecondShadowBitmap, - mSecondShadowBitmap.getWidth() / 2, - mSecondTopHeight, mPaint);
				canvas.rotate(-mSecond * 6.0f);
				canvas.translate(-4f, -8f);
				
				canvas.rotate(mSecond * 6.0f);
				mPaint.setStrokeWidth(0);
				canvas.drawRoundRect(-2, - mSecondTopHeight, 2, mSecondBottomHeight, 2f, 2f, mPaint);
				canvas.rotate(-mSecond * 6.0f);
			} else if(mMilliSecond > 925 && mMilliSecond <= 985) {
				
				canvas.translate(4f, 8f);
				canvas.rotate(mSecond * 6.0f + (mMilliSecond - 925) / 60f * 8.0f);
				canvas.drawBitmap(mSecondShadowBitmap, - mSecondShadowBitmap.getWidth() / 2, - mSecondTopHeight, mPaint);
				canvas.rotate(-(mSecond * 6.0f + (mMilliSecond - 925) / 60f * 8.0f));
				canvas.translate(-4f, -8f);
				
				canvas.rotate(mSecond * 6.0f + (mMilliSecond - 925) / 60f * 8.0f);
				mPaint.setStrokeWidth(0);
				canvas.drawRoundRect(-2, - mSecondTopHeight, 2, mSecondBottomHeight, 2f, 2f, mPaint);
				canvas.rotate(-(mSecond * 6.0f + (mMilliSecond - 925) / 60f * 8.0f));
			} else {
				
				canvas.translate(4f, 8f);
				canvas.rotate((mSecond + 1) * 6.0f + 2.0f - (mMilliSecond - 985) / 15f * 2.0f);
				canvas.drawBitmap(mSecondShadowBitmap, - mSecondShadowBitmap.getWidth() / 2, - mSecondTopHeight, mPaint);
				canvas.rotate(-((mSecond + 1) * 6.0f + 2.0f - (mMilliSecond - 985) / 15f * 2.0f));
				canvas.translate(-4f, -8f);
				
				canvas.rotate((mSecond + 1) * 6.0f + 2.0f - (mMilliSecond - 985) / 15f * 2.0f);
				mPaint.setStrokeWidth(0);
				canvas.drawRoundRect(-2, - mSecondTopHeight, 2, mSecondBottomHeight, 2f, 2f, mPaint);
				canvas.rotate(-((mSecond + 1) * 6.0f + 2.0f - (mMilliSecond - 985) / 15f * 2.0f));
			}
		}
		
		canvas.drawBitmap(mSecondOriginBitmap, - mSecondOriginBitmap.getWidth() / 2 , - mSecondOriginBitmap.getHeight() / 2 , mPaint);
		
		canvas.translate(-mCenterX, -mCenterY);
	}

	public int getBgAlpha() {
		return BgAlpha;
	}

	public void setBgAlpha(int bgAlpha) {
		BgAlpha = bgAlpha;
		invalidate();
	}

	public float getBgScale() {
		return BgScale;
	}

	public void setBgScale(float bgScale) {
		BgScale = bgScale;
		invalidate();
	}

	public float getTrayScale() {
		return TrayScale;
	}

	public void setTrayScale(float trayScale) {
		TrayScale = trayScale;
		invalidate();
	}

	public int getLightTwoAlpha() {
		return LightTwoAlpha;
	}

	public void setLightTwoAlpha(int lightAlpha) {
		LightTwoAlpha = lightAlpha;
		invalidate();
	}
	
	public int getHourAlpha() {
		return HourAlpha;
	}

	public void setHourAlpha(int hourAlpha) {
		HourAlpha = hourAlpha;
		invalidate();
	}

	public float getHourRotate() {
		return HourRotate;
	}

	public void setHourRotate(float hourRotate) {
		HourRotate = hourRotate;
		invalidate();
	}

	public int getMinuteAlpha() {
		return MinuteAlpha;
	}

	public void setMinuteAlpha(int minuteAlpha) {
		MinuteAlpha = minuteAlpha;
		invalidate();
	}

	public float getMinuteRotate() {
		return MinuteRotate;
	}

	public void setMinuteRotate(float minuteRotate) {
		MinuteRotate = minuteRotate;
		invalidate();
	}
	
	public int getSecondAlpha() {
		return SecondAlpha;
	}

	public void setSecondAlpha(int secondAlpha) {
		SecondAlpha = secondAlpha;
		mIsAnimation = true;
		invalidate();
	}

	public float getSecondRotate() {
		return SecondRotate;
	}

	public void setSecondRotate(float secondRotate) {
		SecondRotate = secondRotate;
		invalidate();
	}

	public int getLightThreeAlpha() {
		return LightThreeAlpha;
	}

	public void setLightThreeAlpha(int lightThreeAlpha) {
		LightThreeAlpha = lightThreeAlpha;
		invalidate();
	}

	public int getWeekAlpha() {
		return WeekAlpha;
	}

	public void setWeekAlpha(int weekAlpha) {
		WeekAlpha = weekAlpha;
		invalidate();
	}

	public float getDayOfWeekRotate() {
		return DayOfWeekRotate;
	}

	public void setDayOfWeekRotate(float dayOfWeekRotate) {
		mIsDrawDayOfWeek = true;
		DayOfWeekRotate = dayOfWeekRotate;
		invalidate();
	}
	
	public void setSecondRun(float secondRotate) {
		mIsAnimation = false;
		initTime();
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

