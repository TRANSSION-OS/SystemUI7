package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.systemui.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 状态栏图标重绘工具类
 * @author wujia.lin
 * @version 1.0.0
 * @date 2017/06/5
 * @time 17:42
 */

public class ItelNotificationIconUtil {

    private static ItelNotificationIconUtil NEW_INSTANCE = null;

    private Map<String, String> mPackageClassMap = new HashMap<>();
    private Context mContext;


    private ItelNotificationIconUtil(Context context) {
        mContext = context;
        initMap();
    }

    public static ItelNotificationIconUtil getInstance(Context context) {
        if(NEW_INSTANCE == null) {
            NEW_INSTANCE = new ItelNotificationIconUtil(context);
        }
        return NEW_INSTANCE;
    }

    private void initMap() {
        String[] array = mContext.getResources().getStringArray(R.array.itel_notification_icons);
        for(String item : array) {
            String[] result = item.split("#");
            if(result != null && result.length >= 2) {
                mPackageClassMap.put(result[0], result[1]);
            }
        }
    }

    public boolean itelNotificationIconEquals(String packageName) {
        return packageName != null && mPackageClassMap.containsKey(packageName);
    }

    public Drawable getDrawableByPackageName(String packageName) {
        if(itelNotificationIconEquals(packageName)) {
            String id = mPackageClassMap.get(packageName);
            int drawableId = mContext.getResources().getIdentifier(id, "drawable", mContext.getPackageName());
            return mContext.getDrawable(drawableId);
        }
        return null;
    }
}
