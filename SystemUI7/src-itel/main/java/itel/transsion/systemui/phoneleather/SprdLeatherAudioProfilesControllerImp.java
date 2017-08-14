package itel.transsion.systemui.phoneleather;

import android.content.Context;
import android.media.AudioManager;

import com.android.systemui.statusbar.phoneleather.LeatherAudioProfilesController;

/**
 * Created by wujia.lin on 2017/2/15.
 */

public class SprdLeatherAudioProfilesControllerImp implements LeatherAudioProfilesController {

    private Context mContext;
    private AudioManager mAudioManager;

    private AudioProfileCallback mAudioProfileCallback;

    public SprdLeatherAudioProfilesControllerImp(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void setAudioProfileUpdates(boolean update) {

    }

    @Override
    public void setAudio(String nextkey) {

    }

    @Override
    public String getActiveProfileKey() {
        String activeProfileKey = null;
        int ringerMode = mAudioManager.getRingerModeInternal();
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_SILENT:
                activeProfileKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_SILENT;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                activeProfileKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_MEETING;
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                activeProfileKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_GENERAL;
                break;
            case AudioManager.RINGER_MODE_OUTDOOR:
                activeProfileKey = LeatherAudioProfilesController.TALPA_AUDIOPROFILE_OUTDOOR;
                break;
        }
        return activeProfileKey;
    }

    @Override
    public void setAudioProfileCallback(AudioProfileCallback callback) {
        mAudioProfileCallback = callback;
        String activeProfileKey = getActiveProfileKey();
        callback.onChangeState(activeProfileKey);
    }
}
