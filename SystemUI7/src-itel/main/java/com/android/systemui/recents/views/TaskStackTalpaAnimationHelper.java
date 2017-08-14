package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import com.android.systemui.recents.model.Task;

import java.util.ArrayList;

/**
 * Created by bo.yang1 on 2017/5/11.
 */

public class TaskStackTalpaAnimationHelper {

    private static final int DOUBLE_FRAME_OFFSET_MS = 100;
    private static final int DISMISS_ALL_TASKS_DURATION = 200;
    private static final Interpolator DISMISS_ALL_TRANSLATION_INTERPOLATOR =
            new PathInterpolator(0.4f, 0, 1f, 1f);

    private TaskStackTalpaView mStackView;

    private Context mContext;
    private AnimatorSet mTransformAnimation;
    private int mScreenHeight;

    private TaskTalpaViewTransform mTmpTransform = new TaskTalpaViewTransform();

    public TaskStackTalpaAnimationHelper(Context context,TaskStackTalpaView mStackView) {
        this.mStackView = mStackView;
        mScreenHeight = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getHeight();
    }

    /**
     * Starts the delete animation for all the {@link TaskView}s.
     */
    public void startDeleteAllTasksAnimation(final RecyclerView mRecyclerView,
                                             final ReferenceCountedTrigger postAnimationTrigger) {

        mContext = mRecyclerView.getContext();
        int taskViewCount = mRecyclerView.getAdapter().getItemCount();
        int firstVisibleItemPosition=((LinearLayoutManager)mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        int lastVisibleItemPosition=((LinearLayoutManager)mRecyclerView.getLayoutManager()).findLastVisibleItemPosition();   
        for (int i = firstVisibleItemPosition; i <=lastVisibleItemPosition; i++) {
            final TaskTalpaView tv = (TaskTalpaView)mRecyclerView.getLayoutManager().findViewByPosition(i);
            Task task = tv.getTask();
            if(task!=null && (task.isLocked || task.isLaunchTarget)){
                continue;
            }
            int taskIndexFromFront = i-firstVisibleItemPosition;
            int startDelay = taskIndexFromFront * DOUBLE_FRAME_OFFSET_MS;

            tv.setClipViewInStack(false);

           /* ObjectAnimator anim=ObjectAnimator.ofFloat(tv,"Alpha", 1.0f,0f);
            anim.setStartDelay(startDelay);
            anim.setDuration(DISMISS_ALL_TASKS_DURATION);
            anim.start();*/

            // Compose the new animation and transform and star the animation
            AnimationProps taskAnimation = new AnimationProps(startDelay,
                    DISMISS_ALL_TASKS_DURATION, DISMISS_ALL_TRANSLATION_INTERPOLATOR,
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            postAnimationTrigger.decrement();

                            // Re-enable clipping with the stack (we will reuse this view)
                            tv.setClipViewInStack(true);
                        }
                    });
            postAnimationTrigger.increment();

            mTmpTransform.fillIn(tv);
            mTmpTransform.rect.offset(0, tv.getTop()-mScreenHeight);

            mStackView.updateTaskViewToTransform(tv, mTmpTransform, taskAnimation);
        }
    }


}
