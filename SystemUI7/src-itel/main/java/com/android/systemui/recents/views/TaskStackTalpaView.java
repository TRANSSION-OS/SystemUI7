package com.android.systemui.recents.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto;
import com.android.systemui.R;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.HideRecentsEvent;
import com.android.systemui.recents.events.activity.LaunchNextTaskRequestEvent;
import com.android.systemui.recents.events.activity.LaunchTalpaTaskEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.ui.DeleteTaskDataEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.TaskViewDismissedEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.misc.Utilities;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;
import com.android.systemui.recents.talpa.SwipeRecyclerView;
import com.android.systemui.recents.talpa.TaskViewHeaderMoveHelper;

import java.io.PrintWriter;
import java.util.ArrayList;

import itel.transsion.settingslib.utils.LogUtil;

import static android.app.ActivityManager.StackId.INVALID_STACK_ID;

/**
 * 横版
 * Created by deping.huang on 2016/12/12.
 */

public class TaskStackTalpaView extends FrameLayout implements
        TaskView.TaskViewCallbacks {
    private static final String TAG = TaskStackTalpaView.class.getSimpleName();
    // The current display orientation
    @ViewDebug.ExportedProperty(category="recents")
    private int mDisplayOrientation = Configuration.ORIENTATION_UNDEFINED;
    // The current display bounds
    @ViewDebug.ExportedProperty(category="recents")
    private Rect mDisplayRect = new Rect();
    @ViewDebug.ExportedProperty(category="recents")
    boolean mTouchExplorationEnabled;
    @ViewDebug.ExportedProperty(category="recents")
    boolean mScreenPinningEnabled;
    @ViewDebug.ExportedProperty(category="recents")
    public Rect mSystemInsets = new Rect();

    // SPRD: bug598463, catch ArrayIndexOutOfBoundsException
    private static final int INVALID_TASK_INDEX = -1;

    private Resources mRes;

    private int mDividerSize;
    private TaskStack mStack = new TaskStack();
    private LayoutInflater mInflater;
    //private ArrayList<TaskView> mTaskViews = new ArrayList<>();

    //-------------------------------------ViewPaper 相关 start-----------------------------------//
    private TaskTalpaHorizontalListView mTaskListView;
    private TaskTalpaHorizontalListAdapter mTaskListAdapter;
    private TaskViewHeaderMoveHelper mTaskViewHeaderMoveHelper;

	// Talpa bo.yang1 add for delete Animation @{
    private TaskStackTalpaAnimationHelper mTaskStackTalpaAnimationHelper;

    private int mPageWidth;
    private int mPageHeight;
    private int mPageMargin;
    private int mContainerWidth; // 页面容器总宽度
    private int mContainerHeight; // 页面容器总高度
    private int mTopMargin;
    //-------------------------------------ViewPaper 相关 end-------------------------------------//

    public TaskStackTalpaView(Context context) {
        super(context);
        SystemServicesProxy ssp = Recents.getSystemServices();
        mRes = context.getResources();

        // Set the stack first
        //mStack.setCallbacks(this);
        mInflater = LayoutInflater.from(context);

        mDividerSize = ssp.getDockedDividerSize(context);
        mDisplayOrientation = Utilities.getAppConfiguration(mContext).orientation;
        mDisplayRect = ssp.getDisplayRect();

        //LogUtil.i("ssp.hasDockedTask():"+ssp.hasDockedTask() + "  mDividerSize:"+mDividerSize);
        redefinedListItemSize(ssp.hasDockedTask(), mRes);

        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        initListView(context);
        mTaskListView.setMultiWindowMode(ssp.hasDockedTask());

    }

    /**
     * Reads current system flags related to accessibility and screen pinning.
     */
    private void readSystemFlags() {
        SystemServicesProxy ssp = Recents.getSystemServices();
        mTouchExplorationEnabled = ssp.isTouchExplorationEnabled();
        mScreenPinningEnabled = ssp.getSystemSetting(getContext(),
                Settings.System.LOCK_TO_APP_ENABLED) != 0;
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, RecentsActivity.EVENT_BUS_PRIORITY + 1);
        super.onAttachedToWindow();
        readSystemFlags();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    public TaskTalpaHorizontalListView getListView(){
        return mTaskListView;
    }

    public void setSelectedPosition(int position){
        mTaskListView.smoothScrollToPosition(position);
    }

    public void setSelectedFirstPosition(){
//        // 做一个微小滚动效果
//        if (mStack.getTaskCount() > 1) {
//            // （直接用这个滚动时，会出现滚动不到位（不居中），导致滑动坐标计算混乱）
//            mTaskListView.scrollToPosition(1);
//            //mTaskViewHeaderMoveHelper.resetPosition(1);
//        }
//        mTaskListView.smoothScrollToPosition(0);
        mTaskListView.scrollToPosition(0);
        mTaskViewHeaderMoveHelper.resetPosition(0);
    }

    //-------------------------------------TASK相关回调 start--------------------------------------//

    @Override
    public void onTaskViewClipStateChanged(TaskView tv) {

    }

    public final void onBusEvent(TaskViewDismissedEvent event) {
        // Announce for accessibility
        announceForAccessibility(getContext().getString(
                R.string.accessibility_recents_item_dismissed, event.task.title));

        // Remove the task from the stack
        mStack.removeTask(event.task, event.animation, false /* fromDockGesture */);
        EventBus.getDefault().send(new DeleteTaskDataEvent(event.task));

        mTaskListAdapter.notifyDataSetChanged();

        MetricsLogger.action(getContext(), MetricsProto.MetricsEvent.OVERVIEW_DISMISS,
                event.task.key.getComponent().toString());
    }


    // 收到清除所有任务的事件
    public final void onBusEvent(final DismissAllTaskViewsEvent event) {
        LogUtil.i("DismissAllTaskViewsEvent....");
        // Keep track of the tasks which will have their data removed
        final ArrayList<Task> tasks = new ArrayList<>(mStack.getStackTasks());
        /*mAnimationHelper.startDeleteAllTasksAnimation(getTaskViews(), event.getAnimationTrigger());*/
		// Talpa bo.yang1 add for delete Animation @{
        mTaskStackTalpaAnimationHelper.startDeleteAllTasksAnimation(mTaskListView, event.getAnimationTrigger());
		//@}
        event.addPostAnimationCallback(new Runnable() {
            @Override
            public void run() {
                // Announce for accessibility
                announceForAccessibility(getContext().getString(
                        R.string.accessibility_recents_all_items_dismissed));

                // Remove all tasks and delete the task data for all tasks
				// bo.yang1 modify for remove alltasks @{
                mStack.removeAllTasks();
                for (int i = tasks.size() - 1; i >= 0; i--) {
                    EventBus.getDefault().send(new DeleteTaskDataEvent(tasks.get(i)));
                }
                //mStack.removeAllTasks();
				//@}

                MetricsLogger.action(getContext(), MetricsProto.MetricsEvent.OVERVIEW_DISMISS_ALL);
            }
        });

    }

    public final void onBusEvent(LaunchNextTaskRequestEvent event) {
        if (mStack.getTaskCount() == 0) {
            // If there are no tasks, then just hide recents back to home.
            EventBus.getDefault().send(new HideRecentsEvent(false, true));
            return;
        }

        int launchTaskIndex = mStack.indexOfStackTask(mStack.getLaunchTarget());
        if (launchTaskIndex != -1) {
            if (mStack.getTaskCount() > 1){
                launchTaskIndex =  Math.max(0, mStack.getTaskCount() - launchTaskIndex);
            }
            else{
                launchTaskIndex = 0;
            }

        } else { // 默认首个
            launchTaskIndex = 0;
        }
        if (launchTaskIndex != -1) {
            // Stop all animations
            //cancelAllTaskViewAnimations();
            final Task launchTask;
            // Fixed bug 出错的情况可能为前面launcher加入任务管理器的一瞬间状态读取错误
            if (launchTaskIndex >= mStack.getStackTasks().size()){
                launchTask = mStack.getStackTasks().get(0);
                LogUtil.e("launchTaskIndex > now task stack size");
            }
            else {
                launchTask = mStack.getStackTasks().get(launchTaskIndex);
            }

            TaskTalpaView childViewForTask = mTaskListView.getChildViewForTask(launchTask);
            if (childViewForTask == null) {

//                TaskTalpaView firstVisibleItem = mTaskListView.getFirstVisibleItemView();
//                EventBus.getDefault().send(new LaunchTalpaTaskEvent(
//                        firstVisibleItem, firstVisibleItem.getTask(), null,
//                        INVALID_STACK_ID));

                EventBus.getDefault().send(new HideRecentsEvent(false, true));
                return;

            } else {
                EventBus.getDefault().send(new LaunchTalpaTaskEvent(childViewForTask,
                        launchTask, null, INVALID_STACK_ID, false));
            }

            MetricsLogger.action(getContext(), MetricsProto.MetricsEvent.OVERVIEW_LAUNCH_PREVIOUS_TASK,
                    launchTask.key.getComponent().toString());
        }
    }

    // 分屏/正常模式回调
    public final void onBusEvent(final MultiWindowStateChangedEvent event) {
        Log.i(TAG, "event.inMultiWindow: " + event.inMultiWindow);

        mTaskListView.setMultiWindowMode(event.inMultiWindow);
        /**
         * 重新创建TaskStackTalpaView时有判断当前为分屏模式但实际为全屏的情况（会有一个时间差的延后）
         * 这里做容错处理
         */
        if (!event.inMultiWindow) {
            if (mTaskListAdapter != null) {
                if (mTaskListAdapter.getItemWidth() ==
                        mRes.getDimensionPixelSize(R.dimen.recents_task_view_divider_page_width)){
                    //Log.i(TAG, "大屏模式，但内容为小屏... 修正处理");
                    redefinedListItemSize(false, mRes);

                    // resize listview 高度
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)mTaskListView.getLayoutParams();
                    lp.topMargin = mTopMargin;
                    lp.height = mContainerHeight;


                    if (mTaskViewHeaderMoveHelper != null){
                        mTaskViewHeaderMoveHelper.clearParam(mPageWidth, mPageMargin);
                    }
                    mTaskListAdapter = null; // 清空adapter 重新创建
                    this.setTasks(event.stack, true /* allowNotifyStackChanges */);
                }
            }
        }
    }

    private void redefinedListItemSize(boolean isInMultiWindow, Resources res){
        if (isInMultiWindow) {
            mPageMargin =res.getDimensionPixelSize(R.dimen.recents_task_view_divider_page_margin);
            mPageWidth = res.getDimensionPixelSize(R.dimen.recents_task_view_divider_page_width);
            mPageHeight = res.getDimensionPixelSize(R.dimen.recents_task_view_divider_page_height);
            mContainerWidth = mDisplayRect.width();
            mContainerHeight = mPageHeight;
			// bo.yang1 modify for MultiWindow add topmargin @{
            //mTopMargin = 0;
			mTopMargin = mRes.getDimensionPixelOffset(R.dimen.recents_talpa_list_view_margin_top);
			//@}
        }
        else {
            mPageMargin =res.getDimensionPixelSize(R.dimen.recents_task_view_page_margin);
            mPageWidth = res.getDimensionPixelSize(R.dimen.recents_task_view_page_width);
            if (mPageWidth % 2 != 0){  // 计算滑动时不能为奇数
                mPageWidth = (mPageWidth/2) * 2;
            }
            mPageHeight = res.getDimensionPixelSize(R.dimen.recents_task_view_page_height);
            mContainerWidth = mDisplayRect.width();
            mContainerHeight = mPageHeight
                    + res.getDimensionPixelOffset(R.dimen.recents_talpa_task_view_header_height)
                    + res.getDimensionPixelOffset(R.dimen.recents_talpa_task_view_header_margin_bottom);
            mTopMargin = mRes.getDimensionPixelOffset(R.dimen.recents_talpa_list_view_margin_top);
        }

    }




    //-------------------------------------TASK相关回调 end----------------------------------------//
    /**
     * Updates the system insets.
     */
    public void setSystemInsets(Rect systemInsets) {
        boolean changed = false;
/*        changed |= mStableLayoutAlgorithm.setSystemInsets(systemInsets);
        changed |= mLayoutAlgorithm.setSystemInsets(systemInsets);*/
        if (changed) {
            requestLayout();
        }
    }

    /**
     * Called from RecentsActivity when it is relaunched.
     */
    void onReload(boolean isResumingFromVisible) {
        LogUtil.i("isResumingFromVisible:"+ isResumingFromVisible);

       /* requestLayout();*/
        if (null != mTaskListAdapter) {
          //  mTaskListAdapter.notifyDataSetChanged();
        }
    }


    /** Returns the task stack. */
    public TaskStack getStack() {
        LogUtil.i("getStack");
        return mStack;
    }


    /**
     * Sets the stack tasks of this TaskStackView from the given TaskStack.
     */
    public void setTasks(TaskStack stack, boolean allowNotifyStackChanges) {
        LogUtil.i("setTasks");
             /*  boolean isInitialized = mLayoutAlgorithm.isInitialized();*/
        boolean isInitialized = true;

        // Only notify if we are already initialized, otherwise, everything will pick up all the
        // new and old tasks when we next layout
        mStack.setTasks(getContext(), stack.computeAllTasksList(), allowNotifyStackChanges);

        if (mTaskListAdapter == null) {
            mTaskListAdapter = new TaskTalpaHorizontalListAdapter(mStack, mContainerWidth,
                    mPageWidth,mPageHeight, mPageMargin);
            mTaskListAdapter.setDisplayRect(mDisplayRect);
            mTaskListView.setAdapter(mTaskListAdapter);
        }
        else {
            mTaskListAdapter.setNewStackTasks(mStack);
        }



    }

    /**
     * Returns the focused task.
     */
    Task getFocusedTask() {
        LogUtil.i("getFocusedTask");
        return mTaskListView.getFocusedTask();
    }


    //-------------------------------------其它TasksView 方法 start-------------------------------//

    private void unbindTaskView(TaskView tv, Task task) {
        // Report that this task's data is no longer being used
        Recents.getTaskLoader().unloadTaskData(task);
    }

    //-------------------------------------其它TasksView 方法 end----------------------------------//

    //-------------------------------------HorizontalListView 相关 start-----------------------------------//
    private void initListView(Context context) {
        if (null == mTaskListView) {
            mTaskListView = new TaskTalpaHorizontalListView(context);
            this.setClipChildren(false); // must
            mTaskListView.setClipChildren(false); // must
            // 初始化监听task回调等
            mTaskListView.init(mStack);

            //mTaskListView.setBackgroundColor(0xffff0000); // for test

            // 重定义ViewPaper的宽高,根据当前所处于的模式（分屏/正常）计算
            FrameLayout.LayoutParams lp =new FrameLayout.LayoutParams(mContainerWidth, mContainerHeight);
            lp.gravity = Gravity.CENTER;
            lp.topMargin = mTopMargin;
            mTaskListView.setLayoutParams(lp);
            final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            mTaskListView.setLayoutManager(linearLayoutManager);

            mTaskViewHeaderMoveHelper = new TaskViewHeaderMoveHelper();
            //mCardScaleHelper.setCurrentItemPos(1);
            mTaskViewHeaderMoveHelper.attachToRecyclerView(mTaskListView, mPageWidth, mPageMargin, TaskTalpaView.getHeadViewID());

			// Talpa bo.yang1 add for delete Animation @{
            mTaskStackTalpaAnimationHelper=new TaskStackTalpaAnimationHelper(context,this);
			//@}

            mTaskListView.setRemoveListener(new SwipeRecyclerView.RemoveListener() {
                @Override
                public void removeItem(View itemView, int position) {
                   // Log.d(TAG, "滑动删除recent task position:" + position);
                    if (itemView instanceof TaskTalpaView){
                        // 有锁的清除锁
                        if (((TaskTalpaView) itemView).isLockedIcon())
                                ((TaskTalpaView) itemView).cleanLockedStatus();

                        onChildDismissed(itemView, position);
                    }
                    else {
                        // throw ...
                    }

                }
            });
            this.addView(mTaskListView);
        }
    }

    /**
     * Called after the {@link TaskView} is finished animating away.
     */
    public void onChildDismissed(View v, int position) {
        TaskTalpaView tv = (TaskTalpaView) v;

        // Re-enable clipping with the stack (we will reuse this view)
        tv.setClipViewInStack(true);
        // Re-enable touch events from this task view
        tv.setTouchEnabled(true);
        // Remove the task view from the stack, ignoring the animation if we've started dragging
        // again
        // Announce for accessibility
        Task task = tv.getTask();
        announceForAccessibility(getContext().getString(
                R.string.accessibility_recents_item_dismissed, task.title));

        // Remove the task from the stack
        mStack.removeTask(task, AnimationProps.IMMEDIATE, false /* fromDockGesture */);
        EventBus.getDefault().send(new DeleteTaskDataEvent(task));
        //mStack.removeTask(task, AnimationProps.IMMEDIATE, false /* fromDockGesture */);

        mTaskListAdapter.removeTask(task);
        //mTaskListAdapter.notifyItemRemoved(position);

        // Keep track of deletions by keyboard
        MetricsLogger.histogram(tv.getContext(), "overview_task_dismissed_source",
                Constants.Metrics.DismissSourceSwipeGesture);
    }


    /**
     *  处于编辑模式的Item 可能在RecylerView之外，这里做强制分发处理
     * */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        return super.dispatchTouchEvent(ev);
    }

//-------------------------------------HorizontalListView 相关 end-------------------------------------//




    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        String id = Integer.toHexString(System.identityHashCode(this));

        writer.print(prefix); writer.print(TAG);

        // ..... 具体待添加
    }

    // A convenience update listener to request updating clipping of tasks
    private ValueAnimator.AnimatorUpdateListener mRequestUpdateClippingListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    /*if (!mTaskViewsClipDirty) {
                        mTaskViewsClipDirty = true;
                        invalidate();
                    }*/
                }
            };


    /**
     * Called to update a specific {@link TaskView} to a given {@link TaskViewTransform} with a
     * given set of {@link AnimationProps} properties.
     */
    public void updateTaskViewToTransform(TaskTalpaView tasktalpaView, TaskTalpaViewTransform transform,
                                          AnimationProps animation) {
        if (tasktalpaView.isAnimatingTo(transform)) {
            return;
        }
        tasktalpaView.cancelTransformAnimation();
        tasktalpaView.updateViewPropertiesToTaskTransform(transform, animation,
                mRequestUpdateClippingListener);
    }

}
