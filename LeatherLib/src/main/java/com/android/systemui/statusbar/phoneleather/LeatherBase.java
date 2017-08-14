package com.android.systemui.statusbar.phoneleather;

public interface LeatherBase {
	public void anim();
	public void reDraw();
	public void setPhoneAndSms(int phoneNum, int smsNum);
	public void changePhoneOrSms(int phoneNum, int smsNum);
	public void setVisibility(int visibility);
}
