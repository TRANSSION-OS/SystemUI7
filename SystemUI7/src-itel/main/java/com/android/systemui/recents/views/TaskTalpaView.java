/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.LaunchTalpaTaskEvent;
import com.android.systemui.recents.events.ui.ShowApplicationInfoEvent;
import com.android.systemui.recents.events.ui.ShowIncompatibleAppOverlayEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragTalpaEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragTalpaEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragTalpaStartEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import java.util.ArrayList;

import itel.transsion.settingslib.utils.LogUtil;

import static android.app.ActivityManager.StackId.INVALID_STACK_ID;
import static android.app.ActivityManager.StackId.tasksAreFloating;

/**
 * A {@link TaskTalpaView} represents a fixed view of a task. Because the TaskView's layout is directed
 * solely by the {@link TaskStackView}, we make it a fixed size layout which allows relayouts down
 * the view hierarchy, but not upwards from any of its children (the TaskView will relayout itself
 * with the previous bounds if any child requests layout).
 */
public class TaskTalpaView extends RelativeLayout implements Task.TaskCallbacks, View.OnClickListener, View.OnLongClickListener {

    /**
     * The dim overlay is generally calculated from the task progress, but occasionally (like when
     * launching) needs to be animated independently of the task progress.
     */
    public static final Property<TaskTalpaView, Float> DIM_ALPHA =
            new FloatProperty<TaskTalpaView>("dimAlpha") {
                @Override
                public void setValue(TaskTalpaView tv, float dimAlpha) {
                    tv.setDimAlpha(dimAlpha);
                }

                @Override
                public Float get(TaskTalpaView tv) {
                    return tv.getDimAlpha();
                }
            };

    /**
     * The dim overlay is generally calculated from the task progress, but occasionally (like when
     * launching) needs to be animated independently of the task progress.
     */
    public static final Property<TaskTalpaView, Float> VIEW_OUTLINE_ALPHA =
            new FloatProperty<TaskTalpaView>("viewOutlineAlpha") {
                @Override
                public void setValue(TaskTalpaView tv, float alpha) {
                    tv.getViewBounds().setAlpha(alpha);
                }

                @Override
                public Float get(TaskTalpaView tv) {
                    return tv.getViewBounds().getAlpha();
                }
            };


    @ViewDebug.ExportedProperty(category="recents")
    private float mDimAlpha;
    //private float mActionButtonTranslationZ;

    @ViewDebug.ExportedProperty(deepExport=true, prefix="task_")
    private Task mTask;

    @ViewDebug.ExportedProperty(category="recents")
    private boolean mTouchExplorationEnabled;
    @ViewDebug.ExportedProperty(category="recents")
    boolean mScreenPinningEnabled;
    @ViewDebug.ExportedProperty(category="recents")
    private boolean mIsDisabledInSafeMode;
    @ViewDebug.ExportedProperty(deepExport=true, prefix="view_bounds_")
    private AnimateableViewBounds mViewBounds;

    @ViewDebug.ExportedProperty(category="recents")
    private android.graphics.Point mDownTouchPos = new android.graphics.Point();

    @ViewDebug.ExportedProperty(deepExport=true, prefix="thumbnail_")
    TaskTalpaViewThumbnail mThumbnailView;
    //@ViewDebug.ExportedProperty(deepExport=true, prefix="header_")
    //TaskViewHeader mHeaderIcon;
    private View mHeaderLayout;
    private ImageView mHeaderIcon;
    private TextView mHeaderTitle;
    private View mThumbContainerLayout;
    private View mOperatorLayout;
    private TextView mBtnLockTask, mBtnSplitScreen;
    /**
     * 当前垂直方向的移动距离
     */
    private int mControlOffsetY;

    //private View mActionButtonView;
    private View mIncompatibleAppToastView;
    ImageView mLockIconView;
    private Toast mDisabledAppToast;

    private View mActionButtonView;
    private float mActionButtonTranslationZ;

    // Talpa bo.yang1 add for delete Animator @{
    private ObjectAnimator mDimAnimator;
    private ObjectAnimator mOutlineAnimator;
    private AnimatorSet mTransformAnimation;
    private final TaskTalpaViewTransform mTargetAnimationTransform = new TaskTalpaViewTransform();
    private ArrayList<Animator> mTmpAnimators = new ArrayList<>();

