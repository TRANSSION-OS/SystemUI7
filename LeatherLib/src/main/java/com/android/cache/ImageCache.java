package com.android.cache;

import android.graphics.Bitmap;

public interface ImageCache {
	void put(String url, Bitmap bmp);
	Bitmap get(String url);
}
