package com.android.systemui.recents.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by bo.yang1 on 2017/4/17.
 */

public class ConvertIconsUtil {

    public Context mTalpaResContext;

    public static String PKG_NAME = "com.talpa.theme";

    public String[] mLabelContent;

    HashMap<String, String> packageClassMap = new HashMap<String, String>();
    HashMap<String, String> widgetsPackageNameMap = new HashMap<String, String>();
    HashMap<String, String> gmsAndFbPackageClassMap = new HashMap<String, String>();

    public ConvertIconsUtil(Context l) {
        initResContext(l);
    }

    public void initResContext(Context l) {
        try {
            mTalpaResContext = l.createPackageContext(PKG_NAME, l.CONTEXT_IGNORE_SECURITY | l.CONTEXT_INCLUDE_CODE);
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            Log.e("sunset", "Utils2Icon initResContext exception=" + e);
            mTalpaResContext = null;
            e.printStackTrace();
        }
    }

    public Drawable getDrawableFromSkin(Context skinPackageContext, String resourceName) {
        int id = 0;
        id = getResourcesFromSkin(skinPackageContext, "drawable", resourceName, PKG_NAME);
        return skinPackageContext.getResources().getDrawable(id);
    }

    /**
     * 获得"com.talpa.theme"资源定义的各个应用图标存入容器内
     * 资源定义形式：包名$启动类名#图标名称
     *
     * @return
     */
    public String[] loadLabelMap(Context skinPackageContext, String arrayName) {
        if (null != skinPackageContext) {
            int id = skinPackageContext.getResources().getIdentifier(arrayName, "array", PKG_NAME);
            mLabelContent = skinPackageContext.getResources().getStringArray(id);

            for (String packageClasseIcon : mLabelContent) {
                String[] packageClasses_Icon = packageClasseIcon.split("#");
                if (packageClasses_Icon.length == 2) {
                    String[] packageClasses = packageClasses_Icon[0].split("\\|");
                    for (String s : packageClasses) {
                        // Log.i("loadLabelMap","loadLabelMaps ="+s
                        // +" packageClasses_Icon[1] =
                        // "+packageClasses_Icon[1]);
                        packageClassMap.put(s.trim(), packageClasses_Icon[1]);
                        String[] packageClass = s.split("\\$");
                        if (packageClass.length == 2) {
                            packageClassMap.put(packageClass[0], packageClasses_Icon[1]);
                        } else if (packageClass.length == 1) {
                            widgetsPackageNameMap.put(packageClass[0], packageClasses_Icon[1]);
                        }
                    }
                }
            }
        }
        return mLabelContent;
    }

    private int getResourcesFromSkin(Context talpaResContext, String resourceType, String resourceName,
                                     String packgeName) {
        int id = 0;
        try {
            id = talpaResContext.getResources().getIdentifier(resourceName, resourceType, packgeName);
        } catch (Exception e) {
            id = 0;
        }
        return id;
    }

    public Context getTalpaContext() {
        return mTalpaResContext;
    }

    public String getIconName(String type, String key) {
        String bitmapName = null;
        if (type.equals("lable_map_widgets")) {
            bitmapName = widgetsPackageNameMap.get(key.trim());
        } else {
            bitmapName = packageClassMap.get(key.trim());
        }
        String iconName = null;
        if (bitmapName != null) {
            String[] s = bitmapName.split("\\.");
            if (s.length == 2) {
                iconName = s[0];
            }
        }
        return iconName;
    }
}
