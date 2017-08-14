package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

public class LeatherMusicProgressBar extends View {
	private Paint mBackgroundPaint;
	private Paint mFrontPaint;
	private SweepGradient mSweepGradient;
	//自定义属性
	private float mProgressBarWidth;
	private int mProgressBarBackgroundColor;
	private int mProgressBarFrontStartColor;
	private int mProgressBarFrontEndColor;
	private float mProgressBarMax;
	private float mProgressBarProgress;
	
	public LeatherMusicProgressBar(Context context) {
		this(context, null);
	}
	
	public LeatherMusicProgressBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LeatherMusicProgressBar(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LeatherMusicProgressBar);
		mProgressBarWidth = typedArray.getDimension(R.styleable.LeatherMusicProgressBar_progressbar_width, 0);
		mProgressBarBackgroundColor = typedArray.getColor(R.styleable.LeatherMusicProgressBar_progressbar_background_color, 0);
		mProgressBarFrontStartColor = typedArray.getColor(R.styleable.LeatherMusicProgressBar_progressbar_front_start_color, 0);
		mProgressBarFrontEndColor = typedArray.getColor(R.styleable.LeatherMusicProgressBar_progressbar_front_end_color, 0);
		mProgressBarMax = typedArray.getFloat(R.styleable.LeatherMusicProgressBar_progressbar_max, 0);
		mProgressBarProgress = typedArray.getFloat(R.styleable.LeatherMusicProgressBar_progressbar_progress, 0);
		typedArray.recycle();		
		
		initPaint();		
	}
	
	private void initPaint() {
		mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mBackgroundPaint.setStyle(Paint.Style.STROKE);
		mBackgroundPaint.setStrokeWidth(mProgressBarWidth);
		mBackgroundPaint.setColor(mProgressBarBackgroundColor);
		
		mFrontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mFrontPaint.setStyle(Paint.Style.STROKE);
		mFrontPaint.setStrokeWidth(mProgressBarWidth);		
		
		mFrontPaint.setStrokeCap(Paint.Cap.ROUND);
		mFrontPaint.setStrokeJoin(Paint.Join.ROUND);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawArc(mProgressBarWidth / 2.0f, mProgressBarWidth / 2.0f, getWidth() - mProgressBarWidth  / 2.0f, 
				getHeight() - mProgressBarWidth  / 2.0f, 0, 360, false, mBackgroundPaint);
		canvas.save();
		canvas.rotate(-90f, getWidth() / 2.0f, getHeight() / 2.0f);		
		mSweepGradient = new SweepGradient(128f, 128f, new int[] { mProgressBarFrontStartColor, mProgressBarFrontEndColor, mProgressBarFrontStartColor }, new float[]{ 0.0f, 0.5f, 1.0f});
		mFrontPaint.setShader(mSweepGradient);
		canvas.drawArc(mProgressBarWidth / 2.0f, mProgressBarWidth / 2.0f, getWidth() - mProgressBarWidth  / 2.0f, 
				getHeight() - mProgressBarWidth  / 2.0f, 0, mProgressBarProgress / mProgressBarMax * 360f, false, mFrontPaint);
		canvas.restore();
	}
	
	public void setMax(float max) {
		mProgressBarMax = max;
	}
	
	public void setProgress(float progress) {
		mProgressBarProgress = progress;
		invalidate();
	}

}
