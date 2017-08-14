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
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.RecentsActivityLaunchState;
import com.android.systemui.recents.RecentsActivityUtils;
import com.android.systemui.recents.RecentsConfiguration;
import com.android.systemui.recents.RecentsDebugFlags;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.DockedFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.HideStackActionButtonEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.activity.ShowStackActionButtonEvent;
import com.android.systemui.recents.events.activity.LaunchTalpaTaskEvent;
import com.android.systemui.recents.events.ui.AllTaskViewsDismissedEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragDropTargetChangedEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragTalpaEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragTalpaEndEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragTalpaStartEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;

import static android.app.ActivityManager.StackId.INVALID_STACK_ID;

/**
 * This view is the the top level layout that contains TaskStacks (which are laid out according
 * to their SpaceNode bounds.
 */
public class RecentsTalpaView extends FrameLayout {

    private static final String TAG = "RecentsView";

    private static final int DEFAULT_UPDATE_SCRIM_DURATION = 200;
    private static final float DEFAULT_SCRIM_ALPHA = 0.50f;// 0.33f

    private static final int SHOW_STACK_ACTION_BUTTON_DURATION = 134;
    private static final int HIDE_STACK_ACTION_BUTTON_DURATION = 100;

    private TaskStack mStack;
    private TaskStackTalpaView mTaskStackView;
    private TextView mStackActionButton;
    private TextView mEmptyView;

    private boolean mAwaitingFirstLayout = true;
    private boolean mLastTaskLaunchedWasFreeform;

    @ViewDebug.ExportedProperty(category="recents")
    private Rect mSystemInsets = new Rect();
    private final Handler mHandler = new Handler();
    private int mDividerSize;

    private Drawable mBackgroundScrim = new ColorDrawable(
            Color.argb((int) (DEFAULT_SCRIM_ALPHA * 255), 0, 0, 0)).mutate();
    private Animator mBackgroundScrimAnimator;

    private RecentsTalpaTransitionHelper mTransitionHelper;

    @ViewDebug.ExportedProperty(deepExport=true, prefix="touch_")
    private RecentsTalpaViewTouchHandler mTouchHandler;


    private RecyclerView.OnScrollListener mScrollListener;
    private TaskTalpaHorizontalListView mTaskStackHorizontalView;


    public RecentsTalpaView(Context context) {
        this(context, null);
    }

    public RecentsTalpaView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecentsTalpaView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RecentsTalpaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setWillNotDraw(false);

        SystemServicesProxy ssp = Recents.getSystemServices();
        mTransitionHelper = new RecentsTalpaTransitionHelper(getContext(), mHandler);
        mDividerSize = ssp.getDockedDividerSize(context);
        mTouchHandler = new RecentsTalpaViewTouchHandler(this);

