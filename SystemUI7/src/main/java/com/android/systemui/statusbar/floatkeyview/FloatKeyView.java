
package com.android.systemui.statusbar.floatkeyview;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.ServiceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.IWindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import com.android.systemui.R;
import android.hardware.input.InputManager;
import android.util.Slog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;

/**
 * SPRD: add for assistant touch
 */
public class FloatKeyView extends View implements OnTouchListener {
    private String TAG = "FloatKey";
    private static final String PREF_X = "FloatKeyViewX";
    private static final String PREF_Y = "FloatKeyViewY";
    private static final String POSITION = "position";
    private static final String PERCENT = "Percent";

    private Bitmap mHideIcon;
    private Bitmap mHideIconPressed;

    private Paint mPaint;
    private float mOffsetX;
    private float mOffsetY;
    private long mDownTime;

    private int mKeyWidth;
    private int mBoundWidth;

    private int mHideWidth;
    private int mHideHeight;

    private int mDragTrigger;

    private boolean mOnDrag;
    private float mDownX;
    private float mDownY;

    private WindowManager mWm;
    private WindowManager.LayoutParams mLp;
    private Handler mHandler;

    private Context mContext;
    private FloatPanelView mFloatPanelView;
    private SharedPreferences mSp;
    private boolean mShown = false;
    private int mPosition = 0;
    private float mDelta = 0;

    public FloatKeyView(Context context) {
        super(context);
        mWm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        mContext = context;
        mFloatPanelView = new FloatPanelView(context);
        mFloatPanelView.setFloatKeylView(this);
        mLp = new WindowManager.LayoutParams();
        mLp.width = LayoutParams.WRAP_CONTENT;
        mLp.height = LayoutParams.WRAP_CONTENT;
        mLp.format = PixelFormat.RGBA_8888;
        mLp.type = WindowManager.LayoutParams.TYPE_DRAG;
        mLp.flags |= LayoutParams.FLAG_NOT_FOCUSABLE;
        mLp.privateFlags |= LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        mLp.gravity = Gravity.LEFT | Gravity.TOP;
        mLp.setTitle("FloatKeyView");

        mSp = mContext.getSharedPreferences("FloatKeyView", Context.MODE_PRIVATE);
        mPosition = mSp.getInt(POSITION, 0);
        mDelta = mSp.getFloat(PERCENT, 0);

        Resources res = getResources();

        mHideIcon = BitmapFactory.decodeResource(res, R.drawable.ic_floatkey_drag_icon);
        mHideIconPressed = BitmapFactory.decodeResource(res,
                R.drawable.ic_floatkey_drag_icon_pressed);
        mHideHeight = mHideIcon.getHeight();
        mHideWidth = mHideIcon.getWidth();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setOnTouchListener(this);

        HandlerThread handlerThread = new HandlerThread("floatkey");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                KeyEvent ev = (KeyEvent) msg.obj;
                InputManager.getInstance().injectInputEvent(ev, 0);
            }

        };
    }

    public void setFloatPanelView(FloatPanelView floatPanelView) {
        mFloatPanelView = floatPanelView;
    }

    public void showFloatPanel() {
        Slog.v(TAG, "showFloatPanel");
        setVisibility(GONE);
        mFloatPanelView.show();
    }

    public void addToWindow() {
        if (!mShown) {
            setVisibility(VISIBLE);
            adjustEdge();
            mWm.addView(this, mLp);
            mShown = true;
        }
    }

    public void removeFromWindow() {
        if (mShown) {
            setVisibility(GONE);
            mWm.removeViewImmediate(this);
            mShown = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mBoundWidth = (int) (metrics.density * 7);
        mDragTrigger = (int) (metrics.density * 10);
        setMeasuredDimension(mHideWidth, mHideHeight);
    }

    @Override
    public void draw(Canvas canvas) {
        if (isPressed())
            canvas.drawBitmap(mHideIconPressed, 0, 0, null);
        else
            canvas.drawBitmap(mHideIcon, 0, 0, null);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownTime = SystemClock.uptimeMillis();

                setPressed(true);
                invalidate();

                mDownX = event.getRawX();
                mDownY = event.getRawY();

                mOffsetX = mDownX - mLp.x;
                mOffsetY = mDownY - mLp.y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mOnDrag) {
                    mLp.x = (int) (event.getRawX() - mOffsetX);
                    mLp.y = (int) (event.getRawY() - mOffsetY);
                    mWm.updateViewLayout(this, mLp);
                } else {
                    if (Math.abs(event.getRawX() - mDownX) > mDragTrigger
                            || Math.abs(event.getRawY() - mDownY) > mDragTrigger)
                        mOnDrag = true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                invalidate();
                mOnDrag = false;
                break;

            case MotionEvent.ACTION_UP:
                setPressed(false);
                invalidate();
                if (!mOnDrag) {
                    Slog.v(TAG, "ACTION_UP" + mOnDrag);
                    showFloatPanel();
                    playSoundEffect(SoundEffectConstants.CLICK);
                } else {
                    Slog.v(TAG, "ACTION_UP" + mOnDrag);
                    mOnDrag = false;
                    DisplayMetrics metrics = getResources().getDisplayMetrics();
                    int screenWidth = metrics.widthPixels;
                    int screenHeight = metrics.heightPixels;
                    int xGap = screenWidth - mLp.x - getWidth();
                    int yGap = screenHeight - mLp.y - getHeight();
                    int minist = Math.min(Math.min(mLp.x, mLp.y), Math.min(xGap, yGap));
                    Log.d(TAG, minist + "");
                    if (mLp.x == minist) {
                        mLp.x = 0;
                        mPosition = 0;
                        mDelta = (float) mLp.y / screenHeight;
                    } else if (mLp.y == minist) {
                        mLp.y = 0;
                        mPosition = 1;
                        mDelta = (float) mLp.x / screenWidth;
                    } else if (xGap == minist) {
                        mLp.x = screenWidth - getWidth();
                        mPosition = 2;
                        mDelta = (float) mLp.y / screenHeight;
                    } else {
                        mLp.y = screenHeight - getHeight();
                        mPosition = 3;
                        mDelta = (float) mLp.x / screenWidth;
                    }
                    mWm.updateViewLayout(this, mLp);
                    SharedPreferences.Editor editor = mSp.edit();
                    editor.putInt(POSITION, mPosition);
                    editor.putFloat(PERCENT, mDelta);
                    editor.commit();
                }
                break;
        }
        return false;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        adjustEdge();
        super.onConfigurationChanged(newConfig);
    }

    private void adjustEdge() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;

        switch (mPosition) {
            case 0:
                mLp.x = 0;
                mLp.y = (int) (screenHeight * mDelta);
                break;
            case 1:
                mLp.x = (int) (screenWidth * mDelta);
                mLp.y = 0;
                break;
            case 2:
                mLp.x = screenWidth - getWidth();
                mLp.y = (int) (mDelta * screenHeight);
                break;
            case 3:
                mLp.x = (int) (mDelta * screenWidth);
                mLp.y = screenHeight - getHeight();
                break;
        }
        if (mShown) {
            mWm.updateViewLayout(this, mLp);
        }

    }
}
