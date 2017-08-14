package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationUtils;

import java.util.ArrayList;
import java.util.List;

import itel.transsion.systemui.View.NotificationNumberView;
import itel.transsion.systemui.View.TopIconMerger;

/**
 * A controller for the space in the status bar to the left of the system icons. This area is
 * normally reserved for notifications.
 */
public class NotificationIconAreaController {
    private final NotificationColorUtil mNotificationColorUtil;

    private int mIconSize;
    private int mIconHPadding;
    private int mIconTint = Color.WHITE;
    private int mMostFilterNotificationCount;

    private PhoneStatusBar mPhoneStatusBar;
    protected View mNotificationIconArea;
    /// George:changed for status bar
    private TopIconMerger mNotificationIcons;
    private NotificationNumberView mMoreIcon;
    private ImageView mPhoneIcon;
    private ImageView mMsgIcon;
    private final Rect mTintArea = new Rect();
    private final List<String> listAppShowIcon;

    public NotificationIconAreaController(Context context, PhoneStatusBar phoneStatusBar) {
        mPhoneStatusBar = phoneStatusBar;
        mNotificationColorUtil = NotificationColorUtil.getInstance(context);

        initializeNotificationAreaViews(context);
        listAppShowIcon = loadShowIconList(context);
    }

    protected View inflateIconArea(LayoutInflater inflater) {
        return inflater.inflate(R.layout.notification_icon_area, null);
    }

    /**
     * Initializes the views that will represent the notification area.
     */
    protected void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        mNotificationIconArea = inflateIconArea(layoutInflater);

        mNotificationIcons =
                (TopIconMerger) mNotificationIconArea.findViewById(R.id.notificationIcons);

