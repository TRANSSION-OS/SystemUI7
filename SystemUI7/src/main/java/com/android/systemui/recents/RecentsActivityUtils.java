package com.android.systemui.recents;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AddonManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import com.android.systemui.R;
import com.android.systemui.SystemUIApplication;
import com.android.systemui.recents.views.RecentsTalpaView;
import com.android.systemui.recents.views.TaskStackView;
import com.android.systemui.recents.views.TaskView;
import com.android.systemui.statusbar.phone.PhoneStatusBar;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import itel.transsion.settingslib.utils.LogUtil;
import itel.transsion.settingslib.utils.TalpaUtils;

public class RecentsActivityUtils {
    public static final String LOG_TAG = "RecentsActivityUtils";

    static RecentsActivityUtils mInstance;

    private Context mAddonContext;
    private Activity mActivity;
    private PhoneStatusBar mStatusBar;
    private ActivityManager am;
    private PackageManager pm;
    // TALPA: DepingHuang  modified  add start{
    //private RecentsView mRecentsView;
    private RecentsTalpaView mRecentsView;
    // TALPA: DepingHuang    add end}

    private ImageButton mClearButton;
    private final static String METHOD_TOGGLE_RECENTS = "toggleRecent";
    private final static String METHOD_DISMISS_TASK = "dismissAllTask";

    public static RecentsActivityUtils getInstance() {
        if (mInstance == null) {
            // Tapla DepingHuang Modified @{
            if (TalpaUtils.isSPRDPlatform()) {
                mInstance = (RecentsActivityUtils) AddonManager.getDefault().
                        getAddon(R.string.feature_display_for_clearall_task, RecentsActivityUtils.class);
            }
            else {
                mInstance = new RecentsActivityUtils();
            }
            // @}
        }
        return mInstance;
    }
    // TALPA: DepingHuang  modified  RecentsView to TalpaRecentsView add start{
    public void init(final Context context, RecentsTalpaView recentsview, ImageButton imagebutton) {
        // TALPA: DepingHuang    add end}
        mAddonContext = context;
        mActivity = (Activity) mAddonContext;
        mStatusBar = ((SystemUIApplication) mActivity.getApplication())
                .getComponent(PhoneStatusBar.class);
        am = (ActivityManager) mActivity
                .getSystemService(Context.ACTIVITY_SERVICE);
        pm = (PackageManager) mActivity.getPackageManager();
        mRecentsView = recentsview;
        mClearButton = imagebutton;
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        removeAllTasks(context);
                    }
                }).start();
            }
        });

    }

    /* SPRD: Bug 475644 new feature of quick cleaning. @{ */
    public boolean isSupportClearAllTasks() {
        return true;
    }
    /* @} */

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mRecentsView == null) {
                return;
            }
            int childCount = mRecentsView.getChildCount();
            if (childCount == 0)
                if (mStatusBar != null) {
                    toggleRecents();
                } else {
                    mActivity.finish();
                }
            for (int i = 0; i < childCount; i++) {
                View tsv = mRecentsView.getChildAt(i);
                if (!(tsv instanceof TaskStackView))
                    continue;
                int count = ((TaskStackView) tsv).getChildCount();
                if (count == 0)
                    continue;
                for (int j = 0; j < count; j++) {
                    View tv = ((TaskStackView) tsv).getChildAt(j);
                    if (!(tv instanceof TaskView))
                        continue;
                    dismissAllTask((TaskView) tv);
                }
            }
            if (mStatusBar != null) {
                //toggleRecents();
            } else {
                mActivity.finish();
            }
        }
    };

    public void toggleRecents() {
        try {
            mStatusBar.getClass().getMethod(METHOD_TOGGLE_RECENTS)
                    .invoke(mStatusBar);
        } catch (NoSuchMethodException ex) {
            Log.i(LOG_TAG, "executeInit NoSuchMethodException " + ex);
        } catch (IllegalArgumentException ex) {
            Log.i(LOG_TAG, "executeInit IllegalArgumentException  " + ex);
        } catch (IllegalAccessException ex) {
            Log.i(LOG_TAG, "executeInit IllegalAccessException  " + ex);
        } catch (InvocationTargetException ex) {
            Log.i(LOG_TAG, "executeInit InvocationTargetException  " + ex);
        }
    }

    public void dismissAllTask(TaskView taskview) {
        try {
            taskview.getClass().getMethod(METHOD_DISMISS_TASK)
                    .invoke(taskview);
        } catch (NoSuchMethodException ex) {
            Log.i(LOG_TAG, "executeInit NoSuchMethodException " + ex);
        } catch (IllegalArgumentException ex) {
            Log.i(LOG_TAG, "executeInit IllegalArgumentException  " + ex);
        } catch (IllegalAccessException ex) {
            Log.i(LOG_TAG, "executeInit IllegalAccessException  " + ex);
        } catch (InvocationTargetException ex) {
            Log.i(LOG_TAG, "executeInit InvocationTargetException  " + ex);
        }
    }

    private static final int MAX_TASKS = 100;

    /**
     * 对外接口，提供非RecentActivity存活的情况下直接清理全部任务
     * @param context
     */
    public void removeAllTasks(Context context) {
        if (null == am ) {
            am = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
        }
        final List<ActivityManager.RecentTaskInfo> recentTasks = am
                .getRecentTasks(MAX_TASKS,ActivityManager.RECENT_IGNORE_HOME_STACK_TASKS);
        for (int i = 0; i < recentTasks.size(); ++i) {
            final ActivityManager.RecentTaskInfo recentInfo = recentTasks
                    .get(i);
            String packageName = recentInfo.baseIntent.getComponent().getPackageName();
            LogUtil.d("recentInfo:" + packageName);
            if (packageName.contains("com.transsion.powersaver")
                    || packageName.contains("com.android.battery")){  // 不清理超级省电Task
                continue;
            }
            // 如果锁定任务不清除，这里添加preference的读取判断
            // ..

            am.removeTask(recentInfo.persistentId);
        }
        mHandler.sendEmptyMessage(0);
    }

}
