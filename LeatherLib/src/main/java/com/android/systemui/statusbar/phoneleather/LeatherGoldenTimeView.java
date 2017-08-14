package com.android.systemui.statusbar.phoneleather;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
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
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import itel.transsion.settingslib.utils.LogUtil;


public class LeatherGoldenTimeView extends View implements LeatherBase {
	private Paint mPaint;
	
	private int mWidth, mHeight;
	private int mCenterX, mCenterY;
	private PaintFlagsDrawFilter mDrawFilter;
	
	private Bitmap mBgOneBitmap;
	private Bitmap mBgTwoBitmap;
	private Bitmap mBgThreeBitmap;
	private Bitmap mBgFourBitmap;
	private Bitmap mBgFiveBitmap;
	private Bitmap mBgSixBitmap;
	
	private Bitmap mSecondBitmap;
	private Bitmap mSecondShadowBitmap;
	private Bitmap mSecondTrayBitmap;
	
	private Bitmap mHourOf24Bitmap;
	private Bitmap mHourOf24ShadowBitmap;
	private Bitmap mHourOf24TrayBitmap;
	
	private Bitmap mHourBitmap;
	private Bitmap mHourShadowBitmap;
	
	private Bitmap mMinuteBitmap;
	private Bitmap mMinuteShadowBitmap;
	
	private Bitmap mTrayBitmap;
	
	private float mLeftClockMarginLeft;
	private float mLeftClockRadius;
	private float mRightClockMarginRight;
	private float mRightClockRadius;
	private float mSecondTop;
	
	private int mHour;
	private int mHourOfDay;
	private int mMinute;
	private int mSecond;
	private int mMilliSecond;
	private Calendar mCalendar;
	
	private int mFiveBgRotate;
	private int mThreeBgRotate;
	private int mSecondRotate;
	
	private boolean mAttached;
	
	private BitmapFactory.Options mOptions;
	private Bitmap mBgBitmap;
	private Bitmap mLeftDoorBitmap;
	private Bitmap mRightDoorBitmap;
	private Bitmap mDoorBitmap;
	private Bitmap mDoorHandleBitmap;
	private Bitmap mOverlayBitmap;
	private Bitmap mLightBitmap;
	
	private int[] mBitmapIds = {  R.drawable.leather_golden_time_steam_01, R.drawable.leather_golden_time_steam_02, R.drawable.leather_golden_time_steam_03,
			R.drawable.leather_golden_time_steam_04, R.drawable.leather_golden_time_steam_05, R.drawable.leather_golden_time_steam_06, R.drawable.leather_golden_time_steam_07,
			R.drawable.leather_golden_time_steam_08, R.drawable.leather_golden_time_steam_09, R.drawable.leather_golden_time_steam_10, R.drawable.leather_golden_time_steam_11, 
			R.drawable.leather_golden_time_steam_12, R.drawable.leather_golden_time_steam_13, R.drawable.leather_golden_time_steam_14, R.drawable.leather_golden_time_steam_15,
			R.drawable.leather_golden_time_steam_16, R.drawable.leather_golden_time_steam_17, R.drawable.leather_golden_time_steam_18, R.drawable.leather_golden_time_steam_19,
			R.drawable.leather_golden_time_steam_20, R.drawable.leather_golden_time_steam_21, R.drawable.leather_golden_time_steam_22, R.drawable.leather_golden_time_steam_23};
	private List<Bitmap> mBitmaps;
	
	
	private float mHandleRotate;
	private float mDoorTranslate;
	private float mMaxDoorTranslate;
	private int mBitmapIndex;
	private boolean mDrawStream;
	private int mLigthAlpha;
	private int mBgAlpha;
	private int mTrayAlpha;
	private float mBgScale;
	private boolean mIsDrawPointer;
	private int mMinuteAlpha;
	private float mMinuteRotate;
	private int mHourAlpha;
	private float mHourRotate;
	private int mHourOf24Alpha;
	private float mHourOf24Rotate;
	private int mSecondAlpha;
	
	private boolean mDrawHole;
	private int mHoleAlpha;
	
	private Matrix mMatrix;
	private Animator mStartAnimator;
	private Animator mRunAnimator;
	

	public LeatherGoldenTimeView(Context context) {
		this(context, null);
	}

	public LeatherGoldenTimeView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public LeatherGoldenTimeView(Context context, AttributeSet attrs,
			int defStyleAttr) {		
		super(context, attrs, defStyleAttr);
		long start = System.currentTimeMillis();
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LeatherGoldenTimeView);
		
