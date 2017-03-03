/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-13, The Linux Foundation. All rights reserved
 *
 * Not a Contribution.
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

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.CellBroadcastMessage;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.MSimConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.R;
import com.android.settings.SelectSubscription;
import com.codeaurora.telephony.msim.MSimPhoneFactory;
import com.android.settings.Utils;

import java.lang.ref.WeakReference;

/**
 * Display the following information
 * # Battery Strength  : TODO
 * # Uptime
 * # Awake Time
 * # XMPP/buzz/tickle status : TODO
 *
 */
public class MSimStatus extends PreferenceActivity {

    private static final String TAG = "MSimStatus";
    private static final boolean DEBUG = true;
    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_SERIAL_NUMBER = "serial_number";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String[] PHONE_RELATED_ENTRIES = {
        KEY_DATA_STATE,
        KEY_NETWORK_TYPE
    };

    private static final int SINGLE_SIM = 1;
    private static final int EVENT_UPDATE_STATS = 500;
    private static final int GSM_SIGNAL_UNKNOWN = 99;
    private static final int GSM_SIGNAL_NULL = -113;
    private static final String BUTTON_SELECT_SUB_KEY = "button_aboutphone_msim_status";

    private MSimTelephonyManager mTelephonyManager;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    private PhoneStateListener[] mPhoneStateListener;
    private Resources mRes;
    private Preference mUptime;
    private boolean mShowLatestAreaInfo = false;

    private String mUnknown = null;
    private int mNumPhones = 0;

    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    // private int mDataState = TelephonyManager.DATA_DISCONNECTED;
    private int[] mDataState = new int[] {
            TelephonyManager.DATA_DISCONNECTED, TelephonyManager.DATA_DISCONNECTED
    };

    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_ICC_ID = "icc_id";
    private static final String KEY_PRL_VERSION = "prl_version";
    private static final String KEY_MIN_NUMBER = "min_number";
    private static final String KEY_ESN_NUMBER = "esn_number";
    private static final String KEY_MEID_NUMBER = "meid_number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";

    private static final String[] RELATED_ENTRIES = {
            KEY_SERVICE_STATE,
            KEY_OPERATOR_NAME,
            KEY_ROAMING_STATE,
            KEY_PHONE_NUMBER,
            KEY_IMEI,
            KEY_IMEI_SV,
            KEY_ICC_ID,
            KEY_PRL_VERSION,
            KEY_MIN_NUMBER,
            KEY_ESN_NUMBER,
            KEY_MEID_NUMBER,
            KEY_SIGNAL_STRENGTH,
            KEY_BASEBAND_VERSION,
            KEY_LATEST_AREA_INFO
    };

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";

    private String[] esnNumberSummery;
    private String[] meidNumberSummery;
    private String[] minNumberSummery;
    private String[] prlVersionSummery;
    private String[] imeiSVSummery;
    private String[] imeiSummery;
    private String[] iccIdSummery;
    private String[] numberSummery;
    private String[] serviceStateSummery;
    private String[] roamingStateSummery;
    private String[] operatorNameSummery;
    private String[] mSigStrengthSummery;
    private String[] dataStateSummery;
    private String[] areaInfoSummery;

    private SignalStrength[] mSignalStrength;
    private ServiceState[] mServiceState;

    private Phone[] mPhone;

    private String[] SIM;