    public TaskTalpaView(Context context) {
        this(context, null);
    }

    public TaskTalpaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TaskTalpaView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TaskTalpaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        RecentsConfiguration config = Recents.getConfiguration();
        Resources res = context.getResources();
        mViewBounds = new AnimateableViewBounds(this, res.getDimensionPixelSize(
                R.dimen.recents_task_view_shadow_rounded_corners_radius));
        if (config.fakeShadows) {
            setBackground(new FakeShadowDrawable(res, config));
        }
        setOutlineProvider(mViewBounds);
        // Talpa:peterHuang comment for use new feature@{
        // setOnLongClickListener(this);
        // @}
    }

    /**
     * Called from RecentsActivity when it is relaunched.
     */
    void onReload(boolean isResumingFromVisible) {
        //resetNoUserInteractionState();
        if (!isResumingFromVisible) {
            resetViewProperties();
        }
    }


    public void setHeaderViewVisibility(int visibility){
        mHeaderLayout.setVisibility(visibility);
    }

    /** Gets the task */
    public Task getTask() {
        return mTask;
    }

    /** Returns the view bounds. */
    AnimateableViewBounds getViewBounds() {
        return mViewBounds;
    }

    @Override
    protected void onFinishInflate() {
        // Bind the views
        mHeaderLayout = findViewById(R.id.layout_header);
        mHeaderLayout.setOnClickListener(this);
        mHeaderIcon = (ImageView) findViewById(R.id.icon_task);
        mHeaderTitle = (TextView) findViewById(R.id.icon_title);
        mThumbContainerLayout = findViewById(R.id.view_thumbnail_container);
        mThumbContainerLayout.setOnClickListener(this);
        mOperatorLayout = findViewById(R.id.layout_operator);
        mThumbnailView = (TaskTalpaViewThumbnail) findViewById(R.id.task_view_thumbnail);
        mBtnLockTask = (TextView)findViewById(R.id.btn_lock);
        mBtnLockTask.setOnClickListener(this);
        mBtnSplitScreen = (TextView)findViewById(R.id.btn_split);
        mBtnSplitScreen.setOnClickListener(this);
        //mThumbnailView.updateClipToTaskBar(mHeaderIcon);
        /* SPRD: Bug 535096 new feature of lock recent apps @{ */
        mLockIconView = (ImageView)findViewById(R.id.app_lock_image);
        mLockIconView.setVisibility(View.INVISIBLE);
        /* @} */


        mActionButtonView = findViewById(R.id.lock_to_app_fab);
        mActionButtonView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                // Set the outline to match the FAB background
                outline.setOval(0, 0, mActionButtonView.getWidth(), mActionButtonView.getHeight());
                outline.setAlpha(0.35f);
            }
        });
        mActionButtonView.setOnClickListener(this);
        mActionButtonTranslationZ = mActionButtonView.getTranslationZ();

    }

    /**
     * Update the task view when the configuration changes.
     */
    void onConfigurationChanged() {
       // mHeaderIcon.onConfigurationChanged();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
           // mHeaderIcon.onTaskViewSizeChanged(w, h);
            mThumbnailView.onTaskViewSizeChanged(w, h);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }



    /** Resets this view's properties */
    void resetViewProperties() {
        cancelTransformAnimation();
        setDimAlpha(0);
        setVisibility(View.VISIBLE);
        getViewBounds().reset();
        //getHeaderView().reset();

        mActionButtonView.setScaleX(1f);
        mActionButtonView.setScaleY(1f);
        mActionButtonView.setAlpha(0f);
        mActionButtonView.setTranslationX(0f);
        mActionButtonView.setTranslationY(0f);
        mActionButtonView.setTranslationZ(mActionButtonTranslationZ);
        if (mIncompatibleAppToastView != null) {
            mIncompatibleAppToastView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Cancels any current transform animations.
     */
    public void cancelTransformAnimation() {
/*        Utilities.cancelAnimationWithoutCallbacks(mTransformAnimation);
        Utilities.cancelAnimationWithoutCallbacks(mDimAnimator);
        Utilities.cancelAnimationWithoutCallbacks(mOutlineAnimator);*/
    }

    /** Enables/disables handling touch on this task view. */
    void setTouchEnabled(boolean enabled) {
        // TALPA: DepingHuang  comment  add start{
        //setOnClickListener(enabled ? this : null);
        // TALPA: DepingHuang    add end}
    }




    /** Sets whether this view should be clipped, or clipped against. */
    void setClipViewInStack(boolean clip) {
       /* if (clip != mClipViewInStack) {
            mClipViewInStack = clip;
            if (mCb != null) {
                mCb.onTaskViewClipStateChanged(this);
            }
        }*/
    }


    /**
     * Sets the current dim.
     */
    public void setDimAlpha(float dimAlpha) {
        mDimAlpha = dimAlpha;
        //mThumbnailView.setDimAlpha(dimAlpha);
        //mHeaderIcon.setDimAlpha(dimAlpha);
    }


    /**
     * Returns the current dim.
     */
    public float getDimAlpha() {
        return mDimAlpha;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDownTouchPos.set((int) (ev.getX() * getScaleX()), (int) (ev.getY() * getScaleY()));
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**** TaskCallbacks Implementation ****/

    public void onTaskBound(Task t, boolean touchExplorationEnabled, int displayOrientation,
            Rect displayRect) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        mTouchExplorationEnabled = touchExplorationEnabled;
        mScreenPinningEnabled = ssp.getSystemSetting(getContext(),
                Settings.System.LOCK_TO_APP_ENABLED) != 0;
        mTask = t;
        mTask.addCallback(this);
        mIsDisabledInSafeMode = !mTask.isSystemApp && ssp.isInSafeMode();
        mThumbnailView.bindToTask(mTask, mIsDisabledInSafeMode, displayOrientation, displayRect);
        //mHeaderIcon.bindToTask(mTask, mTouchExplorationEnabled, mIsDisabledInSafeMode);

        if (!t.isDockable && ssp.hasDockedTask()) {
            if (mIncompatibleAppToastView == null) {
                mIncompatibleAppToastView = Utilities.findViewStubById(this,
                        R.id.incompatible_app_toast_stub).inflate();
                TextView msg = (TextView) findViewById(R.id.message);
                msg.setText(R.string.recents_incompatible_app_message);
            }
            mIncompatibleAppToastView.setVisibility(View.VISIBLE);
            /* SPRD: Bug 601307 Ensure toast view visible @{ */
            requestLayout();
            /* @} */
        } else if (mIncompatibleAppToastView != null) {
            mIncompatibleAppToastView.setVisibility(View.INVISIBLE);
        }

        mHeaderTitle.setText(mTask.title);

        if (ssp.hasDockedTask()){
            mHeaderLayout.setVisibility(View.GONE);
            mOperatorLayout.setVisibility(View.GONE);
        }
        else{
            mHeaderLayout.setVisibility(View.VISIBLE);
            mOperatorLayout.setVisibility(View.VISIBLE);
        }


        if (mScreenPinningEnabled) {
            this.showActionButton(false /* fadeIn */, 0 /* fadeInDuration */);
        }
        else {
            this.hideActionButton(false /* fadeOut */, 0 /* duration */, false /* scaleDown */, null);
        }

        resetStatus();
    }

    @Override
    public void onTaskDataLoaded(Task task, ActivityManager.TaskThumbnailInfo thumbnailInfo) {

        if (task.key.id != mTask.key.id) {
            //LogUtil.d("task unequals：current is "+ mTask.title + " != " + task.title);
            return;
        }
        //LogUtil.d("--------------task equals: " + task.title);

        // Update each of the views to the new task data
        mThumbnailView.onTaskDataLoaded(thumbnailInfo);

        if (null != task.icon)
            mHeaderIcon.setImageDrawable(task.icon);
        //mHeaderIcon.onTaskDataLoaded();

        /* SPRD: Bug 535096 new feature of lock recent apps @{ */
        if(PhoneStatusBar.mSupportLockApp) {
            if (task.isLocked) {
                mLockIconView.setVisibility(VISIBLE);
            } else {
                mLockIconView.setVisibility(INVISIBLE);
            }
            setLockOperatorBtnStatus(task.isLocked);
        }
        /* @} */

        if (task.isDockable){
            mBtnSplitScreen.setVisibility(VISIBLE);
        }
        else {
            mBtnSplitScreen.setVisibility(GONE);
        }

    }

    @Override
    public void onTaskDataUnloaded() {
        // Unbind each of the views from the task and remove the task callback
        //LogUtil.d("==========task unbind: "+mTask.title);
        mTask.removeCallback(this);
        mThumbnailView.unbindFromTask();
        mHeaderIcon.setImageDrawable(null);
        mTask = null;
        //mHeaderIcon.unbindFromTask(mTouchExplorationEnabled);
    }

    @Override
    public void onTaskStackIdChanged() {
        // Force rebind the header, the thumbnail does not change due to stack changes
        //mHeaderIcon.bindToTask(mTask, mTouchExplorationEnabled, mIsDisabledInSafeMode);
        //mHeaderIcon.onTaskDataLoaded();
    }

    /**** View.OnClickListener Implementation ****/

    @Override
     public void onClick(final View v) {
        if (mIsDisabledInSafeMode) {
            Context context = getContext();
            String msg = context.getString(R.string.recents_launch_disabled_message, mTask.title);
            if (mDisabledAppToast != null) {
                mDisabledAppToast.cancel();
            }
            mDisabledAppToast = Toast.makeText(context, msg, Toast.LENGTH_SHORT);
            mDisabledAppToast.show();
            return;
        }


        if (v == mHeaderLayout) {
            // In accessibility, a single click on the focused app info button will show it
            EventBus.getDefault().send(new ShowApplicationInfoEvent(mTask));
        }
        else if (v == mThumbContainerLayout){
            LogUtil.d("mThumbContainerLayout");
            try {

                EventBus.getDefault().send(new LaunchTalpaTaskEvent(this, mTask,
                        null, INVALID_STACK_ID,false));
            } catch (Exception e) {
                LogUtil.e(v.getContext().getString(R.string.recents_launch_error_message, mTask.title), e);
            }
        }
        else if (v == mBtnLockTask){
            LogUtil.d("mBtnLockTask");
            onLockIcon();
        }
        else if (v == mBtnSplitScreen){
           LogUtil.d("mBtnSplitScreen");
            mThumbContainerLayout.setTranslationY(0);
           goToDockMode(mTask);
        }
        else if (v == mActionButtonView){
            LogUtil.d("mActionButtonView");
            mActionButtonView.setTranslationZ(0f);
            try {

                EventBus.getDefault().send(new LaunchTalpaTaskEvent(this, mTask,
                        null, INVALID_STACK_ID, true));
            } catch (Exception e) {
                LogUtil.e(v.getContext().getString(R.string.recents_launch_error_message, mTask.title), e);
            }
        }

        // TALPA: DepingHuang  comment  add start{
//        EventBus.getDefault().send(new LaunchTaskEvent(this, mTask, null, INVALID_STACK_ID,
//                screenPinningRequested));
        // TALPA: DepingHuang    add end}

        MetricsLogger.action(v.getContext(), MetricsEvent.ACTION_OVERVIEW_SELECT,
                mTask.key.getComponent().toString());
    }


    /**** Events ****/
    public final void onBusEvent(DragTalpaEndEvent event) {
        if (!(event.dropTarget instanceof TaskStack.DockState)) {
            event.addPostAnimationCallback(new Runnable() {
				@Override
				public void run() {
				    // Reset the clip state for the drag view after the end animation completes
				    setClipViewInStack(true);
				}
			});
        }
        EventBus.getDefault().unregister(this);
    }

    public final void onBusEvent(DragTalpaEndCancelledEvent event) {
        // Reset the clip state for the drag view after the cancel animation completes
        event.addPostAnimationCallback(new Runnable() {
			@Override
			public void run() {
			    setClipViewInStack(true);
			}
		});
    }

    /* SPRD: Bug 535096 new feature of lock recent apps @{ */
    public void onLockIcon() {
        SharedPreferences sharedPreference =
                PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        Editor editor = sharedPreference.edit();
        if (mTask.isLocked == false) {
            mTask.isLocked = true;
            // 显示锁动画 @{
            mLockIconView.setVisibility(View.VISIBLE);
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mLockIconView,"scaleX",0,1.5f,1);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mLockIconView,"scaleY",0,1.5f,1);
            animatorSet.setDuration(500);
            animatorSet.setInterpolator(new DecelerateInterpolator());
            animatorSet.play(scaleX).with(scaleY);
            animatorSet.start();
            // @}
            setLockOperatorBtnStatus(mTask.isLocked);
            resetStatus();
            // Talpa:bo.yang1 add @{
            alphaMemoryTvAndClearBtn(mHeaderLayout);
            //@}
            /*SPRD:bugfix 596961 @{*/
            //mHeaderIcon.mDismissButton.setVisibility(View.INVISIBLE);
            /*SPRD:bugfix 596961 @}*/
            editor.putInt(mTask.key.toStringKey(), mTask.key.id);
            editor.apply();
        } else {
            mTask.isLocked = false;
            // 关闭锁动画 @{
            AnimatorSet animatorSet = new AnimatorSet();
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(mLockIconView,"scaleX",1f,0);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(mLockIconView,"scaleY",1f,0);
            animatorSet.setDuration(200);
            animatorSet.setInterpolator(new AccelerateInterpolator());
            animatorSet.play(scaleX).with(scaleY);
            animatorSet.start();
            //mLockIconView.setVisibility(View.INVISIBLE);
            // @}
            setLockOperatorBtnStatus(mTask.isLocked);
            resetStatus();
            // Talpa:bo.yang1 add @{
            alphaMemoryTvAndClearBtn(mHeaderLayout);
            //@}
            /*SPRD:bugfix 596961 @{*/
            //mHeaderIcon.mDismissButton.setVisibility(View.VISIBLE);
            /*SPRD:bugfix 596961 @}*/
            editor.remove(mTask.key.toStringKey());
            editor.apply();
        }
    }
    private void setLockOperatorBtnStatus(boolean isLocked){
        if (isLocked){
            Drawable top = getResources().getDrawable(R.drawable.talpa_ic_recents_action_unlock,null);
            mBtnLockTask.setCompoundDrawablesWithIntrinsicBounds(null, top , null, null);
            mBtnLockTask.setText(R.string.unlock_task);
        }
        else {
            Drawable top = getResources().getDrawable(R.drawable.talpa_ic_recents_action_lock,null);
            mBtnLockTask.setCompoundDrawablesWithIntrinsicBounds(null, top , null, null);
            mBtnLockTask.setText(R.string.lock_task);
        }
    }
    public void cleanLockedStatus(){
        mTask.isLocked = false;
        mLockIconView.setVisibility(View.INVISIBLE);
        SharedPreferences sharedPreference =
                PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        Editor editor = sharedPreference.edit();
        editor.remove(mTask.key.toStringKey());
        editor.apply();
    }

    public boolean isLockedIcon() {
        // Task mTask = getTask();
        if (mTask == null) {
            return false;
        }
        return mTask.isLocked;
    }
    /* @} */

    // 恢复控件状态
    public void resetStatus(){
        setControlOffsetY(0);
        mThumbContainerLayout.setTranslationY(0);
        mHeaderLayout.setTranslationY(0);
        mHeaderLayout.setAlpha(1.0f);
    }

    // Talpa:bo.yang1 add @{
    private void alphaMemoryTvAndClearBtn(View mItemView){
        ViewGroup parentView=(ViewGroup) mItemView.getParent();

        while (parentView.getId()!=R.id.recents_view){
            parentView=(ViewGroup)parentView.getParent();
        }
        parentView=(ViewGroup)parentView.getParent();
        for(int i=0; i < parentView.getChildCount(); i++){
            View child = parentView.getChildAt(i);
            if(child.getId()==R.id.memory_show || child.getId()==R.id.clear_button){
                if(child.getAlpha() != 1.0f) {
                    child.setAlpha(1.0f);
                }
            }
        }
    }
    //@}

    public View getThumbContainerLayout(){
        return mThumbContainerLayout;
    }
    public View getHeaderLayout(){
        return mHeaderLayout;
    }
    public View getOperatorLayout(){
        return mOperatorLayout;
    }

    public static int getHeadViewID(){
        return R.id.layout_header;
    }
    /**
     * 获取点击task的区域(包括不可视区域)
     * @return
     */
    public Rect getFocusedThumbnailRect() {
        Rect r = new Rect();
        mThumbnailView.getGlobalVisibleRect(r);

        // 如果r为部分可视，修正rect的大小
        if (r.width() != mThumbnailView.getWidth()) {
            r.right = r.left + mThumbnailView.getWidth();
        }

        return r;
    }





    private ArrayList<DropTarget> mDropTargets = new ArrayList<>();
    private ArrayList<TaskStack.DockState> mVisibleDockStates = new ArrayList<>();
    @Override
    public boolean onLongClick(View v) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        // Since we are clipping the view to the bounds, manually do the hit test
        Rect clipBounds = new Rect(mViewBounds.mClipBounds);
        clipBounds.scale(getScaleX());
        boolean inBounds = clipBounds.contains(mDownTouchPos.x, mDownTouchPos.y);
        if (v == this /*&& inBounds*/ && !ssp.hasDockedTask()) {
            // Start listening for drag events
            setClipViewInStack(false);

            mDownTouchPos.x += ((1f - getScaleX()) * getWidth()) / 2;
            mDownTouchPos.y += ((1f - getScaleY()) * getHeight()) / 2;

            EventBus.getDefault().register(this, RecentsActivity.EVENT_BUS_PRIORITY + 1);
            EventBus.getDefault().send(new DragTalpaStartEvent(mTask, this, mDownTouchPos));
            return true;
        }
        LogUtil.d("on Long Click....");
        return false;
    }

    /**
     * 跳转至分屏模式
     */
    private  void goToDockMode(Task task){
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ActivityManager.supportsMultiWindow() && !ssp.hasDockedTask()) {
            Recents.logDockAttempt(getContext(), task.getTopComponent(),
                    task.resizeMode);
            if (!task.isDockable) {
                EventBus.getDefault().send(new ShowIncompatibleAppOverlayEvent());
            } else {
                // Add the dock state drop targets (these take priority)
                TaskStack.DockState[] dockStates = getDockStatesForCurrentOrientation();
                for (TaskStack.DockState dockState : dockStates) {
                    registerDropTargetForCurrentDrag(dockState);
                    dockState.update(getContext());
                    mVisibleDockStates.add(dockState);
                }
                EventBus.getDefault().send(new DragTalpaEndEvent(mTask, this, mDropTargets.get(0)));

            }
        }
    }

    /**
     * Registers a new drop target for the current drag only.
     */
    public void registerDropTargetForCurrentDrag(DropTarget target) {
        mDropTargets.add(target);
    }

    /**
     * Returns the preferred dock states for the current orientation.
     */
    public TaskStack.DockState[] getDockStatesForCurrentOrientation() {
        boolean isLandscape = this.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        RecentsConfiguration config = Recents.getConfiguration();
        TaskStack.DockState[] dockStates = isLandscape ?
                (config.isLargeScreen ? DockRegion.TABLET_LANDSCAPE : DockRegion.PHONE_LANDSCAPE) :
                (config.isLargeScreen ? DockRegion.TABLET_PORTRAIT : DockRegion.PHONE_PORTRAIT);
        return dockStates;
    }

    public int getControlOffsetY() {
        return mControlOffsetY;
    }

    public void setControlOffsetY(int controlOffsetY) {
        mControlOffsetY = controlOffsetY;
    }


    /**
     * Shows the action button.
     * @param fadeIn whether or not to animate the action button in.
     * @param fadeInDuration the duration of the action button animation, only used if
     *                       {@param fadeIn} is true.
     */
    public void showActionButton(boolean fadeIn, int fadeInDuration) {
        mActionButtonView.setVisibility(View.VISIBLE);

        if (fadeIn && mActionButtonView.getAlpha() < 1f) {
            mActionButtonView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(fadeInDuration)
                    .setInterpolator(Interpolators.ALPHA_IN)
                    .start();
        } else {
            mActionButtonView.setScaleX(1f);
            mActionButtonView.setScaleY(1f);
            mActionButtonView.setAlpha(1f);
            mActionButtonView.setTranslationZ(mActionButtonTranslationZ);
        }
    }

    /**
     * Immediately hides the action button.
     *
     * @param fadeOut whether or not to animate the action button out.
     */
    public void hideActionButton(boolean fadeOut, int fadeOutDuration, boolean scaleDown,
                                 final Animator.AnimatorListener animListener) {
        if (fadeOut && mActionButtonView.getAlpha() > 0f) {
            if (scaleDown) {
                float toScale = 0.9f;
                mActionButtonView.animate()
                        .scaleX(toScale)
                        .scaleY(toScale);
            }
            mActionButtonView.animate()
                    .alpha(0f)
                    .setDuration(fadeOutDuration)
                    .setInterpolator(Interpolators.ALPHA_OUT)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (animListener != null) {
                                animListener.onAnimationEnd(null);
                            }
                            mActionButtonView.setVisibility(View.INVISIBLE);
                        }
                    })
                    .start();
        } else {
            mActionButtonView.setAlpha(0f);
            mActionButtonView.setVisibility(View.INVISIBLE);
            if (animListener != null) {
                animListener.onAnimationEnd(null);
            }
        }
    }

    /**
     * @return whether we are animating towards {@param transform}
     */
    boolean isAnimatingTo(TaskTalpaViewTransform transform) {
        return mTransformAnimation != null && mTransformAnimation.isStarted()
                && mTargetAnimationTransform.isSame(transform);
    }

    void updateViewPropertiesToTaskTransform(TaskTalpaViewTransform toTransform,
                                             AnimationProps toAnimation, ValueAnimator.AnimatorUpdateListener updateCallback) {
        RecentsConfiguration config = Recents.getConfiguration();
        cancelTransformAnimation();

        // Compose the animations for the transform
        mTmpAnimators.clear();
        toTransform.applyToTaskView(this, mTmpAnimators, toAnimation, !config.fakeShadows);
        if (toAnimation.isImmediate()) {
            if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
                setDimAlpha(toTransform.dimAlpha);
            }
            if (Float.compare(mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
                mViewBounds.setAlpha(toTransform.viewOutlineAlpha);
            }
            // Manually call back to the animator listener and update callback
            if (toAnimation.getListener() != null) {
                toAnimation.getListener().onAnimationEnd(null);
            }
            if (updateCallback != null) {
                updateCallback.onAnimationUpdate(null);
            }
        } else {
            // Both the progress and the update are a function of the bounds movement of the task
            if (Float.compare(getDimAlpha(), toTransform.dimAlpha) != 0) {
                mDimAnimator = ObjectAnimator.ofFloat(this, DIM_ALPHA, getDimAlpha(),
                        toTransform.dimAlpha);
                mTmpAnimators.add(toAnimation.apply(AnimationProps.BOUNDS, mDimAnimator));
            }
            if (Float.compare(mViewBounds.getAlpha(), toTransform.viewOutlineAlpha) != 0) {
                mOutlineAnimator = ObjectAnimator.ofFloat(this, VIEW_OUTLINE_ALPHA,
                        mViewBounds.getAlpha(), toTransform.viewOutlineAlpha);
                mTmpAnimators.add(toAnimation.apply(AnimationProps.BOUNDS, mOutlineAnimator));
            }
            if (updateCallback != null) {
                ValueAnimator updateCallbackAnim = ValueAnimator.ofInt(0, 1);
                updateCallbackAnim.addUpdateListener(updateCallback);
                mTmpAnimators.add(toAnimation.apply(AnimationProps.BOUNDS, updateCallbackAnim));               
            }

            // Create the animator
            mTransformAnimation = toAnimation.createAnimator(mTmpAnimators);
            mTransformAnimation.start();
            mTargetAnimationTransform.copyFrom(toTransform);
        }
    }
}
