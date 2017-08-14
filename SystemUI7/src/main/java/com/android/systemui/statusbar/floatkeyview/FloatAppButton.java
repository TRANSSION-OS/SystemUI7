
package com.android.systemui.statusbar.floatkeyview;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;

/**
 * SPRD:
 */
public class FloatAppButton extends ImageButton implements OnClickListener {
    private static final String TAG = "FloatAppButton";

    private ComponentName mApp;
    private Context mContext;
    FloatPanelView mFloatPanelView;

    public FloatAppButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setOnClickListener(this);
        setEnabled(false);
    }

    @Override
    public void onClick(View v) {
        mFloatPanelView.hide();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(mApp);
        try {
            mContext.startActivityAsUser(intent, new UserHandle(UserHandle.USER_CURRENT));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "start activity fail for " + mApp);
        }

    }

    public void setApp(ComponentName component) {
        mApp = component;
        Drawable icon = getAppIcon(component);
        setImageDrawable(icon);
        setAdjustViewBounds(true);
        if (icon == null) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }

    }

    private Drawable getAppIcon(ComponentName component) {
        PackageManager pm = mContext.getPackageManager();
        Drawable icon = null;
        Bitmap image = null;
        try {
            icon = pm.getActivityIcon(component);
            if (icon instanceof BitmapDrawable) {
                BitmapDrawable bpd = (BitmapDrawable) icon;
                image = bpd.getBitmap();
                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager wm = (WindowManager) mContext
                        .getSystemService(Context.WINDOW_SERVICE);
                wm.getDefaultDisplay().getMetrics(metrics);
                image = Bitmap.createScaledBitmap(image, (int) (88 * metrics.density),
                        (int) (88 * metrics.density), false);
                return new BitmapDrawable(image);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "getAppIcon fail for " + component);
        }
        return icon;
    }
    public void setFloatPanelView(FloatPanelView floatPanelView) {
        mFloatPanelView = floatPanelView;
    }

}
