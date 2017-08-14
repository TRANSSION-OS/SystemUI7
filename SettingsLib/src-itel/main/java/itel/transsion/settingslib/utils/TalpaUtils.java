package itel.transsion.settingslib.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.provider.Settings;

/**
 * Created by Talpa DepingHuang on 17/2/11.
 */

public class TalpaUtils {
    /**
     * 平台定义
     */
    public static int PLATFORM_DEFAULT      = 0;
    public static int PLATFORM_MTK          = 1;
    public static int PLATFORM_SPRD         = 2;

    public static String PKG_NAME = "com.talpa.theme";

    /**
     * 此方法用于判断当前是否为展讯平台
     * （SystemUI7代码基于展讯平台移植，特殊代码需用此方法抽离判断）
     * @return
     */
    public static boolean isSPRDPlatform(){
        return getCurrentPlatform() == PLATFORM_SPRD;
    }

    /**
     * 此方法用于判断当前是否为MTK平台
     * @return
     */
    public static boolean isMTKPlatform(){
        return getCurrentPlatform() == PLATFORM_MTK;
    }

    public static int getCurrentPlatform(){

        // 目前只能判断两种平台
        boolean ISMTK_PLATFORM = !SystemProperties.get("ro.mediatek.platform").equals("");
        if (ISMTK_PLATFORM){
            return  PLATFORM_MTK;
        }

        return PLATFORM_SPRD;
    }


    /**
     * 是否为超级省电模式
     * @param context
     * @return
     */
    public static boolean isSuperPowerSaveMode(Context context) {
        if (TalpaUtils.isMTKPlatform()){
            return Settings.Secure.getInt(context.getContentResolver(), "super_power_saving_mode", 0)== 1 ? true : false;
        }
        else {
            return
                    Settings.System.getInt(context.getContentResolver(), "super_power_saving_mode", 0) == 1 ? true : false ||
                            Settings.System.getInt(context.getContentResolver(), "itel_super_power_save_mode", 0) == 1 ? true : false ;
        }
    }


    /**
     * 是否支持多用户（默认为支持）
     * 说明：上层控制，只是菜单隐藏，不是去掉实际功能，以免影响CTS测试
     * @return
     */
    public static boolean isSupportMultiUser() {
        int usersFlag = SystemProperties.getInt("ro.sys.configure.users", 1);
        return (usersFlag == 0)? false:true;
    }


    /**
     * 获取TalpaTheme应用图标
     * @return
     */
    public Drawable getTalpaThemeIcon(Context skinPackageContext, String resourceName) {
        int id = 0;
        id = getResourcesFromSkin(skinPackageContext, "drawable", resourceName, PKG_NAME);
        return skinPackageContext.getResources().getDrawable(id, null);
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

}
