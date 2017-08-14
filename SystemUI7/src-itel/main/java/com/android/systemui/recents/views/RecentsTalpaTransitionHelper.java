/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.Nullable;
import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.RemoteException;
import android.view.AppTransitionAnimationSpec;
import android.view.IAppTransitionAnimationSpecsFuture;
import android.view.WindowManagerGlobal;

import com.android.internal.annotations.GuardedBy;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsDebugFlags;
import com.android.systemui.recents.events.EventBus;
import com.android.systemui.recents.events.activity.CancelEnterRecentsWindowAnimationEvent;
import com.android.systemui.recents.events.activity.ExitRecentsWindowFirstAnimationFrameEvent;
import com.android.systemui.recents.events.activity.LaunchTalpaTaskStartedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskFailedEvent;
import com.android.systemui.recents.events.activity.LaunchTaskSucceededEvent;
import com.android.systemui.recents.events.component.ScreenPinningRequestEvent;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.recents.model.Task;
import com.android.systemui.recents.model.TaskStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;


public class RecentsTalpaTransitionHelper {
    private static final String TAG = "RecentsTalpaTransitionHelper";

    private Context mContext;
    private Handler mHandler;

    /**
     * Special value for {@link #mAppTransitionAnimationSpecs}: Indicate that we are currently
     * waiting for the specs to be retrieved.
     */
    private static final List<AppTransitionAnimationSpec> SPECS_WAITING = new ArrayList<>();

    @GuardedBy("this")
    private List<AppTransitionAnimationSpec> mAppTransitionAnimationSpecs = SPECS_WAITING;

    private TaskTalpaViewTransform mTmpTransform = new TaskTalpaViewTransform();

    private class StartScreenPinningRunnableRunnable implements Runnable {

        private int taskId = -1;

        @Override
        public void run() {
            EventBus.getDefault().send(new ScreenPinningRequestEvent(mContext, taskId));
        }
    }
    private StartScreenPinningRunnableRunnable mStartScreenPinningRunnable
            = new StartScreenPinningRunnableRunnable();


    public RecentsTalpaTransitionHelper(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
    }

