/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;
import java.util.Iterator;
import java.util.Map;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.device.ScanManager;
import android.util.Log;
import android.view.KeyEvent;

public class UrovoSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener{
    private static final String TAG = "UrovoSettings";

    private static final String SCAN_TYPE = "scan_type";
    private ScanManager mScanManager;
//    private NVAccess mNVAccess;
    ListPreference ScanTypePreference;
    SharedPreferences prefs;
    CharSequence entries[];
    CharSequence entriesvaule[];  
    int typeInit;
    boolean isTypeValid = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.urovo_settings);
		ScanTypePreference = (ListPreference) findPreference(SCAN_TYPE);

		ScanTypePreference.setOnPreferenceChangeListener(this);
		mScanManager = new ScanManager();
//		mNVAccess = new NVAccess();
		
		typeInit = syncScantype();

		Log.i(TAG, "mTypeinit = " + typeInit);
		Map m =mScanManager.getScanerList();
		if(m != null) {
		int length = m.size();
		entries = new String[length];
		entriesvaule = new String[length];
        entryList();
        
		if (typeInit == 0) {
			ScanTypePreference.setSummary("NULL");
		} else {
			if (isSnWriten())
				ScanTypePreference.setEnabled(false);
			if (!isTypeValid)
				return;
			updateTimeoutPreferenceDescription(typeInit);
		}
		int ii=systemtolocal(typeInit);
		ScanTypePreference.setValue(String.valueOf(ii+1));
		} else {
		ScanTypePreference.setEnabled(false);
		}
	}
	
	private boolean isSnWriten(){
		String deviceSn = android.device.provider.Settings.System.getString(getContentResolver(), 
				Settings.System.DEVICE_SN);
		Log.i(TAG,"SN=====" + deviceSn);
		if(deviceSn != null && (deviceSn.length() == 14 || deviceSn.length()==9))
			return true;
		return false;
	}

	private int localtosystem(int typeInit) {
		Log.d(TAG, "localtosystem " + typeInit + "=" + entriesvaule[typeInit]);
		return Integer.parseInt(entriesvaule[typeInit].toString());
	}

	private int systemtolocal(int typeInit) {
		int i;
		int length = mScanManager.getScanerList().size();
		for (i = 0; i < length; i++) {
			Log.d(TAG, "systemtolocal " + String.valueOf(typeInit) + "="
					+ entriesvaule[i]);
			if (String.valueOf(typeInit) == entriesvaule[i]) {
				return i;
			}
		}
		return 0;
	}

	private void entryList() {

		Map m = mScanManager.getScanerList();
		Iterator mi = m.entrySet().iterator();
		int i = 0;
		while (mi.hasNext()) {
			Map.Entry e = (Map.Entry) mi.next();
			Log.d(TAG, "entryList " + e.getKey() + "=" + e.getValue());
			entries[i] = e.getKey().toString();
			entriesvaule[i] = e.getValue().toString();
			if(entriesvaule[i].equals(String.valueOf(typeInit))) isTypeValid = true;
			i++;
		}
		ScanTypePreference.setEntries(entries);
	}

	private String updateentryList(int v) {

		Map m = mScanManager.getScanerList();
		Iterator mi = m.entrySet().iterator();
		int i = 0;
		while (mi.hasNext()) {
			Map.Entry e = (Map.Entry) mi.next();
			Log.d(TAG,"updateentryList "+e.getKey() + "=" + e.getValue());
			if (String.valueOf(v) == e.getValue().toString()) {
				return e.getKey().toString();
			}
			i++;
		}
		return null;
	}
	private int syncScantype() {
/*		if (mScanManager.getOutputParameter(7) == 0) {
			int tmp = mNVAccess.getScanType();
			Log.i(TAG, "scan type " + tmp);
			if (tmp > 0) {
				mScanManager.setOutputParameter(7, tmp);
				return tmp;
			}
			tmp = mNVAccess.getScanType();
			Log.i(TAG, "scan -type " + tmp);
			if (tmp > 0) {
				mScanManager.setOutputParameter(7, tmp);
				return tmp;
			}
		}*/
		return mScanManager.getOutputParameter(7);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		Log.i(TAG, "onPreferenceTreeClick...");
		final String key = preference.getKey();
		if (SCAN_TYPE.equals(key)) {
			int value = mScanManager.getOutputParameter(7);
			Log.i(TAG, "onPreferenceTreeClick..." + value);
			int value2 = systemtolocal(mScanManager.getOutputParameter(7));
			ScanTypePreference.setValue(String.valueOf(value2+1));
			updateTimeoutPreferenceDescription(value);
		}
		return true;
	}

	public boolean onPreferenceChange(Preference preference, Object objValue) {
		Log.i(TAG, "onPreferenceChange...");
		final String key = preference.getKey();
		if (SCAN_TYPE.equals(key)) {
			int value = Integer.parseInt((String) objValue);
			value = localtosystem(value-1);
			try {
				Log.i(TAG, "onPreferenceChange..." + value);
				mScanManager.setOutputParameter(7, value);
				// mNVAccess.setScanType(value);
				updateTimeoutPreferenceDescription(value);
			} catch (NumberFormatException e) {
				Log.e(TAG, "could not persist tv mode setting", e);
			}
			// mScanManager.setScannerType(value);
		}
		return true;
	}
    private void updateTimeoutPreferenceDescription(int value) {
        ListPreference preference = ScanTypePreference;
        String summary;
		Log.i(TAG, "updateTimeoutPreferenceDescription..." + value);
        if (value <= -1) {
            summary = "NULL";
        }  else {
            summary = preference.getContext().getString(R.string.scan_type_summary,
            		updateentryList(value));
        }
        preference.setSummary(summary);
    }
}
