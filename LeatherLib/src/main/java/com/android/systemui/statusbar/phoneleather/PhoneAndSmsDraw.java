package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;

public class PhoneAndSmsDraw {
	//自定义属性
	private float mPhoneMarginSms;
	private float mPhoneOrSmsRadius;
	private float mPhoneOrSmsMiddleRectWidth;
	private float mPhoneOrSmsBigRectWidth;
	private float mPhoneOrSmsNumTextSize;
	private float mUpButtonMarginBottom;
	
	
	private Paint mPaint;
	private PaintFlagsDrawFilter mDrawFilter;
	
	private Matrix mMatrix;
	private float mPhoneAndSmsScale;
	private boolean mDrawPhoneAndSms;
	private float mPhoneAndSmsNumberScale;
	private int UpTranslate;
	private float UpAlpha;

	private int mWidth, mHeight;

	private int mCenterX, mCenterY;
	
	private Bitmap mBitmapPhone;
	private float xPhone, yPhone;
	private float xInitPhone;
	
	private Bitmap mBitmapSms;
	private float xSms, ySms;
	
	private Bitmap mBitmapUp;
	private float xUp, yUp;
	
	private float xPhoneNumber, yPhoneNumber;
	private float xSmsNumber, ySmsNumber;
	
	private boolean mAttached;
	
	private int MissedSmsCount = 0;
	private int MissPhoneCallCount = 0;
	
	private BitmapFactory.Options mOptions;
	
	private Context mContext;
	
	public PhoneAndSmsDraw(Context context, AttributeSet attrs) {
		
		mContext = context;
		
		TypedArray  typeArray = context.obtainStyledAttributes(attrs, R.styleable.LeatherClockView);
		
		mUpButtonMarginBottom = typeArray.getDimension(R.styleable.LeatherClockView_up_button_margin_bottom, 34 * 1.5f);
		
		xInitPhone = typeArray.getDimension(R.styleable.LeatherClockView_phone_margin_left, 101 * 1.5f);
		xPhone = xInitPhone;
		yPhone = typeArray.getDimension(R.styleable.LeatherClockView_phone_margin_top, 50 * 1.5f);
		mPhoneMarginSms = typeArray.getDimension(R.styleable.LeatherClockView_phone_margin_sms, 28 * 1.5f);
		mPhoneOrSmsRadius = typeArray.getDimension(R.styleable.LeatherClockView_phone_or_sms_num_circle_radius, 7 * 1.5f);
		mPhoneOrSmsMiddleRectWidth = typeArray.getDimension(R.styleable.LeatherClockView_phone_or_sms_middle_rect_width, 8 * 1.5f);
		mPhoneOrSmsBigRectWidth = typeArray.getDimension(R.styleable.LeatherClockView_phone_or_sms_big_rect_width, 14 * 1.5f);
		mPhoneOrSmsNumTextSize = typeArray.getDimension(R.styleable.LeatherClockView_phone_or_sms_num_textSize, 10 * 1.5f);
	
		typeArray.recycle();
		
		mOptions = new BitmapFactory.Options();
		mOptions.inPreferredConfig = Bitmap.Config.ALPHA_8;
		
		mMatrix = new Matrix();
		
		initData();
		initPaint();
		initBitmap();
	}
	
	private void initData() {
		mDrawPhoneAndSms = false;
		mPhoneAndSmsScale = 0.0f;
		mPhoneAndSmsNumberScale = 0.0f;
		
		UpTranslate = 0;
		UpAlpha = 0.0f;
	}

	private void initPaint() {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setColor(0xffee2233);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setTextSize(mPhoneOrSmsNumTextSize);
		mPaint.setStrokeWidth(0);
	}
	
	private void initBitmap() {
		final Resources resource = mContext.getResources();

		mBitmapPhone = BitmapFactory.decodeResource(resource, R.drawable.leather_phone, mOptions);
		mBitmapSms = BitmapFactory.decodeResource(resource, R.drawable.leather_message, mOptions);
		
		mBitmapUp = BitmapFactory.decodeResource(resource, R.drawable.leather_up, mOptions);
	}
	
	public void resetState() {
		initData();
		initPaint();
	}
	
	public void setPhoneAndSms(int phoneNum, int smsNum) {
		MissPhoneCallCount = phoneNum;
		MissedSmsCount = smsNum;
	}
	
	public void onAttachedToWindow() {
		if (!mAttached) {
			mAttached = true;
		}
	}

