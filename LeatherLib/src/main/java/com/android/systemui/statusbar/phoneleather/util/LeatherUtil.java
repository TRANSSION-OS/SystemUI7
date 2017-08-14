package com.android.systemui.statusbar.phoneleather.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemProperties;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import itel.transsion.settingslib.utils.LogUtil;
import libcore.io.DiskLruCache;

/**
 * Created by wujia.lin on 2017/3/3.
 */

public class LeatherUtil {

    public static void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM, "Camera");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = generalFileName();
        File file = new File(appDir, fileName);
        LogUtil.d(file.getAbsolutePath());
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*// 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    public static void saveImageToGallery(Context context, byte[] data) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DCIM, "Camera");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = generalFileName();
        File file = new File(appDir, fileName);
        LogUtil.d(file.getAbsolutePath());
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    public static String generalFileName() {
        String format = "yyyyMMdd_HHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(System.currentTimeMillis());
        String fileName = "IMG_" + simpleDateFormat.format(calendar.getTime()) + ".jpg";
        LogUtil.d(fileName);
        return fileName;
    }

    public static String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                cachePath = context.getExternalCacheDir().getPath();
            } else {
                cachePath = context.getCacheDir().getPath();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LogUtil.d(ex.toString());
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 获取平台是否支持皮套模式
     * @return 状态值
     */
    public static boolean isSupportLeather() {
        InputStream is = null;
        DataInputStream dis = null;
        boolean isSupportLeather = false;

        try {
            is = new FileInputStream("/sys/class/switch/hall/state");
            dis = new DataInputStream(is);
            byte[] buf = new byte[1];
            dis.read(buf);
            int state = Integer.parseInt(new String(buf,0,1));
            isSupportLeather = true;
        } catch(Exception e){
            // if any error occurs
            e.printStackTrace();
            isSupportLeather = false;
        }finally{
            DiskLruCache.closeQuietly(is);
            DiskLruCache.closeQuietly(dis);
        }
        return isSupportLeather;
    }

    /**
     * 主动获取皮套状态值
     * @return 状态值
     */
    public static int readHallState() {
        InputStream is = null;
        DataInputStream dis = null;
        int state = -1;

        try {
            is = new FileInputStream("/sys/class/switch/hall/state");
            dis = new DataInputStream(is);
            byte[] buf = new byte[1];
            dis.read(buf);
            state = Integer.parseInt(new String(buf,0,1));
            LogUtil.d("-hallsd-stateds="+state);
            return state;
        } catch(Exception e){
            // if any error occurs
            e.printStackTrace();
        } finally{
            DiskLruCache.closeQuietly(is);
            DiskLruCache.closeQuietly(dis);
        }
        return state;
    }

    /**M: @{
     * Whether this boot is from power off alarm or schedule power on or normal boot.
     * @return
     */
    public static boolean bootFromPoweroffAlarm() {
        boolean ret = false;
        try {
            String bootReason = SystemProperties.get("sys.boot.reason");
            ret = (bootReason != null && bootReason.equals("1")) ? true : false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
