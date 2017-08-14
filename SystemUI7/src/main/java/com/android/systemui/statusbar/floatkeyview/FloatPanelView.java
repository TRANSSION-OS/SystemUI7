
package com.android.systemui.statusbar.floatkeyview;

import java.util.List;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.android.systemui.R;

/**
 * SPRD:
 */
public class FloatPanelView extends FrameLayout implements OnClickListener, OnLongClickListener {
    private static final String TAG = "FloatPanelView";
    private WindowManager mWm;
    private WindowManager.LayoutParams mLp;
    private View mKeyPanel;
    private View mAppPanel;
    private boolean mShown = false;
    private boolean mAnimating = false;
    private int[] mKeys = new int[4];
    private ComponentName[] mApps = new ComponentName[4];

    private PackageManager mPm;
    private List<ResolveInfo>  mAppSetting ;
    FloatKeyView mFloatKeyView;
    FloatKeySettings mFloatKeySettings;

    private Map<Integer, Integer> mKeyIcons = new ArrayMap<Integer, Integer>();

    private static final int HIDE_TIMEOUT = 3000;
    Handler mHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        public void run() {
            hide();
        };
    };

    public FloatPanelView(Context context) {
        super(context);

        LayoutInflater i = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mKeyPanel = i.inflate(R.layout.float_panel_key, null);
        mAppPanel = i.inflate(R.layout.float_panel_app, null);
        addView(mKeyPanel);
        addView(mAppPanel);
        mAppPanel.setVisibility(View.INVISIBLE);

        setBackgroundResource(android.R.color.transparent);

        mFloatKeySettings = new FloatKeySettings(context);
        initKeyIcons();
        //initButtons();

        mWm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mLp = new WindowManager.LayoutParams();
        mLp.width = WindowManager.LayoutParams.MATCH_PARENT;
        mLp.height = WindowManager.LayoutParams.MATCH_PARENT;
        mLp.format = PixelFormat.RGBA_8888;
        mLp.gravity = Gravity.CENTER;
        mLp.type = WindowManager.LayoutParams.TYPE_DRAG;
        mLp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mLp.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;;
        mLp.windowAnimations = R.style.FloatPanelStyle;
        mLp.setTitle("FloatPanelView");
    }

    private void initKeyIcons() {
        mKeyIcons.put(KeyEvent.KEYCODE_BACK, R.drawable.ic_floatkey_back);
        mKeyIcons.put(KeyEvent.KEYCODE_MENU, R.drawable.ic_floatkey_menu);
        mKeyIcons.put(KeyEvent.KEYCODE_SEARCH, R.drawable.ic_floatkey_search);
        mKeyIcons.put(KeyEvent.KEYCODE_HOME, R.drawable.ic_floatkey_home);
        //mKeyIcons.put(KeyEvent.KEYCODE_DEL, R.drawable.ic_floatkey_delete);
    }

    private void initButtons() {
        mKeys[0] = mFloatKeySettings.getShortcutTopValue();
        mKeys[1] = mFloatKeySettings.getShortcutLeftValue();
        mKeys[2] = mFloatKeySettings.getShortcutRightValue();
        mKeys[3] = mFloatKeySettings.getShortcutBottomValue();

        boolean isApptop = false;
        boolean isAppleft = false;
        boolean isAppright = false;
        boolean isAppbottom = false;

        mPm = mContext.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        mAppSetting = mPm.queryIntentActivities(intent, 0);
        String[] comnameInfo =new String[mAppSetting.size()];

        for(int i = 0; i <= mAppSetting.size() - 1;i ++){

            comnameInfo[i]=mAppSetting.get(i).activityInfo.packageName+":"+mAppSetting.get(i).activityInfo.name;
            /* SPRD: 595844 keep user setting for assistant @{*/
            /* SPRD: modify 20160621 Spreadtrum of 569089 :Assis default applications is same. @{*/
            /*for (int j = 0; j <= comnameInfo.length - 1; j++) {
                if (mFloatKeySettings.getAppLeftValue().equals(mFloatKeySettings.getAppTopValue())) {
                    if (comnameInfo[j] != null) {
                        mApps[0] = getApp(comnameInfo[j]);
                    }
                }
                if (mFloatKeySettings.getAppBottomValue().equals(mFloatKeySettings.getAppLeftValue())) {
                    if (comnameInfo[j] != null) {
                        mApps[1] = getApp(comnameInfo[j]);
                    }
                }
                if (mFloatKeySettings.getAppTopValue().equals(mFloatKeySettings.getAppRightValue())) {
                    if (comnameInfo[j] != null) {
                         mApps[2] = getApp(comnameInfo[j]);
                    }
                }
                if (mFloatKeySettings.getAppRightValue().equals(mFloatKeySettings.getAppBottomValue())) {
                    if (comnameInfo[j] != null) {
                        mApps[3] = getApp(comnameInfo[j]);
                    }
                }
            }*/
            /*@}*/
            /*@}*/
            if(mFloatKeySettings.getAppTopValue() .equals(comnameInfo[i])){
                isApptop = true;
                mApps[0] = getApp(mFloatKeySettings.getAppTopValue());
            }
            if(mFloatKeySettings.getAppLeftValue().equals(comnameInfo[i])){
                isAppleft = true;
                mApps[1] = getApp(mFloatKeySettings.getAppLeftValue());
            }
            if(mFloatKeySettings.getAppRightValue() .equals(comnameInfo[i])){
                isAppright = true;
                mApps[2] = getApp(mFloatKeySettings.getAppRightValue());
            }
            if(mFloatKeySettings.getAppBottomValue().equals(comnameInfo[i])){
                isAppbottom = true;
                mApps[3] = getApp(mFloatKeySettings.getAppBottomValue());
            }
        }

        if(!isApptop){
            if (comnameInfo != null) {
                mApps[0] = getApp(comnameInfo[0]);
            }
        }
        if(!isAppleft){
            if (comnameInfo != null) {
                mApps[1] = getApp(comnameInfo[2]);
            }
        }
        if(!isAppright){
            if (comnameInfo != null) {
                mApps[2] = getApp(comnameInfo[3]);
            }
        }
        if(!isAppbottom){
            if (comnameInfo != null) {
                mApps[3] = getApp(comnameInfo[1]);
            }
        }

        ImageButton button = (ImageButton) mKeyPanel.findViewById(R.id.btn_center_key);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);

        button = (ImageButton) mAppPanel.findViewById(R.id.btn_center_app);
        button.setOnClickListener(this);
        button.setOnLongClickListener(this);

        FloatKeyButton floatKeyButton = (FloatKeyButton) mKeyPanel.findViewById(R.id.btn_top_key);
        floatKeyButton.setKey(mKeys[0], mKeyIcons.get(mKeys[0]));
        floatKeyButton.setFloatPanelView(this);

        floatKeyButton = (FloatKeyButton) mKeyPanel.findViewById(R.id.btn_left_key);
        floatKeyButton.setKey(mKeys[1], mKeyIcons.get(mKeys[1]));
        floatKeyButton.setFloatPanelView(this);

        floatKeyButton = (FloatKeyButton) mKeyPanel.findViewById(R.id.btn_right_key);
        floatKeyButton.setKey(mKeys[2], mKeyIcons.get(mKeys[2]));
        floatKeyButton.setFloatPanelView(this);

        floatKeyButton = (FloatKeyButton) mKeyPanel.findViewById(R.id.btn_bottom_key);
        floatKeyButton.setKey(mKeys[3], mKeyIcons.get(mKeys[3]));
        floatKeyButton.setFloatPanelView(this);

        FloatAppButton floatAppButton = (FloatAppButton) mAppPanel.findViewById(R.id.btn_top_app);
        floatAppButton.setApp(mApps[0]);
        floatAppButton.setFloatPanelView(this);

        floatAppButton = (FloatAppButton) mAppPanel.findViewById(R.id.btn_left_app);
        floatAppButton.setApp(mApps[1]);
        floatAppButton.setFloatPanelView(this);

        floatAppButton = (FloatAppButton) mAppPanel.findViewById(R.id.btn_right_app);
        floatAppButton.setApp(mApps[2]);
        floatAppButton.setFloatPanelView(this);

        floatAppButton = (FloatAppButton) mAppPanel.findViewById(R.id.btn_bottom_app);
        floatAppButton.setApp(mApps[3]);
        floatAppButton.setFloatPanelView(this);

    }

    public void show() {
        if (!mShown) {
            initButtons();
            mWm.addView(this, mLp);
            mShown = true;
            if (mFloatKeyView != null) {
                mFloatKeyView.removeFromWindow();
            }
            mHandler.postDelayed(mHideRunnable, HIDE_TIMEOUT);
        }
    }

    public void hide() {
        if (mShown) {
            mWm.removeView(this);
            mShown = false;
            if (mFloatKeyView != null) {
                mFloatKeyView.addToWindow();
            }
            mHandler.removeCallbacks(mHideRunnable);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            hide();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mHandler.removeCallbacks(mHideRunnable);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            mHandler.postDelayed(mHideRunnable, HIDE_TIMEOUT);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_UP:
            hide();
            break;
        }
        return false;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_center_app:
            case R.id.btn_center_key:
                switchPanel(true);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.btn_center_app:
            case R.id.btn_center_key:
                hide();
                startSetting();
                return true;
        }
        return false;
    }

    private void switchPanel(boolean anim) {
        if (mAnimating)
            return;
        final View visibleView = mKeyPanel.getVisibility() == View.VISIBLE ? mKeyPanel : mAppPanel;
        final View inVisibleView = mKeyPanel.getVisibility() == View.VISIBLE ? mAppPanel
                : mKeyPanel;

        if (!anim) {
            visibleView.setVisibility(View.INVISIBLE);
            inVisibleView.setVisibility(View.VISIBLE);
            return;
        }

        mAnimating = true;
        inVisibleView.setVisibility(View.VISIBLE);

        int width = getWidth();
        int height = getHeight();

        final Animator revealAnimator =
                ViewAnimationUtils.createCircularReveal(inVisibleView,
                        width / 2, height / 2, 0.0f, width / 2);
        revealAnimator.setDuration(
                getResources().getInteger(android.R.integer.config_longAnimTime));

        final Animator alphaAnimator1 = ObjectAnimator.ofFloat(visibleView, View.ALPHA, 0.0f);
        alphaAnimator1.setDuration(
                getResources().getInteger(android.R.integer.config_mediumAnimTime));

        final Animator alphaAnimator2 = ObjectAnimator.ofFloat(inVisibleView, View.ALPHA, 1.0f);
        alphaAnimator2.setDuration(
                getResources().getInteger(android.R.integer.config_mediumAnimTime));

        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(revealAnimator, alphaAnimator1, alphaAnimator2);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                visibleView.setVisibility(View.INVISIBLE);
                mAnimating = false;
            }
        });

        animatorSet.start();
    }

    private ComponentName getApp(String str) {
        String d[] = str.split(":");
        ComponentName component = new ComponentName(d[0], d[1]);
        return component;
    }

    void setFloatKeylView(FloatKeyView floatKeyView) {
        mFloatKeyView = floatKeyView;
    }

    void startSetting() {
        Intent intent = new Intent("android.settings.ASSISTANT_HANDLE_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "start activity fail for " + intent);
        }
    }
}