        mPhoneIcon = (ImageView) mNotificationIconArea.findViewById(R.id.phoneIcon);
        if (mPhoneIcon != null) {
            mPhoneIcon.setImageTintList(ColorStateList.valueOf(mIconTint));
        }
        mMsgIcon = (ImageView) mNotificationIconArea.findViewById(R.id.msgIcon);
        if (mMsgIcon != null) {
            mMsgIcon.setImageTintList(ColorStateList.valueOf(mIconTint));
        }
        mMoreIcon = (NotificationNumberView) mNotificationIconArea.findViewById(R.id.moreIcon);
        if (mMoreIcon != null) {
            /// George:Changed for color tint
            // mMoreIcon.setImageTintList(mIconTint);
            mMoreIcon.setDark(mIconTint);
        }
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        final LinearLayout.LayoutParams params = generateIconLayoutParams();
        for (int i = 0; i < mNotificationIcons.getChildCount(); i++) {
            View child = mNotificationIcons.getChildAt(i);
            child.setLayoutParams(params);
        }
    }

    @NonNull
    private LinearLayout.LayoutParams generateIconLayoutParams() {
        //linwujia add begin
        /*return new LinearLayout.LayoutParams(
                mIconSize + 2 * mIconHPadding, getHeight());*/
        return new LinearLayout.LayoutParams(
                mIconSize + 2 * mIconHPadding, mIconSize);
        //linwujia add end
    }

    private void reloadDimens(Context context) {
        Resources res = context.getResources();
        //linwujia edit begin
        //mIconSize = res.getDimensionPixelSize(Resources.getSystem().getIdentifier("status_bar_icon_size", "dimen","android"));
        mIconSize = res.getDimensionPixelSize(R.dimen.itel_status_bar_icon_size);
        //linwujia edit end
        mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
        mMostFilterNotificationCount = res.getInteger(R.integer.most_filter_notification_icon_count);
    }

    /**
     * Returns the view that represents the notification area.
     */
    public View getNotificationInnerAreaView() {
        return mNotificationIconArea;
    }

    /**
     * See {@link StatusBarIconController#setIconsDarkArea}.
     *
     * @param tintArea the area in which to tint the icons, specified in screen coordinates
     */
    public void setTintArea(Rect tintArea) {
        if (tintArea == null) {
            mTintArea.setEmpty();
        } else {
            mTintArea.set(tintArea);
        }
        applyNotificationIconsTint();
    }

    /**
     * Sets the color that should be used to tint any icons in the notification area. If this
     * method is not called, the default tint is {@link Color#WHITE}.
     */
    public void setIconTint(int iconTint) {
        mIconTint = iconTint;
        if (mMoreIcon != null) {
            /// George:Changed for color tint
            //mMoreIcon.setImageTintList(ColorStateList.valueOf(mIconTint));
            mMoreIcon.setDark(mIconTint);
        }
        if (mPhoneIcon != null) {
            mPhoneIcon.setImageTintList(ColorStateList.valueOf(mIconTint));
        }
        if (mMsgIcon != null) {
            mMsgIcon.setImageTintList(ColorStateList.valueOf(mIconTint));
        }
        applyNotificationIconsTint();
    }

    protected int getHeight() {
        return mPhoneStatusBar.getStatusBarHeight();
    }

    protected boolean shouldShowNotification(NotificationData.Entry entry,
                                             NotificationData notificationData) {
        if (notificationData.isAmbient(entry.key)
                && !NotificationData.showNotificationEvenIfUnprovisioned(entry.notification)) {
            return false;
        }
        if (!PhoneStatusBar.isTopLevelChild(entry)) {
            return false;
        }
        if (entry.row.getVisibility() == View.GONE) {
            return false;
        }

        return true;
    }

    /**
     * Updates the notifications with the given list of notifications to display.
     */
    public void updateNotificationIcons(NotificationData notificationData) {
        final LinearLayout.LayoutParams params = generateIconLayoutParams();

        ArrayList<NotificationData.Entry> activeNotifications =
                notificationData.getActiveNotifications();
        final int size = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(size);

        // Filter out ambient notifications and notification children.
        boolean isMsgShow = false;
        boolean isPhoneShow = false;
        int otherAppcount = 0;
        int overfilterAppcount = 0;
        int showcount = 0;
        for (int i = 0; i < size; i++) {
            NotificationData.Entry ent = activeNotifications.get(i);
            if (shouldShowNotification(ent, notificationData)) {
                /// George:add for show misscall or mms on the left of statusbar
                String packageName = ent.notification.getPackageName();
                /// George: android 6.0 use com.android.server.telecom and 7.0 use com.android.dialer for misscall
                if (packageName.equals("com.android.dialer") || packageName.equals("com.android.server.telecom")) {
                    ((StatusBarIconView) mPhoneIcon).updateNotificationIconScale();
                    ((StatusBarIconView) mPhoneIcon).set(ent.icon.getStatusBarIcon(), ent.notification.getPackageName());
                    mPhoneIcon.setVisibility(View.VISIBLE);
                    isPhoneShow = true;
                } else if (packageName.equals("com.android.mms")) {
                    /// George: use com.android.mms  for missed mms
                    ((StatusBarIconView) mMsgIcon).updateNotificationIconScale();
                    ((StatusBarIconView) mMsgIcon).set(ent.icon.getStatusBarIcon(), ent.notification.getPackageName());
                    mMsgIcon.setVisibility(View.VISIBLE);
                    isMsgShow = true;
                } else {
                    int length = toShow.size();
                    if (listAppShowIcon.contains(packageName)) {
                        if (length <= mMostFilterNotificationCount) {
                            toShow.add(ent.icon);
                            ++showcount;
                        } else {
                            ++overfilterAppcount;
                        }
                    } else {
                        ++otherAppcount;
                    }
                }
            }
        }

        mNotificationIcons.setShowAppCount(showcount);
        ArrayList<View> toRemove = new ArrayList<>();
        for (int i = 0; i < mNotificationIcons.getChildCount(); i++) {
            View child = mNotificationIcons.getChildAt(i);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }
        /// George:if we swipe from notificationStackScrollLayout we  should dissmiss  it on status bar for phone or mms
        if (!isPhoneShow) {
            mPhoneIcon.setVisibility(View.GONE);
        }
        if (!isMsgShow) {
            mMsgIcon.setVisibility(View.GONE);
        }
        if ((otherAppcount + overfilterAppcount) == 0) {
            mMoreIcon.setVisibility(View.GONE);
        } else {
            mMoreIcon.setNumber(otherAppcount + overfilterAppcount);
            mMoreIcon.setVisibility(View.VISIBLE);
        }
        final int toRemoveCount = toRemove.size();
        for (int i = 0; i < toRemoveCount; i++) {
            mNotificationIcons.removeView(toRemove.get(i));
        }

        for (int i = 0; i < toShow.size(); i++) {
            View v = toShow.get(i);
            if (v.getParent() == null) {
                mNotificationIcons.addView(v, i, params);
            }
        }

        /// George:dont need to resort notificaiton icons,because we sort as time
        /*// Re-sort notification icons
        final int childCount = mNotificationIcons.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View actual = mNotificationIcons.getChildAt(i);
            StatusBarIconView expected = toShow.get(i);
            if (actual == expected) {
                continue;
            }
            mNotificationIcons.removeView(expected);
            mNotificationIcons.addView(expected, i);
        }*/

        applyNotificationIconsTint();
    }

    /**
     * Applies {@link #mIconTint} to the notification icons.
     */
    private void applyNotificationIconsTint() {
        for (int i = 0; i < mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) mNotificationIcons.getChildAt(i);
            boolean isPreL = Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L));
            boolean colorize = !isPreL || NotificationUtils.isGrayscale(v, mNotificationColorUtil);
            if (colorize) {
                v.setImageTintList(ColorStateList.valueOf(
                        StatusBarIconController.getTint(mTintArea, v, mIconTint)));
            }
        }
    }

    //linwujia add begin for fix tfs bug#16100
    public void onConfigurationChanged() {
        applyNotificationIconsTint();
    }
    //linwujia add end

    /// George: load app list for show icon on notification area
    protected List<String> loadShowIconList(Context context) {
        final Resources res = context.getResources();
        String tileList = res.getString(R.string.show_icon_app_array);

        if (tileList == null) {
             Log.d("NIconAreaController","Loaded tile specs from config: wrong" );
            final ArrayList<String> tiles = new ArrayList<String>();
            return tiles;
        }
        final ArrayList<String> tiles = new ArrayList<String>();
        for (String tile : tileList.split(",")) {
            tile = tile.trim();
            if (tile.isEmpty()) continue;
            tiles.add(tile);
        }
        return tiles;
    }
}
