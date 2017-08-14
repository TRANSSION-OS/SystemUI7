package itel.transsion.systemui.phoneleather;

import android.content.Context;

import com.android.systemui.statusbar.phoneleather.LeatherAudioProfilesController;

/**
 * Created by wujia.lin on 2017/2/15.
 */

public class MtkLeatherAudioProfilesControllerImp implements LeatherAudioProfilesController {
    public static final String MTK_AUDIOPROFILE_GENERAL = "mtk_audioprofile_general";
    public static final String MTK_AUDIOPROFILE_SILENT = "mtk_audioprofile_silent";
    public static final String MTK_AUDIOPROFILE_MEETING = "mtk_audioprofile_meeting";
    public static final String MTK_AUDIOPROFILE_OUTDOOR = "mtk_audioprofile_outdoor";

    private Context mContext;

    private AudioProfileCallback mAudioProfileCallback;

    public MtkLeatherAudioProfilesControllerImp(Context context) {
        mContext = context;
    }

    @Override
    public void setAudioProfileUpdates(boolean update) {

    }

    @Override
    public void setAudio(String nextkey) {

    }

    @Override
    public String getActiveProfileKey() {
        return null;
    }

    @Override
    public void setAudioProfileCallback(AudioProfileCallback callback) {
        mAudioProfileCallback = callback;
        String activeProfileKey = getActiveProfileKey();
        callback.onChangeState(activeProfileKey);
    }
}
