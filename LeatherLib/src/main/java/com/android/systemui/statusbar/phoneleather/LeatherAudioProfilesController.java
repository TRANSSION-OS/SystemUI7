package com.android.systemui.statusbar.phoneleather;

/**
 * Created by wujia.lin on 2017/2/15.
 */

public interface LeatherAudioProfilesController {
    String TALPA_AUDIOPROFILE_GENERAL = "talpa_audioprofile_general";
    String TALPA_AUDIOPROFILE_SILENT = "talpa_audioprofile_silent";
    String TALPA_AUDIOPROFILE_MEETING = "talpa_audioprofile_meeting";
    String TALPA_AUDIOPROFILE_OUTDOOR = "talpa_audioprofile_outdoor";

    void setAudioProfileUpdates(boolean update);
    void setAudio(String nextkey);
    String getActiveProfileKey();
    void setAudioProfileCallback(AudioProfileCallback callback);

    interface AudioProfileCallback {
        void onChangeState(String profileKey);
    }
}
