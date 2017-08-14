package com.android.systemui.statusbar.phoneleather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.SweepGradient;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockView extends View implements LeatherBase {
	//自定义属性
	private float mTimeTextSize;
	private float mDateWeekTextSize;
	private float mSweepGradientWidth;

	private Paint mPaint;
	private PaintFlagsDrawFilter mDrawFilter;

	private int mWidth, mHeight;

	private int mCenterX, mCenterY;

	private Paint mtextPaint;
	private Paint msmalltextPaint;
	private Paint mampmPaint;
	private Paint mBitmapPaint;
	private Paint mThreeCirclePaint;
	private Paint marcPaint;

	private String mTime;

	private ArrayList<String> mTimeList = new ArrayList<String>();
	
	private int mFirstCircleAlpha;
	private float mSecondCircleScale;
	private int mThreeCircleAlpha;
	private float mThreeCircleStrokeWidth;
	private boolean mDrawThreeCircleAlpha;
	private boolean mDrawThreeCircleStrokeWidth;
	private SweepGradient mSweepGradient;
	private int[] mSweepGradientColor;
	private float[] mSweepGradientPosition;
	private float mSweepAngle;
	private float mSweepStrokeWidthScale;
	private boolean mDrawSweep;
	private boolean mDrawSweepScale;
	private Matrix mMatrix;
	private boolean mDrawFirstCircle;

	private int alphaOne;
	private float translateOne;	
	private float xOne, yOne;
	
	
	private int alphaTwo;
	private float translateTwo;
	private float xTwo, yTwo;
	
	
	private int alphaThree;
	private float translateThree;
	private float xThree, yThree;
	
	private int alphaFour;
	private float translateFour;
	private float xFour, yFour;
	
	private int alphaFive;
	private float translateFive;
	private float xFive, yFive;

	private int alphaWeekAndDay;
	private float xWeekAndDay, yWeekAndDay;

	private String mMonthAndWeek;
	
	public static final String ACTION_LOCKSCREEN_CHANGE = "android.intent.action.wallpaper.LOCKSCREEN_CHANGE";
	
	private Animator mAnimator;
	private PhoneAndSmsDraw mPhoneAndSmsDraw;


	private boolean mAttached;

	// 时间的每一个字符移动的距离
	private float TextMoveDistance = 32;// 20dp

	private float mPaintTextDesent;
	private float mPaintTextHeight;
	private float mSmallPaintTextHeight;
	private float mSmallPaintDesent;

	private BitmapFactory.Options mOptions;
	private Bitmap mBitmapBg;

	private String mAmPmValues = "";

	private int mWallPagerColor;

	private Calendar mCalendar;
	private String mClockFormatString;
	private SimpleDateFormat mClockFormat;
	private Locale mLocale;
	
	private int mTextDirection;

	public ClockView(Context context) {
		this(context, null);
	}

	public ClockView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ClockView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		TypedArray  typeArray = context.obtainStyledAttributes(attrs, R.styleable.LeatherClockView);
		mTimeTextSize = typeArray.getDimension(R.styleable.LeatherClockView_time_textSize, 52 * 1.5f);
		mDateWeekTextSize = typeArray.getDimension(R.styleable.LeatherClockView_dateweek_textSize, 16 * 1.5f);
		mSweepGradientWidth = typeArray.getDimension(R.styleable.LeatherClockView_sweep_gradient_width, 15 * 1.5f);
		
		typeArray.recycle();
		
		mPhoneAndSmsDraw = new PhoneAndSmsDraw(context, attrs);
		
		mTextDirection = getResources().getConfiguration().getLayoutDirection();
		
		mWallPagerColor = 0xFF0d5e72;
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
		
		initData();
		initPaint();
		initTime();	
		initBitmap();
        mMatrix = new Matrix();
        mSweepGradientColor = new int[3];
		mSweepGradientColor[0] = 0x80ffffff;
		mSweepGradientColor[1] = 0xffffffff;
		mSweepGradientColor[2] = 0x80ffffff;

		mSweepGradientPosition = new float[3];
		mSweepGradientPosition[0] = 0.0f;
		mSweepGradientPosition[1] = 0.5f;
		mSweepGradientPosition[2] = 1.0f;
		
		mAnimator = AnimatorInflater.loadAnimator(mContext, R.animator.leather_clock_anim);
		mAnimator.setTarget(this);
        
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
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
		
		mPaintTextDesent = mtextPaint.descent();
		mPaintTextHeight = mtextPaint.descent() - mtextPaint.ascent();
		mSmallPaintTextHeight = msmalltextPaint.descent() - msmalltextPaint.ascent();
		mSmallPaintDesent = msmalltextPaint.descent();
		
		mSweepGradient = new SweepGradient(mCenterX, mCenterY, mSweepGradientColor, mSweepGradientPosition);
		
		mPhoneAndSmsDraw.measure(widthMeasureSpec, heightMeasureSpec);
		
		setMeasuredDimension(mWidth, mHeight);
	}
	
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		WindowManager wm = (WindowManager) (mContext
				.getSystemService(Context.WINDOW_SERVICE));
		DisplayMetrics dm = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(dm);
		int screenHeight = dm.heightPixels;
		int screenWidth = dm.widthPixels;

		if (screenWidth < screenHeight) {
			mCenterX = screenWidth / 2;
		} else {
			mCenterX = screenHeight / 2;	
		}
		mCenterX = mWidth / 2;
		mCenterY = mHeight / 2;
		//获取文字方向ltr or rtl
		mTextDirection = newConfig.getLayoutDirection();
	}

	private final void updateTime() {

		initTime();
		invalidate();
	}

	private void initPaint() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(Color.BLACK);
		mPaint.setStyle(Style.FILL);
		mPaint.setStrokeWidth(0);

		mThreeCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mThreeCirclePaint.setColor(Color.WHITE);
		mThreeCirclePaint.setStyle(Style.STROKE);
		mThreeCirclePaint.setStrokeWidth(0);//

		marcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		marcPaint.setStyle(Style.STROKE);
		marcPaint.setStrokeWidth(mSweepGradientWidth);// 15dp
		marcPaint.setStrokeCap(Paint.Cap.ROUND);
		marcPaint.setStrokeJoin(Paint.Join.ROUND);

		mtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mtextPaint.setColor(Color.WHITE);
		mtextPaint.setStyle(Style.STROKE);
		mtextPaint.setTextSize(mTimeTextSize);
		//linwujia change
		//Typeface face = Typeface.create("sans-serif-light", Typeface.NORMAL);
		//linwujia change可能出现内存泄漏
		//Typeface face = Typeface.createFromAsset(getContext().getAssets(), "fonts/ItelNumber.ttf");
		
		//Typeface face = SystemUIApplication.getInstance().face;
		mtextPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Akrobat-Regular.otf"));

		mampmPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mampmPaint.setColor(Color.WHITE);
		mampmPaint.setStyle(Style.STROKE);
		mampmPaint.setTextSize(mDateWeekTextSize);// 36
		mampmPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Roboto-Regular.ttf"));

		msmalltextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		msmalltextPaint.setColor(Color.WHITE);
		msmalltextPaint.setStyle(Style.FILL);
		msmalltextPaint.setTextSize(mDateWeekTextSize);
		msmalltextPaint.setTypeface(SystemUIFontFactory.getInstance(mContext).getTypefaceByName("Roboto-Regular.ttf"));

		mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBitmapPaint.setStyle(Style.STROKE);
		mBitmapPaint.setStrokeWidth(0);

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
		
		//linwujia add start
		Date now = new Date();
		
		String fmt = DateFormat.getBestDateTimePattern(l, "EEEMMMMd");
		sdf = new SimpleDateFormat(fmt, l);
		mMonthAndWeek = sdf.format(now);
		//linwujia add end
	}

	private void initBitmap() {
		final Resources resource = getResources();
		mBitmapBg = BitmapFactory.decodeResource(resource, R.drawable.leather_bg, mOptions);
		
	}
	
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

			updateTime();
		}
	};

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!mAttached) {
			mAttached = true;
			anim();
			mPhoneAndSmsDraw.onAttachedToWindow();

			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
			getContext().registerReceiver(mIntentReceiver, filter);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAttached) {
			getContext().unregisterReceiver(mIntentReceiver);
			mAttached = false;
			mOptions = null;
			mPhoneAndSmsDraw.onDetachedFromWindow();

			if (mBitmapBg != null) {
				mBitmapBg = null;
			}
			if(mAnimator != null && mAnimator.isRunning()) {
				mAnimator.cancel();
				mAnimator = null;
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.setDrawFilter(mDrawFilter);
		
		if(mDrawFirstCircle) {
			mPaint.setColor(Color.BLACK);
			mPaint.setAlpha(mFirstCircleAlpha);
			canvas.drawCircle(mCenterX, mCenterY, mWidth / 2.0f, mPaint);
		}
		
		mPaint.setAlpha(255);
		mPaint.setColor(mWallPagerColor);
		canvas.drawCircle(mCenterX, mCenterY, mWidth / 2f * mSecondCircleScale, mPaint);
		
		if(mFirstCircleAlpha == 255) {
			canvas.drawBitmap(mBitmapBg, 0, 0, mBitmapPaint);
		}
		
		if(mDrawThreeCircleAlpha) {
			mThreeCirclePaint.setAlpha(mThreeCircleAlpha);
			mThreeCirclePaint.setStyle(Style.FILL);
			canvas.drawCircle(mCenterX, mCenterY, mWidth / 2.0f, mThreeCirclePaint);
		}
		
		if(mDrawThreeCircleStrokeWidth) {
			float strokeWidth = mWidth / 2.0f * mThreeCircleStrokeWidth;
			mThreeCirclePaint.setAlpha(mThreeCircleAlpha);
			mThreeCirclePaint.setStyle(Style.STROKE);
			mThreeCirclePaint.setStrokeWidth(strokeWidth);
			float radius = mWidth / 2.0f - strokeWidth / 2.0f;
			canvas.drawCircle(mCenterX, mCenterY, radius, mThreeCirclePaint);
		}
		
		if(mDrawSweep) {
			mMatrix.reset();
			mMatrix.setRotate(-85, mCenterX, mCenterY);// -90 start
			mSweepGradient.setLocalMatrix(mMatrix);
	
			marcPaint.setShader(mSweepGradient);
			float strokeWidth = mSweepGradientWidth / 2.0f;
			canvas.drawArc(strokeWidth, strokeWidth, mWidth - strokeWidth, mHeight - strokeWidth, -85, mSweepAngle, false, marcPaint);
		}
		
		if(mDrawSweepScale) {
			float strokeheight = mSweepGradientWidth * mSweepStrokeWidthScale;
			float radis = mWidth / 2f - strokeheight / 2f;
			marcPaint.setStrokeWidth(strokeheight);
			marcPaint.setStyle(Style.STROKE);
			canvas.drawCircle(mCenterX, mCenterY, radis, marcPaint);
		}

		float basesmallX = (mCenterX - msmalltextPaint.measureText(mMonthAndWeek) / 2);
		xWeekAndDay = basesmallX;
		yWeekAndDay = (mCenterX + (mSmallPaintTextHeight + mPaintTextHeight) / 2.0f - mSmallPaintDesent);
		msmalltextPaint.setAlpha(alphaWeekAndDay);
		canvas.drawText(mMonthAndWeek, xWeekAndDay, yWeekAndDay,
				msmalltextPaint);
		
		mPhoneAndSmsDraw.draw(canvas);
		
		if(mTextDirection == View.LAYOUT_DIRECTION_LTR) {
			
			float first = mtextPaint.measureText(mTimeList.get(0));
			float second = mtextPaint.measureText(mTimeList.get(1));
			float third = mtextPaint.measureText(mTimeList.get(2));
			float four = mtextPaint.measureText(mTimeList.get(3));
			float five = mtextPaint.measureText(mTimeList.get(4));
			
			float baseX = (mCenterX - mtextPaint.measureText(mTime) / 2f);
			float baseY = (mCenterY - (mSmallPaintTextHeight + mPaintTextHeight) / 2.0f + mPaintTextHeight - mPaintTextDesent);

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
			
	}
	
	@Override
	public void anim() {
		if(mAnimator.isRunning()) {
			mAnimator.cancel();
		}
		mAnimator.start();
	}
	
	private void initData() {
		mDrawFirstCircle = true;
		mFirstCircleAlpha = 0;
		mSecondCircleScale = 0.0f;
		
		mDrawThreeCircleAlpha = false;
		mThreeCircleAlpha = 0;
		mDrawThreeCircleStrokeWidth = false;
		mThreeCircleStrokeWidth = 1.0f;
		
		mDrawSweep = false;
		mSweepAngle = 0.0f;
		mDrawSweepScale = false;
		mSweepStrokeWidthScale = 1.0f;
		
		alphaWeekAndDay = 0;
		
		alphaOne = 0;
		translateOne = 0.0f;
		
		alphaTwo = 0;
		translateTwo = 0.0f;
		
		alphaThree = 0;
		translateThree = 0.0f;
		
		alphaFour = 0;
		translateFour = 0.0f;
		
		alphaFive = 0;
		translateFive = 0.0f;
		
	}
	
	@Override
	public void reDraw() {
		initData();
		initPaint();
		initTime();
		mPhoneAndSmsDraw.resetState();
		anim();
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

	public int getFirstCircleAlpha() {
		return mFirstCircleAlpha;
	}

	public void setFirstCircleAlpha(int mFirstCircleAlpha) {
		this.mFirstCircleAlpha = mFirstCircleAlpha;
		invalidate();
	}

	public float getSecondCircleScale() {
		return mSecondCircleScale;
	}

	public void setSecondCircleScale(float mSecondCircleScale) {
		this.mSecondCircleScale = mSecondCircleScale;
		invalidate();
	}

	public int getThreeCircleAlpha() {
		return mThreeCircleAlpha;
	}

	public void setThreeCircleAlpha(int mThreeCircleAlpha) {
		this.mThreeCircleAlpha = mThreeCircleAlpha;
		mDrawThreeCircleAlpha = true;
		mDrawFirstCircle = false;
		invalidate();
	}

	public float getThreeCircleStrokeWidth() {
		return mThreeCircleStrokeWidth;
	}

	public void setThreeCircleStrokeWidth(float mThreeCircleStrokeWidth) {
		this.mThreeCircleStrokeWidth = mThreeCircleStrokeWidth;
		mDrawThreeCircleStrokeWidth = true;
		mDrawThreeCircleAlpha = false;
		invalidate();
	}

	public float getSweepAngle() {
		return mSweepAngle;
	}

	public void setSweepAngle(float mSweepAngle) {
		this.mSweepAngle = mSweepAngle;
		mDrawSweep = true;
		invalidate();
	}

	public float getSweepStrokeWidthScale() {
		return mSweepStrokeWidthScale;
	}

	public void setSweepStrokeWidthScale(float mSweepStrokeWidthScale) {
		this.mSweepStrokeWidthScale = mSweepStrokeWidthScale;
		mDrawSweepScale = true;
		mDrawSweep = false;
		invalidate();
	}

	public int getAlphaWeekAndDay() {
		return alphaWeekAndDay;
	}

	public void setAlphaWeekAndDay(int alphaWeekAndDay) {
		this.alphaWeekAndDay = alphaWeekAndDay;
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
		mDrawSweepScale = false;
		invalidate();
	}

	public int getAlphaOne() {
		return alphaOne;
	}

	public void setAlphaOne(int alphaOne) {
		this.alphaOne = alphaOne;
		invalidate();
	}

	public float getTranslateOne() {
		return translateOne;
	}

	public void setTranslateOne(float translateOne) {
		this.translateOne = translateOne;
		invalidate();
	}

	public int getAlphaTwo() {
		return alphaTwo;
	}

	public void setAlphaTwo(int alphaTwo) {
		this.alphaTwo = alphaTwo;
		invalidate();
	}

	public float getTranslateTwo() {
		return translateTwo;
	}

	public void setTranslateTwo(float translateTwo) {
		this.translateTwo = translateTwo;
		invalidate();
	}

	public int getAlphaThree() {
		return alphaThree;
	}

	public void setAlphaThree(int alphaThree) {
		this.alphaThree = alphaThree;
		invalidate();
	}

	public float getTranslateThree() {
		return translateThree;
	}

	public void setTranslateThree(float translateThree) {
		this.translateThree = translateThree;
		invalidate();
	}

	public int getAlphaFour() {
		return alphaFour;
	}

	public void setAlphaFour(int alphaFour) {
		this.alphaFour = alphaFour;
		invalidate();
	}

	public float getTranslateFour() {
		return translateFour;
	}

	public void setTranslateFour(float translateFour) {
		this.translateFour = translateFour;
		invalidate();
	}

	public int getAlphaFive() {
		return alphaFive;
	}

	public void setAlphaFive(int alphaFive) {
		this.alphaFive = alphaFive;
		invalidate();
	}

	public float getTranslateFive() {
		return translateFive;
	}

	public void setTranslateFive(float translateFive) {
		this.translateFive = translateFive;
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
