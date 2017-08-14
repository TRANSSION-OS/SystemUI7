package itel.transsion.settingslib.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

/**
 * 参数存储工具类
 * @author andy
 * @version 1.0.0
 * @date 2017/06/5
 * @time 17:27
 */
public class ConfigService {
	private final String PREFERENCES_NAME = "systemui_config";
	private SharedPreferences iPreferences;

	public ConfigService(Context context) {
		iPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
	}

	public ConfigService(Context context, String name) {
		iPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
	}
	
	public boolean setString(String key, String value) {
		if(TextUtils.isEmpty(key))
			return false;
		
		Editor editor = iPreferences.edit();
		editor.remove(key);
		editor.putString(key, value);
		editor.commit();
		return true;
	}
	
	public String getString(String key) {
		return getString(key, "");
	}
	
	public String getString(String key, String defaultValue) {
		return  iPreferences.getString(key, defaultValue);
	}
	
	public boolean setInteger(String key, int value) {
		if(TextUtils.isEmpty(key))
			return false;
		Editor editor = iPreferences.edit();
		editor.remove(key);
		editor.putInt(key, value);
		editor.commit();
		return true;		
	}
	
	public int getInteger(String key) {
		return  getInteger(key, 0);		
	}
	
	public int getInteger(String key, int defaultValue) {
		return  iPreferences.getInt(key, defaultValue);		
	}
	
	public boolean setBoolean(String key, boolean value) {
		if(TextUtils.isEmpty(key))
			return false;
		Editor editor = iPreferences.edit();
		editor.remove(key);
		editor.putBoolean(key, value);
		editor.commit();
		return true;			
	}
	
	public boolean getBoolean(String key) {
		return  getBoolean(key, false);		
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		return  iPreferences.getBoolean(key, defaultValue);		
	}
	
	public boolean remove(String key) {
		if(TextUtils.isEmpty(key))
			return false;
		Editor editor = iPreferences.edit();
		editor.remove(key).commit();
		return true;
	}
	
	public void clear() {		
		Editor editor = iPreferences.edit();
		editor.clear().commit();
	}
	public Editor getEditor(){
		Editor editor = iPreferences.edit();
		return editor;
	}
	public SharedPreferences getSharedPreferences(){
		return iPreferences;
	}
}