    private String[] networkSummery;

    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                int subscriptionId = extras.getInt("subscription");
                if (cbMessage != null && cbMessage.getServiceCategory() == 50) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo, subscriptionId);
                }
            }
        }
    };

    private void initMSimSummery(String[] str) {
        for (int i = 0; i < mTelephonyManager.getPhoneCount(); i++) {
            if (str[i] == null) {
                str[i] = SIM[i] + ": " + mUnknown;
            }
        }
    }

    private String getSimSummery(int subscription, String msg) {
        //The msg get from SystemProperties.get() my be "unknown"
        if ((msg == null) || msg.equalsIgnoreCase("unknown")) {
            msg = mUnknown;
        }
        return SIM[subscription] + ": " + msg;
    }

    private void setMSimSummery(String key, String... msgs) {
        if (mNumPhones == SINGLE_SIM) {
            if (msgs[0] == null)
                removePreferenceFromScreen(key);
            else
                setSummaryText(key, msgs[0]);
        } else if (mNumPhones == mTelephonyManager.getPhoneCount()) {
            if (msgs[MSimConstants.SUB1] == null && msgs[MSimConstants.SUB2] == null)
                removePreferenceFromScreen(key);
            else {
                StringBuffer summery = new StringBuffer();
                if (msgs[MSimConstants.SUB1] != null)
                    summery.append(msgs[MSimConstants.SUB1]);
                if (msgs[MSimConstants.SUB2] != null) {
                    if (summery.length() > 0) {
                        summery.append("\n");
                    }
                    summery.append(msgs[MSimConstants.SUB2]);
                }
                setSummaryText(key, summery.toString());
            }
        }
    }

    private String getMultiSimName(int subscription) {
        return Settings.System.getString(getContentResolver(),
                Settings.System.MULTI_SIM_NAME[subscription]);
    }

    private Handler mHandler;

    private static class MyHandler extends Handler {
        private WeakReference<MSimStatus> mStatus;

        public MyHandler(MSimStatus activity) {
            mStatus = new WeakReference<MSimStatus>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MSimStatus status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new MyHandler(this);

        mTelephonyManager = (MSimTelephonyManager)getSystemService(MSIM_TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.device_info_msim_status);

        mNumPhones = MSimTelephonyManager.getDefault().getPhoneCount();
        mPhoneStateListener = new PhoneStateListener[mNumPhones];

        esnNumberSummery = new String[mTelephonyManager.getPhoneCount()];
        meidNumberSummery = new String[mTelephonyManager.getPhoneCount()];
        minNumberSummery = new String[mTelephonyManager.getPhoneCount()];
        prlVersionSummery = new String[mTelephonyManager.getPhoneCount()];
        imeiSVSummery = new String[mTelephonyManager.getPhoneCount()];
        imeiSummery = new String[mTelephonyManager.getPhoneCount()];
        iccIdSummery = new String[mTelephonyManager.getPhoneCount()];
        numberSummery = new String[mTelephonyManager.getPhoneCount()];
        serviceStateSummery = new String[mTelephonyManager.getPhoneCount()];
        roamingStateSummery = new String[mTelephonyManager.getPhoneCount()];
        operatorNameSummery = new String[mTelephonyManager.getPhoneCount()];
        mSigStrengthSummery = new String[mTelephonyManager.getPhoneCount()];
        dataStateSummery = new String[mTelephonyManager.getPhoneCount()];
        areaInfoSummery = new String[mTelephonyManager.getPhoneCount()];

        mSignalStrength = new SignalStrength[mTelephonyManager.getPhoneCount()];
        mServiceState = new ServiceState[mTelephonyManager.getPhoneCount()];

        mPhone = new Phone[mTelephonyManager.getPhoneCount()];

        SIM = new String[mTelephonyManager.getPhoneCount()];

        networkSummery = new String[mTelephonyManager.getPhoneCount()];

        int indexOfCDMA = -1;

        for (int i = 0; i < mNumPhones; i++) {
            SIM[i] = getMultiSimName(i);
            mPhone[i] = MSimPhoneFactory.getPhone(i);
            if ("CDMA".equals(mPhone[i].getPhoneName())) {
                indexOfCDMA = i;
            } else {
                // only show area info when SIM country is Brazil
                if ("br".equals(mTelephonyManager.getSimCountryIso(0))) {
                    mShowLatestAreaInfo = true;
                }
            }
            mPhoneStateListener[i] = getPhoneStateListener(i);
        }
        if (!mShowLatestAreaInfo) {
            removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
        }

        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);

        PreferenceScreen selectSub = (PreferenceScreen) findPreference(BUTTON_SELECT_SUB_KEY);
        if (selectSub != null) {
            Intent intent = selectSub.getIntent();
            intent.putExtra(SelectSubscription.PACKAGE, "com.android.settings");
            intent.putExtra(SelectSubscription.TARGET_CLASS,
                    "com.android.settings.deviceinfo.MSimSubscriptionStatus");
        }

        mRes = getResources();
        // Real-time update language for icon.
        mUnknown = mRes.getString(R.string.device_info_default);

        mUptime = findPreference("up_time");

        if (Utils.isWifiOnly(getApplicationContext())) {
            for (String key : PHONE_RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        }

        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();

        String serial = Build.SERIAL;
        if (serial != null && !serial.equals("")) {
            setSummaryText(KEY_SERIAL_NUMBER, serial);
        } else {
            removePreferenceFromScreen(KEY_SERIAL_NUMBER);
        }

        initMSimSummery(esnNumberSummery);
        initMSimSummery(meidNumberSummery);
        initMSimSummery(minNumberSummery);
        initMSimSummery(prlVersionSummery);
        initMSimSummery(imeiSVSummery);
        initMSimSummery(imeiSummery);
        initMSimSummery(iccIdSummery);
        initMSimSummery(numberSummery);
        initMSimSummery(serviceStateSummery);
        initMSimSummery(roamingStateSummery);
        initMSimSummery(operatorNameSummery);
        initMSimSummery(mSigStrengthSummery);
        initMSimSummery(dataStateSummery);
        initMSimSummery(networkSummery);
        initMSimSummery(areaInfoSummery);

        updateMSimSummery(indexOfCDMA);
    }

    private void updateMSimSummery(int indexOfCDMA) {
        if (DEBUG)
            Log.d(TAG, "cdma index is " + indexOfCDMA);

        if (Utils.isWifiOnly(getApplicationContext())) {
            for (String key : RELATED_ENTRIES) {
                removePreferenceFromScreen(key);
            }
        } else {
            for (int i = 0; i < mNumPhones; i++) {

                String rawNumber = mPhone[i].getLine1Number(); // may be null or empty
                String formattedNumber = null;
                if (!TextUtils.isEmpty(rawNumber)) {
                    formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
                }
                numberSummery[i] = getSimSummery(i, formattedNumber);

                if (i == indexOfCDMA) {
                    prlVersionSummery[i] = getSimSummery(i, mPhone[i].getCdmaPrlVersion());
                    esnNumberSummery[i] = getSimSummery(i, mPhone[i].getEsn());
                    meidNumberSummery[i] = getSimSummery(i, mPhone[i].getMeid());
                    minNumberSummery[i] = getSimSummery(i, mPhone[i].getCdmaMin());

                    if (getResources().getBoolean(R.bool.config_msid_enable)) {
                        findPreference(KEY_MIN_NUMBER).setTitle(R.string.status_msid_number);
                    }

                    imeiSVSummery[i] = null;

                    if (mPhone[i].getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                        // Show ICC ID and IMEI for LTE device
                        iccIdSummery[i] = getSimSummery(i, mPhone[i].getIccSerialNumber());
                        imeiSummery[i] = getSimSummery(i, mPhone[i].getImei());
                    } else {
                        // device is not GSM/UMTS, do not display GSM/UMTS
                        // features
                        // check Null in case no specified preference in overlay
                        // xml
                        iccIdSummery[i] = null;
                        imeiSummery[i] = null;
                    }

                } else {
                    prlVersionSummery[i] = null;
                    esnNumberSummery[i] = null;
                    if (SystemProperties.getBoolean("persist.env.settings.showMEID", false)) {
                        meidNumberSummery[i] = getSimSummery(i, mPhone[i].getMeid());
                    } else {
                        meidNumberSummery[i] = null;
                    }
                    if (SystemProperties.getBoolean("persist.env.settings.showESN", false)) {
                        esnNumberSummery[i] = getSimSummery(i, mPhone[i].getEsn());
                    } else {
                        esnNumberSummery[i] = null;
                    }
                    minNumberSummery[i] = null;
                    iccIdSummery[i] = null;
                    imeiSummery[i] = getSimSummery(i, mPhone[i].getDeviceId());
                    imeiSVSummery[i] = getSimSummery(i, mPhone[i].getDeviceSvn());
                }
            }
            setMSimSummery(KEY_PRL_VERSION, prlVersionSummery);
            setMSimSummery(KEY_ESN_NUMBER, esnNumberSummery);
            setMSimSummery(KEY_MEID_NUMBER, meidNumberSummery);
            setMSimSummery(KEY_MIN_NUMBER, minNumberSummery);
            setMSimSummery(KEY_ICC_ID, iccIdSummery);
            setMSimSummery(KEY_IMEI, imeiSummery);
            setMSimSummery(KEY_IMEI_SV, imeiSVSummery);
            setMSimSummery(KEY_PHONE_NUMBER, numberSummery);

            //baseband is not related to DSDS, one phone has one base band.
            String basebandVersionSummery =
                MSimTelephonyManager.getTelephonyProperty("gsm.version.baseband",
                        MSimTelephonyManager.getDefault().getDefaultSubscription(), null);
            setSummaryText(KEY_BASEBAND_VERSION,basebandVersionSummery);

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            for (int i = 0; i < mNumPhones; i++) {
                mTelephonyManager.listen(mPhoneStateListener[i],
                        PhoneStateListener.LISTEN_SERVICE_STATE
                                | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
                updateSignalStrength(i);
                updateServiceState(i);
                updateDataState(i);
                updateNetworkType(i);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                getLatestIntent.putExtra(MSimConstants.SUBSCRIPTION_KEY, i);
                sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);
            }
        }
        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
        registerReceiver(mAreaInfoReceiver, new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                CB_AREA_INFO_SENDER_PERMISSION, null);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!Utils.isWifiOnly(getApplicationContext())) {
            for (int i=0; i < mNumPhones; i++) {
                mTelephonyManager.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
            }
        }
        unregisterReceiver(mBatteryInfoReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
        unregisterReceiver(mAreaInfoReceiver);
    }

    private PhoneStateListener getPhoneStateListener(final int subscription) {
        PhoneStateListener phoneStateListener = new PhoneStateListener(subscription) {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                mSignalStrength[subscription] = signalStrength;
                updateSignalStrength(subscription);
            }

            @Override
            public void onServiceStateChanged(ServiceState state) {
                mServiceState[subscription] = state;
                updateServiceState(subscription);
                updateNetworkType(subscription);
            }
            @Override
            public void onDataConnectionStateChanged(int state) {
                mDataState[subscription] = state;
                updateDataState(subscription);
                updateNetworkType(subscription);
            }
        };
        return phoneStateListener;
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    /**
     * @param preference The key for the Preference item
     * @param property The system property to fetch
     * @param alt The default value, if the property doesn't exist
     */
    private void setSummary(String preference, String property, String alt) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property, alt));
        } catch (RuntimeException e) {

        }
    }

    private void setSummaryText(String preference, String text) {
            if (TextUtils.isEmpty(text)) {
               text = mUnknown;
            }
             // some preferences may be missing
             if (findPreference(preference) != null) {
                 findPreference(preference).setSummary(text);
             }
    }

    private void updateServiceState(int subscription) {
        String display = mRes.getString(R.string.radioInfo_unknown);

        if (mServiceState[subscription] != null) {
            int state = mServiceState[subscription].getState();

            switch (state) {
                case ServiceState.STATE_IN_SERVICE:
                    display = mRes.getString(R.string.radioInfo_service_in);
                    break;
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_EMERGENCY_ONLY:
                    display = mRes.getString(R.string.radioInfo_service_out);
                    break;
                case ServiceState.STATE_POWER_OFF:
                    display = mRes.getString(R.string.radioInfo_service_off);
                    break;
            }

            serviceStateSummery[subscription] = getSimSummery(subscription, display);
            setMSimSummery(KEY_SERVICE_STATE, serviceStateSummery);

            if (mServiceState[subscription].getRoaming()) {
                roamingStateSummery[subscription] = getSimSummery(subscription,
                        mRes.getString(R.string.radioInfo_roaming_in));
            } else {
                roamingStateSummery[subscription] = getSimSummery(subscription,
                        mRes.getString(R.string.radioInfo_roaming_not));
            }
            setMSimSummery(KEY_ROAMING_STATE, roamingStateSummery);

            String operatorName = null;
            if (/*FeatureQuery.FEATURE_SHOW_CARRIER_BY_MCCMNC*/false) {
                String spn = mTelephonyManager.getDefault().getNetworkOperator(subscription);
                operatorName = spn;
            } else {
                operatorName = mServiceState[subscription].getOperatorAlphaLong();
                // parse the string to current language string in public resources
                if (operatorName != null) {
                    operatorName = getLocalString(operatorName,
                        com.android.internal.R.array.origin_carrier_names,
                        com.android.internal.R.array.locale_carrier_names);
                }
            }
            operatorNameSummery[subscription] = getSimSummery(subscription, operatorName);
            setMSimSummery(KEY_OPERATOR_NAME, operatorNameSummery);
        }
    }

    void updateSignalStrength(int subscription) {
        // not loaded in some versions of the code (e.g., zaku)
        int signalDbm = 0;

        if (mSignalStrength[subscription] != null) {
            int state = mServiceState[subscription].getState();
            Resources r = getResources();

            if ((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) {
                mSigStrengthSummery[subscription] = getSimSummery(subscription, "0");
            } else {
                signalDbm = mSignalStrength[subscription].getDbm();

                if (-1 == signalDbm) {
                    signalDbm = 0;
                }

                int signalAsu = mSignalStrength[subscription].getAsuLevel();
                if (-1 == signalAsu) {
                    signalAsu = 0;
                }
                mSigStrengthSummery[subscription] = getSimSummery(subscription,
                        String.valueOf(signalDbm) + " "
                                + r.getString(R.string.radioInfo_display_dbm) + "   "
                                + String.valueOf(signalAsu) + " "
                                + r.getString(R.string.radioInfo_display_asu));
            }
            setMSimSummery(KEY_SIGNAL_STRENGTH, mSigStrengthSummery);
        }
    }

    private void updateNetworkType(int subscription) {
        // Whether EDGE, UMTS, etc...
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN !=
                mTelephonyManager.getNetworkType(subscription)) {
            networkSummery[subscription] = getSimSummery(subscription,
                    mTelephonyManager.getNetworkTypeName(subscription));
        }
        setMSimSummery(KEY_NETWORK_TYPE,networkSummery);
    }

    private void updateDataState(int subscription) {
        String display = null;
        if (MSimPhoneFactory.getDataSubscription() == subscription
                && isDataServiceEnable(subscription)) {
            switch (mDataState[subscription]) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }
        } else {
            display = mRes.getString(R.string.radioInfo_data_disconnected);
        }

        dataStateSummery[subscription] = getSimSummery(subscription, display);
        setMSimSummery(KEY_DATA_STATE, dataStateSummery);
    }

    private void updateAreaInfo(String areaInfo, int sub) {
        if (DEBUG) Log.i(TAG, "updateAreaInfo areaInfo="+areaInfo+" sub="+sub);
        if (areaInfo != null) {
            areaInfoSummery[sub] = getSimSummery(sub, areaInfo);
            setMSimSummery(KEY_LATEST_AREA_INFO, areaInfoSummery);
        }
    }

    private boolean isDataServiceEnable(int subscription) {
        if (mServiceState[subscription] != null &&
            mServiceState[subscription].getState() == ServiceState.STATE_IN_SERVICE) {
            ConnectivityManager connMgr =
                   (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if (connMgr != null && connMgr.getMobileDataEnabled()) {
                return true;
            }
        }

        return false;
    }

    private void setWimaxStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);

        if (ni == null) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_MAC_ADDRESS);
            if (ps != null) root.removePreference(ps);
        } else {
            Preference wimaxMacAddressPref = findPreference(KEY_WIMAX_MAC_ADDRESS);
            String macAddress = SystemProperties.get("net.wimax.mac.address",
                    getString(R.string.status_unavailable));
            wimaxMacAddressPref.setSummary(macAddress);
        }
    }
    private void setWifiStatus() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_WIFI_MAC_ADDRESS);

        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress
                : getString(R.string.status_unavailable));
    }

    private void setIpAddressStatus() {
        Preference ipAddressPref = findPreference(KEY_IP_ADDRESS);
        String ipAddress = Utils.getDefaultIpAddresses(this);
        if (ipAddress != null) {
            ipAddressPref.setSummary(ipAddress);
        } else {
            ipAddressPref.setSummary(getString(R.string.status_unavailable));
        }
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        Preference btAddressPref = findPreference(KEY_BT_ADDRESS);

        if (bluetooth == null) {
            // device not BT capable
            getPreferenceScreen().removePreference(btAddressPref);
        } else {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            btAddressPref.setSummary(!TextUtils.isEmpty(address) ? address
                    : getString(R.string.status_unavailable));
        }
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
    }
}
