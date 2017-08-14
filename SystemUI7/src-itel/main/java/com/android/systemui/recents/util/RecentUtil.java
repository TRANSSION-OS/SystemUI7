package com.android.systemui.recents.util;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import itel.transsion.settingslib.utils.LogUtil;

/**
 * Created by deping.huang on 2016/12/7.
 */

public class RecentUtil {


    public static  void saveMyBitmap(String bitName,Bitmap mBitmap){
        File f = new File("/mnt/sdcard/" + bitName + ".png");
        try {
            f.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            LogUtil.e("在保存图片时出错："+e.toString());
        }
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得机身内存总大小
     *
     * @return
     */
    public static String getRamTotalSizeFormat(Context context) {
        // 系统内存信息文件
        /*String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(
                    localFileReader, 8192);
            // 读取meminfo第一行，系统总内存大小
            str2 = localBufferedReader.readLine();

            arrayOfString = str2.split("\\s+");
            // 获得系统总内存，单位是KB
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue();
            localBufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 内存大小规格化  Formatter.formatFileSize 大于900格式化为GB 不符合需求
        //return Formatter.formatFileSize(context.getApplicationContext(), 935860*1024L);
        if (initial_memory >= 1048576L) { // 1G
            return String.format("%.2fG",(float)initial_memory/1048576);
        }
        else {
            return String.format("%dMB", initial_memory/1024);
        }*/

        // TALPA bo.yang modify for tfs bug 17551 @{
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        // Talpa bo.yang1 modify for totalMem value @{
        //long initial_memory = 0;
        //initial_memory=mi.totalMem/1024;
        /*if (initial_memory >= 1048576L) { // 1G
            return String.format("%.2fG",(float)initial_memory/1048576);
        }
        else {
            return String.format("%dMB", initial_memory / 1024);
        }*/
        float totalMem=(((float)mi.totalMem/1048576L)/(float) 1024);
        return String.format("%.2f",(float)Math.ceil(totalMem));
        /*if (mi.totalMem >= 1000000000L) { // 1G
            return String.format("%.2f",(float)mi.totalMem/1000000000);
        }
        else {
            return String.format("%d", mi.totalMem/1048576);
        }*/
    }

    /**
     * 获得当前可用内存大小
     *
     * @return
     */
    public static String getRamAvailableSizeFormat(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        // mi.availMem; 当前系统的可用内存
        // 将获取的内存大小规格化
        //return Formatter.formatFileSize(context.getApplicationContext(), mi.availMem);
        // TALPA bo.yang modify for tfs bug 17551 @{
        if (mi.availMem >= 1073741824L) { // 1G
            return String.format("%.2f", (float)mi.availMem/1073741824L);
        }
        else {
            return String.format("%d", mi.availMem/1048576L);
        }
    }
    public static String getRamAvailableSizeFormat(long availMemMB) {

        if (availMemMB >= 1024) { // 1G
            return String.format("%.2f", (float)availMemMB/1024);
        }
        else {
            return String.format("%d", availMemMB);
        }

    }
    public static long getRamAvailableSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);

        return  mi.availMem/1048576L;

    }
}
