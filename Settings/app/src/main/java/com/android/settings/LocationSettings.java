/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2013, The Linux Foundation. All rights reserved.
 *
 * Not a Contribution. Apache license notifications and license are
 * retained for attribution purposes only.
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


import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import com.android.location.XT.IXTSrv;
import com.android.location.XT.IXTSrvCb;
import com.android.location.XT.IXTSrvCb.Stub;
import android.text.Html;


/**
 * Gesture lock pattern settings.
 */
public class LocationSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // Location Settings
    private static final String KEY_LOCATION_TOGGLE = "location_toggle";
    private static final String KEY_LOCATION_NETWORK = "location_network";
    private static final String KEY_LOCATION_IZAT = "location_izat";
    private static final String KEY_LOCATION_GPS = "location_gps";
    private static final String KEY_ASSISTED_GPS = "assisted_gps";
    private static final String KEY_ASSISTED_GPS_PARAS = "assisted_gps_params";
    private static final String AGPS_PROPERTY = "persist.env.settings.agps";
    private static final String TAG = "LocationSettings";

    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mIZat;
    private CheckBoxPreference mGps;
    private CheckBoxPreference mAssistedGps;
    private SwitchPreference mLocationAccess;
    private Preference mAGpsParas;
    private Context mContext;
    private static final boolean VERBOSE_DBG = false;

    private static final int IZat_MENU_TEXT = 0;
    private static final int IZat_SUB_TITLE_TEXT = 1;
    private static final int POPUP_BOX_DISAGREE = 0;
    private static final int POPUP_BOX_AGREE = 1;
    private static final int PRINT = 1;

    private IXTSrv mXTService = null;
    private XTServiceConnection mServiceConn = null;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;

    //This variable is used to record the IZat service connection result
    private boolean izatConnResult = false;
    private static final String PROPERTIES_FILE = "/etc/gps.conf";

    //This is the IZat handler
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case PRINT:
                    if(POPUP_BOX_DISAGREE == msg.arg1){
                        mIZat.setChecked(false);
                    }else if(POPUP_BOX_AGREE == msg.arg1){
                        mIZat.setChecked(true);
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private IXTSrvCb mCallback = new IXTSrvCb.Stub() {
        public void statusChanged(boolean status) {
            if(false == status)
            {
                mHandler.sendMessage(mHandler.obtainMessage(PRINT, 0, 0));
            }else
            {
                mHandler.sendMessage(mHandler.obtainMessage(PRINT, 1, 0));
            }
        }
    };

    /**
     * Bind Izat service
     */
    private void initUserPrefService(){
        mServiceConn = new XTServiceConnection();
        Intent i = new Intent(IXTSrv.class.getName());
        izatConnResult = getActivity().bindService(i, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    /**
     * IZat service connection
     */
    private class XTServiceConnection implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            mXTService = IXTSrv.Stub.asInterface((IBinder)service);
            Log.d(TAG, "onServiceConnected, service=" + mXTService);
            try{
                if(null != mXTService){
                    String izatMenuTitle = mXTService.getText(IZat_MENU_TEXT);
                    String izatSubtitle = mXTService.getText(IZat_SUB_TITLE_TEXT);
                    if(null != mIZat){
                        mIZat.setTitle(Html.fromHtml(izatMenuTitle));
                        mIZat.setSummary(izatSubtitle);
                    }
                    updateLocationToggles();
                    mXTService.registerCallback(mCallback);
                }
            }catch(RemoteException e){
                if (VERBOSE_DBG)
                    Log.d(TAG,"Failed connecting service!");
                }
            }

        @Override
        public void onServiceDisconnected(ComponentName name){
            if (null != mXTService){
            try {
                mXTService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
                mXTService = null;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initUserPrefService();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
        mContentQueryMap.close();
        getActivity().unbindService(mServiceConn);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.location_settings);
        root = getPreferenceScreen();

        mLocationAccess = (SwitchPreference) root.findPreference(KEY_LOCATION_TOGGLE);
        mNetwork = (CheckBoxPreference) root.findPreference(KEY_LOCATION_NETWORK);
        mIZat = (CheckBoxPreference) root.findPreference(KEY_LOCATION_IZAT);
        mGps = (CheckBoxPreference) root.findPreference(KEY_LOCATION_GPS);
        mAssistedGps = (CheckBoxPreference) root.findPreference(KEY_ASSISTED_GPS);

        if(!izatConnResult){
            root.removePreference(mIZat);
        }

        if (!SystemProperties.getBoolean(AGPS_PROPERTY, false)) {
            root.removePreference(mAssistedGps);
            mAGpsParas = (Preference) root.findPreference(KEY_ASSISTED_GPS_PARAS);
            root.removePreference(mAGpsParas);
        }
        // Only enable these controls if this user is allowed to change location
        // sharing settings.
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        boolean isToggleAllowed = !um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
        if (mLocationAccess != null) mLocationAccess.setEnabled(isToggleAllowed);
        if (mNetwork != null) mNetwork.setEnabled(isToggleAllowed);
        if (mIZat!= null) mIZat.setEnabled(isToggleAllowed);
        if (mGps != null) mGps.setEnabled(isToggleAllowed);
        if (mAssistedGps != null) mAssistedGps.setEnabled(isToggleAllowed);

        mLocationAccess.setOnPreferenceChangeListener(this);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
        try{
            if (null != mXTService){
                String izatMenuTitle = mXTService.getText(IZat_MENU_TEXT);
                String izatSubtitle = mXTService.getText(IZat_SUB_TITLE_TEXT);
                mIZat.setTitle(Html.fromHtml(izatMenuTitle));
                mIZat.setSummary(izatSubtitle);
                updateLocationToggles();
            }else{
                updateLocationToggles();
            }

        }catch(RemoteException e){

        }
        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    updateLocationToggles();
                }
            };
        }

        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final ContentResolver cr = getContentResolver();
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (preference == mNetwork) {
            if (!um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
                Settings.Secure.setLocationProviderEnabled(cr,
                        LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
            }
        } else if (preference == mIZat) {
            if((!um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION))){
                if(mIZat.isChecked()){
                     try{
                         if(null != mXTService){
                            mXTService.showDialog();
                         }
                     }catch(RemoteException e){
                         if (VERBOSE_DBG)
                            Log.d(TAG, "XTService connection failed");

                     }
                } else {
                    try{
                        mXTService.disable();
                        mIZat.setChecked(false);
                        updateLocationToggles();

                    }catch(RemoteException e){
                        if (VERBOSE_DBG)
                            Log.d(TAG, "An error happened during the service connection");
                    }
                }

            }

        } else if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            if (!um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
                Settings.Secure.setLocationProviderEnabled(cr,
                        LocationManager.GPS_PROVIDER, enabled);
                if (mAssistedGps != null) {
                    mAssistedGps.setEnabled(enabled);
                }
            }
        } else if (preference == mAssistedGps) {
            if(mAssistedGps.isChecked()) {
                if( Settings.Global.getString(cr, Settings.Global.ASSISTED_GPS_SUPL_HOST)==null
                   || Settings.Global.getString(cr, Settings.Global.ASSISTED_GPS_SUPL_PORT)==null)
                {
                    FileInputStream stream = null;
                    try {
                        Properties properties = new Properties();
                        File file = new File(PROPERTIES_FILE);
                        stream = new FileInputStream(file);
                        properties.load(stream);
                        Settings.Global.putString(cr, Settings.Global.ASSISTED_GPS_SUPL_HOST,
                                properties.getProperty("SUPL_HOST", null));
                        Settings.Global.putString(cr, Settings.Global.ASSISTED_GPS_SUPL_PORT,
                                properties.getProperty("SUPL_PORT", null));
                    } catch (IOException e) {
                        Log.e("LocationSettings", "Could not open GPS configuration file " +
                              PROPERTIES_FILE + ", e=" + e);
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            Settings.Global.putInt(cr, Settings.Global.ASSISTED_GPS_ENABLED,
                    mAssistedGps.isChecked() ? 1 : 0);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateLocationToggles() {
        ContentResolver res = getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER);
        mGps.setChecked(gpsEnabled);
        mNetwork.setChecked(networkEnabled);
         try{
            if(null != mXTService){
                boolean izatEnabled = mXTService.getStatus();
                mIZat.setChecked(izatEnabled);
                mLocationAccess.setChecked(gpsEnabled || networkEnabled || izatEnabled);
            }else{
                mLocationAccess.setChecked(gpsEnabled || networkEnabled);
            }

        }catch(RemoteException e){

        }
        if (mAssistedGps != null) {
            mAssistedGps.setChecked(Settings.Global.getInt(res,
                    Settings.Global.ASSISTED_GPS_ENABLED, 2) == 1);
            mAssistedGps.setEnabled(gpsEnabled);
        }
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        createPreferenceHierarchy();
    }

    /** Enable or disable all providers when the master toggle is changed. */
    private void onToggleLocationAccess(boolean checked) {
        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)) {
            return;
        }
        final ContentResolver cr = getContentResolver();
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.GPS_PROVIDER, checked);
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.NETWORK_PROVIDER, checked);
        if(false == checked){
            try{
                if(null != mXTService){
                    boolean status = mXTService.disable();
                    mIZat.setChecked(false);
                }
            }catch(RemoteException e){
                e.printStackTrace();
            }
        }else{
            try{
                if(null != mXTService){
                    mXTService.showDialog();
                }
            }catch(RemoteException e){
                e.printStackTrace();
            }
        }
        updateLocationToggles();
    }

    /**
     * When the user turn on location access, pop up a dialog to let user choose
     * whether allow Google service collect user location data.
     */
    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        final boolean chooseValue = (Boolean) newValue;
        if (pref.getKey().equals(KEY_LOCATION_TOGGLE)) {
            if (!chooseValue) {
                updateGoogleServiceAccess(false);
            }
            onToggleLocationAccess(chooseValue);
        }
        return true;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_location_access;
    }

    /**
     * Enable or disable google service to collect anonymous location data.
     */
    private void updateGoogleServiceAccess(boolean newValue) {
        ContentResolver cr = getContentResolver();
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.NETWORK_PROVIDER, newValue);
        mNetwork.setChecked(newValue);
    }

}

class WrappingSwitchPreference extends SwitchPreference {

    public WrappingSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WrappingSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }
}

class WrappingCheckBoxPreference extends CheckBoxPreference {

    public WrappingCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WrappingCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }
}

class WrappingIZatCheckBoxPreference extends CheckBoxPreference {
    public WrappingIZatCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WrappingIZatCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView title = (TextView) view.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(3);
        }
    }
}

