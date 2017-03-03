/* Copyright (C) 2010 The Android-x86 Open Source Project
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
 *
 * Author: Yi Sun <beyounn@gmail.com>
 */

package com.android.settings.ethernet;


import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ethernet.EthernetManager;
import android.net.NetworkInfo;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.util.Slog;
import android.widget.Switch;

public class EthernetEnabler implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "EthenetEnabler";

    private static final boolean LOCAL_LOGD = true;
    //private final IntentFilter mEthStateFilter;
    private Context mContext;
    private CheckBoxPreference mEthCheckBoxPref;
    private final CharSequence mOriginalSummary;
    private EthernetConfigDialog mEthConfigDialog;
    private EthernetManager mEthManager;

//    private final BroadcastReceiver mEthStateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            if (intent.getAction().equals(EthernetDataTracker.ETHERNET_STATE_CHANGED_ACTION)) {
//                handleEthStateChanged(
//                        intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE,
//                                EthernetManager.ETHERNET_STATE_UNKNOWN),
//                        intent.getIntExtra(EthernetManager.EXTRA_PREVIOUS_ETHERNET_STATE,
//                                EthernetManager.ETHERNET_STATE_UNKNOWN));
//            } else if (intent.getAction().equals(EthernetManager.NETWORK_STATE_CHANGED_ACTION)) {
//                handleNetworkStateChanged(
//                        (NetworkInfo) intent.getParcelableExtra(EthernetManager.EXTRA_NETWORK_INFO));
//            }
//            
//        }
//        
//    };

    public void setConfigDialog (EthernetConfigDialog Dialog) {
        mEthConfigDialog = Dialog;
    }

    public EthernetEnabler(CheckBoxPreference ethernetCheckBoxPreference, Context context) {
        mEthCheckBoxPref = ethernetCheckBoxPreference;
        mEthManager = (EthernetManager)context.getSystemService(Context.ETHERNET_SERVICE);
        mOriginalSummary = ethernetCheckBoxPreference.getSummary();
        ethernetCheckBoxPreference.setPersistent(false);
        int state = mEthManager.getState();
        if (state == EthernetManager.ETHERNET_STATE_ENABLED) {
            mEthCheckBoxPref.setChecked(true);
        }

        /*
        mEthStateFilter = new IntentFilter(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        mEthStateFilter.addAction(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
        */
    }

    public void resume() {
        mEthCheckBoxPref.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        //  mContext.unregisterReceiver(mEthStateReceiver);
        mEthCheckBoxPref.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        setEthEnabled((Boolean)newValue);
        return false;
    }

    private void setEthEnabled(final boolean enable) {

        int state = mEthManager.getState();

        Slog.i(TAG,"Show configuration dialog " + enable + state);

        if (state != EthernetManager.ETHERNET_STATE_ENABLED && enable) {
            if (mEthManager.isConfigured() != true) {
            	mEthConfigDialog.show();
                mEthConfigDialog.enableAfterConfig();
            } else {
            	mEthManager.setEnabled(enable);
            }
        } else {
        	mEthManager.setEnabled(enable);
        }

        mEthCheckBoxPref.setChecked(enable);
//        // Disable button
//        mEthCheckBoxPref.setEnabled(true);
    }

    private void handleEthStateChanged(int ethState, int previousEthState) {

    }

    private void handleNetworkStateChanged(NetworkInfo networkInfo) {
        if (LOCAL_LOGD) {
            Slog.d(TAG, "Received network state changed to " + networkInfo);
        }
    }
}