		mLeftClockMarginLeft = typedArray.getDimension(R.styleable.LeatherGoldenTimeView_left_clock_margin_left, 0);
		mLeftClockRadius = typedArray.getDimension(R.styleable.LeatherGoldenTimeView_left_clock_radius, 0);
		mRightClockMarginRight = typedArray.getDimension(R.styleable.LeatherGoldenTimeView_right_clock_margin_right, 0);
		mRightClockRadius = typedArray.getDimension(R.styleable.LeatherGoldenTimeView_right_clock_radius, 0);
		mSecondTop = typedArray.getDimension(R.styleable.LeatherGoldenTimeView_left_clock_second_top_height, 0);
		
		typedArray.recycle();
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
		
		mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
		mStartAnimator = AnimatorInflater.loadAnimator(getContext(), R.animator.leather_golden_time_anim);
		mRunAnimator = AnimatorInflater.loadAnimator(getContext(), R.animator.leather_golden_time_run_anim);
		mStartAnimator.addListener(new AnimatorListenerAdapter() {
			
			@Override
			public void onAnimationStart(Animator animation) {
				//总动画时长3220
				mMilliSecond += 220;
				if(mMilliSecond >= 1000) {
					mMilliSecond -= 1000;
					mSecond += 1;
				}
				mSecond += 3;
				if(mSecond >= 60) {
					mSecond -= 60;
					mMinute += 1;
				}
				if(mMinute >= 60) {
					mMinute -= 60;
				}
				if(mHour >= 12) {
					mHour -= 12;
				}
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				run();
			}
		});
		
		mMatrix = new Matrix();
		mBitmaps = new ArrayList<Bitmap>();
		mMaxDoorTranslate = getResources().getDimension(R.dimen.door_three_translate);
		
		initData();
		initPaint();
		
		initBitmap();
		initTime();
		long end = System.currentTimeMillis();
		LogUtil.d("LeatherGoldenTimeView constuctor spend time:" + (end - start));
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heighMode = MeasureSpec.getMode(heightMeasureSpec);
		if(widthMode == MeasureSpec.EXACTLY) {
			mWidth = MeasureSpec.getSize(widthMeasureSpec);
		} else {
			mWidth = mOverlayBitmap.getWidth();
		}
		if(heighMode == MeasureSpec.EXACTLY) {
			mHeight = MeasureSpec.getSize(heightMeasureSpec);
		} else {
			mHeight = mOverlayBitmap.getHeight();
		}
		mCenterX = mWidth / 2;
		mCenterY = mHeight / 2;
		setMeasuredDimension(mWidth, mHeight);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		long start = System.currentTimeMillis();
		float rotate = 0f;
		canvas.setDrawFilter(mDrawFilter);
		
		if(mBgScale < 1.0f) {
			canvas.translate(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) * mBgScale, mCenterY);
			resetScaleAndTranslateMatrix(mBgOneBitmap);
			canvas.drawBitmap(mBgOneBitmap, mMatrix, mPaint);
			
