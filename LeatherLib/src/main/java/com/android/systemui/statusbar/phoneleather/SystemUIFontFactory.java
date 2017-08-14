package com.android.systemui.statusbar.phoneleather;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wujia.lin on 2016/12/13.
 */

public class SystemUIFontFactory {
    private static SystemUIFontFactory mInstance;

    private Context mContext;
    private Map<String, Typeface> mTypefaceMap;

    private SystemUIFontFactory(Context context)  {
        mContext = context;
        mTypefaceMap = new HashMap<>();
    }

    public static SystemUIFontFactory getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new SystemUIFontFactory(context);
        }
        return mInstance;
    }

    /*
    @typefaceName 字体库文件名称，包括后缀名
     */
    public Typeface getTypefaceByName(String typefaceName) {
        Typeface typeface = mTypefaceMap.get(typefaceName);
        if(typeface == null) {
            typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/" + typefaceName);
            mTypefaceMap.put(typefaceName, typeface);
        }
        return typeface;
    }

}
