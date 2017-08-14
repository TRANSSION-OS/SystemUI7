//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.mediatek.keyguard;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;
import android.util.Log;

/**
 * 兼容MTK平台
 * @author andy
 * @version 1.0.0
 * @date 2017/03/20
 * @time 13:56
 */
public class TeleUtils {
    private static final String LOG_TAG = "TeleUtils";

    public TeleUtils() {
    }

    public static String updateOperator(String value, String arrayName) {
        Resources r = Resources.getSystem();
        Log.d("TeleUtils", " changeOperator: old value= " + value);

        try {
            int e = r.getIdentifier(arrayName, "array", "android");
            String[] itemList = r.getStringArray(e);
            Log.d("TeleUtils", " changeOperator: itemList length is " + itemList.length);
            String[] var6 = itemList;
            int var7 = itemList.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                String item = var6[var8];
                String[] parts = item.split("=");
                if(parts[0].equalsIgnoreCase(value)) {
                    String newName = parts[1];
                    Log.d("TeleUtils", "itemList found: parts[0]= " + parts[0] + " parts[1]= " + parts[1] + "  newName= " + newName);
                    return newName;
                }
            }
        } catch (NotFoundException var11) {
            Log.e("TeleUtils", "Error, string array resource ID not found: " + arrayName);
        }

        Log.d("TeleUtils", "changeOperator not found: original value= " + value + " newName= " + value);
        return value;
    }

    public static String concatenateEccList(String eccList, String number) {
        if(!TextUtils.isEmpty(number)) {
            if(!TextUtils.isEmpty(eccList)) {
                eccList = eccList + "," + number;
            } else {
                eccList = number;
            }
        }

        return eccList;
    }

    public static String concatenateCategoryList(String eccList, String category) {
        if(!TextUtils.isEmpty(category) && !TextUtils.isEmpty(eccList)) {
            eccList = eccList + "@" + category;
        }

        return eccList;
    }

    public static int bytesToInt(byte[] data) {
        if(data == null) {
            return -1;
        } else {
            int value = 0;

            for(int i = 0; i < data.length; ++i) {
                value |= (data[i] & 255) << (data.length - i - 1) * 8;
            }

            return value;
        }
    }

    public static byte[] intToBytes(int value, int len) {
        byte[] data = new byte[len];

        for(int i = 0; i < len; ++i) {
            data[i] = (byte)(value >> (len - i - 1) * 8 & 255);
        }

        return data;
    }

    public static String removeDupNumber(String str1, String str2) {
        String eccList = "";
        if(str1 != null && str2 != null) {
            String[] str1WithCategory = str1.split(",");
            String[] str2WithCategory = str2.split(",");
            boolean noSame = true;

            for(int i = 0; i < str1WithCategory.length; ++i) {
                for(int j = 0; j < str2WithCategory.length; ++j) {
                    String[] numberFromStr1 = str1WithCategory[i].split("@");
                    String[] numberFromStr2 = str2WithCategory[j].split("@");
                    if(numberFromStr1[0].equals(numberFromStr2[0])) {
                        noSame = false;
                        break;
                    }
                }

                if(noSame) {
                    eccList = concatenateEccList(eccList, str1WithCategory[i]);
                }

                noSame = true;
            }
        } else {
            eccList = str1;
        }

        return eccList;
    }

    public static String removeCategory(String eccListWithCategory) {
        String eccList = "";
        if(eccListWithCategory != null && eccListWithCategory != "") {
            String[] eccWithCategory = eccListWithCategory.split(",");

            for(int i = 0; i < eccWithCategory.length; ++i) {
                String[] numberFromEcc = eccWithCategory[i].split("@");
                eccList = concatenateEccList(eccList, numberFromEcc[0]);
            }
        }

        Log.d("TeleUtils", "ril.eccList:" + eccList);
        return eccList;
    }
}
