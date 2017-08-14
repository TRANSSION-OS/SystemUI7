/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.NetworkCapabilities;
import android.os.Looper;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.Config;
import com.android.systemui.statusbar.policy.NetworkControllerImpl.SubscriptionDefaults;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.RemoteException;
import com.android.ims.ImsManager;
import com.android.ims.internal.ImsManagerEx;
import com.android.ims.internal.IImsServiceEx;
import com.android.ims.internal.IImsRegisterListener;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.Objects;

import itel.transsion.settingslib.utils.TalpaUtils;


public class MobileSignalController extends SignalController<
        MobileSignalController.MobileState, MobileSignalController.MobileIconGroup> {
    private final TelephonyManager mPhone;
    private final SubscriptionDefaults mDefaults;
    private final String mNetworkNameDefault;
    private final String mNetworkNameSeparator;
    @VisibleForTesting
    final PhoneStateListener mPhoneStateListener;
    // Save entire info for logging, we only use the id.
    final SubscriptionInfo mSubscriptionInfo;

    // @VisibleForDemoMode
    final SparseArray<MobileIconGroup> mNetworkToIconLookup;

    // Since some pieces of the phone state are interdependent we store it locally,
    // this could potentially become part of MobileState for simplification/complication
    // of code.
    private int mDataNetType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private ServiceState mServiceState;
    private SignalStrength mSignalStrength;
    private MobileIconGroup mDefaultIcons;
    private Config mConfig;
    // SPRD: FEATURE_SHOW_SPREADTRUM_SIGNAL_CLUSTER_VIEW
    private final CallbackHandler mCallbackHandler;
    /* SPRD: Add For VOLTE @{ */
    private boolean mIsImsListenerRegistered;
    private boolean mIsVoLteEnable;
    private boolean mIsVoLteBoard;
    private IImsServiceEx mIImsServiceEx;
    private IImsRegisterListener.Stub mImsUtListenerExBinder;
    /* @} */
    // TODO: Reduce number of vars passed in, if we have the NetworkController, probably don't
    // need listener lists anymore.
    public MobileSignalController(Context context, Config config, boolean hasMobileData,
            TelephonyManager phone, CallbackHandler callbackHandler,
            NetworkControllerImpl networkController, SubscriptionInfo info,
            SubscriptionDefaults defaults, Looper receiverLooper) {
        super("MobileSignalController(" + info.getSubscriptionId() + ")", context,
                NetworkCapabilities.TRANSPORT_CELLULAR, callbackHandler,
                networkController);
        mNetworkToIconLookup = new SparseArray<>();
        mConfig = config;
        mPhone = phone;
        mDefaults = defaults;
        mSubscriptionInfo = info;
        mPhoneStateListener = new MobilePhoneStateListener(info.getSubscriptionId(),
                receiverLooper);
        mNetworkNameSeparator = getStringIfExists(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = getStringIfExists(
                Resources.getSystem().getIdentifier("lockscreen_carrier_default", "string","android"));
        // SPRD: FEATURE_SHOW_SPREADTRUM_SIGNAL_CLUSTER_VIEW
        mCallbackHandler = callbackHandler;

        mapIconSets();

        String networkName = info.getCarrierName() != null ? info.getCarrierName().toString()
                : mNetworkNameDefault;
        mLastState.networkName = mCurrentState.networkName = networkName;
        mLastState.networkNameData = mCurrentState.networkNameData = networkName;
        mLastState.enabled = mCurrentState.enabled = hasMobileData;
        mLastState.iconGroup = mCurrentState.iconGroup = mDefaultIcons;
        // Get initial data sim state.
        updateDataSim();
        if (TalpaUtils.isSPRDPlatform()) {
            initSPRCVoLTE();
            initIImsRegisterListener();
        }
        if(TalpaUtils.isMTKPlatform())
        {
            /// George:add for volte on status bar for mtk platform
            /// M: Support volte icon
            initImsRegisterState();
        }
    }
    /// M: Support volte icon @{
    private void initImsRegisterState(){
        int phoneId = SubscriptionManager.getPhoneId(mSubscriptionInfo.getSubscriptionId());
        try {
            boolean imsRegStatus = ImsManager
                    .getInstance(mContext, phoneId).getImsRegInfo();
            mCurrentState.imsRegState = imsRegStatus
                    ? ServiceState.STATE_IN_SERVICE : ServiceState.STATE_OUT_OF_SERVICE;
            Log.d(mTag, "init imsRegState:" + mCurrentState.imsRegState
                    + ",phoneId:" + phoneId);
        } catch (ImsException ex) {
            Log.e(mTag, "Fail to get Ims Status");
        }
    }
    /// @}

    public void initSPRCVoLTE() {
          /* SPRD: add for VoLTE @{*/
        mIsVoLteBoard = mContext.getResources().getBoolean(
                Resources.getSystem().getIdentifier("config_device_volte_available", "bool","android"));
        if(mIsVoLteBoard){
            Log.d(mTag, "mIsVoLteBoard: " + mIsVoLteBoard);
            IntentFilter filter = new IntentFilter();
            filter.addAction(ImsManager.ACTION_IMS_SERVICE_UP);
            filter.addAction(ImsManager.ACTION_IMS_SERVICE_DOWN);
            mContext.registerReceiver(mImsIntentReceiver, filter);
            tryRegisterImsListener();
        }
        /* @} */
    }

    public void setConfiguration(Config config) {
        mConfig = config;
        mapIconSets();
        updateTelephony();
    }

    public int getDataContentDescription() {
        return getIcons().mDataContentDescription;
    }

    public void setAirplaneMode(boolean airplaneMode) {
        mCurrentState.airplaneMode = airplaneMode;
        notifyListenersIfNecessary();
    }

    public void setUserSetupComplete(boolean userSetup) {
        mCurrentState.userSetup = userSetup;
        notifyListenersIfNecessary();
    }

    @Override
    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        boolean isValidated = validatedTransports.get(mTransportType);
        mCurrentState.isDefault = connectedTransports.get(mTransportType);
        // Only show this as not having connectivity if we are default.
        mCurrentState.inetCondition = 1;
        ///(isValidated || !mCurrentState.isDefault) ? 1 : 0; /// George:changed for overlay icon
        notifyListenersIfNecessary();
    }

    public void setCarrierNetworkChangeMode(boolean carrierNetworkChangeMode) {
        mCurrentState.carrierNetworkChangeMode = carrierNetworkChangeMode;
        updateTelephony();
    }

    /**
     * Start listening for phone state changes.
     */
    public void registerListener() {
        mPhone.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_CARRIER_NETWORK_CHANGE);
    }

    /**
     * Stop listening for phone state changes.
     */
    public void unregisterListener() {

        mPhone.listen(mPhoneStateListener, 0);
    }

    /* SPRD: Add For VOLTE @{ */
    private BroadcastReceiver mImsIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tryRegisterImsListener();
        }
    };

    private synchronized void tryRegisterImsListener() {

        if(mIsVoLteBoard){
            mIImsServiceEx = ImsManagerEx.getIImsServiceEx();
            if(mIImsServiceEx != null){
                try{
                    if(!mIsImsListenerRegistered){
                        mIsImsListenerRegistered = true;
                        mIImsServiceEx.registerforImsRegisterStateChanged(mImsUtListenerExBinder);
                    }
                }catch(RemoteException e){
                    Log.e(mTag, "regiseterforImsException: "+ e);
                }
            }
        }
    }

    public void initIImsRegisterListener() {
        mImsUtListenerExBinder = new IImsRegisterListener.Stub() {
                    @Override
                    public void imsRegisterStateChange(boolean isRegistered) {
//                        Log.d(mTag, "imsRegisterStateChange. isRegistered: " + isRegistered);
                        if (mIsVoLteBoard && mIsVoLteEnable != isRegistered) {
                            mIsVoLteEnable = isRegistered;
                            mCallbackHandler.setMobileVolteIndicators(mIsVoLteEnable,
                                    SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                                    TelephonyIcons.ICON_VOLTE);
                        }
                    }
         };
    }



    protected void finalize() throws Throwable {
        try{
            if(mIsImsListenerRegistered){
                mIsImsListenerRegistered = false;
                mIImsServiceEx.unregisterforImsRegisterStateChanged(mImsUtListenerExBinder);
            }
            mContext.unregisterReceiver(mImsIntentReceiver);
        }catch(RemoteException e){
            Log.e(mTag, "RemoteException: " + e);
        }
        super.finalize();
    }
    /* @} */

    /**
     * Produce a mapping of data network types to icon groups for simple and quick use in
     * updateTelephony.
     */
    private void mapIconSets() {
        mNetworkToIconLookup.clear();

        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyIcons.THREE_G);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UMTS, TelephonyIcons.THREE_G);

        if (!mConfig.showAtLeast3G) {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyIcons.UNKNOWN);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EDGE, TelephonyIcons.E);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_CDMA, TelephonyIcons.ONE_X);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyIcons.ONE_X);

            mDefaultIcons = TelephonyIcons.G;
        } else {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_EDGE,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_CDMA,
                    TelephonyIcons.THREE_G);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_1xRTT,
                    TelephonyIcons.THREE_G);
            mDefaultIcons = TelephonyIcons.THREE_G;
        }

        /* SPRD: FEATURE_SHOW_H/H+_FOR_VODAFONE @{ */
        MobileIconGroup hGroup = TelephonyIcons.THREE_G;
        MobileIconGroup hpGroup = TelephonyIcons.THREE_G;
        if (mConfig.hspaDataDistinguishable) {
            hGroup = TelephonyIcons.H;
            hpGroup = TelephonyIcons.HP;
        }
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSDPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSUPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSPA, hGroup);
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_HSPAP, hpGroup);
        /* @} */
        if (mConfig.show4gForLte) {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE, TelephonyIcons.FOUR_G);
            /// George:change it because we dont hava this state
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,TelephonyIcons.FOUR_G);
                    ///TelephonyIcons.FOUR_G_PLUS);
        } else {
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE, TelephonyIcons.LTE);
            mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_LTE_CA,
                    TelephonyIcons.LTE);
        }
        mNetworkToIconLookup.put(TelephonyManager.NETWORK_TYPE_IWLAN, TelephonyIcons.WFC);
    }

    @Override
    public void notifyListeners(SignalCallback callback) {
        MobileIconGroup icons = getIcons();

        String contentDescription = getStringIfExists(getContentDescription());
        String dataContentDescription = getStringIfExists(icons.mDataContentDescription);
        final boolean dataDisabled = mCurrentState.iconGroup == TelephonyIcons.DATA_DISABLED
                && mCurrentState.userSetup;

        // Show icon in QS when we are connected or need to show roaming or data is disabled.
        boolean showDataIcon = mCurrentState.dataConnected
                || mCurrentState.iconGroup == TelephonyIcons.ROAMING
                || dataDisabled;
        IconState statusIcon = new IconState(mCurrentState.enabled && !mCurrentState.airplaneMode,
                getCurrentIconId(), contentDescription);

        int qsTypeIcon = 0;
        IconState qsIcon = null;
        String description = null;
        // Only send data sim callbacks to QS.
        if (mCurrentState.dataSim) {
            qsTypeIcon = showDataIcon ? icons.mQsDataType : 0;
            qsIcon = new IconState(mCurrentState.enabled
                    && !mCurrentState.isEmergency, getQsCurrentIconId(), contentDescription);
            description = mCurrentState.isEmergency ? null : mCurrentState.networkName;
        }
        boolean activityIn = mCurrentState.dataConnected
                        && !mCurrentState.carrierNetworkChangeMode
                        && mCurrentState.activityIn;
        boolean activityOut = mCurrentState.dataConnected
                        && !mCurrentState.carrierNetworkChangeMode
                        && mCurrentState.activityOut;
        showDataIcon &= mCurrentState.isDefault
                || mCurrentState.iconGroup == TelephonyIcons.ROAMING
                || dataDisabled;
        int typeIcon = (showDataIcon || (mConfig.alwaysShowRAT && hasService()))
                ? icons.mDataType : 0;
        /* SPRD: FEATURE_SHOW_SPREADTRUM_SIGNAL_CLUSTER_VIEW @{ */
       if (DEBUG) {
            Log.d(mTag, "subId : " + mSubscriptionInfo.getSubscriptionId() + "; isRoaming : "
                + isRoaming() + "; dataConnected : " + mCurrentState.dataConnected
                + "; typeIcon : " + typeIcon + "; activityIn : " + activityIn + "; activityOut : "
                + activityOut);
        }
        /// George: add for volte on StatuaBar for Mtk platform
        int volteIcon = 0;
        if(TalpaUtils.isMTKPlatform()) {
            /// M: Support volte icon.Bug fix when airplane mode is on go to hide volte icon
            volteIcon = mCurrentState.airplaneMode && !isWfcEnable()
                    ? 0 : mCurrentState.volteIcon;
        }
        mCallbackHandler.setMobileRoamingIndicators(isRoaming(),
                mSubscriptionInfo.getSubscriptionId(),
                isRoaming() ? TelephonyIcons.ROAMING_ICON : 0);
        mCallbackHandler.setMobileDataConnectedIndicators(mCurrentState.dataConnected,
                mSubscriptionInfo.getSubscriptionId());
        /* @} */
        callback.setMobileDataIndicators(statusIcon, qsIcon, typeIcon, qsTypeIcon,
                activityIn, activityOut, dataContentDescription, description, icons.mIsWide,
                mSubscriptionInfo.getSubscriptionId(),volteIcon);
    }

    @Override
    protected MobileState cleanState() {
        return new MobileState();
    }

    private boolean hasService() {
        if (mServiceState != null) {
            // Consider the device to be in service if either voice or data
            // service is available. Some SIM cards are marketed as data-only
            // and do not support voice service, and on these SIM cards, we
            // want to show signal bars for data service as well as the "no
            // service" or "emergency calls only" text that indicates that voice
            // is not available.
            switch (mServiceState.getVoiceRegState()) {
                case ServiceState.STATE_POWER_OFF:
                    return false;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    return mServiceState.getDataRegState() == ServiceState.STATE_IN_SERVICE;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private boolean isCdma() {
        return (mSignalStrength != null) && !mSignalStrength.isGsm();
    }

    public boolean isEmergencyOnly() {
        return (mServiceState != null && mServiceState.isEmergencyOnly());
    }

    private boolean isRoaming() {
        if (isCdma()) {
            final int iconMode = mServiceState.getCdmaEriIconMode();
            return mServiceState.getCdmaEriIconIndex() != EriInfo.ROAMING_INDICATOR_OFF
                    && (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH);
        } else {
            return mServiceState != null && mServiceState.getRoaming();
        }
    }
    /// M: Support VoLte @{
    public boolean isLteNetWork() {
        return (mDataNetType == TelephonyManager.NETWORK_TYPE_LTE
            || mDataNetType == 139);//TelephonyManager.NETWORK_TYPE_LTEA
    }
    /// M: @}

    private boolean isCarrierNetworkChangeActive() {
        return mCurrentState.carrierNetworkChangeMode;
    }

    public void handleBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION)) {
            updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false),
                    intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                    intent.getStringExtra(TelephonyIntents.EXTRA_DATA_SPN),
                    intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                    intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
            notifyListenersIfNecessary();
        } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
            updateDataSim();
            notifyListenersIfNecessary();
        } else if (action.equals(ImsManager.ACTION_IMS_STATE_CHANGED)) {
          /// M: support dual Ims. /// George add for volte on status bar for mtk platform@{
            handleImsAction(intent);
            notifyListenersIfNecessary();
            /// @}
        }
    }
    /// M: Add for volte /// George:copy for mtk s31n for volte on StatusBar @{
    private void handleImsAction(Intent intent){
        mCurrentState.imsRegState = intent.getIntExtra(ImsManager.EXTRA_IMS_REG_STATE_KEY,
                ServiceState.STATE_OUT_OF_SERVICE);
        mCurrentState.isOverwfc = isImsOverWfc(intent);
        if (mCurrentState.isOverwfc){
            if (!(SystemProperties.get("persist.radio.multisim.config", "ss").equals("ss"))) {
                //mCurrentState.volteIcon = NetworkTypeUtils.WFC_ICON;
                mCurrentState.volteIcon = TelephonyIcons.ICON_VOWIFI;
            }
        } else {
            mCurrentState.volteIcon =
                mCurrentState.imsRegState == ServiceState.STATE_IN_SERVICE && isLteNetWork() ?
                        TelephonyIcons.ICON_VOLTE: 0; //NetworkTypeUtils.VOLTE_ICON : 0;//
        }
        Log.d(mTag, "handleImsAction imsRegstate=" + mCurrentState.imsRegState + ",overwfc=" +
                mCurrentState.isOverwfc + ",volteIconId=" + mCurrentState.volteIcon);
    }

    public boolean isImsOverWfc(Intent intent) {
        boolean[] enabledFeatures =
                intent.getBooleanArrayExtra(ImsManager.EXTRA_IMS_ENABLE_CAP_KEY);
        boolean wfcCapabilities = false;
        if (enabledFeatures != null && (enabledFeatures.length > 1)) {
            //Check if voice over wifi capability is available
            wfcCapabilities =
              (enabledFeatures[ImsConfig.FeatureConstants.FEATURE_TYPE_VOICE_OVER_WIFI] == true);
       }
       return wfcCapabilities;
    }

    public boolean isWfcEnable() {
        Class e1 = null;
        Method getDefaultMethod = null;
        Object telephonyManagerEx = null;
        Method isWifiCallingEnabledMethod = null;
        boolean isWifiCallingEnabledBoolean = false;
        try {
            e1 = Class.forName("com.mediatek.telephony.TelephonyManagerEx");
            getDefaultMethod = e1.getMethod("getDefault",new Class[0]);
            telephonyManagerEx = getDefaultMethod.invoke((Object) null,new Object[0]);
            isWifiCallingEnabledMethod = e1.getMethod("isWifiCallingEnabled",new Class[]{Integer.TYPE});
            isWifiCallingEnabledBoolean = (Boolean) isWifiCallingEnabledMethod.invoke(telephonyManagerEx,mSubscriptionInfo.getSubscriptionId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isWifiCallingEnabledBoolean;
        /// George:comment it because we use sprid framework.jar
       /* boolean isWfcEnabled = TelephonyManagerEx.getDefault().isWifiCallingEnabled(
            mSubscriptionInfo.getSubscriptionId());*/

     /*   int phoneId = SubscriptionManager.getPhoneId(mSubscriptionInfo.getSubscriptionId());
        Phone phone =  null;
        try {
             phone = PhoneFactory.getPhone(((phoneId < 0) ? SubscriptionManager.DEFAULT_PHONE_INDEX : phoneId));
        }catch (Exception e)
        {

        }
        if (phone != null){
            return phone.isWifiCallingEnabled();
        }*/
    }
    /// @}
    private void updateDataSim() {
        int defaultDataSub = mDefaults.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(defaultDataSub)) {
            mCurrentState.dataSim = defaultDataSub == mSubscriptionInfo.getSubscriptionId();
        } else {
            // There doesn't seem to be a data sim selected, however if
            // there isn't a MobileSignalController with dataSim set, then
            // QS won't get any callbacks and will be blank.  Instead
            // lets just assume we are the data sim (which will basically
            // show one at random) in QS until one is selected.  The user
            // should pick one soon after, so we shouldn't be in this state
            // for long.
            mCurrentState.dataSim = true;
        }
    }

    /**
     * Updates the network's name based on incoming spn and plmn.
     */
    void updateNetworkName(boolean showSpn, String spn, String dataSpn,
            boolean showPlmn, String plmn) {
        if (CHATTY) {
            Log.d("CarrierLabel", "updateNetworkName showSpn=" + showSpn
                    + " spn=" + spn + " dataSpn=" + dataSpn
                    + " showPlmn=" + showPlmn + " plmn=" + plmn);
        }
        StringBuilder str = new StringBuilder();
        StringBuilder strData = new StringBuilder();
        if (showPlmn && plmn != null) {
            str.append(plmn);
            strData.append(plmn);
        }
        if (showSpn && spn != null) {
            if (str.length() != 0) {
                str.append(mNetworkNameSeparator);
            }
            str.append(spn);
        }
        if (str.length() != 0) {
            mCurrentState.networkName = str.toString();
        } else {
            mCurrentState.networkName = mNetworkNameDefault;
        }
        if (showSpn && dataSpn != null) {
            if (strData.length() != 0) {
                strData.append(mNetworkNameSeparator);
            }
            strData.append(dataSpn);
        }

        // M: ALPS02744648 for C2K, there isn't dataspn parameter, when no plmn
        // and no dataspn, show spn instead "no service" here @{
        if (strData.length() == 0 && showSpn && spn != null) {
            Log.d("CarrierLabel", "show spn instead 'no service' here: " + spn);
            strData.append(spn);
        }
        // @}

        if (strData.length() != 0) {
            mCurrentState.networkNameData = strData.toString();
        } else {
            mCurrentState.networkNameData = mNetworkNameDefault;
        }
    }

    /**
     * Updates the current state based on mServiceState, mSignalStrength, mDataNetType,
     * mDataState, and mSimState.  It should be called any time one of these is updated.
     * This will call listeners if necessary.
     */
    private final void updateTelephony() {
        if (DEBUG) {
            Log.d(mTag, "updateTelephonySignalStrength: hasService=" + hasService()
                    + " ss=" + mSignalStrength);
        }
        mCurrentState.connected = hasService() && mSignalStrength != null;
        if (mCurrentState.connected) {
            if (!mSignalStrength.isGsm() && mConfig.alwaysShowCdmaRssi) {
                mCurrentState.level = mSignalStrength.getCdmaLevel();
            } else {
                mCurrentState.level = mSignalStrength.getLevel();
            }
        }
        if (mNetworkToIconLookup.indexOfKey(mDataNetType) >= 0) {
            mCurrentState.iconGroup = mNetworkToIconLookup.get(mDataNetType);
        } else {
            mCurrentState.iconGroup = mDefaultIcons;
        }
        mCurrentState.dataConnected = mCurrentState.connected
                && mDataState == TelephonyManager.DATA_CONNECTED;

        if (isCarrierNetworkChangeActive()) {
            mCurrentState.iconGroup = TelephonyIcons.CARRIER_NETWORK_CHANGE;
        }
        /* SPRD: FEATURE_ALWAYS_SHOW_RAT_ICON @{
        else if (!mConfig.alwaysShowRAT && isDataDisabled()) {
            mCurrentState.iconGroup = TelephonyIcons.DATA_DISABLED;
        }
        @} */

        if (isEmergencyOnly() != mCurrentState.isEmergency) {
            mCurrentState.isEmergency = isEmergencyOnly();
            mNetworkController.recalculateEmergency();
        }
        // Fill in the network name if we think we have it.
        if (mCurrentState.networkName == mNetworkNameDefault && mServiceState != null
                && !TextUtils.isEmpty(mServiceState.getOperatorAlphaShort())) {
            mCurrentState.networkName = mServiceState.getOperatorAlphaShort();
        }
        /// M: For volte type icon.
        if (!mCurrentState.isOverwfc) {
            mCurrentState.volteIcon = mCurrentState.imsRegState == ServiceState.STATE_IN_SERVICE
                && isLteNetWork() ? R.drawable.stat_sys_volte_itel: 0;//NetworkTypeUtils.VOLTE_ICON
        }

        notifyListenersIfNecessary();
    }

    private boolean isDataDisabled() {
        return !mPhone.getDataEnabled(mSubscriptionInfo.getSubscriptionId());
    }

    @VisibleForTesting
    void setActivity(int activity) {
        mCurrentState.activityIn = activity == TelephonyManager.DATA_ACTIVITY_INOUT
                || activity == TelephonyManager.DATA_ACTIVITY_IN;
        mCurrentState.activityOut = activity == TelephonyManager.DATA_ACTIVITY_INOUT
                || activity == TelephonyManager.DATA_ACTIVITY_OUT;
        notifyListenersIfNecessary();
    }

    @Override
    public void dump(PrintWriter pw) {
        super.dump(pw);
        pw.println("  mSubscription=" + mSubscriptionInfo + ",");
        pw.println("  mServiceState=" + mServiceState + ",");
        pw.println("  mSignalStrength=" + mSignalStrength + ",");
        pw.println("  mDataState=" + mDataState + ",");
        pw.println("  mDataNetType=" + mDataNetType + ",");
    }

    class MobilePhoneStateListener extends PhoneStateListener {
        public MobilePhoneStateListener(int subId, Looper looper) {
            super(subId, looper);
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            if (DEBUG) {
                Log.d(mTag, "onSignalStrengthsChanged signalStrength=" + signalStrength +
                        ((signalStrength == null) ? "" : (" level=" + signalStrength.getLevel())));
            }
            mSignalStrength = signalStrength;
            updateTelephony();
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            if (DEBUG) {
                Log.d(mTag, "onServiceStateChanged voiceState=" + state.getVoiceRegState()
                        + " dataState=" + state.getDataRegState());
            }
            mServiceState = state;

            /* SPRD: FEATURE_ALWAYS_SHOW_RAT_ICON @{ */
            if (mConfig.alwaysShowRAT) {
                mDataNetType = getRegNetworkType(state);
            }
            /* @} */
            updateTelephony();
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            if (DEBUG) {
                Log.d(mTag, "onDataConnectionStateChanged: state=" + state
                        + " type=" + networkType);
            }
            mDataState = state;
            mDataNetType = networkType;

            /* SPRD: FEATURE_ALWAYS_SHOW_RAT_ICON @{ */
            if (mConfig.alwaysShowRAT
                    && networkType == TelephonyManager.NETWORK_TYPE_UNKNOWN) {
                mDataNetType = getRegNetworkType(mServiceState);
            }
            /* @} */
            updateTelephony();
        }

        @Override
        public void onDataActivity(int direction) {
            if (DEBUG) {
                Log.d(mTag, "onDataActivity: direction=" + direction);
            }
            setActivity(direction);
        }

        @Override
        public void onCarrierNetworkChange(boolean active) {
            if (DEBUG) {
                Log.d(mTag, "onCarrierNetworkChange: active=" + active);
            }
            mCurrentState.carrierNetworkChangeMode = active;

            updateTelephony();
        }
    };

    static class MobileIconGroup extends SignalController.IconGroup {
        final int mDataContentDescription; // mContentDescriptionDataType
        final int mDataType;
        final boolean mIsWide;
        final int mQsDataType;

        public MobileIconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc,
                int sbNullState, int qsNullState, int sbDiscState, int qsDiscState,
                int discContentDesc, int dataContentDesc, int dataType, boolean isWide,
                int qsDataType) {
            super(name, sbIcons, qsIcons, contentDesc, sbNullState, qsNullState, sbDiscState,
                    qsDiscState, discContentDesc);
            mDataContentDescription = dataContentDesc;
            mDataType = dataType;
            mIsWide = isWide;
            mQsDataType = qsDataType;
        }
    }

    static class MobileState extends SignalController.State {
        String networkName;
        String networkNameData;
        boolean dataSim;
        boolean dataConnected;
        boolean isEmergency;
        boolean airplaneMode;
        boolean carrierNetworkChangeMode;
        boolean isDefault;
        boolean userSetup;

        /// M: Add for volte @{
        int imsRegState = ServiceState.STATE_POWER_OFF;
        boolean isOverwfc;
        int volteIcon;
        /// @}
        @Override
        public void copyFrom(State s) {
            super.copyFrom(s);
            MobileState state = (MobileState) s;
            dataSim = state.dataSim;
            networkName = state.networkName;
            networkNameData = state.networkNameData;
            dataConnected = state.dataConnected;
            isDefault = state.isDefault;
            isEmergency = state.isEmergency;
            airplaneMode = state.airplaneMode;
            carrierNetworkChangeMode = state.carrierNetworkChangeMode;
            userSetup = state.userSetup;
            /// George:add for volte on statusbar for mtk platform
            if(TalpaUtils.isMTKPlatform()) {
                /// M: Add for volte
                imsRegState = state.imsRegState;
                isOverwfc = state.isOverwfc;
                volteIcon = state.volteIcon;
            }
        }

        @Override
        protected void toString(StringBuilder builder) {
            super.toString(builder);
            builder.append(',');
            builder.append("dataSim=").append(dataSim).append(',');
            builder.append("networkName=").append(networkName).append(',');
            builder.append("networkNameData=").append(networkNameData).append(',');
            builder.append("dataConnected=").append(dataConnected).append(',');
            builder.append("isDefault=").append(isDefault).append(',');
            builder.append("isEmergency=").append(isEmergency).append(',');
            builder.append("airplaneMode=").append(airplaneMode).append(',');
            builder.append("carrierNetworkChangeMode=").append(carrierNetworkChangeMode)
                    .append(',');
            builder.append("userSetup=").append(userSetup);
            if(TalpaUtils.isMTKPlatform()) {
                /// M: Add for volte. /// George: add for volte for mtk platform;
                builder.append("imsRegState=").append(imsRegState).append(',');
                builder.append("isOverwfc=").append(isOverwfc).append(',');
                builder.append("volteIconId=").append(volteIcon).append(',');
            }
        }

        @Override
        public boolean equals(Object o) {
            /// George:add for volte on statusbar for mtk platform
            return super.equals(o)
                    && Objects.equals(((MobileState) o).networkName, networkName)
                    && Objects.equals(((MobileState) o).networkNameData, networkNameData)
                    && ((MobileState) o).dataSim == dataSim
                    && ((MobileState) o).dataConnected == dataConnected
                    && ((MobileState) o).isEmergency == isEmergency
                    && ((MobileState) o).airplaneMode == airplaneMode
                    && ((MobileState) o).carrierNetworkChangeMode == carrierNetworkChangeMode
                    && ((MobileState) o).userSetup == userSetup
                    && ((MobileState) o).isDefault == isDefault
                    && (TalpaUtils.isMTKPlatform()?(((MobileState) o).volteIcon == volteIcon):true);
        }
    }

    /* SPRD: FEATURE_ALWAYS_SHOW_RAT_ICON @{ */
    private int getRegNetworkType(ServiceState state) {
        int voiceNetworkType = state.getVoiceNetworkType();
        int dataNetworkType = state.getDataNetworkType();

        int retNetworkType =
                (dataNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN)
                ? voiceNetworkType : dataNetworkType;
        return retNetworkType;
    }
    /* @} */
    /// M: Support for PLMN.George @{
    public SubscriptionInfo getControllerSubInfo() {
        return mSubscriptionInfo;
    }
}
