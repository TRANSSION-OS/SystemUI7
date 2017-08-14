package itel.transsion.settingslib.utils;

import android.content.res.Resources;

/**
 * Created by peterHuang on 2017/4/13.
 */

public final class TalpaResUtil {
    public static final class android{
        public static final class id {
            private static int getIDFromAndroid(String name){
                return  Resources.getSystem().getIdentifier(name, "id","android");
            }

            private static int big_text = 0;
            public static int big_text(){
                if (big_text == 0){
                    big_text = getIDFromAndroid("big_text");
                }
                return big_text;
            }

            private static int icon = 0;
            public static int icon(){
                if (icon == 0){
                    icon = getIDFromAndroid("icon");
                }
                return icon;
            }

            private static int notification_header = 0;
            public static int notification_header(){
                if (notification_header == 0){
                    notification_header = getIDFromAndroid("notification_header");
                }
                return notification_header;
            }

            private static int expand_button = 0;
            public static int expand_button(){
                if (expand_button == 0) {
                    expand_button = getIDFromAndroid("expand_button");
                }
                return expand_button;
            }

            private static int media_actions = 0;
            public static int media_actions(){
                if (media_actions == 0) {
                    media_actions = getIDFromAndroid("media_actions");
                }
                return media_actions;
            }

            private static int notification_messaging = 0;
            public static int notification_messaging(){
                if (notification_messaging == 0) {
                    notification_messaging = getIDFromAndroid( "notification_messaging");
                }
                return notification_messaging;
            }

            // NotificationTemplateViewWrapper.java @{
            private static int right_icon = 0;
            public static int right_icon(){
                if (right_icon == 0) {
                    right_icon = getIDFromAndroid("right_icon");
                }
                return right_icon;
            }

            private static int title = 0;
            public static int title(){
                if ( title == 0) {
                    title = getIDFromAndroid("title");
                }
                return title;
            }

            private static int text = 0;
            public static int text(){
                if (text == 0) {
                    text = getIDFromAndroid("text");
                }
                return text;
            }

            private static int progress = 0;
            public static int progress(){
                if (progress == 0) {
                    progress = getIDFromAndroid("progress");
                }
                return progress;
            }

            private static int actions_container = 0;
            public static int actions_container(){
                if (actions_container == 0){
                    actions_container = getIDFromAndroid("actions_container");
                }
                return actions_container;
            }

            private static int notification_main_column = 0;
            public static int notification_main_column(){
                if (notification_main_column == 0){
                    notification_main_column = getIDFromAndroid("notification_main_column");
                }
                return notification_main_column;
            }
            // @}

            private static int status_bar_latest_event_content = 0;
            public static int status_bar_latest_event_content(){
                if (status_bar_latest_event_content == 0) {
                    status_bar_latest_event_content = getIDFromAndroid("status_bar_latest_event_content");
                }
                return status_bar_latest_event_content;
            }

            // statusbar package @{
            private static int profile_badge = 0;
            public static int profile_badge(){
                if (profile_badge == 0) {
                    profile_badge = getIDFromAndroid("profile_badge");
                }
                return profile_badge;
            }

            private static int app_name_text = 0;
            public static int app_name_text(){
                if (app_name_text == 0){
                    app_name_text = getIDFromAndroid("app_name_text");
                }
                return app_name_text;
            }

            private static int header_text = 0;
            public static int header_text(){
                if (header_text == 0) {
                    header_text = getIDFromAndroid("header_text");
                }
                return header_text;
            }

            private static int header_text_divider = 0;
            public static int header_text_divider(){
                if (header_text_divider == 0) {
                    header_text_divider = getIDFromAndroid("header_text_divider");
                }
                return header_text_divider;
            }

            private static int time_divider = 0;
            public static int time_divider(){
                if (time_divider == 0){
                    time_divider = getIDFromAndroid("time_divider");
                }
                return time_divider;
            }

            private static int time = 0;
            public static int time(){
                if (time == 0){
                    time = getIDFromAndroid("time");
                }
                return time;
            }
            private static int chronometer = 0;
            public static int chronometer(){
                if (chronometer == 0){
                    chronometer = getIDFromAndroid("chronometer");
                }
                return chronometer;
            }
            //@}

        }


        public static final class dimen {
            private static int getIDFromAndroid(String name){
                return Resources.getSystem().getIdentifier(name, "dimen","android");
            }

            // statusbar package @{
            private static int notification_content_margin_end = 0;
            public static int notification_content_margin_end(){
                if (notification_content_margin_end == 0){
                    notification_content_margin_end = getIDFromAndroid("notification_content_margin_end");
                }
                return notification_content_margin_end;
            }

            private static int notification_action_list_height = 0;
            public static int notification_action_list_height(){
                if (notification_action_list_height == 0){
                    notification_action_list_height = getIDFromAndroid("notification_action_list_height");
                }
                return notification_action_list_height;
            }

            private static int status_bar_icon_size = 0;
            public static int status_bar_icon_size(){
                if (status_bar_icon_size == 0){
                    status_bar_icon_size = getIDFromAndroid("status_bar_icon_size");
                }
                return status_bar_icon_size;
            }
            // @}
            private static int notification_content_margin_top = 0;
            public static int notification_content_margin_top(){
                if (notification_content_margin_top == 0){
                    notification_content_margin_top = getIDFromAndroid("notification_content_margin_top");
                }
                return notification_content_margin_top;
            }

            private static int notification_content_margin_bottom = 0;
            public static int notification_content_margin_bottom(){
                if (notification_content_margin_bottom == 0){
                    notification_content_margin_bottom = getIDFromAndroid("notification_content_margin_bottom");
                }
                return notification_content_margin_bottom;
            }
        }

        public static final class color {
            private static int getIDFromAndroid(String name){
                return Resources.getSystem().getIdentifier(name, "color","android");
            }

            private static int notification_icon_default_color = 0;
            public static int notification_icon_default_color(){
                if (notification_icon_default_color == 0) {
                    notification_icon_default_color = getIDFromAndroid("notification_icon_default_color");
                }
                return notification_icon_default_color;
            }

            private static int system_notification_accent_color = 0;
            public static int system_notification_accent_color(){
                if (system_notification_accent_color == 0) {
                    system_notification_accent_color = getIDFromAndroid("system_notification_accent_color");
                }
                return system_notification_accent_color;
            }
        }

        public static final class string {
            private static int getIDFromAndroid(String name) {
                return Resources.getSystem().getIdentifier(name, "string", "android");
            }
            private static int share = 0;
            public static int share(){
                if (share == 0) {
                    share = getIDFromAndroid("share");
                }
                return share;
            }

            private static int delete = 0;
            public static int delete(){
                if (delete == 0) {
                    delete = getIDFromAndroid("delete");
                }
                return delete;
            }
        }

    }

}
