package com.android.cache;

import android.content.Context;
import android.graphics.Bitmap;

public class DoubleCache implements ImageCache {
	private MemoryCache mMemoryCache;
	private DiskCache mDiskCache;

	public DoubleCache(Context context) {
		mMemoryCache = new MemoryCache();
		mDiskCache = new DiskCache(context);
	}
	
	@Override
	public void put(String url, Bitmap bmp) {
		mMemoryCache.put(url, bmp);
		mDiskCache.put(url, bmp);
	}

	@Override
	public Bitmap get(String url) {
		Bitmap bitmap = mMemoryCache.get(url);
		if(bitmap == null) {
			bitmap = mDiskCache.get(url);
			if(bitmap != null) {
				mMemoryCache.put(url, bitmap);
			}
		}
		return bitmap;
	}

}