        LayoutInflater inflater = LayoutInflater.from(context);
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (RecentsDebugFlags.Static.EnableStackActionButton && !RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            final float cornerRadius = context.getResources().getDimensionPixelSize(
                    R.dimen.recents_task_view_rounded_corners_radius);
            mStackActionButton = (TextView) inflater.inflate(R.layout.recents_stack_action_button,
                    this, false);
            mStackActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EventBus.getDefault().send(new DismissAllTaskViewsEvent());
                }
            });
            addView(mStackActionButton);
            mStackActionButton.setClipToOutline(true);
            mStackActionButton.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });
        }
        mEmptyView = (TextView) inflater.inflate(R.layout.recents_empty, this, false);
        addView(mEmptyView);
    }

    /**
     * Called from RecentsActivity when it is relaunched.
     */
    public void onReload(boolean isResumingFromVisible, boolean isTaskStackEmpty) {
        RecentsConfiguration config = Recents.getConfiguration();
        RecentsActivityLaunchState launchState = config.getLaunchState();

        if (mTaskStackView == null) {
            isResumingFromVisible = false;
            mTaskStackView = new TaskStackTalpaView(getContext());
            mTaskStackHorizontalView = mTaskStackView.getListView();
            mTaskStackView.setSystemInsets(mSystemInsets);
            addView(mTaskStackView);
        }

        // Reset the state
        mAwaitingFirstLayout = !isResumingFromVisible;
        mLastTaskLaunchedWasFreeform = false;

        // Update the stack
        mTaskStackView.onReload(isResumingFromVisible);

        if (isResumingFromVisible) {
            // If we are already visible, then restore the background scrim
            animateBackgroundScrim(1f, DEFAULT_UPDATE_SCRIM_DURATION);
        } else {
            // If we are already occluded by the app, then set the final background scrim alpha now.
            // Otherwise, defer until the enter animation completes to animate the scrim alpha with
            // the tasks for the home animation.
            if (launchState.launchedViaDockGesture || launchState.launchedFromApp
                    || isTaskStackEmpty) {
                mBackgroundScrim.setAlpha(255);
                // Talpa DepingHuang add @{
                if (null != mStack) {
                    if (mStack.getTaskCount() >1 ){
                        mTaskStackView.setSelectedPosition(1);
                    }
                    else {
                        mTaskStackView.setSelectedPosition(0);
                    }
                }
                // Talpa DepingHuang add @}

            } else {
                //mBackgroundScrim.setAlpha(0);
				// Talpa bo.yang1 modify @{
				mBackgroundScrim.setAlpha(255);
				//@}

                // Talpa DepingHuang add @{
                mTaskStackView.setSelectedFirstPosition();
                // Talpa DepingHuang add @}
            }



        }
    }

    /**
     * Called from RecentsActivity when the task stack is updated.
     */
    public void updateStack(TaskStack stack, boolean setStackViewTasks) {
        mStack = stack;
        if (setStackViewTasks) {
            mTaskStackView.setTasks(stack, true /* allowNotifyStackChanges */);
        }

        // Update the top level view's visibilities
        if (stack.getTaskCount() > 0) {
            hideEmptyView();
        } else {
            showEmptyView(R.string.recents_empty_message);
        }
    }

    /**
     * Returns the current TaskStack.
     */
    public TaskStack getStack() {
        return mStack;
    }

    /*
     * Returns the window background scrim.
     */
    public Drawable getBackgroundScrim() {
        return mBackgroundScrim;
    }

    /**
     * Returns whether the last task launched was in the freeform stack or not.
     */
    public boolean isLastTaskLaunchedFreeform() {
        return mLastTaskLaunchedWasFreeform;
    }

    // 以下参照RecentsTvView 更改
    /** Launches the focused task from the first stack if possible */
    public boolean launchFocusedTask(int logEvent) {
        if (mTaskStackView != null) {
            Task task = mTaskStackView.getFocusedTask();
            if (task != null) {

                launchTaskFomRecents(task, true);

                if (logEvent != 0) {
                    MetricsLogger.action(getContext(), logEvent,
                            task.key.getComponent().toString());
                }
                return true;
            }
        }
        return false;
    }

    /** Launches the task that recents was launched from if possible */
    public boolean launchPreviousTask() {
        if (mTaskStackView != null) {
            TaskStack stack = mTaskStackView.getStack();
            Task task = stack.getLaunchTarget();
            if (task != null) {

                launchTaskFomRecents(task, true);

                return true;
            }
        }
        return false;
    }

    /**
     * Launch the given task from recents with animation. If the task is not focused, this will
     * attempt to scroll to focus the task before launching.
     * @param task
     */
    private void launchTaskFomRecents(final Task task, boolean animate) {
        if (!animate) {
            SystemServicesProxy ssp = Recents.getSystemServices();
            ssp.startActivityFromRecents(getContext(), task.key, task.title, null);
            return;
        }
        // 处理动画
        // ..
        mTaskStackHorizontalView.requestFocus();
        Task focusedTask = mTaskStackHorizontalView.getFocusedTask();
        if (focusedTask != null && task != focusedTask) {
            if (mScrollListener != null) {
                mTaskStackHorizontalView.removeOnScrollListener(mScrollListener);
            }
            mScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        TaskTalpaView cardView = mTaskStackHorizontalView.getChildViewForTask(task);
                        if (cardView != null) {
                            mTransitionHelper.launchTaskFromRecents(mStack, task,
                                    mTaskStackHorizontalView, cardView, false, null, INVALID_STACK_ID);
                        } else {
                            // This should not happen normally. If this happens then the data in
                            // the grid view was altered during the scroll. Log error and launch
                            // task with no animation.
                            Log.e(TAG, "Card view for task : " + task + ", returned null.");
                            SystemServicesProxy ssp = Recents.getSystemServices();
                            ssp.startActivityFromRecents(getContext(), task.key, task.title, null);
                        }
                        mTaskStackHorizontalView.removeOnScrollListener(mScrollListener);
                    }
                }
            };
            mTaskStackHorizontalView.addOnScrollListener(mScrollListener);
            TaskTalpaHorizontalListAdapter adapter = (TaskTalpaHorizontalListAdapter) mTaskStackHorizontalView.getAdapter();
            int position =  adapter.getPositionOfTask(task);

            // smoothScroll的距离不要太长
            int itemCount = adapter.getItemCount();
            if (position + 2 < itemCount){
                mTaskStackHorizontalView.scrollToPosition(position + 2);
            }
            else if (position + 1 < itemCount) {
                mTaskStackHorizontalView.scrollToPosition(position + 1);
            }
            mTaskStackHorizontalView.smoothScrollToPosition(position);

        } else {
            mTransitionHelper.launchTaskFromRecents(mStack, task, mTaskStackHorizontalView,
                    mTaskStackHorizontalView.getChildViewForTask(task), false, null,
                    INVALID_STACK_ID);
        }
    }



    /**
     * Hides the task stack and shows the empty view.
     */
    public void showEmptyView(int msgResId) {
        mTaskStackView.setVisibility(View.INVISIBLE);
        mEmptyView.setText(msgResId);
        mEmptyView.setVisibility(View.VISIBLE);
        mEmptyView.bringToFront();
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (RecentsDebugFlags.Static.EnableStackActionButton && !RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            mStackActionButton.bringToFront();
        }
    }

    /**
     * Shows the task stack and hides the empty view.
     */
    public void hideEmptyView() {
        mEmptyView.setVisibility(View.INVISIBLE);
        mTaskStackView.setVisibility(View.VISIBLE);
        mTaskStackView.bringToFront();
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (RecentsDebugFlags.Static.EnableStackActionButton && !RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            mStackActionButton.bringToFront();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, RecentsActivity.EVENT_BUS_PRIORITY + 1);
        // Talpa:peterHuang remove drag mode @{
        //EventBus.getDefault().register(mTouchHandler, RecentsActivity.EVENT_BUS_PRIORITY + 2);
        // @}
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
        EventBus.getDefault().unregister(mTouchHandler);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mTouchHandler.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mTouchHandler.onTouchEvent(ev);
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);

        ArrayList<TaskStack.DockState> visDockStates = mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            visDockStates.get(i).viewState.draw(canvas);
        }
    }


    /**
     * This is called with the full size of the window since we are handling our own insets.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mTaskStackView.getVisibility() != GONE) {
            mTaskStackView.measure(widthMeasureSpec, heightMeasureSpec);
        }

        // Measure the empty view to the full size of the screen
        if (mEmptyView.getVisibility() != GONE) {
            measureChild(mEmptyView, MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        }


        setMeasuredDimension(width, height);
    }

    /**
     * This is called with the full size of the window since we are handling our own insets.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        LogUtil.d("changed: "+ changed + " left: "+ left +" top: "+top +" right: "+right+ " bottom: "+ bottom);
        if (mTaskStackView.getVisibility() != GONE) {
            mTaskStackView.layout(left, top, left + getMeasuredWidth(), top + getMeasuredHeight());
        }

        // Layout the empty view
        if (mEmptyView.getVisibility() != GONE) {
            int leftRightInsets = mSystemInsets.left + mSystemInsets.right;
            int topBottomInsets = mSystemInsets.top + mSystemInsets.bottom;
            int childWidth = mEmptyView.getMeasuredWidth();
            int childHeight = mEmptyView.getMeasuredHeight();
            int childLeft = left + mSystemInsets.left +
                    Math.max(0, (right - left - leftRightInsets - childWidth)) / 2;
            int childTop = top + mSystemInsets.top +
                    Math.max(0, (bottom - top - topBottomInsets - childHeight)) / 2;
            mEmptyView.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }

        if (RecentsDebugFlags.Static.EnableStackActionButton && !RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            // Layout the stack action button such that its drawable is start-aligned with the
            // stack, vertically centered in the available space above the stack
            Rect buttonBounds = getStackActionButtonBoundsFromStackLayout();
            mStackActionButton.layout(buttonBounds.left, buttonBounds.top, buttonBounds.right,
                    buttonBounds.bottom);
        }

        if (mAwaitingFirstLayout) {
            mAwaitingFirstLayout = false;

            // If launched via dragging from the nav bar, then we should translate the whole view
            // down offscreen
            RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
            if (launchState.launchedViaDragGesture) {
                setTranslationY(getMeasuredHeight());
            } else {
                setTranslationY(0f);
            }
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mSystemInsets.set(insets.getSystemWindowInsets());
        mTaskStackView.setSystemInsets(mSystemInsets);
        requestLayout();
        return insets;
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        ArrayList<TaskStack.DockState> visDockStates = mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            Drawable d = visDockStates.get(i).viewState.dockAreaOverlay;
            if (d == who) {
                return true;
            }
        }
        return super.verifyDrawable(who);
    }

    private Rect getTaskRect(View taskView) {
        int[] location = taskView.getLocationOnScreen();
        int viewX = location[0];
        int viewY = location[1];
        return new Rect(viewX, viewY,
                (int) (viewX + taskView.getWidth() * taskView.getScaleX()),
                (int) (viewY + taskView.getHeight() * taskView.getScaleY()));
    }

    /**** EventBus Events ****/
    // 启动任务
    public final void onBusEvent(LaunchTalpaTaskEvent event) {
        mLastTaskLaunchedWasFreeform = event.task.isFreeformTask();

        //  TALPA: DepingHuang modified start
//        mTransitionHelper.launchTaskFromRecents(mStack, event.task, mTaskStackView, event.taskView,
//                event.screenPinningRequested, event.targetTaskBounds, event.targetTaskStack);
        mTransitionHelper.launchTaskFromRecents(mStack, event.task, mTaskStackHorizontalView,
                event.taskView, event.screenPinningRequested, event.targetTaskBounds, event.targetTaskStack);
        // TALPA: DepingHuang modified end

    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        int taskViewExitToHomeDuration = TaskStackAnimationHelper.EXIT_TO_HOME_TRANSLATION_DURATION;
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (RecentsDebugFlags.Static.EnableStackActionButton && !RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            // Hide the stack action button
            hideStackActionButton(taskViewExitToHomeDuration, false /* translate */);
        }
        animateBackgroundScrim(0f, taskViewExitToHomeDuration);
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        RecentsActivityLaunchState launchState = Recents.getConfiguration().getLaunchState();
        if (!launchState.launchedViaDockGesture && !launchState.launchedFromApp
                && mStack.getTaskCount() > 0) {
			// Talpa bo.yang1 modify @{		
            //animateBackgroundScrim(1f,
             //       TaskStackAnimationHelper.ENTER_FROM_HOME_TRANSLATION_DURATION);
			//@}
        }
    }

    public final void onBusEvent(AllTaskViewsDismissedEvent event) {
        hideStackActionButton(HIDE_STACK_ACTION_BUTTON_DURATION, true /* translate */);
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (!ssp.hasDockedTask()) {
            // Animate the background away only if we are dismissing Recents to home
            animateBackgroundScrim(0f, DEFAULT_UPDATE_SCRIM_DURATION);
        }
    }

    public final void onBusEvent(ShowStackActionButtonEvent event) {
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (!RecentsDebugFlags.Static.EnableStackActionButton || RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            return;
        }

        showStackActionButton(SHOW_STACK_ACTION_BUTTON_DURATION, event.translate);
    }

    public final void onBusEvent(HideStackActionButtonEvent event) {
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (!RecentsDebugFlags.Static.EnableStackActionButton || RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            return;
        }

        hideStackActionButton(HIDE_STACK_ACTION_BUTTON_DURATION, true /* translate */);
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        updateStack(event.stack, false /* setStackViewTasks */);
    }

    // 开始拖拽
    public final void onBusEvent(DragTalpaStartEvent event) {
        updateVisibleDockRegions(mTouchHandler.getDockStatesForCurrentOrientation(),
                true /* isDefaultDockState */, TaskStack.DockState.NONE.viewState.dockAreaAlpha,
                TaskStack.DockState.NONE.viewState.hintTextAlpha,
                true /* animateAlpha */, false /* animateBounds */);

        // Temporarily hide the stack action button without changing visibility
        if (mStackActionButton != null) {
            mStackActionButton.animate()
                    .alpha(0f)
                    .setDuration(HIDE_STACK_ACTION_BUTTON_DURATION)
                    .setInterpolator(Interpolators.ALPHA_OUT)
                    .start();
        }
    }
    public final void onBusEvent(DragDropTargetChangedEvent event) {
        if (event.dropTarget == null || !(event.dropTarget instanceof TaskStack.DockState)) {
            updateVisibleDockRegions(mTouchHandler.getDockStatesForCurrentOrientation(),
                    true /* isDefaultDockState */, TaskStack.DockState.NONE.viewState.dockAreaAlpha,
                    TaskStack.DockState.NONE.viewState.hintTextAlpha,
                    true /* animateAlpha */, true /* animateBounds */);
        } else {
            final TaskStack.DockState dockState = (TaskStack.DockState) event.dropTarget;
            updateVisibleDockRegions(new TaskStack.DockState[] {dockState},
                    false /* isDefaultDockState */, -1, -1, true /* animateAlpha */,
                    true /* animateBounds */);
        }
        if (mStackActionButton != null) {
            event.addPostAnimationCallback(new Runnable() {
                @Override
                public void run() {
                    // Move the clear all button to its new position
                    Rect buttonBounds = getStackActionButtonBoundsFromStackLayout();
                    mStackActionButton.setLeftTopRightBottom(buttonBounds.left, buttonBounds.top,
                            buttonBounds.right, buttonBounds.bottom);
                }
            });
        }
    }

    public final void onBusEvent(final DragTalpaEndEvent event) {
        // Handle the case where we drop onto a dock region
        if (event.dropTarget instanceof TaskStack.DockState) {
            final TaskStack.DockState dockState = (TaskStack.DockState) event.dropTarget;

            // Hide the dock region
            updateVisibleDockRegions(null, false /* isDefaultDockState */, -1, -1,
                    false /* animateAlpha */, false /* animateBounds */);

            // We translated the view but we need to animate it back from the current layout-space
            // rect to its final layout-space rect
            Utilities.setViewFrameFromTranslation(event.taskView);

            // Dock the task and launch it
            SystemServicesProxy ssp = Recents.getSystemServices();
            if (ssp.startTaskInDockedMode(event.task.key.id, dockState.createMode)) {
                final ActivityOptions.OnAnimationStartedListener startedListener =
                        new ActivityOptions.OnAnimationStartedListener() {
                            @Override
                            public void onAnimationStarted() {
                                EventBus.getDefault().send(new DockedFirstAnimationFrameEvent());
                                // Remove the task and don't bother relaying out, as all the tasks will be
                                // relaid out when the stack changes on the multiwindow change event
                                // Talpa:bo.yang1 add Fixed cdn bug#9946  @{
                                mTaskStackView.setVisibility(View.INVISIBLE);
                                //mTaskStackView.getStack().removeTask(event.task, null,
                                //        true /* fromDockGesture */);
                                //@}
                            }
                        };

                final Rect taskRect = getTaskRect(event.taskView);
                IAppTransitionAnimationSpecsFuture future =
                        mTransitionHelper.getAppTransitionFuture(
                                new RecentsTransitionHelper.AnimationSpecComposer() {
                                    @Override
                                    public List<AppTransitionAnimationSpec> composeSpecs() {
                                        return mTransitionHelper.composeDockAnimationSpec(
                                                event.taskView, taskRect);
                                    }
                                });
                ssp.overridePendingAppTransitionMultiThumbFuture(future,
                        mTransitionHelper.wrapStartedListener(startedListener),
                        true /* scaleUp */);

                MetricsLogger.action(mContext, MetricsProto.MetricsEvent.ACTION_WINDOW_DOCK_DRAG_DROP,
                        event.task.getTopComponent().flattenToShortString());
            } else {
                EventBus.getDefault().send(new DragTalpaEndCancelledEvent(mStack, event.task,
                        event.taskView));
            }
        } else {
            // Animate the overlay alpha back to 0
            updateVisibleDockRegions(null, true /* isDefaultDockState */, -1, -1,
                    true /* animateAlpha */, false /* animateBounds */);
        }

        // Show the stack action button again without changing visibility
        if (mStackActionButton != null) {
            mStackActionButton.animate()
                    .alpha(1f)
                    .setDuration(SHOW_STACK_ACTION_BUTTON_DURATION)
                    .setInterpolator(Interpolators.ALPHA_IN)
                    .start();
        }
    }


    public final void onBusEvent(final DragTalpaEndCancelledEvent event) {
        // Animate the overlay alpha back to 0
        updateVisibleDockRegions(null, true /* isDefaultDockState */, -1, -1,
                true /* animateAlpha */, false /* animateBounds */);
    }

    // ------------------------- //

    /**
     * Shows the stack action button.
     */
    private void showStackActionButton(final int duration, final boolean translate) {
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (!RecentsDebugFlags.Static.EnableStackActionButton || RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            return;
        }

        final ReferenceCountedTrigger postAnimationTrigger = new ReferenceCountedTrigger();
        if (mStackActionButton.getVisibility() == View.INVISIBLE) {
            mStackActionButton.setVisibility(View.VISIBLE);
            mStackActionButton.setAlpha(0f);
            if (translate) {
                mStackActionButton.setTranslationY(-mStackActionButton.getMeasuredHeight() * 0.25f);
            } else {
                mStackActionButton.setTranslationY(0f);
            }
            postAnimationTrigger.addLastDecrementRunnable(new Runnable() {
                @Override
                public void run() {
                    if (translate) {
                        mStackActionButton.animate()
                                .translationY(0f);
                    }
                    mStackActionButton.animate()
                            .alpha(1f)
                            .setDuration(duration)
                            .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                            .start();
                }
            });
        }
        postAnimationTrigger.flushLastDecrementRunnables();
    }

    /**
     * Hides the stack action button.
     */
    private void hideStackActionButton(int duration, boolean translate) {
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (!RecentsDebugFlags.Static.EnableStackActionButton || RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            return;
        }

        final ReferenceCountedTrigger postAnimationTrigger = new ReferenceCountedTrigger();
        hideStackActionButton(duration, translate, postAnimationTrigger);
        postAnimationTrigger.flushLastDecrementRunnables();
    }

    /**
     * Hides the stack action button.
     */
    private void hideStackActionButton(int duration, boolean translate,
                                       final ReferenceCountedTrigger postAnimationTrigger) {
        // SPRD: Bug 535096 new feature of quick cleaning.
        if (!RecentsDebugFlags.Static.EnableStackActionButton || RecentsActivityUtils.getInstance().isSupportClearAllTasks()) {
            return;
        }

        if (mStackActionButton.getVisibility() == View.VISIBLE) {
            if (translate) {
                mStackActionButton.animate()
                        .translationY(-mStackActionButton.getMeasuredHeight() * 0.25f);
            }
            mStackActionButton.animate()
                    .alpha(0f)
                    .setDuration(duration)
                    .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mStackActionButton.setVisibility(View.INVISIBLE);
                            postAnimationTrigger.decrement();
                        }
                    })
                    .start();
            postAnimationTrigger.increment();
        }
    }


    /**
     * Updates the dock region to match the specified dock state.
     */
    private void updateVisibleDockRegions(TaskStack.DockState[] newDockStates,
                                          boolean isDefaultDockState, int overrideAreaAlpha, int overrideHintAlpha,
                                          boolean animateAlpha, boolean animateBounds) {
        ArraySet<TaskStack.DockState> newDockStatesSet = Utilities.arrayToSet(newDockStates,
                new ArraySet<TaskStack.DockState>());
        ArrayList<TaskStack.DockState> visDockStates = mTouchHandler.getVisibleDockStates();
        for (int i = visDockStates.size() - 1; i >= 0; i--) {
            TaskStack.DockState dockState = visDockStates.get(i);
            TaskStack.DockState.ViewState viewState = dockState.viewState;
            if (newDockStates == null || !newDockStatesSet.contains(dockState)) {
                // This is no longer visible, so hide it
                viewState.startAnimation(null, 0, 0, TaskStackView.SLOW_SYNC_STACK_DURATION,
                        Interpolators.FAST_OUT_SLOW_IN, animateAlpha, animateBounds);
            } else {
                // This state is now visible, update the bounds and show it
                int areaAlpha = overrideAreaAlpha != -1
                        ? overrideAreaAlpha
                        : viewState.dockAreaAlpha;
                int hintAlpha = overrideHintAlpha != -1
                        ? overrideHintAlpha
                        : viewState.hintTextAlpha;
                Rect bounds = isDefaultDockState
                        ? dockState.getPreDockedBounds(getMeasuredWidth(), getMeasuredHeight())
                        : dockState.getDockedBounds(getMeasuredWidth(), getMeasuredHeight(),
                        mDividerSize, mSystemInsets, getResources());
                if (viewState.dockAreaOverlay.getCallback() != this) {
                    viewState.dockAreaOverlay.setCallback(this);
                    viewState.dockAreaOverlay.setBounds(bounds);
                }
                viewState.startAnimation(bounds, areaAlpha, hintAlpha,
                        TaskStackView.SLOW_SYNC_STACK_DURATION, Interpolators.FAST_OUT_SLOW_IN,
                        animateAlpha, animateBounds);
            }
        }
    }

    /**
     * Animates the background scrim to the given {@param alpha}.
     */
    private void animateBackgroundScrim(float alpha, int duration) {
        Utilities.cancelAnimationWithoutCallbacks(mBackgroundScrimAnimator);
        // Calculate the absolute alpha to animate from
        int fromAlpha = (int) ((mBackgroundScrim.getAlpha() / (DEFAULT_SCRIM_ALPHA * 255)) * 255);
        int toAlpha = (int) (alpha * 255);
        mBackgroundScrimAnimator = ObjectAnimator.ofInt(mBackgroundScrim, Utilities.DRAWABLE_ALPHA,
                fromAlpha, toAlpha);
        mBackgroundScrimAnimator.setDuration(duration);
        mBackgroundScrimAnimator.setInterpolator(toAlpha > fromAlpha
                ? Interpolators.ALPHA_IN
                : Interpolators.ALPHA_OUT);
        mBackgroundScrimAnimator.start();
    }

    /**
     * @return the bounds of the stack action button.
     */
    private Rect getStackActionButtonBoundsFromStackLayout() {
        // TALPA: DepingHuang modified start
        //Rect actionButtonRect = new Rect(mTaskStackView.mLayoutAlgorithm.mStackActionButtonRect);
        Rect actionButtonRect = new Rect(0,0,10,10);
        // TALPA: DepingHuang modified end
        int left = isLayoutRtl()
                ? actionButtonRect.left - mStackActionButton.getPaddingLeft()
                : actionButtonRect.right + mStackActionButton.getPaddingRight()
                - mStackActionButton.getMeasuredWidth();
        int top = actionButtonRect.top +
                (actionButtonRect.height() - mStackActionButton.getMeasuredHeight()) / 2;
        actionButtonRect.set(left, top, left + mStackActionButton.getMeasuredWidth(),
                top + mStackActionButton.getMeasuredHeight());
        return actionButtonRect;
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        String id = Integer.toHexString(System.identityHashCode(this));

        writer.print(prefix); writer.print(TAG);
        writer.print(" awaitingFirstLayout="); writer.print(mAwaitingFirstLayout ? "Y" : "N");
        writer.print(" insets="); writer.print(Utilities.dumpRect(mSystemInsets));
        writer.print(" [0x"); writer.print(id); writer.print("]");
        writer.println();

        if (mStack != null) {
            mStack.dump(innerPrefix, writer);
        }
        if (mTaskStackView != null) {
            mTaskStackView.dump(innerPrefix, writer);
        }
    }


}
