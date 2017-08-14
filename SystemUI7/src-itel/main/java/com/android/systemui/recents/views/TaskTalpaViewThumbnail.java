package com.android.systemui.recents.views;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.android.systemui.R;
import com.android.systemui.recents.model.Task;

/**
 * Created by deping.huang on 2017/1/17.
 */

public class TaskTalpaViewThumbnail extends ImageView {
    private static final String TAG = "TaskTalpaViewThumbnail";
    private Task mTask;

    private int mDisplayOrientation = Configuration.ORIENTATION_UNDEFINED;
    private Rect mDisplayRect = new Rect();

    @ViewDebug.ExportedProperty(category="recents")
    private boolean mDisabledInSafeMode;

    private ActivityManager.TaskThumbnailInfo mThumbnailInfo;

    private int mCornerRadius;
    //bo.yang1 add for CornerRadius extra to clear white edge @{
    private int CORNERRADIUSEXTRA=2;
    // @}

    public TaskTalpaViewThumbnail(Context context) {
        super(context);
        init(context);
    }

    public TaskTalpaViewThumbnail(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TaskTalpaViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public TaskTalpaViewThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        mCornerRadius  = getResources().getDimensionPixelSize(
                R.dimen.recents_task_view_rounded_corners_radius);
    }

    /**
     * Binds the thumbnail view to the task.
     */
    void bindToTask(Task t, boolean disabledInSafeMode, int displayOrientation, Rect displayRect) {
        mTask = t;
        mDisabledInSafeMode = disabledInSafeMode;
        mDisplayOrientation = displayOrientation;
        mDisplayRect.set(displayRect);
        // bo.yang1 modify for clear white edge @{
        if (t.colorBackground != 0) {
            //this.setBackgroundColor(t.colorBackground);
            //bo.yang1 add fixed cdn 10353 bug @{
            GradientDrawable gd=new GradientDrawable();
            gd.setColor(t.colorBackground);
            gd.setCornerRadius(mCornerRadius+CORNERRADIUSEXTRA);
            this.setBackgroundDrawable(gd);
        }
        //this.setBackgroundColor(android.graphics.Color.parseColor("#00ffffff"));
        // @}
    }
    /** Unbinds the thumbnail view from the task */
    void unbindFromTask() {
        mTask = null;
        setThumbnail(null, null);
    }


    /**
     * Called when the bound task's data has loaded and this view should update to reflect the
     * changes.
     */
    void onTaskDataLoaded(ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        if (mTask.thumbnail != null) {
            setThumbnail(mTask.thumbnail, thumbnailInfo);
        } else {
            setThumbnail(null, null);
        }
    }
    /**
     * Called when the task view frame changes, allowing us to move the contents of the header
     * to match the frame changes.
     */
    public void onTaskViewSizeChanged(int width, int height) {
        // Return early if the bounds have not changed

    }


    private void setThumbnail(Bitmap bm, ActivityManager.TaskThumbnailInfo thumbnailInfo) {
        if (bm != null) {
            mThumbnailInfo = thumbnailInfo;
            setAsScreenShotView(bm, this);
        } else {
            mThumbnailInfo = null;

            // 默认显示白色背景
            Log.i(TAG, "TaskTalpaViewThumbnail setThumbnail null!");
            // bo.yang1 modify for clear white edge @{
            //this.setBackgroundColor(Color.WHITE);
            //bo.yang1 add fixed cdn 10353 bug @{
            GradientDrawable gd=new GradientDrawable();
            gd.setColor(Color.WHITE);
            gd.setCornerRadius(mCornerRadius+CORNERRADIUSEXTRA);
            this.setBackgroundDrawable(gd);
        }
        //this.setBackgroundColor(android.graphics.Color.parseColor("#00ffffff"));
        // @}
    }
    private void setAsScreenShotView(Bitmap screenshot, ImageView screenshotView) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) screenshotView.getLayoutParams();
        lp.width = LinearLayout.LayoutParams.MATCH_PARENT;
        lp.height = LinearLayout.LayoutParams.MATCH_PARENT;
        // 分屏模式下的截图要显示全
        if (screenshot.getWidth() > screenshot.getHeight()){
            screenshotView.setScaleType(ScaleType.FIT_START);
        }
        else {
            screenshotView.setScaleType(ScaleType.CENTER_CROP);
        }
        screenshotView.setLayoutParams(lp);
        screenshotView.setClipToOutline(true);
        screenshotView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mCornerRadius);
            }
        });
        screenshotView.setImageBitmap(screenshot);
    }

}
