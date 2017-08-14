package com.android.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.text.TextUtils;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import itel.transsion.settingslib.utils.LogUtil;

public class ImageLoader {
	private static ImageLoader instance; 
	
	private RequestQueue mQueue;
	
	private ImageCache mImageCache = new MemoryCache();
	
	private ImageLoader(Context context) {
		mQueue = Volley.newRequestQueue(context); 
	}
	
	public static ImageLoader getInstance(Context context) {
		if(instance == null) {
			instance = new ImageLoader(context);
		}
		return instance;
	}
	
	public void setImageCache(ImageCache imageCache) {
		mImageCache = imageCache;
	}
	
	public void displayImage(String imageUrl, ImageView imageView, int resId) {
		LogUtil.d("ImageLoader:" + imageUrl);
		if(TextUtils.isEmpty(imageUrl) || imageView == null || imageUrl.equals(imageView.getTag())) {
			LogUtil.d("return");
			return;
		}
		Bitmap bitmap = mImageCache.get(imageUrl);
		if(bitmap != null) {
			imageView.setImageBitmap(bitmap);
			return;
		}
		//图片没有缓存，提交到线程池中下载图片
		submitLoadRequest(imageUrl, imageView, resId);
	}
	
	private void submitLoadRequest(final String imageUrl, final ImageView imageView, final int resId) {
		imageView.setImageBitmap(null);
		imageView.setTag(imageUrl);
		ImageRequest imageRequest = new ImageRequest(
				imageUrl,
				new Response.Listener<Bitmap>() {
					@Override
					public void onResponse(Bitmap response) {
						if(imageView.getTag().equals(imageUrl)) {
							imageView.setImageBitmap(response);
						}
						mImageCache.put(imageUrl, response);
					}
				}, 0, 0, Config.RGB_565, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				LogUtil.d("onErrorResponse", error);
				imageView.setImageResource(resId);
			}
		});
		mQueue.add(imageRequest);
	}
}