	public void onDetachedFromWindow() {
		if (mAttached) {
			mAttached = false;
			mOptions = null;
			if (mBitmapPhone != null && !mBitmapPhone.isRecycled()) {
				mBitmapPhone.recycle();
				mBitmapPhone = null;
			}

			if (mBitmapSms != null && !mBitmapSms.isRecycled()) {
				mBitmapSms.recycle();
				mBitmapSms = null;
			}
			
			if(mBitmapUp != null && !mBitmapUp.isRecycled()) {
				mBitmapUp.recycle();
				mBitmapUp = null;
			}
		}
	}
	
	public void measure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heighMode = MeasureSpec.getMode(heightMeasureSpec);
		if(widthMode == MeasureSpec.EXACTLY) {
			mWidth = MeasureSpec.getSize(widthMeasureSpec);
		} else {
			mWidth = (int) mContext.getResources().getDimension(R.dimen.leather_menu_radius);
		}
		if(heighMode == MeasureSpec.EXACTLY) {
			mHeight = MeasureSpec.getSize(heightMeasureSpec);
		} else {
			mHeight = (int) mContext.getResources().getDimension(R.dimen.leather_menu_radius);
		}
		mCenterX = mWidth / 2;
		mCenterY = mHeight / 2;
		
		xUp = mCenterX - mBitmapUp.getWidth() / 2f;
		yUp = mHeight - mUpButtonMarginBottom - mBitmapUp.getHeight();
		
	}
	
	public void draw(Canvas canvas) {
		canvas.setDrawFilter(mDrawFilter);
		
		if(mDrawPhoneAndSms) {
			mPaint.setAlpha(255);
			if(!(MissedSmsCount > 0 && MissPhoneCallCount > 0)) {
				if(MissedSmsCount > 0) {
					xSms = mCenterX - mBitmapSms.getWidth() / 2.0f;
					ySms = yPhone;
				} else if(MissPhoneCallCount > 0) {
					xPhone = mCenterX - mBitmapPhone.getWidth() / 2.0f;
				}
			} else {
				xPhone = xInitPhone;
				xSms = xPhone + mBitmapPhone.getWidth() + mPhoneMarginSms;
				ySms = yPhone;
			}
			
			if (MissPhoneCallCount > 0) {
				xPhoneNumber = xPhone + mBitmapPhone.getWidth();
				yPhoneNumber = yPhone;
			}
	
			if (MissedSmsCount > 0) {
				xSmsNumber = xSms + mBitmapSms.getWidth();
				ySmsNumber = yPhone;
			}
			
			if(MissPhoneCallCount > 0) {
				mMatrix.reset();
				mMatrix.setScale(mPhoneAndSmsScale, mPhoneAndSmsScale, mBitmapPhone.getWidth() / 2.0f, mBitmapPhone.getHeight() / 2.0f);
				canvas.translate(xPhone, yPhone);
				canvas.drawBitmap(mBitmapPhone, mMatrix, mPaint);
				canvas.translate(-xPhone, -yPhone);
			}
			if(MissedSmsCount > 0) {
				mMatrix.reset();
				mMatrix.setScale(mPhoneAndSmsScale, mPhoneAndSmsScale, mBitmapSms.getWidth() / 2.0f, mBitmapSms.getHeight() / 2.0f);
				canvas.translate(xSms, ySms);
				canvas.drawBitmap(mBitmapSms, mMatrix, mPaint);
				canvas.translate(-xSms, -ySms);
			}
			
			if (MissPhoneCallCount > 0) {
				mPaint.setColor(0xffee2233);
				if (MissPhoneCallCount < 10) {
					float radisPhone = mPhoneAndSmsNumberScale * mPhoneOrSmsRadius;
					canvas.drawCircle(xPhoneNumber, yPhoneNumber,
							radisPhone - 0.5f, mPaint);
				} else if (MissPhoneCallCount >= 10 && MissPhoneCallCount <= 99) {
					float top = yPhoneNumber - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
					float bottom = yPhoneNumber + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
					
					canvas.drawRoundRect(xPhoneNumber - mPhoneOrSmsMiddleRectWidth / 2f * mPhoneAndSmsNumberScale - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, top,
							xPhoneNumber + mPhoneOrSmsMiddleRectWidth / 2f * mPhoneAndSmsNumberScale + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, bottom,
							mPhoneOrSmsRadius, mPhoneOrSmsRadius, mPaint);

				} else if (MissPhoneCallCount >= 100) {
					float top = yPhoneNumber - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
					float bottom = yPhoneNumber + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
					canvas.drawRoundRect(xPhoneNumber - mPhoneOrSmsBigRectWidth / 2f * mPhoneAndSmsNumberScale - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, top, 
							xPhoneNumber + mPhoneOrSmsBigRectWidth / 2f * mPhoneAndSmsNumberScale + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, 
							bottom, mPhoneOrSmsRadius, mPhoneOrSmsRadius, mPaint);
				}

				if (mPhoneAndSmsNumberScale == 1.0f) {
					mPaint.setColor(Color.WHITE);
					if (MissPhoneCallCount > 99) {
						float textPhoneX = (xPhoneNumber - mPaint
								.measureText("99+") / 2f);
						float textPhoneY = (yPhoneNumber - (mPaint.descent() + mPaint
								.ascent()) / 2f);
						canvas.drawText("99+", textPhoneX, textPhoneY, mPaint);
					} else {
						float textPhoneX = (xPhoneNumber - mPaint
								.measureText(MissPhoneCallCount + "") / 2.0f);
						float textPhoneY = (yPhoneNumber - (mPaint.descent() + mPaint
								.ascent()) / 2.0f);
						canvas.drawText("" + MissPhoneCallCount, textPhoneX,
								textPhoneY, mPaint);
					}
				}
			}
			if(MissedSmsCount > 0) {
				mPaint.setColor(0xffee2233);
				if (MissedSmsCount < 10) {
					float radisPhone = mPhoneAndSmsNumberScale * mPhoneOrSmsRadius;
					canvas.drawCircle(xSmsNumber, ySmsNumber, radisPhone - 0.5f,
							mPaint);
				} else if (MissedSmsCount >= 10 && MissedSmsCount <= 99) {
					float top = ySmsNumber - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
					float bottom = ySmsNumber + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;				
					canvas.drawRoundRect(xSmsNumber - mPhoneOrSmsMiddleRectWidth / 2f * mPhoneAndSmsNumberScale - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, top, 
							xSmsNumber + mPhoneOrSmsMiddleRectWidth / 2f * mPhoneAndSmsNumberScale + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, bottom, 
							mPhoneOrSmsRadius, mPhoneOrSmsRadius, mPaint);
				} else if (MissedSmsCount >= 100) {
					float top = ySmsNumber - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
					float bottom = ySmsNumber + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale;
	
					canvas.drawRoundRect(xSmsNumber - mPhoneOrSmsBigRectWidth / 2f * mPhoneAndSmsNumberScale - mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, top, 
							xSmsNumber + mPhoneOrSmsBigRectWidth / 2f * mPhoneAndSmsNumberScale + mPhoneOrSmsRadius * mPhoneAndSmsNumberScale, bottom, 
							mPhoneOrSmsRadius, mPhoneOrSmsRadius, mPaint);
				} 
				
				if(mPhoneAndSmsNumberScale == 1.0f) {
					mPaint.setColor(Color.WHITE);
					if (MissedSmsCount > 99) {
						float textEmailX = (xSmsNumber - mPaint
								.measureText("99+") / 2f);
						float textEmailY = (ySmsNumber - (mPaint.descent() + mPaint
								.ascent()) / 2f);
						canvas.drawText("99+", textEmailX, textEmailY, mPaint);
					} else {
						float textEmailX = (xSmsNumber - mPaint
								.measureText("" + MissedSmsCount) / 2f);
						float textEmailY = (ySmsNumber - (mPaint.descent() + mPaint
								.ascent()) / 2f);
						canvas.drawText("" + MissedSmsCount, textEmailX,
								textEmailY, mPaint);

					}
				}
			}
		}
		
		float upY = yUp + 30 - UpTranslate;
		mPaint.setAlpha((int) (Math.sin(UpAlpha * Math.PI) * 255));
		canvas.drawBitmap(mBitmapUp, xUp, upY, mPaint);
		
	}
	
	public float getPhoneAndSmsScale() {
		return mPhoneAndSmsScale;
	}

	public void setPhoneAndSmsScale(float mPhoneAndSmsScale) {
		this.mPhoneAndSmsScale = mPhoneAndSmsScale;
		mDrawPhoneAndSms = true;
	}

	public float getPhoneAndSmsNumberScale() {
		return mPhoneAndSmsNumberScale;
	}

	public void setPhoneAndSmsNumberScale(float mPhoneAndSmsNumberScale) {
		this.mPhoneAndSmsNumberScale = mPhoneAndSmsNumberScale;
	}

	public int getUpTranslate() {
		return UpTranslate;
	}

	public void setUpTranslate(int upTranslate) {
		UpTranslate = upTranslate;
	}

	public float getUpAlpha() {
		return UpAlpha;
	}

	public void setUpAlpha(float upAlpha) {
		UpAlpha = upAlpha;
	}

}
