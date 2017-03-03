/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:
     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above
       copyright notice, this list of conditions and the following
       disclaimer in the documentation and/or other materials provided
       with the distribution.
     * Neither the name of The Linux Foundation, Inc. nor the names of its
       contributors may be used to endorse or promote products derived
       from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

package com.android.settings;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import com.android.internal.telephony.TelephonyIntents;
import android.telephony.TelephonyManager;

import com.android.settings.R;
import com.android.settings.WirelessSettings;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.android.internal.telephony.MSimConstants.MAX_PHONE_COUNT_TRI_SIM;

public class DataEnabler {
    private static final String TAG = "DataEnabler";
    private static final String PRE_MOBILE_DATA_STATE = "dataState";
    private final Context mContext;
    private Switch mSwitch;
    private final IntentFilter mIntentFilter;
    private ConnectivityManager mConnService;
    private final NetworkStatusChangeIntentReceiver mReceiver;
    private Boolean mMobileDataEnabled;
    private TelephonyManager telephony;

    public DataEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;

        mConnService = (ConnectivityManager) mContext.
                getSystemService(Context.CONNECTIVITY_SERVICE);

        mMobileDataEnabled = mConnService.getMobileDataEnabled();
        mSwitch.setChecked(mMobileDataEnabled);
        
        telephony = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);

        // Register a broadcast receiver to listen the mobile connectivity
        // changed.
        mReceiver = new NetworkStatusChangeIntentReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
    }

    public void resume() {
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(mDataEnabledListener);
    }

    public void pause() {
    }

    public void destroy() {
        // if unregister receiver on method pause,
        // can't receive the broadcast when toggle airplane mode in
        // Wireless&networks
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;

        mSwitch.setOnCheckedChangeListener(mDataEnabledListener);
        mMobileDataEnabled = mConnService.getMobileDataEnabled();
        mSwitch.setChecked(mMobileDataEnabled);
    }

    private OnCheckedChangeListener mDataEnabledListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mMobileDataEnabled = mConnService.getMobileDataEnabled();
            if(mMobileDataEnabled != mSwitch.isChecked()){
            	if(telephony.getSimState() != TelephonyManager.SIM_STATE_READY)
            	{
            		Settings.Global.putInt(mContext.getContentResolver(),
            			Settings.Global.MOBILE_DATA , isChecked ? 1 : 0);
            	}
                mConnService.setMobileDataEnabled(isChecked);
                mMobileDataEnabled = isChecked;
                for (int i = 0; i < MAX_PHONE_COUNT_TRI_SIM; i++) {
                    Settings.Global.putInt(mContext.getContentResolver(),
                            Settings.Global.MOBILE_DATA + i, isChecked ? 1 : 0);
                }
            }
        }
    };

    /**
     * Receives notifications when enable/disable mobile data.
     */
    private class NetworkStatusChangeIntentReceiver extends BroadcastReceiver {
         @Override
         public void onReceive(Context context, Intent intent) {
             String actionStr = intent.getAction();
             if (TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED.equals(actionStr)) {
                 mMobileDataEnabled = mConnService.getMobileDataEnabled();
                 mSwitch.setChecked(mMobileDataEnabled);
             } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(actionStr)) {
                if (intent.getBooleanExtra("state", false)) {
                    saveMobileDataState();
                    if (mMobileDataEnabled) {
                        mSwitch.setChecked(false);
                    }

                } else {
                    if (getMobileDataState()) {
                        mSwitch.setChecked(true);
                    }
                }
             }
         }
    }

    // save the state of mobile data when turn on airplane mode.
    private void saveMobileDataState() {
        SharedPreferences saveDataState = mContext.getSharedPreferences(
                PRE_MOBILE_DATA_STATE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = saveDataState.edit();
        editor.putBoolean(PRE_MOBILE_DATA_STATE, mMobileDataEnabled);
        editor.commit();
    }

    // get the state of mobile data before turn on airplane mode.
    private Boolean getMobileDataState() {
        SharedPreferences getDataState = mContext.getSharedPreferences(
                PRE_MOBILE_DATA_STATE, Context.MODE_PRIVATE);
        Boolean dataState = getDataState.getBoolean(PRE_MOBILE_DATA_STATE, false);
        return dataState;
    }
}