			resetScaleAndTranslateMatrix(mBgTwoBitmap);
			canvas.drawBitmap(mBgTwoBitmap, mMatrix, mPaint);
			canvas.translate(-(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) * mBgScale), -mCenterY);
			
			canvas.translate(mCenterX, mCenterY);
			resetScaleAndTranslateMatrix(mBgThreeBitmap);
			canvas.drawBitmap(mBgThreeBitmap, mMatrix, mPaint);
			canvas.translate(-mCenterX, -mCenterY);
			
			canvas.translate(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) * mBgScale, mCenterY);
			resetScaleAndTranslateMatrix(mBgFourBitmap);
			canvas.drawBitmap(mBgFourBitmap, mMatrix, mPaint);

			resetScaleAndTranslateMatrix(mBgFiveBitmap);
			canvas.drawBitmap(mBgFiveBitmap, mMatrix, mPaint);
			canvas.translate(-(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) * mBgScale), -mCenterY);
			
			canvas.translate(mCenterX, mCenterY);

			resetScaleAndTranslateMatrix(mBgSixBitmap);
			canvas.drawBitmap(mBgSixBitmap, mMatrix, mPaint);
			
			mMatrix.reset();
			mMatrix.setScale(mBgScale, mBgScale, mBgBitmap.getWidth() / 2.0f, mBgBitmap.getHeight() / 2.0f);
			canvas.translate(-mBgBitmap.getWidth() / 2.0f, -mBgBitmap.getHeight() / 2.0f);
			canvas.drawBitmap(mBgBitmap, mMatrix, mPaint);
			canvas.translate(mLightBitmap.getWidth() / 2.0f, mLightBitmap.getHeight() / 2.0f);
			
			canvas.translate(-mCenterX, -mCenterY);
			
		} else {
			canvas.translate(mLeftClockMarginLeft + mLeftClockRadius / 2.0f, mCenterY);
			canvas.drawBitmap(mBgOneBitmap, -mBgOneBitmap.getWidth() / 2.0f, -mBgOneBitmap.getHeight() / 2.0f, mPaint);
			rotate = mThreeBgRotate / 1000.0f * 360.0f;
			canvas.rotate(-rotate);
			canvas.drawBitmap(mBgTwoBitmap, -mBgTwoBitmap.getWidth()/ 2.0f, -mBgTwoBitmap.getHeight()/ 2.0f, mPaint);
			canvas.rotate(rotate);
			canvas.translate(-(mLeftClockMarginLeft + mLeftClockRadius / 2.0f), -mCenterY);
			
			canvas.translate(mCenterX, mCenterY);
			rotate = mThreeBgRotate / 1000.0f * 360.0f;
			canvas.rotate(rotate);
			canvas.drawBitmap(mBgThreeBitmap, -mBgThreeBitmap.getWidth() / 2.0f, -mBgThreeBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-rotate);
			canvas.translate(-mCenterX, -mCenterY);
			
			canvas.translate(mLeftClockMarginLeft + mLeftClockRadius / 2.0f, mCenterY);
			rotate = mFiveBgRotate / 1000.0f * 360.0f;
			canvas.rotate(-rotate);
			canvas.drawBitmap(mBgFourBitmap, -mBgFourBitmap.getWidth() / 2.0f, -mBgFourBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(rotate);

			canvas.drawBitmap(mBgFiveBitmap, -mBgFiveBitmap.getWidth() / 2.0f, -mBgFiveBitmap.getHeight() / 2.0f, mPaint);
			canvas.translate(-(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) * mBgScale), -mCenterY);
			
			
			canvas.translate(mCenterX, mCenterY);

			canvas.rotate(rotate);
			canvas.drawBitmap(mBgSixBitmap, -mBgSixBitmap.getWidth() / 2.0f, -mBgSixBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-rotate);
			
			mMatrix.reset();
			mMatrix.setScale(mBgScale, mBgScale, mBgBitmap.getWidth() / 2.0f, mBgBitmap.getHeight() / 2.0f);
			canvas.translate(-mBgBitmap.getWidth() / 2.0f, -mBgBitmap.getHeight() / 2.0f);
			canvas.drawBitmap(mBgBitmap, mMatrix, mPaint);
			canvas.translate(mLightBitmap.getWidth() / 2.0f, mLightBitmap.getHeight() / 2.0f);
			
			canvas.translate(-mCenterX, -mCenterY);
		}		
		
		if(mIsDrawPointer) {
			canvas.translate(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) *  mBgScale, mCenterY);
			mPaint.setAlpha(mSecondAlpha);
			canvas.translate(2.18f, 6);
			rotate = mSecond * 6.0f + mMilliSecond / 1000.0f * 6.0f ;
			canvas.rotate(rotate);
			canvas.drawBitmap(mSecondShadowBitmap, - mSecondShadowBitmap.getWidth() / 2.0f, - mSecondTop, mPaint);
			canvas.rotate(-rotate);
			canvas.translate(-2.18f, -6);
			
			canvas.rotate(rotate);
			canvas.drawBitmap(mSecondBitmap, - mSecondBitmap.getWidth() / 2.0f, - mSecondBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-rotate);
			canvas.drawBitmap(mSecondTrayBitmap, - mSecondTrayBitmap.getWidth() / 2.0f, - mSecondTrayBitmap.getHeight() / 2.0f, mPaint);
			canvas.translate(-(mCenterX - (mCenterX - mLeftClockMarginLeft - mLeftClockRadius / 2.0f) *  mBgScale), -mCenterY);
			
			
			canvas.translate(mCenterX + (mWidth - mRightClockMarginRight - mRightClockRadius / 2.0f - mCenterX) * mBgScale, mCenterY);
			
			mPaint.setAlpha(mHourOf24Alpha);
			canvas.translate(2.18f, 6);
			rotate = mHourOfDay * 15 - 30 + mHourOf24Rotate;
			canvas.rotate(rotate);
			canvas.drawBitmap(mHourOf24ShadowBitmap, - mHourOf24ShadowBitmap.getWidth() / 2.0f, - mHourOf24ShadowBitmap.getHeight(), mPaint);
			canvas.rotate(-rotate);
			canvas.translate(-2.18f, -6);
			
			canvas.rotate(rotate);
			canvas.drawBitmap(mHourOf24Bitmap, - mHourOf24Bitmap.getWidth() / 2.0f, - mHourOf24Bitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-rotate);
			
			canvas.drawBitmap(mHourOf24TrayBitmap, - mHourOf24TrayBitmap.getWidth() / 2.0f, - mHourOf24TrayBitmap.getHeight() / 2.0f, mPaint);
			
			canvas.translate(-(mCenterX + (mWidth - mRightClockMarginRight - mRightClockRadius / 2.0f - mCenterX) * mBgScale), -mCenterY);
		}
	
		
		canvas.translate(mCenterX, mCenterY);	
		
		if(mIsDrawPointer) {
			rotate = mHour * 30f + mMinute * 0.5f - 30f + mHourRotate;
			mPaint.setAlpha(mHourAlpha);
			canvas.translate(4.3f, 12);
			canvas.rotate(rotate);
			canvas.drawBitmap(mHourShadowBitmap, -mHourShadowBitmap.getWidth() / 2.0f, - mHourShadowBitmap.getHeight(), mPaint);
			canvas.rotate(-rotate);
			canvas.translate(-4.3f, -12);
			
			canvas.rotate(rotate);
			canvas.drawBitmap(mHourBitmap, -mHourBitmap.getWidth() / 2.0f, -mHourBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-rotate);
			
			rotate = mMinute * 6.0f + mSecond * 0.1f - 30 + mMinuteRotate;
			mPaint.setAlpha(mMinuteAlpha);
			canvas.translate(4.3f, 12);
			canvas.rotate(rotate);
			canvas.drawBitmap(mMinuteShadowBitmap, -mMinuteShadowBitmap.getWidth() / 2.0f, -mMinuteShadowBitmap.getHeight(), mPaint);
			canvas.rotate(-rotate);
			canvas.translate(-4.3f, -12);
			
			canvas.rotate(rotate);
			canvas.drawBitmap(mMinuteBitmap, -mMinuteBitmap.getWidth() / 2.0f, -mMinuteBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-rotate);
			
			mPaint.setAlpha(mTrayAlpha);
			canvas.drawBitmap(mTrayBitmap, -mTrayBitmap.getWidth() / 2.0f, -mTrayBitmap.getHeight() / 2.0f, mPaint);
		}
		
		if(mBgAlpha > 0) {
			mPaint.setColor(Color.BLACK);
			mPaint.setAlpha(mBgAlpha);
			mPaint.setStyle(Style.FILL);
			canvas.drawCircle(0, 0, mWidth / 2.0f, mPaint);
		}
		
		if(mBgScale < 1.0f) {
			mPaint.setAlpha(mLigthAlpha);
			mMatrix.reset();
			mMatrix.setScale(mBgScale, mBgScale, mLightBitmap.getWidth() / 2.0f, mLightBitmap.getHeight() / 2.0f);
			canvas.translate(-mLightBitmap.getWidth() / 2.0f, -mLightBitmap.getHeight() / 2.0f);
			canvas.drawBitmap(mLightBitmap, mMatrix, mPaint);
			canvas.translate(mLightBitmap.getWidth() / 2.0f, mLightBitmap.getHeight() / 2.0f);
		}
		
		mPaint.setAlpha(255);
		
		if(mDoorTranslate <= mMaxDoorTranslate) {
			canvas.translate(-mDoorTranslate, 0);
			canvas.drawBitmap(mLeftDoorBitmap, -mLeftDoorBitmap.getWidth() / 2.0f, -mLeftDoorBitmap.getHeight() / 2.0f, mPaint);
			canvas.translate(mDoorTranslate, 0);
			
			canvas.translate(mDoorTranslate, 0);
			canvas.drawBitmap(mRightDoorBitmap, -mRightDoorBitmap.getWidth() / 2.0f, -mRightDoorBitmap.getHeight() / 2.0f, mPaint);
			canvas.translate(-mDoorTranslate, 0);
		}
		/*
		 * ��ʼ������
		 */
		if(mDrawStream) {
			Bitmap bmp = mBitmaps.get(mBitmapIndex);
			mMatrix.reset();
			mMatrix.setScale(2.0f, 2.0f, bmp.getWidth() / 2.0f, bmp.getHeight() / 2.0f); 
			canvas.translate(-bmp.getWidth() / 2.0f, -bmp.getHeight() / 2.0f);
			canvas.drawBitmap(bmp, mMatrix, mPaint);
			canvas.translate(bmp.getWidth() / 2.0f, bmp.getHeight() / 2.0f);
		}
		
		
		if(mDoorTranslate <= mMaxDoorTranslate) {
			canvas.translate(mDoorTranslate, 0);
			canvas.drawBitmap(mDoorBitmap, -mDoorBitmap.getWidth() / 2.0f, -mDoorBitmap.getHeight() / 2.0f, mPaint);
			
			canvas.rotate(mHandleRotate);		
			canvas.drawBitmap(mDoorHandleBitmap, -mDoorHandleBitmap.getWidth() / 2.0f, -mDoorHandleBitmap.getHeight() / 2.0f, mPaint);
			canvas.rotate(-mHandleRotate);
			canvas.translate(-mDoorTranslate, 0);
		}
		
		canvas.drawBitmap(mOverlayBitmap, -mOverlayBitmap.getWidth() / 2.0f, -mOverlayBitmap.getHeight() / 2.0f, mPaint);
		
		if(mDrawHole) {
			mPaint.setColor(Color.BLACK);
			mPaint.setAlpha(mHoleAlpha);
			mPaint.setStyle(Style.FILL);
			mPaint.setStrokeWidth(0);
			canvas.drawCircle(0, 0, mWidth / 2.0f, mPaint);
			mPaint.setAlpha(255);
		}
		
		canvas.translate(-mCenterX, -mCenterY);
		
		long end = System.currentTimeMillis();
		if(end - start >= 3) {
			LogUtil.d("onDraw wasted time:" + (end - start));
		}
	}
	
	protected void ondraw(Canvas canvas) {
		float rotate = 0.0f;
		
		canvas.translate(mLeftClockMarginLeft + mLeftClockRadius / 2.0f, mCenterY);
		canvas.drawBitmap(mBgOneBitmap, -mBgOneBitmap.getWidth() / 2.0f, -mBgOneBitmap.getHeight() / 2.0f, mPaint);
		canvas.drawBitmap(mBgTwoBitmap, -mBgTwoBitmap.getWidth()/ 2.0f, -mBgTwoBitmap.getHeight()/ 2.0f, mPaint);
		
		rotate = mThreeBgRotate / 1000.0f * 360.0f;
		canvas.rotate(rotate);
		canvas.drawBitmap(mBgThreeBitmap, -mBgThreeBitmap.getWidth() / 2.0f, -mBgThreeBitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(-rotate);
		
		rotate = mFiveBgRotate / 1000.0f * 360.0f;
		canvas.rotate(-rotate);
		canvas.drawBitmap(mBgFourBitmap, -mBgFourBitmap.getWidth() / 2.0f, -mBgFourBitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(rotate);
		
		canvas.drawBitmap(mBgFiveBitmap, -mBgFiveBitmap.getWidth() / 2.0f, -mBgFiveBitmap.getHeight() / 2.0f, mPaint);
		
		canvas.translate(-(mLeftClockMarginLeft + mLeftClockRadius / 2.0f), -mCenterY);
		
		canvas.translate(mCenterX, mCenterY);
		canvas.rotate(rotate);
		canvas.drawBitmap(mBgSixBitmap, -mBgSixBitmap.getWidth() / 2.0f, -mBgSixBitmap.getHeight() / 2.0f, mPaint);
		/*canvas.rotate(-rotate);
		canvas.drawBitmap(mBgSevenBitmap, -mBgSevenBitmap.getWidth() / 2.0f, -mBgSevenBitmap.getHeight() / 2.0f, mPaint);
		canvas.translate(-mCenterX, -mCenterY);*/
		
		canvas.translate(mLeftClockMarginLeft + mLeftClockRadius / 2.0f, mCenterY);
		
		canvas.translate(2.18f, 6);
		rotate = mSecond * 6.0f + mMilliSecond / 1000.0f * 6.0f ;
		canvas.rotate(rotate);
		canvas.drawBitmap(mSecondShadowBitmap, - mSecondShadowBitmap.getWidth() / 2.0f, - mSecondTop, mPaint);
		canvas.rotate(-rotate);
		canvas.translate(-2.18f, -6);
		
		canvas.rotate(rotate);
		canvas.drawBitmap(mSecondBitmap, - mSecondBitmap.getWidth() / 2.0f, - mSecondBitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(-rotate);
		canvas.drawBitmap(mSecondTrayBitmap, - mSecondTrayBitmap.getWidth() / 2.0f, - mSecondTrayBitmap.getHeight() / 2.0f, mPaint);
		
		canvas.translate(-(mLeftClockMarginLeft + mLeftClockRadius / 2.0f), -mCenterY);
		
		canvas.translate(mWidth - mRightClockMarginRight - mRightClockRadius / 2.0f, mCenterY);
		canvas.translate(2.18f, 6);
		rotate = mHourOfDay * 15.0f;
		canvas.rotate(rotate);
		canvas.drawBitmap(mHourOf24ShadowBitmap, - mHourOf24ShadowBitmap.getWidth() / 2.0f, - mHourOf24ShadowBitmap.getHeight(), mPaint);
		canvas.rotate(-rotate);
		canvas.translate(-2.18f, -6);
		
		canvas.rotate(rotate);
		canvas.drawBitmap(mHourOf24Bitmap, - mHourOf24Bitmap.getWidth() / 2.0f, - mHourOf24Bitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(-rotate);
		
		canvas.drawBitmap(mHourOf24TrayBitmap, - mHourOf24TrayBitmap.getWidth() / 2.0f, - mHourOf24TrayBitmap.getHeight() / 2.0f, mPaint);
		canvas.translate(-(mWidth - mRightClockMarginRight - mRightClockRadius / 2.0f), -mCenterY);
		
		
		canvas.translate(mCenterX, mCenterY);
		
		rotate = mHour * 30.0f + mMinute * 0.5f;
		canvas.translate(4.3f, 12);
		canvas.rotate(rotate);
		canvas.drawBitmap(mHourShadowBitmap, -mHourShadowBitmap.getWidth() / 2.0f, - mHourShadowBitmap.getHeight(), mPaint);
		canvas.rotate(-rotate);
		canvas.translate(-4.3f, -12);
		
		canvas.rotate(rotate);
		canvas.drawBitmap(mHourBitmap, -mHourBitmap.getWidth() / 2.0f, -mHourBitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(-rotate);
		
		rotate = mMinute * 6.0f + mSecond * 0.1f;
		canvas.translate(4.3f, 12);
		canvas.rotate(rotate);
		canvas.drawBitmap(mMinuteShadowBitmap, -mMinuteShadowBitmap.getWidth() / 2.0f, -mMinuteShadowBitmap.getHeight(), mPaint);
		canvas.rotate(-rotate);
		canvas.translate(-4.3f, -12);
		
		canvas.rotate(rotate);
		canvas.drawBitmap(mMinuteBitmap, -mMinuteBitmap.getWidth() / 2.0f, -mMinuteBitmap.getHeight() / 2.0f, mPaint);
		canvas.rotate(-rotate);
		
		canvas.drawBitmap(mTrayBitmap, - mTrayBitmap.getWidth() / 2.0f, - mTrayBitmap.getHeight() / 2.0f, mPaint);
		
		canvas.translate(-mCenterX, -mCenterY);
	}
	
	private void initData() {
		mHandleRotate = 0.0f;
		mDoorTranslate = 0.0f;
		mBitmapIndex = 0;
		mDrawStream = false;
		mLigthAlpha = 0;
		mBgAlpha = 255;
		mTrayAlpha = 0;
		mBgScale = 0.85f;
		mIsDrawPointer = false;
		mMinuteAlpha = 0;
		mMinuteRotate = 0.0f;
		mHourAlpha = 0;
		mHourRotate = 0.0f;
		mTrayAlpha = 0;
		mHourOf24Alpha = 0;
		mHourOf24Rotate = 0.0f;
		mSecondAlpha = 0;
		
		
		mFiveBgRotate = 0;
		mThreeBgRotate = 0;
		mSecondRotate = 0;
		
		mDrawHole = false;
		mHoleAlpha = 0;
		
	}
	
	private void initPaint() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setStyle(Style.STROKE);
		mPaint.setAntiAlias(true);
		mPaint.setFilterBitmap(true);
		mPaint.setStrokeWidth(0);
	}
	
	private void initBitmap() {
		Resources res = getResources();

		mBgOneBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg1, mOptions);
		mBgTwoBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg2, mOptions);
		mBgThreeBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg3, mOptions);
		mBgFourBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg4, mOptions);
		mBgFiveBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg5, mOptions);
		mBgSixBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg6, mOptions);
		
		mSecondBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_second, mOptions);
		mSecondShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_second_shadow, mOptions);
		mSecondTrayBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_second_tray, mOptions);
		
		mHourOf24Bitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_hourof24, mOptions);
		mHourOf24ShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_hourof24_shadow, mOptions);
		mHourOf24TrayBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_hourof24_tray, mOptions);
		
		mHourBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_hour, mOptions);
		mHourShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_hour_shadow, mOptions);
		
		mMinuteBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_minute, mOptions);
		mMinuteShadowBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_minute_shadow, mOptions);
		mTrayBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_tray, mOptions);
		
		mBgBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_bg, mOptions);
		mLeftDoorBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_door_left, mOptions);
		mRightDoorBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_door_right, mOptions);
		mDoorBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_door, mOptions);
		mDoorHandleBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_door_handler, mOptions);
		mOverlayBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_overlay, mOptions);
		mLightBitmap = BitmapFactory.decodeResource(res, R.drawable.leather_golden_time_light, mOptions);
		
		for(int i = 0; i < mBitmapIds.length; i++) {
			Bitmap bmp = BitmapFactory.decodeResource(res, mBitmapIds[i], mOptions);
			mBitmaps.add(bmp);
		}
	}
	
	private void initTime() {
		mCalendar = Calendar.getInstance(TimeZone.getDefault());
		mHour = mCalendar.get(Calendar.HOUR);
		mHourOfDay = mCalendar.get(Calendar.HOUR_OF_DAY);
		mMinute = mCalendar.get(Calendar.MINUTE);
		mSecond = mCalendar.get(Calendar.SECOND);
		mMilliSecond = mCalendar.get(Calendar.MILLISECOND);
	}
	
	private void resetScaleAndTranslateMatrix(Bitmap bmp) {
		mMatrix.reset();
		mMatrix.setScale(mBgScale, mBgScale);
		mMatrix.postTranslate(-bmp.getWidth() / 2.0f * mBgScale, -bmp.getHeight() / 2.0f * mBgScale);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if(!mAttached) {
			mAttached = true;
			anim();
		}
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if(mAttached) {
			mAttached = false;
			mOptions = null;
			if(mBgOneBitmap != null && !mBgOneBitmap.isRecycled()) {
				mBgOneBitmap.recycle();
				mBgOneBitmap = null;
			}
			if(mBgTwoBitmap != null && !mBgTwoBitmap.isRecycled()) {
				mBgTwoBitmap.recycle();
				mBgTwoBitmap = null;
			}
			if(mBgThreeBitmap != null && !mBgThreeBitmap.isRecycled()) {
				mBgThreeBitmap.recycle();
				mBgThreeBitmap = null;
			}
			if(mBgFourBitmap != null && !mBgFourBitmap.isRecycled()) {
				mBgFourBitmap.recycle();
				mBgFourBitmap = null;
			}
			if(mBgFiveBitmap != null && !mBgFiveBitmap.isRecycled()) {
				mBgFiveBitmap.recycle();
				mBgFiveBitmap = null;
			}
			if(mBgSixBitmap != null && !mBgSixBitmap.isRecycled()) {
				mBgSixBitmap.recycle();
				mBgSixBitmap = null;
			}
			
			if(mSecondBitmap != null && !mSecondBitmap.isRecycled()) {
				mSecondBitmap.recycle();
				mSecondBitmap = null;
			}
			if(mSecondShadowBitmap != null && !mSecondShadowBitmap.isRecycled()) {
				mSecondShadowBitmap.recycle();
				mSecondShadowBitmap = null;
			}
			if(mSecondTrayBitmap != null && !mSecondTrayBitmap.isRecycled()) {
				mSecondTrayBitmap.recycle();
				mSecondTrayBitmap = null;
			}
			
			
			if(mHourOf24Bitmap != null && !mHourOf24Bitmap.isRecycled()) {
				mHourOf24Bitmap.recycle();
				mHourOf24Bitmap = null;
			}
			if(mHourOf24ShadowBitmap != null && !mHourOf24ShadowBitmap.isRecycled()) {
				mHourOf24ShadowBitmap.recycle();
				mHourOf24ShadowBitmap = null;
			}
			if(mHourOf24TrayBitmap != null && !mHourOf24TrayBitmap.isRecycled()) {
				mHourOf24TrayBitmap.recycle();
				mHourOf24TrayBitmap = null;
			}
			
			
			if(mHourBitmap != null && !mHourBitmap.isRecycled()) {
				mHourBitmap.recycle();
				mHourBitmap = null;
			}
			if(mHourShadowBitmap != null && !mHourShadowBitmap.isRecycled()) {
				mHourShadowBitmap.recycle();
				mHourShadowBitmap = null;
			}
			
			
			if(mMinuteBitmap != null && !mMinuteBitmap.isRecycled()) {
				mMinuteBitmap.recycle();
				mMinuteBitmap = null;
			}
			if(mMinuteShadowBitmap != null && !mMinuteShadowBitmap.isRecycled()) {
				mMinuteShadowBitmap.recycle();
				mMinuteShadowBitmap = null;
			}
			if(mTrayBitmap != null && !mTrayBitmap.isRecycled()) {
				mTrayBitmap.recycle();
				mTrayBitmap = null;
			}
			
			
			
			if(mBgBitmap != null && !mBgBitmap.isRecycled()) {
				mBgBitmap.recycle();
				mBgBitmap = null;
			}
			if(mLeftDoorBitmap != null && !mLeftDoorBitmap.isRecycled()) {
				mLeftDoorBitmap.recycle();
				mLeftDoorBitmap = null;
			}
			if(mRightDoorBitmap != null && !mRightDoorBitmap.isRecycled()) {
				mRightDoorBitmap.recycle();
				mRightDoorBitmap = null;
			}
			if(mDoorBitmap != null && !mDoorBitmap.isRecycled()) {
				mDoorBitmap.recycle();
				mDoorBitmap = null;
			}
			if(mDoorHandleBitmap != null && !mDoorHandleBitmap.isRecycled()) {
				mDoorHandleBitmap.recycle();
				mDoorHandleBitmap = null;
			}
			if(mOverlayBitmap != null && !mOverlayBitmap.isRecycled()) {
				mOverlayBitmap.recycle();
				mOverlayBitmap = null;
			}
			if(mLightBitmap != null && !mLightBitmap.isRecycled()) {
				mLightBitmap.recycle();
				mLightBitmap = null;
			}
			if(mBitmaps != null && mBitmaps.size() > 0) {
				for(Bitmap bmp : mBitmaps) {
					if(!bmp.isRecycled()) {
						bmp.recycle();
						bmp = null;
					}
				}
				
				mBitmaps.clear();
				mBitmaps = null;
			}
			
			if(mStartAnimator != null && mStartAnimator.isRunning()) {
				mStartAnimator.cancel();
				mStartAnimator = null;
			}
			
			if(mRunAnimator != null && mRunAnimator.isRunning()) {
				mRunAnimator.cancel();
				mRunAnimator = null;
			}
		}
	}
	
	@Override
	public void anim() {
		mStartAnimator.setTarget(this);
		mStartAnimator.start();
	}
	
	private void run() {
		mRunAnimator.setTarget(this);
		mRunAnimator.start();
	}
	
	@Override
	public void reDraw() {
		initData();
		initPaint();
		initTime();
		if(mStartAnimator.isRunning()) {
			mStartAnimator.cancel();
		}
		if(mRunAnimator.isRunning()) {
			mRunAnimator.cancel();
		}
		mStartAnimator.start();
	}
	
	@Override
	public void setPhoneAndSms(int phoneNum, int smsNum) {
		
	}

	@Override
	public void changePhoneOrSms(int phoneNum, int smsNum) {
		
	}

	public int getFiveBgRotate() {
		return mFiveBgRotate;
	}

	public void setFiveBgRotate(int FiveBgRotate) {
		this.mFiveBgRotate = FiveBgRotate;
		invalidate();
	}

	public int getThreeBgRotate() {
		return mThreeBgRotate;
	}

	public void setThreeBgRotate(int ThreeBgRotate) {
		this.mThreeBgRotate = ThreeBgRotate;
		invalidate();
	}

	public int getSecondRotate() {
		return mSecondRotate;
	}

	public void setSecondRotate(int SecondRotate) {
		this.mSecondRotate = SecondRotate;
		initTime();
		invalidate();
	}

	public float getHandleRotate() {
		return mHandleRotate;
	}

	public void setHandleRotate(float mHandleRotate) {
		this.mHandleRotate = mHandleRotate;
		mDrawHole = false;
		invalidate();
	}

	public float getDoorTranslate() {
		return mDoorTranslate;
	}

	public void setDoorTranslate(float mDoorTranslate) {
		this.mDoorTranslate = mDoorTranslate;
		LogUtil.d("mDoorTranslate:" + mDoorTranslate);
		invalidate();
	}

	public int getLigthAlpha() {
		return mLigthAlpha;
	}

	public void setLigthAlpha(int mLigthAlpha) {
		this.mLigthAlpha = mLigthAlpha;
		invalidate();
	}

	public int getBgAlpha() {
		return mBgAlpha;
	}

	public void setBgAlpha(int mBgAlpha) {
		this.mBgAlpha = mBgAlpha;
		invalidate();
	}

	public int getTrayAlpha() {
		return mTrayAlpha;
	}

	public void setTrayAlpha(int mTrayAlpha) {
		this.mTrayAlpha = mTrayAlpha;
		LogUtil.d("mTrayAlpha:" + mTrayAlpha);
		invalidate();
	}

	public float getBgScale() {
		return mBgScale;
	}

	public void setBgScale(float mBgScale) {
		this.mBgScale = mBgScale;
		invalidate();
	}

	public int getHourAlpha() {
		return mHourAlpha;
	}

	public void setHourAlpha(int mHourAlpha) {
		this.mHourAlpha = mHourAlpha;
		invalidate();
	}

	public int getMinuteAlpha() {
		return mMinuteAlpha;
	}

	public void setMinuteAlpha(int mMinuteAlpha) {
		this.mMinuteAlpha = mMinuteAlpha;
		mIsDrawPointer = true;
		invalidate();
	}

	public float getMinuteRotate() {
		return mMinuteRotate;
	}

	public void setMinuteRotate(float mMinuteRotate) {
		this.mMinuteRotate = mMinuteRotate;
		invalidate();
	}

	public float getHourRotate() {
		return mHourRotate;
	}

	public void setHourRotate(float mHourRotate) {
		this.mHourRotate = mHourRotate;
		invalidate();
	}

	public int getHourOf24Alpha() {
		return mHourOf24Alpha;
	}

	public void setHourOf24Alpha(int mHourOf24Alpha) {
		this.mHourOf24Alpha = mHourOf24Alpha;
		invalidate();
	}

	public float getHourOf24Rotate() {
		return mHourOf24Rotate;
	}

	public void setHourOf24Rotate(float mHourOf24Rotate) {
		this.mHourOf24Rotate = mHourOf24Rotate;
		invalidate();
	}

	public int getSecondAlpha() {
		return mSecondAlpha;
	}

	public void setSecondAlpha(int mSecondAlpha) {
		this.mSecondAlpha = mSecondAlpha;
		invalidate();
	}

	public int getBitmapIndex() {
		return mBitmapIndex;
	}

	public void setBitmapIndex(int mBitmapIndex) {
		this.mBitmapIndex = mBitmapIndex;
		mDrawStream = true;
		invalidate();
	}

	public int getHoleAlpha() {
		return mHoleAlpha;
	}

	public void setHoleAlpha(int mHoleAlpha) {
		this.mHoleAlpha = mHoleAlpha;
		mDrawHole = true;
		invalidate();
	}

}