    public void launchTaskFromRecents(final TaskStack stack, @Nullable final Task task,
            final TaskTalpaHorizontalListView stackView,   final TaskTalpaView taskView, final boolean screenPinningRequested,
            final Rect bounds, int destinationStack) {
        final ActivityOptions opts = ActivityOptions.makeBasic();
        if (bounds != null) {
            opts.setLaunchBounds(bounds.isEmpty() ? null : bounds);
        }

        final ActivityOptions.OnAnimationStartedListener animStartedListener;
        if (task.thumbnail != null && task.thumbnail.getWidth() > 0 &&
                task.thumbnail.getHeight() > 0) {
            animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                @Override
                public void onAnimationStarted() {
                    // If we are launching into another task, cancel the previous task's
                    // window transition
                    EventBus.getDefault().send(new CancelEnterRecentsWindowAnimationEvent(task));
                    EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());

                    if (screenPinningRequested) {
                        // Request screen pinning after the animation runs
                        mStartScreenPinningRunnable.taskId = task.key.id;
                        mHandler.postDelayed(mStartScreenPinningRunnable, 350);
                    }
                }
            };
        } else {
            // This is only the case if the task is not on screen (scrolled offscreen for example)
            animStartedListener = new ActivityOptions.OnAnimationStartedListener() {
                @Override
                public void onAnimationStarted() {
                    EventBus.getDefault().send(new ExitRecentsWindowFirstAnimationFrameEvent());
                }
            };
        }

        if (taskView == null) {
            // If there is no task view, then we do not need to worry about animating out occluding
            // task views, and we can launch immediately
            startTaskActivity(stack, task, taskView, opts, animStartedListener);
        } else {
            LaunchTalpaTaskStartedEvent launchStartedEvent = new LaunchTalpaTaskStartedEvent(taskView);
            EventBus.getDefault().send(launchStartedEvent);
            startTaskActivity(stack, task, taskView, opts, animStartedListener);
        }
    }

    private void startTaskActivity(TaskStack stack, Task task, @Nullable TaskTalpaView taskView,
            ActivityOptions opts,final ActivityOptions.OnAnimationStartedListener animStartedListener) {
        SystemServicesProxy ssp = Recents.getSystemServices();
        if (ssp.startActivityFromRecents(mContext, task.key, task.title, opts)) {
            // Keep track of the index of the task launch
            int taskIndexFromFront = 0;
            int taskIndex = stack.indexOfStackTask(task);
            if (taskIndex > -1) {
                taskIndexFromFront = stack.getTaskCount() - taskIndex - 1;
            }
            EventBus.getDefault().send(new LaunchTaskSucceededEvent(taskIndexFromFront));
        } else {
            // Keep track of failed launches
            EventBus.getDefault().send(new LaunchTaskFailedEvent());
        }
		
		// TALPA bo.yang add@{
		if(taskView==null){
            return ;
        }
		// }

        Rect taskRect = taskView.getFocusedThumbnailRect();
        // Check both the rect and the thumbnail for null. The rect can be null if the user
        // decides to disallow animations, so automatic scrolling does not happen properly.

        // The thumbnail can be null if the app was partially launched on TV. In this case
        // we do not override the transition.
        if (taskRect == null || task.thumbnail == null) {
            return;
        }

        IRemoteCallback.Stub callback = null;
        if (animStartedListener != null) {
            callback = new IRemoteCallback.Stub() {
                @Override
                public void sendResult(Bundle data) throws RemoteException {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (animStartedListener != null) {
                                animStartedListener.onAnimationStarted();
                            }
                        }
                    });
                }
            };
        }
        try {
            Bitmap thumbnail = Bitmap.createScaledBitmap(task.thumbnail, taskRect.width(),
                    taskRect.height(), false);
            WindowManagerGlobal.getWindowManagerService()
                    .overridePendingAppTransitionAspectScaledThumb(thumbnail, taskRect.left,
                            taskRect.top, taskRect.width(), taskRect.height(), callback, true);
        } catch (RemoteException e) {
            LogUtil.w("Failed to override transition: " + e);
        }
    }

    public IRemoteCallback wrapStartedListener(final ActivityOptions.OnAnimationStartedListener listener) {
        if (listener == null) {
            return null;
        }
        return new IRemoteCallback.Stub() {
            @Override
            public void sendResult(Bundle data) throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onAnimationStarted();
                    }
                });
            }
        };
    }

    /**
     * Composes the transition spec when docking a task, which includes a full task bitmap.
     */
    public List<AppTransitionAnimationSpec> composeDockAnimationSpec(TaskTalpaView taskView,
                                                                     Rect bounds) {
        mTmpTransform.fillIn(taskView);
        Task task = taskView.getTask();
        Bitmap thumbnail = RecentsTalpaTransitionHelper.composeTaskBitmap(taskView, mTmpTransform);
        return Collections.singletonList(new AppTransitionAnimationSpec(task.key.id, thumbnail,
                bounds));
    }

    public static Bitmap composeTaskBitmap(TaskTalpaView taskView, TaskTalpaViewTransform transform) {
        float scale = transform.scale;
        int fromWidth = (int) (transform.rect.width() * scale);
        int fromHeight = (int) (transform.rect.height() * scale);
        if (fromWidth == 0 || fromHeight == 0) {
            LogUtil.e("Could not compose thumbnail for task: " + taskView.getTask() +
                    " at transform: " + transform);

            Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            b.eraseColor(Color.TRANSPARENT);
            return b;
        } else {
            Bitmap b = Bitmap.createBitmap(fromWidth, fromHeight,
                    Bitmap.Config.ARGB_8888);

            if (RecentsDebugFlags.Static.EnableTransitionThumbnailDebugMode) {
                b.eraseColor(0xFFff0000);
            } else {
                Canvas c = new Canvas(b);
                c.scale(scale, scale);
                taskView.draw(c);
                c.setBitmap(null);
            }
            return b.createAshmemBitmap();
        }
    }

    /**
     * Creates a future which will later be queried for animation specs for this current transition.
     *
     * @param composer The implementation that composes the specs on the UI thread.
     */
    public IAppTransitionAnimationSpecsFuture getAppTransitionFuture(
            final RecentsTransitionHelper.AnimationSpecComposer composer) {
        synchronized (this) {
            mAppTransitionAnimationSpecs = SPECS_WAITING;
        }
        return new IAppTransitionAnimationSpecsFuture.Stub() {
            @Override
            public AppTransitionAnimationSpec[] get() throws RemoteException {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (RecentsTalpaTransitionHelper.this) {
                            mAppTransitionAnimationSpecs = composer.composeSpecs();
                            RecentsTalpaTransitionHelper.this.notifyAll();
                        }
                    }
                });
                synchronized (RecentsTalpaTransitionHelper.this) {
                    while (mAppTransitionAnimationSpecs == SPECS_WAITING) {
                        try {
                            RecentsTalpaTransitionHelper.this.wait();
                        } catch (InterruptedException e) {}
                    }
                    if (mAppTransitionAnimationSpecs == null) {
                        return null;
                    }
                    AppTransitionAnimationSpec[] specs
                            = new AppTransitionAnimationSpec[mAppTransitionAnimationSpecs.size()];
                    mAppTransitionAnimationSpecs.toArray(specs);
                    mAppTransitionAnimationSpecs = SPECS_WAITING;
                    return specs;
                }
            }
        };
    }

}
