package com.android.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.android.systemui.statusbar.phoneleather.util.LeatherUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import itel.transsion.settingslib.utils.LogUtil;
import libcore.io.DiskLruCache;

public class DiskCache implements ImageCache {
	
	private DiskLruCache mDiskLruCache;
	
	public DiskCache(Context context) {
		try {  
		    File cacheDir = LeatherUtil.getDiskCacheDir(context, "bitmap");
		    LogUtil.d("cacheDir:" + cacheDir);
		    if (!cacheDir.exists()) {
		        cacheDir.mkdirs();  
		    }  
		    mDiskLruCache = DiskLruCache.open(cacheDir, LeatherUtil.getAppVersion(context), 1, 10 * 1024 * 1024);  
		} catch (IOException e) {  
		    e.printStackTrace();  
		}  
	}

	@Override
	public void put(String url, Bitmap bmp) {		
		try {
			if(!TextUtils.isEmpty(url) && bmp != null) {
				String key = LeatherUtil.hashKeyForDisk(url);
				DiskLruCache.Editor editor = mDiskLruCache.edit(key);  
				if (editor != null) {  
				    OutputStream outputStream = editor.newOutputStream(0);  
				    bmp.compress(CompressFormat.PNG, 100, outputStream);
				    editor.commit();
				}  
				mDiskLruCache.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Bitmap get(String url) {
		Bitmap bitmap = null;
		try {
			if(!TextUtils.isEmpty(url)) {
				String key = LeatherUtil.hashKeyForDisk(url);
				DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);  
			    if (snapShot != null) {
					InputStream is = snapShot.getInputStream(0);
			        bitmap = BitmapFactory.decodeStream(is);  
			        snapShot.close();
			    }
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

}
