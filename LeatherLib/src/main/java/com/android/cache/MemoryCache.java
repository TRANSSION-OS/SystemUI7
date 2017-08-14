package com.android.cache;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.LruCache;

import com.android.systemui.statusbar.phoneleather.util.LeatherUtil;

public class MemoryCache implements ImageCache {
	private LruCache<String, Bitmap> mLruCache;
	
	public MemoryCache() {
		int memorySize = (int) (Runtime.getRuntime().maxMemory() / 1024);
		mLruCache = new LruCache<String, Bitmap>(memorySize / 8) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount() / 1024;
			}
		};
	}

	@Override
	public void put(String url, Bitmap bmp) {
		if(!TextUtils.isEmpty(url) && bmp != null) {
			String key = LeatherUtil.hashKeyForDisk(url);
			mLruCache.put(key, bmp);
		}
	}

	@Override
	public Bitmap get(String url) {
		if(!TextUtils.isEmpty(url)) {
			String key = LeatherUtil.hashKeyForDisk(url);
			return mLruCache.get(key);
		}
		return null;
	}

}
