/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.MobileSignalController.MobileIconGroup;

class TelephonyIcons {
    //***** Signal strength icons

    static final int TELEPHONY_NUM_LEVELS = 5;

    //GSM/UMTS
    static final int TELEPHONY_NO_NETWORK = R.drawable.stat_sys_signal_null_itel;

    static final int[][] TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.stat_sys_signal_0_itel,
          R.drawable.stat_sys_signal_1_itel,
          R.drawable.stat_sys_signal_2_itel,
          R.drawable.stat_sys_signal_3_itel,
          R.drawable.stat_sys_signal_4_itel },
        { R.drawable.stat_sys_signal_0_fully_itel,
          R.drawable.stat_sys_signal_1_fully_itel,
          R.drawable.stat_sys_signal_2_fully_itel,
          R.drawable.stat_sys_signal_3_fully_itel,
          R.drawable.stat_sys_signal_4_fully_itel }
    };

    static final int QS_TELEPHONY_NO_NETWORK = R.drawable.ic_qs_signal_no_signal;

    static final int[][] QS_TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.ic_qs_signal_0,
          R.drawable.ic_qs_signal_1,
          R.drawable.ic_qs_signal_2,
          R.drawable.ic_qs_signal_3,
          R.drawable.ic_qs_signal_4 },
        { R.drawable.ic_qs_signal_full_0,
          R.drawable.ic_qs_signal_full_1,
          R.drawable.ic_qs_signal_full_2,
          R.drawable.ic_qs_signal_full_3,
          R.drawable.ic_qs_signal_full_4 }
    };

    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING = {
        { R.drawable.stat_sys_signal_0_itel,
          R.drawable.stat_sys_signal_1_itel,
          R.drawable.stat_sys_signal_2_itel,
          R.drawable.stat_sys_signal_3_itel,
          R.drawable.stat_sys_signal_4_itel },
        { R.drawable.stat_sys_signal_0_fully_itel,
          R.drawable.stat_sys_signal_1_fully_itel,
          R.drawable.stat_sys_signal_2_fully_itel,
          R.drawable.stat_sys_signal_3_fully_itel,
          R.drawable.stat_sys_signal_4_fully_itel }
    };

    //CarrierNetworkChange
    static final int[][] TELEPHONY_CARRIER_NETWORK_CHANGE = {
            { R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation },
            { R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation,
              R.drawable.stat_sys_signal_carrier_network_change_animation }
        };

    static final int[][] QS_TELEPHONY_CARRIER_NETWORK_CHANGE = {
        { R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation },
        { R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation,
          R.drawable.ic_qs_signal_carrier_network_change_animation }
    };

    static final int QS_DATA_R = R.drawable.ic_qs_signal_r;

    //***** Data connection icons

    //GSM/UMTS
    static final int[][] DATA_G = {
            { R.drawable.stat_sys_data_fully_connected_g_itel,
              R.drawable.stat_sys_data_fully_connected_g_itel,
              R.drawable.stat_sys_data_fully_connected_g_itel,
              R.drawable.stat_sys_data_fully_connected_g_itel},
            { R.drawable.stat_sys_data_fully_connected_g_itel,
              R.drawable.stat_sys_data_fully_connected_g_itel,
              R.drawable.stat_sys_data_fully_connected_g_itel,
              R.drawable.stat_sys_data_fully_connected_g_itel }
        };

    static final int QS_DATA_G = R.drawable.ic_qs_signal_g;

    static final int[][] DATA_3G = {
            { R.drawable.stat_sys_data_fully_connected_3g_itel,
              R.drawable.stat_sys_data_fully_connected_3g_itel,
              R.drawable.stat_sys_data_fully_connected_3g_itel,
              R.drawable.stat_sys_data_fully_connected_3g_itel },
            { R.drawable.stat_sys_data_fully_connected_3g_itel,
              R.drawable.stat_sys_data_fully_connected_3g_itel,
              R.drawable.stat_sys_data_fully_connected_3g_itel,
              R.drawable.stat_sys_data_fully_connected_3g_itel }
        };

    static final int QS_DATA_3G = R.drawable.ic_qs_signal_3g;

    static final int[][] DATA_E = {
            { R.drawable.stat_sys_data_fully_connected_e_itel,
              R.drawable.stat_sys_data_fully_connected_e_itel,
              R.drawable.stat_sys_data_fully_connected_e_itel,
              R.drawable.stat_sys_data_fully_connected_e_itel },
            { R.drawable.stat_sys_data_fully_connected_e_itel,
              R.drawable.stat_sys_data_fully_connected_e_itel,
              R.drawable.stat_sys_data_fully_connected_e_itel,
              R.drawable.stat_sys_data_fully_connected_e_itel }
        };

    static final int QS_DATA_E = R.drawable.ic_qs_signal_e;

    //3.5G
    static final int[][] DATA_H = {
            { R.drawable.stat_sys_data_fully_connected_h_itel,
              R.drawable.stat_sys_data_fully_connected_h_itel,
              R.drawable.stat_sys_data_fully_connected_h_itel,
              R.drawable.stat_sys_data_fully_connected_h_itel },
            { R.drawable.stat_sys_data_fully_connected_h_itel,
              R.drawable.stat_sys_data_fully_connected_h_itel,
              R.drawable.stat_sys_data_fully_connected_h_itel,
              R.drawable.stat_sys_data_fully_connected_h_itel }
    };

    static final int QS_DATA_H = R.drawable.ic_qs_signal_h;

    //CDMA
    // Use 3G icons for EVDO data and 1x icons for 1XRTT data
    static final int[][] DATA_1X = {
            { R.drawable.stat_sys_data_fully_connected_1x_itel,
              R.drawable.stat_sys_data_fully_connected_1x_itel,
              R.drawable.stat_sys_data_fully_connected_1x_itel,
              R.drawable.stat_sys_data_fully_connected_1x_itel },
            { R.drawable.stat_sys_data_fully_connected_1x_itel,
              R.drawable.stat_sys_data_fully_connected_1x_itel,
              R.drawable.stat_sys_data_fully_connected_1x_itel,
              R.drawable.stat_sys_data_fully_connected_1x_itel }
            };

    static final int QS_DATA_1X = R.drawable.ic_qs_signal_1x;

    // LTE and eHRPD
    static final int[][] DATA_4G = {
            { R.drawable.stat_sys_data_fully_connected_4g_itel,
              R.drawable.stat_sys_data_fully_connected_4g_itel,
              R.drawable.stat_sys_data_fully_connected_4g_itel,
              R.drawable.stat_sys_data_fully_connected_4g_itel },
            { R.drawable.stat_sys_data_fully_connected_4g_itel,
              R.drawable.stat_sys_data_fully_connected_4g_itel,
              R.drawable.stat_sys_data_fully_connected_4g_itel,
              R.drawable.stat_sys_data_fully_connected_4g_itel }
        };

    static final int QS_DATA_4G = R.drawable.ic_qs_signal_4g;

    // LTE branded "LTE"
    static final int[][] DATA_LTE = {
            { R.drawable.stat_sys_data_fully_connected_lte_itel,
                    R.drawable.stat_sys_data_fully_connected_lte_itel,
                    R.drawable.stat_sys_data_fully_connected_lte_itel,
                    R.drawable.stat_sys_data_fully_connected_lte_itel },
            { R.drawable.stat_sys_data_fully_connected_lte_itel,
                    R.drawable.stat_sys_data_fully_connected_lte_itel,
                    R.drawable.stat_sys_data_fully_connected_lte_itel,
                    R.drawable.stat_sys_data_fully_connected_lte_itel }
    };
    /// George:comment it because we dont hava this state
    /* SPRD: Add For LTE_CA @{ *//*
    static final int[][] DATA_4G_PLUS = {
            { R.drawable.stat_sys_data_fully_connected_4g_plus_itel,
              R.drawable.stat_sys_data_fully_connected_4g_plus_itel,
              R.drawable.stat_sys_data_fully_connected_4g_plus_itel,
              R.drawable.stat_sys_data_fully_connected_4g_plus_itel },
            { R.drawable.stat_sys_data_fully_connected_4g_plus_itel,
              R.drawable.stat_sys_data_fully_connected_4g_plus_itel,
              R.drawable.stat_sys_data_fully_connected_4g_plus_itel,
              R.drawable.stat_sys_data_fully_connected_4g_plus_itel }
    };*/

    static final int QS_DATA_4G_PLUS = R.drawable.ic_qs_signal_4g_plus;
    /* @} */

    static final int QS_DATA_LTE = R.drawable.ic_qs_signal_lte;

    static final int FLIGHT_MODE_ICON = R.drawable.stat_sys_airplane_mode;
    static final int ROAMING_ICON = R.drawable.stat_sys_data_fully_connected_roam_itel;
    static final int ICON_LTE = R.drawable.stat_sys_data_fully_connected_lte_itel;
    static final int ICON_G = R.drawable.stat_sys_data_fully_connected_g_itel;
    static final int ICON_E = R.drawable.stat_sys_data_fully_connected_e_itel;
    static final int ICON_H = R.drawable.stat_sys_data_fully_connected_h_itel;
    static final int ICON_3G = R.drawable.stat_sys_data_fully_connected_3g_itel;
    static final int ICON_4G = R.drawable.stat_sys_data_fully_connected_4g_itel;
    /// George:comment it we dont need this
    //static final int ICON_4G_PLUS = R.drawable.stat_sys_data_fully_connected_4g_plus_itel;
    static final int ICON_1X = R.drawable.stat_sys_data_fully_connected_1x_itel;
    static final int ICON_CARRIER_NETWORK_CHANGE =
            R.drawable.stat_sys_signal_carrier_network_change_animation;

    static final int ICON_DATA_DISABLED = R.drawable.stat_sys_data_disabled;

    static final int QS_ICON_LTE = R.drawable.ic_qs_signal_lte;
    static final int QS_ICON_3G = R.drawable.ic_qs_signal_3g;
    static final int QS_ICON_4G = R.drawable.ic_qs_signal_4g;
    static final int QS_ICON_4G_PLUS = R.drawable.ic_qs_signal_4g_plus;
    static final int QS_ICON_1X = R.drawable.ic_qs_signal_1x;
    static final int QS_ICON_CARRIER_NETWORK_CHANGE =
            R.drawable.ic_qs_signal_carrier_network_change_animation;

    static final int QS_ICON_DATA_DISABLED = R.drawable.ic_qs_data_disabled;

    static final int ICON_VOLTE = R.drawable.stat_sys_volte_itel;
    static final int ICON_HD_VOICE = R.drawable.stat_sys_volte_itel;
    static final int ICON_VOWIFI = R.drawable.stat_sys_vowifi_itel;

    static final MobileIconGroup CARRIER_NETWORK_CHANGE = new MobileIconGroup(
            "CARRIER_NETWORK_CHANGE",
            TelephonyIcons.TELEPHONY_CARRIER_NETWORK_CHANGE,
            TelephonyIcons.QS_TELEPHONY_CARRIER_NETWORK_CHANGE,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.ICON_CARRIER_NETWORK_CHANGE,
            TelephonyIcons.QS_ICON_CARRIER_NETWORK_CHANGE,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_carrier_network_change_mode,
            0,
            false,
            0
            );

    static final MobileIconGroup THREE_G = new MobileIconGroup(
            "3G",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_3g,
            TelephonyIcons.ICON_3G,
            true,
            TelephonyIcons.QS_DATA_3G
            );

    static final MobileIconGroup WFC = new MobileIconGroup(
            "WFC",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            0, 0, false, 0
            );

    static final MobileIconGroup UNKNOWN = new MobileIconGroup(
            "Unknown",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            0, 0, false, 0
            );

    static final MobileIconGroup E = new MobileIconGroup(
            "E",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_edge,
            TelephonyIcons.ICON_E,
            false,
            TelephonyIcons.QS_DATA_E
            );

    static final MobileIconGroup ONE_X = new MobileIconGroup(
            "1X",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_cdma,
            TelephonyIcons.ICON_1X,
            true,
            TelephonyIcons.QS_DATA_1X
            );

    static final MobileIconGroup G = new MobileIconGroup(
            "G",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_gprs,
            TelephonyIcons.ICON_G,
            false,
            TelephonyIcons.QS_DATA_G
            );

    static final MobileIconGroup H = new MobileIconGroup(
            "H",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_3_5g,
            TelephonyIcons.ICON_H,
            false,
            TelephonyIcons.QS_DATA_H
            );

    /* SPRD: FEATURE_SHOW_H/H+_FOR_VODAFONE @{ */
    static final MobileIconGroup HP = new MobileIconGroup(
            "H+",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_3_5g,
            /// George: change our name
            //R.drawable.stat_sys_data_fully_connected_hp_ex,
            R.drawable.stat_sys_data_fully_connected_hp_itel,
            false,
            TelephonyIcons.QS_DATA_H
            );
    /* @} */

    static final MobileIconGroup FOUR_G = new MobileIconGroup(
            "4G",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_4g,
            TelephonyIcons.ICON_4G,
            true,
            TelephonyIcons.QS_DATA_4G
            );
    /// George:comment it,Because we dont need this
    /* SPRD: Add For LTE_CA @{ */
    /*static final MobileIconGroup FOUR_G_PLUS = new MobileIconGroup(
            "4G+",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0,0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_4g_plus,
            TelephonyIcons.ICON_4G_PLUS,
            true,
            TelephonyIcons.QS_DATA_4G_PLUS
            );*/
    /* @} */

    static final MobileIconGroup LTE = new MobileIconGroup(
            "LTE",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_lte,
            TelephonyIcons.ICON_LTE,
            true,
            TelephonyIcons.QS_DATA_LTE
            );

    static final MobileIconGroup ROAMING = new MobileIconGroup(
            "Roaming",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH_ROAMING,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_data_connection_roaming,
            TelephonyIcons.ROAMING_ICON,
            false,
            TelephonyIcons.QS_DATA_R
            );

    static final MobileIconGroup DATA_DISABLED = new MobileIconGroup(
            "DataDisabled",
            TelephonyIcons.TELEPHONY_SIGNAL_STRENGTH,
            TelephonyIcons.QS_TELEPHONY_SIGNAL_STRENGTH,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH,
            0, 0,
            TelephonyIcons.TELEPHONY_NO_NETWORK,
            TelephonyIcons.QS_TELEPHONY_NO_NETWORK,
            AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0],
            R.string.accessibility_cell_data_off,
            TelephonyIcons.ICON_DATA_DISABLED,
            false,
            TelephonyIcons.QS_ICON_DATA_DISABLED
            );
}

