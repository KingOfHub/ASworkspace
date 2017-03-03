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

package com.android.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.device.UfsManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.telephony.MSimTelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.util.ArrayUtils;
import com.android.settings.AccessibilitySettings.ToggleAccessibilityServicePreferenceFragment;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.AuthenticatorHelper;
import com.android.settings.accounts.ManageAccountsSettings;
import com.android.settings.bluetooth.BluetoothEnabler;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.wifi.WifiEnabler;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.settings.multisimsettings.MultiSimSettingsConstants;
import com.tesla.tunguska.cpos.system.SystemManager;

import android.os.Build;
import android.widget.Toast;

/**
 * Top-level settings activity to handle single pane and double pane UI layout.
 */
public class Settings extends PreferenceActivity
        implements ButtonBarHandler, OnAccountsUpdateListener {

    private static final String LOG_TAG = "Settings";

    private static final String META_DATA_KEY_HEADER_ID =
        "com.android.settings.TOP_LEVEL_HEADER_ID";
    private static final String META_DATA_KEY_FRAGMENT_CLASS =
        "com.android.settings.FRAGMENT_CLASS";
    private static final String META_DATA_KEY_PARENT_TITLE =
        "com.android.settings.PARENT_FRAGMENT_TITLE";
    private static final String META_DATA_KEY_PARENT_FRAGMENT_CLASS =
        "com.android.settings.PARENT_FRAGMENT_CLASS";

    private static final String GLOBAL_PROP = "persist.env.phone.global";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private static final String SAVE_KEY_CURRENT_HEADER = "com.android.settings.CURRENT_HEADER";
    private static final String SAVE_KEY_PARENT_HEADER = "com.android.settings.PARENT_HEADER";
    public static final String ACCOUNT_TYPE_PHONE = "com.android.localphone";
    public static final String ACCOUNT_TYPE_SIM = "com.android.sim";
    
    private static final int MENU_ADMIN = 0;
    private static final int MENU_PASSWORD = 1;
    private static final int MENU_CERT = 2;
    private static final int MENU_SN = 3;
    
    private static final int MENU_CERTS_MANAGER = 4;

    private String mFragmentClass;
    private int mTopLevelHeaderId;
    private Header mFirstHeader;
    private Header mCurrentHeader;
    private Header mParentHeader;
    private boolean mInLocalHeaderSwitch;
    private boolean mShowWifi = true;
    private boolean mShowData = true;
    private boolean mShowBluetooth = true;

    // CPOS
    private SystemManager mSystemManager;
    

    // Show only these settings for restricted users
    private int[] SETTINGS_FOR_RESTRICTED = {
            R.id.wireless_section,
            R.id.wifi_settings,
            R.id.mobiledata_settings,
            R.id.bluetooth_settings,
            R.id.data_usage_settings,
            R.id.wireless_settings,
            R.id.device_section,
            R.id.sound_settings,
            R.id.display_settings,
            R.id.storage_settings,
            R.id.application_settings,
            R.id.battery_settings,
            R.id.personal_section,
            R.id.location_settings,
            R.id.security_settings,
            R.id.language_settings,
            R.id.user_settings,
            R.id.account_settings,
            R.id.account_add,
            R.id.system_section,
            R.id.date_time_settings,
            R.id.about_settings,
            R.id.accessibility_settings
    };
    
    private int[] SETTINGS_GZJ = {

            R.id.wireless_section,
            R.id.wifi_settings,
            R.id.mobiledata_settings,
            R.id.bluetooth_settings,
            R.id.data_usage_settings,
            R.id.wireless_settings

    };

    private int[] SETTINGS_MV_WJ = {
            R.id.wireless_section,
            R.id.wifi_settings,
            R.id.mobiledata_settings,
            R.id.bluetooth_settings,
            R.id.data_usage_settings,
            R.id.wireless_settings,
            R.id.location_settings,
            R.id.account_settings,
            R.id.account_add
    };
    
    private int[] SETTINGS_MV_KQ = {
    		R.id.wireless_settings,
    		R.id.storage_settings,
    		R.id.application_settings,
    		R.id.language_settings,
    		R.id.security_settings,
    		R.id.privacy_settings,
    		R.id.account_settings,
    		R.id.account_add,
    		R.id.system_section,
    		R.id.development_settings,
    		R.id.date_time_settings,
    		R.id.accessibility_settings,
    		R.id.about_settings,
    };
    private int[] SETTINGS_MV_WTM = {
            R.id.application_settings,
            R.id.scanner_settings,
            R.id.development_settings,
            R.id.location_settings,
    };
    
    boolean isKPOS = Build.PWV_CUSTOM_CUSTOM.equals("KPOS");
    boolean isSYB =Build.PWV_CUSTOM_CUSTOM.equals("SYB");
    private int[] SETTINGS_MV_KPOS = {
    		R.id.data_usage_settings,
    		R.id.storage_settings,
            R.id.battery_settings,
            R.id.security_settings,
            R.id.account_settings,
    		R.id.account_add,
    		R.id.accessibility_settings,
    		R.id.location_settings,
    		R.id.development_settings
    };

    static boolean isCPOS = Build.PWV_CUSTOM_CUSTOM.equals("CPOS");
    static boolean isCPOSAdmin = false;
    private int[] SETTINGS_MV_CPOS = {
    		R.id.mobiledata_settings,
    		R.id.bluetooth_settings,
    		R.id.storage_settings,
    		R.id.application_settings,
    		R.id.location_settings,
    		R.id.security_settings,
    		R.id.privacy_settings,
            R.id.account_settings,
    		R.id.account_add,
    		R.id.date_time_settings,
    		R.id.accessibility_settings,
    		R.id.development_settings
    };
    
    private int[] SETTINGS_SQ27D = {
            R.id.scanner_settings,
            R.id.battery_settings
    };
    ServiceMenuKeySequence mServiceInvoker;
    
    boolean isKPOSAdmin = false;
    private int[] SETTINGS_MV_KPOS_ADMIN = {
    		R.id.application_settings,
    		R.id.privacy_settings
    };
    private int[] SETTINGS_MV_CPOS_ADMIN = {
    		R.id.mobiledata_settings,
    		R.id.bluetooth_settings,
            R.id.account_settings,
    		R.id.account_add
    };
    /**gouwei-20160613-start**/
    private int[] SETTINGS_MV_SQ42 = {
    		R.id.wifi_settings,
    		R.id.data_usage_settings,
    		R.id.mobiledata_settings
    };
    /**gouwei-20160613-end**/
    private static boolean isAdminNotificationShown = false;
    
    private EditText adminAccount;
    private EditText adminPassword;
    private Dialog adminDialog;
    private Dialog changePwDialog;
    private EditText oldPwEt;
    private EditText newPwEt;
    private static final String ADMIN_ACCOUNT = "admin";
    private static final String ADMIN_PASSWORD = "u123456";
    private static final int MENU_GROUP_ID = 1 << 3;
    private static final int MENU_ITEM_ID = 1 << 4;

    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

    // TODO: Update Call Settings based on airplane mode state.

    protected HashMap<Integer, Integer> mHeaderIndexMap = new HashMap<Integer, Integer>();

    private AuthenticatorHelper mAuthenticatorHelper;
    private Header mLastHeader;
    private boolean mListeningToAccountUpdates;
    
    private ArrayList<MenuItemView> menuItems;
    private ArrayList<MenuItemView> certsList;
    private Dialog menuDialog;
    private Dialog certDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(getIntent().getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        mAuthenticatorHelper = new AuthenticatorHelper();
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, null);

        mDevelopmentPreferences = getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);

        getMetaData();
        mInLocalHeaderSwitch = true;
        super.onCreate(savedInstanceState);
        mInLocalHeaderSwitch = false;

        if (!onIsHidingHeaders() && onIsMultiPane()) {
            highlightHeader(mTopLevelHeaderId);
            // Force the title so that it doesn't get overridden by a direct launch of
            // a specific settings screen.
            setTitle(R.string.settings_label);
        }

        // Retrieve any saved state
        if (savedInstanceState != null) {
            mCurrentHeader = savedInstanceState.getParcelable(SAVE_KEY_CURRENT_HEADER);
            mParentHeader = savedInstanceState.getParcelable(SAVE_KEY_PARENT_HEADER);
        }

        // If the current header was saved, switch to it
        if (savedInstanceState != null && mCurrentHeader != null) {
            //switchToHeaderLocal(mCurrentHeader);
            showBreadCrumbs(mCurrentHeader.title, null);
        }

        if (mParentHeader != null) {
            setParentTitle(mParentHeader.title, null, new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToParent(mParentHeader.fragment);
                }
            });
        }

        // Override up navigation for multi-pane, since we handle it in the fragment breadcrumbs
        if (onIsMultiPane()) {
            getActionBar().setDisplayHomeAsUpEnabled(false);
            getActionBar().setHomeButtonEnabled(false);
        }
        if(isCPOS)
            mSystemManager = new SystemManager(this);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current fragment, if it is the same as originally launched
        if (mCurrentHeader != null) {
            outState.putParcelable(SAVE_KEY_CURRENT_HEADER, mCurrentHeader);
        }
        if (mParentHeader != null) {
            outState.putParcelable(SAVE_KEY_PARENT_HEADER, mParentHeader);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                invalidateHeaders();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof HeaderAdapter) {
            ((HeaderAdapter) listAdapter).resume();
        }
        invalidateHeaders();
        if(mServiceInvoker == null)
            mServiceInvoker = new ServiceMenuKeySequence();
        mServiceInvoker.setOnInvokeServiceMenuListener(new ServiceMenuKeySequence.OnInvokeServiceMenuListener(){
            public void onServiceMenu() {
                if(getTitle().toString().equals(getResources().getString(R.string.settings_label))) {
                    Intent virginPage = new Intent("android.intent.action.INTERNAL_TEST");
                    startActivity(virginPage);
                }
            }

        });
    }

    @Override
    public void onPause() {
        super.onPause();
        
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof HeaderAdapter) {
            ((HeaderAdapter) listAdapter).pause();
        }

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
        mDevelopmentPreferencesListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ListAdapter listAdapter = getListAdapter();
        if (listAdapter instanceof HeaderAdapter) {
            ((HeaderAdapter) listAdapter).destroy();
        }
        if (mListeningToAccountUpdates) {
            AccountManager.get(this).removeOnAccountsUpdatedListener(this);
        }
    }

    private void switchToHeaderLocal(Header header) {
        mInLocalHeaderSwitch = true;
        switchToHeader(header);
        mInLocalHeaderSwitch = false;
    }

    @Override
    public void switchToHeader(Header header) {
        if (!mInLocalHeaderSwitch) {
            mCurrentHeader = null;
            mParentHeader = null;
        }
        super.switchToHeader(header);
    }

    /**
     * Switch to parent fragment and store the grand parent's info
     * @param className name of the activity wrapper for the parent fragment.
     */
    private void switchToParent(String className) {
        final ComponentName cn = new ComponentName(this, className);
        try {
            final PackageManager pm = getPackageManager();
            final ActivityInfo parentInfo = pm.getActivityInfo(cn, PackageManager.GET_META_DATA);

            if (parentInfo != null && parentInfo.metaData != null) {
                String fragmentClass = parentInfo.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
                CharSequence fragmentTitle = parentInfo.loadLabel(pm);
                Header parentHeader = new Header();
                parentHeader.fragment = fragmentClass;
                parentHeader.title = fragmentTitle;
                mCurrentHeader = parentHeader;

                switchToHeaderLocal(parentHeader);
                highlightHeader(mTopLevelHeaderId);

                mParentHeader = new Header();
                mParentHeader.fragment
                        = parentInfo.metaData.getString(META_DATA_KEY_PARENT_FRAGMENT_CLASS);
                mParentHeader.title = parentInfo.metaData.getString(META_DATA_KEY_PARENT_TITLE);
            }
        } catch (NameNotFoundException nnfe) {
            Log.w(LOG_TAG, "Could not find parent activity : " + className);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // If it is not launched from history, then reset to top-level
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0) {
            if (mFirstHeader != null && !onIsHidingHeaders() && onIsMultiPane()) {
                switchToHeaderLocal(mFirstHeader);
            }
            getListView().setSelectionFromTop(0, 0);
        }
    }

    private void highlightHeader(int id) {
        if (id != 0) {
            Integer index = mHeaderIndexMap.get(id);
            if (index != null) {
                getListView().setItemChecked(index, true);
                if (isMultiPane()) {
                    getListView().smoothScrollToPosition(index);
                }
            }
        }
    }

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        // This is called from super.onCreate, isMultiPane() is not yet reliable
        // Do not use onIsHidingHeaders either, which relies itself on this method
        
        if (startingFragment != null && !onIsMultiPane()) {
            Intent modIntent = new Intent(superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, startingFragment);
            Bundle args = superIntent.getExtras();
            if (args != null) {
                args = new Bundle(args);
            } else {
                args = new Bundle();
            }
            args.putParcelable("intent", superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, superIntent.getExtras());
            return modIntent;
        }
        return superIntent;
    }

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    protected String getStartingFragmentClass(Intent intent) {
        if (mFragmentClass != null) return mFragmentClass;

        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) return null;

        if ("com.android.settings.ManageApplications".equals(intentClass)
                || "com.android.settings.RunningServices".equals(intentClass)
                || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            // Old names of manage apps.
            intentClass = com.android.settings.applications.ManageApplications.class.getName();
        }

        return intentClass;
    }

    /**
     * Override initial header when an activity-alias is causing Settings to be launched
     * for a specific fragment encoded in the android:name parameter.
     */
    @Override
    public Header onGetInitialHeader() {
        String fragmentClass = getStartingFragmentClass(super.getIntent());
        if (fragmentClass != null) {
            Header header = new Header();
            header.fragment = fragmentClass;
            header.title = getTitle();
            header.fragmentArguments = getIntent().getExtras();
            mCurrentHeader = header;
            return header;
        }

        return mFirstHeader;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args,
            int titleRes, int shortTitleRes) {
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args,
                titleRes, shortTitleRes);

        // Some fragments want split ActionBar; these should stay in sync with
        // uiOptions for fragments also defined as activities in manifest.
        if (WifiSettings.class.getName().equals(fragmentName) ||
                WifiP2pSettings.class.getName().equals(fragmentName) ||
                WifiDisplaySettings.class.getName().equals(fragmentName) ||
                BluetoothSettings.class.getName().equals(fragmentName) ||
                DreamSettings.class.getName().equals(fragmentName) ||
                ToggleAccessibilityServicePreferenceFragment.class.getName().equals(fragmentName)) {
            intent.putExtra(EXTRA_UI_OPTIONS, ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
        }
        
        if(isKPOS ||isSYB){
        	intent.putExtra("isKPOSAdmin", isKPOSAdmin);
        }

        intent.setClass(this, SubSettings.class);
        return intent;
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> headers) {
        loadHeadersFromResource(R.xml.settings_headers, headers);
        updateHeaderList(headers);
        useApplicationSettingsIfNeeded(headers);
    }

    private void useApplicationSettingsIfNeeded(List<Header> target) {
        if ("Xolo".equals(SystemProperties.get("persist.env.spec"))) {
            Log.d(LOG_TAG, "useApplicationSettingsIfNeeded, myUserId = " + UserHandle.myUserId());
            for (int i = 0; i < target.size(); i++) {
                Header header = target.get(i);
                if (header.id == R.id.application_settings) {
                    header.fragment = "com.android.settings.ApplicationSettings";
                    Log.d(LOG_TAG, "use ApplicationSettings instead");
                    break;
                }
            }
        }
    }
    
    private void parseJSONString(String JSONString) {
        try {
            JSONObject config = new JSONObject(JSONString);
            JSONObject ShowdownUI = config.getJSONObject("SettingsUI");
            if(ShowdownUI != null) {
            	mShowWifi = (ShowdownUI.getInt("wifi") == 1);
            	mShowData = (ShowdownUI.getInt("data") == 1);
            	mShowBluetooth =  (ShowdownUI.getInt("bluetooth") == 1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mShowWifi = true;
            mShowData = true;
            mShowBluetooth = true;
        }
        android.util.Log.i(LOG_TAG,"parseJSONString mShowWifi=" + mShowWifi + "mShowData=" + mShowData);
    }

    private void updateHeaderList(List<Header> target) {
        final boolean showDev = mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));
        int i = 0;
        final boolean signed = SystemProperties.getBoolean("pwv.custom.sign", false);
        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        UfsManager manager = new UfsManager();
        if(manager.init() == 0) {
            byte[] config = new byte[4096];
            int ret = manager.getConfig(config);
            if(ret > 0 &&  ret < config.length )
                parseJSONString(new String(config, 0 , ret));
            manager.release();
        }
        mHeaderIndexMap.clear();
        while (i < target.size()) {
            Header header = target.get(i);
            // Ids are integers, so downcasting
            int id = (int) header.id;
            if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                Utils.updateHeaderToSpecificActivityFromMetaDataOrRemove(this, target, header);
            } else if (id == R.id.wifi_settings) {
                // Remove WiFi Settings if WiFi service is not available.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) {
                    target.remove(i);
                }else if(!mShowWifi){
                	 target.remove(i);
                }
            } else if (id == R.id.mobiledata_settings){
            	if(!mShowData){
            		 target.remove(i);
            	}
            } else if (id == R.id.bluetooth_settings) {
                // Remove Bluetooth Settings if Bluetooth service is not available.
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                    target.remove(i);
                }else if(!mShowBluetooth){
                	target.remove(i);
                }
            } else if (id == R.id.docker_settings) {    
            	if (!SystemProperties.getBoolean("pwv.docker.support", false))
            		target.remove(i);
//                if(DockerSettings.isUSBHost())
//                    target.get(i).titleRes = R.string.usb_host_title;
            } else if (id == R.id.data_usage_settings) {
                // Remove data usage when kernel module not enabled
                final INetworkManagementService netManager = INetworkManagementService.Stub
                        .asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
                try {
                    if (!netManager.isBandwidthControlEnabled()) {
                        target.remove(i);
                    }else if (!mShowData){
                    	 target.remove(i);
                    }
                } catch (RemoteException e) {
                    // ignored
                }
            } else if (id == R.id.account_settings) {
				if (!((Build.PROJECT.equals("ID80") && Build.PWV_CUSTOM_CUSTOM.equals("WJ")) 
						|| ((Build.PROJECT.equals("SQ27") || Build.PROJECT.equals("SQ27C"))  
								&& Build.PWV_CUSTOM_CUSTOM.equals("KQ")) 
						|| isCPOS
						|| isKPOS
                        || isSYB)) {
					int headerIndex = i + 1;
					i = insertAccountsHeaders(target, headerIndex);
				}
            } else if (id == R.id.user_settings) {
                if (!UserHandle.MU_ENABLED
                        || !UserManager.supportsMultipleUsers()
                        || Utils.isMonkeyRunning()) {
                    target.remove(i);
                }
            } else if (id == R.id.development_settings) {
                if (!showDev) {
                    target.remove(i);
                }
            } else if (id == R.id.account_add) {
                if (um.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS)) {
                    target.remove(i);
                }
            } else if (id == R.id.multi_sim_settings) {
                if (!MSimTelephonyManager.getDefault().isMultiSimEnabled())
                    target.remove(header);
            } else if (id == R.id.global_roaming_settings) {
                if (!SystemProperties.getBoolean(GLOBAL_PROP, false)) {
                    target.remove(header);
                }
            }
            
            if(Build.PROJECT.equals("ID80") && Build.PWV_CUSTOM_CUSTOM.equals("WJ")
            		&& ArrayUtils.contains(SETTINGS_MV_WJ, id)){
            	target.remove(i);
            }
            if(Build.PROJECT.equals("SQ31Q") && Build.PWV_CUSTOM_CUSTOM.equals("GZJ")
                    && ArrayUtils.contains(SETTINGS_GZJ, id)){
                target.remove(i);

            }
            if(Build.PWV_CUSTOM_CUSTOM.equals("WT-M") && ArrayUtils.contains(SETTINGS_MV_WTM, id)){
                target.remove(i);
            }

            if(Build.PROJECT.equals("SQ27D") && ArrayUtils.contains(SETTINGS_SQ27D, id)){
                target.remove(i);
            }
            if((Build.PROJECT.equals("SQ27") || Build.PROJECT.equals("SQ27C")) && Build.PWV_CUSTOM_CUSTOM.equals("KQ")
            		&& ArrayUtils.contains(SETTINGS_MV_KQ, id)){
                if (id == R.id.development_settings && !signed) {
                    //nothing TODO
                } else {
                    target.remove(i);
                }
            }
            
            if((isSYB||isKPOS) && ArrayUtils.contains(SETTINGS_MV_KPOS, id)){
                if (id == R.id.development_settings && !signed) {
                    //nothing TODO
                } else {
                    target.remove(i);
                }
            }
            
            if(isCPOS ){
                if(id == R.id.about_settings)
                    target.get(i).titleRes = R.string.about_settings_cpos;
                if(!isCPOSAdmin && ArrayUtils.contains(SETTINGS_MV_CPOS, id))
            	    target.remove(i);
                else if(isCPOSAdmin && ArrayUtils.contains(SETTINGS_MV_CPOS_ADMIN, id))
                    target.remove(i);
            }
            
            if((isSYB||isKPOS) && !isKPOSAdmin && ArrayUtils.contains(SETTINGS_MV_KPOS_ADMIN, id)){
            	target.remove(i);
            }

            if (i < target.size() && target.get(i) == header
                    && UserHandle.MU_ENABLED && UserHandle.myUserId() != 0
                    && !ArrayUtils.contains(SETTINGS_FOR_RESTRICTED, id)) {
                target.remove(i);
            }
	    if(Build.PROJECT.equals("SQ27C") && (id == R.id.scanner_settings)) {
		 target.remove(i);
	    }
        /**gouwei-20160613-start**/
        if(Build.PWV_CUSTOM_CUSTOM.equals("LCH")&&ArrayUtils.contains(SETTINGS_MV_SQ42, id)){
        	target.remove(header);
        }
    /**gouwei-20160613-end**/
 
            // Increment if the current one wasn't removed by the Utils code.
            if (i < target.size() && target.get(i) == header) {
                // Hold on to the first header, when we need to reset to the top-level
                if (mFirstHeader == null &&
                        HeaderAdapter.getHeaderType(header) != HeaderAdapter.HEADER_TYPE_CATEGORY) {
                    mFirstHeader = header;
                }
                mHeaderIndexMap.put(id, i);
                i++;
            }
          
        }
    }

    private int insertAccountsHeaders(List<Header> target, int headerIndex) {
        String[] accountTypes = mAuthenticatorHelper.getEnabledAccountTypes();
        List<Header> accountHeaders = new ArrayList<Header>(accountTypes.length);
        for (String accountType : accountTypes) {
            CharSequence label = mAuthenticatorHelper.getLabelForType(this, accountType);
            if (label == null) {
                continue;
            }

            Account[] accounts = AccountManager.get(this).getAccountsByType(accountType);
            boolean skipToAccount = accounts.length == 1
                    && !mAuthenticatorHelper.hasAccountPreferences(accountType);
            Header accHeader = new Header();
            accHeader.title = label;
            if (accHeader.extras == null) {
                accHeader.extras = new Bundle();
            }
            if (skipToAccount) {
                accHeader.breadCrumbTitleRes = R.string.account_sync_settings_title;
                accHeader.breadCrumbShortTitleRes = R.string.account_sync_settings_title;
                accHeader.fragment = AccountSyncSettings.class.getName();
                accHeader.fragmentArguments = new Bundle();
                // Need this for the icon
                accHeader.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accHeader.extras.putParcelable(AccountSyncSettings.ACCOUNT_KEY, accounts[0]);
                accHeader.fragmentArguments.putParcelable(AccountSyncSettings.ACCOUNT_KEY,
                        accounts[0]);
            } else {
                accHeader.breadCrumbTitle = label;
                accHeader.breadCrumbShortTitle = label;
                accHeader.fragment = ManageAccountsSettings.class.getName();
                accHeader.fragmentArguments = new Bundle();
                accHeader.extras.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE, accountType);
                accHeader.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_TYPE,
                        accountType);
                if (!isMultiPane()) {
                    accHeader.fragmentArguments.putString(ManageAccountsSettings.KEY_ACCOUNT_LABEL,
                            label.toString());
                }
            }
            //If the account type is Phone or SIM.It will dismiss
            if (!accountType.equals(ACCOUNT_TYPE_PHONE) && !accountType.equals(ACCOUNT_TYPE_SIM)) {
                accountHeaders.add(accHeader);
            }
        }

        // Sort by label
        Collections.sort(accountHeaders, new Comparator<Header>() {
            @Override
            public int compare(Header h1, Header h2) {
                return h1.title.toString().compareTo(h2.title.toString());
            }
        });

        for (Header header : accountHeaders) {
            target.add(headerIndex++, header);
        }
        if (!mListeningToAccountUpdates) {
            AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);
            mListeningToAccountUpdates = true;
        }
        return headerIndex;
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mTopLevelHeaderId = ai.metaData.getInt(META_DATA_KEY_HEADER_ID);
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);

            // Check if it has a parent specified and create a Header object
            final int parentHeaderTitleRes = ai.metaData.getInt(META_DATA_KEY_PARENT_TITLE);
            String parentFragmentClass = ai.metaData.getString(META_DATA_KEY_PARENT_FRAGMENT_CLASS);
            if (parentFragmentClass != null) {
                mParentHeader = new Header();
                mParentHeader.fragment = parentFragmentClass;
                if (parentHeaderTitleRes != 0) {
                    mParentHeader.title = getResources().getString(parentHeaderTitleRes);
                }
            }
        } catch (NameNotFoundException nnfe) {
            // No recovery
        }
    }

    @Override
    public boolean hasNextButton() {
        return super.hasNextButton();
    }

    @Override
    public Button getNextButton() {
        return super.getNextButton();
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static final int HEADER_TYPE_CATEGORY = 0;
        static final int HEADER_TYPE_NORMAL = 1;
        static final int HEADER_TYPE_SWITCH = 2;
        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_SWITCH + 1;

        private final WifiEnabler mWifiEnabler;
        private final BluetoothEnabler mBluetoothEnabler;
        private final DataEnabler mDataEnabler;
        private final ScannerEnable mScannerEnable;
        private final DockerEnable mDockerEnable;
        //private final PiccEnabler mPiccEnabler;
        private AuthenticatorHelper mAuthHelper;

        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
            Switch switch_;
        }

        private LayoutInflater mInflater;

        static int getHeaderType(Header header) {
            if (header.fragment == null && header.intent == null) {
                return HEADER_TYPE_CATEGORY;
            } else if (header.id == R.id.wifi_settings 
                    || header.id == R.id.bluetooth_settings
                    || header.id == R.id.mobiledata_settings
                    || header.id == R.id.scanner_settings) {
                return HEADER_TYPE_SWITCH;
            } else if(header.id == R.id.docker_settings){
                if(DockerSettings.isUSBHost())
                    return HEADER_TYPE_NORMAL;
                else
                    return HEADER_TYPE_SWITCH;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != HEADER_TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, List<Header> objects,
                AuthenticatorHelper authenticatorHelper) {
            super(context, 0, objects);

            mAuthHelper = authenticatorHelper;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // Temp Switches provided as placeholder until the adapter replaces these with actual
            // Switches inflated from their layouts. Must be done before adapter is set in super
            mWifiEnabler = new WifiEnabler(context, new Switch(context));
            mBluetoothEnabler = new BluetoothEnabler(context, new Switch(context));
            mDataEnabler = new DataEnabler(context, new Switch(context));
            mScannerEnable = new ScannerEnable(context,new Switch(context));
            mDockerEnable = new DockerEnable(context, new Switch(context));
            //mPiccEnabler = new PiccEnabler(context,new Switch(context));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();
                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = new TextView(getContext(), null,
                                android.R.attr.listSeparatorTextViewStyle);
                        holder.title = (TextView) view;
                        break;

                    case HEADER_TYPE_SWITCH:
                        view = mInflater.inflate(R.layout.preference_header_switch_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.summary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        holder.switch_ = (Switch) view.findViewById(R.id.switchWidget);
                        break;

                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(
                                R.layout.preference_header_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.summary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        break;
                }
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    break;

                case HEADER_TYPE_SWITCH:
                    // Would need a different treatment if the main menu had more switches
                    if (header.id == R.id.wifi_settings) {
                        mWifiEnabler.setSwitch(holder.switch_);
                    } else if(header.id == R.id.bluetooth_settings){
                        mBluetoothEnabler.setSwitch(holder.switch_);
                    } else if(header.id == R.id.mobiledata_settings){
                        mDataEnabler.setSwitch(holder.switch_);
                        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
                            header.intent.setClassName("com.android.settings",
                                    "com.android.settings.SelectSubscription");
                            header.intent.putExtra(MultiSimSettingsConstants.TARGET_PACKAGE,
                                    "com.android.phone");
                            header.intent.putExtra(MultiSimSettingsConstants.TARGET_CLASS,
                                    "com.android.phone.MSimMobileNetworkSubSettings");
                        }
                    } else if(header.id == R.id.scanner_settings){
                    	mScannerEnable.setSwitch(holder.switch_);
                    } else if(header.id == R.id.docker_settings){
                    	mDockerEnable.setSwitch(holder.switch_);
                    }
                    // No break, fall through on purpose to update common fields

                    //$FALL-THROUGH$
                case HEADER_TYPE_NORMAL:
                    if (header.extras != null
                            && header.extras.containsKey(ManageAccountsSettings.KEY_ACCOUNT_TYPE)) {
                        String accType = header.extras.getString(
                                ManageAccountsSettings.KEY_ACCOUNT_TYPE);
                        ViewGroup.LayoutParams lp = holder.icon.getLayoutParams();
                        lp.width = getContext().getResources().getDimensionPixelSize(
                                R.dimen.header_icon_width);
                        lp.height = lp.width;
                        holder.icon.setLayoutParams(lp);
                        Drawable icon = mAuthHelper.getDrawableForType(getContext(), accType);
                        holder.icon.setImageDrawable(icon);
                    } else {
                        holder.icon.setImageResource(header.iconRes);
                    }
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    CharSequence summary = header.getSummary(getContext().getResources());
                    if (!TextUtils.isEmpty(summary)) {
                        holder.summary.setVisibility(View.VISIBLE);
                        holder.summary.setText(summary);
                    } else {
                        holder.summary.setVisibility(View.GONE);
                    }
                    break;
            }

            return view;
        }

        public void resume() {
            mWifiEnabler.resume();
            mBluetoothEnabler.resume();
            mDataEnabler.resume();
            mScannerEnable.resume();
            mDockerEnable.resume();
            //mPiccEnabler.resume();
        }

        public void pause() {
            mWifiEnabler.pause();
            mBluetoothEnabler.pause();
            mDataEnabler.pause();
            mScannerEnable.pause();
            mDockerEnable.pause();
            //mPiccEnabler.resume();
        }

        public void destroy() {
            mDataEnabler.destroy();
        }
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        boolean revert = false;
        if (header.id == R.id.account_add) {
            revert = true;
        }
        /*if(Build.PROJECT.equals("SQ26")||Build.PROJECT.equals("SQ27")){
              if(header.id==R.id.picc_settings){
                      //nothing
               }else{
                      super.onHeaderClick(header, position);
               }
        }else{
              super.onHeaderClick(header, position);
        }*/
        super.onHeaderClick(header, position);
        if (revert && mLastHeader != null) {
            highlightHeader((int) mLastHeader.id);
        } else {
            mLastHeader = header;
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        // Override the fragment title for Wallpaper settings
        int titleRes = pref.getTitleRes();
        if (pref.getFragment().equals(WallpaperTypeSettings.class.getName())) {
            titleRes = R.string.wallpaper_settings_fragment_title;
        } else if (pref.getFragment().equals(OwnerInfoSettings.class.getName())
                && UserHandle.myUserId() != UserHandle.USER_OWNER) {
            if (UserManager.get(this).isLinkedUser()) {
                titleRes = R.string.profile_info_settings_title;
            } else {
                titleRes = R.string.user_info_settings_title;
            }
        }
        startPreferencePanel(pref.getFragment(), pref.getExtras(), titleRes, pref.getTitle(),
                null, 0);
        return true;
    }

    @Override
    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, Settings.class));
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            super.setListAdapter(new HeaderAdapter(this, getHeaders(), mAuthenticatorHelper));
        }
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // TODO: watch for package upgrades to invalidate cache; see 7206643
        mAuthenticatorHelper.updateAuthDescriptions(this);
        mAuthenticatorHelper.onAccountsUpdated(this, accounts);
        invalidateHeaders();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Let the system ignore the menu key when the activity is foreground.
    	if(isKPOS ||isSYB)
    		menu.add(MENU_GROUP_ID, MENU_ITEM_ID, Menu.NONE, R.string.admin_login);
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	// TODO Auto-generated method stub
    	if((isSYB||isKPOS) && !isKPOSAdmin){
    		return true;
    	}
    	return false;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// TODO Auto-generated method stub
		if (isKPOS || isSYB) {
			switch (item.getItemId()) {
			case MENU_ITEM_ID:
				showAdminLoginDialog();
				break;

			default:
				break;
			}
		}
    	return super.onOptionsItemSelected(item);
    }
    
    void refreshSettings(){
    	final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());  
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
        startActivity(intent);
    }
    
    void showAdminLoginDialog(){
		if (adminDialog == null) {
			View view = getLayoutInflater().inflate(R.layout.admin_login, null);
			adminAccount = (EditText) view.findViewById(R.id.admin_account_et);
			adminPassword = (EditText) view
					.findViewById(R.id.admin_password_et);
            if(isCPOS){
                TextView account = (TextView) view.findViewById(R.id.admin_account_tv);
                account.setVisibility(View.GONE);
                adminAccount.setVisibility(View.GONE);
            }
			adminDialog = new AlertDialog.Builder(this)
					.setTitle(R.string.admin_login)
					.setView(view)
					.setPositiveButton(R.string.admin_login_ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									// TODO Auto-generated method stub
									if (isKPOS || isSYB) {
										if (adminAccount.getText().toString()
												.equals(ADMIN_ACCOUNT)
												&& adminPassword.getText()
														.toString()
														.equals(ADMIN_PASSWORD)) {
											isKPOSAdmin = true;
											refreshSettings();
										}
									} else if (isCPOS) {
                                        String pword = adminPassword.getText().toString();
										if(pword != null && mSystemManager != null &&  mSystemManager.isAdminPassword(pword)){
											isCPOSAdmin = true;
											refreshSettings();
											updateAdminNotification();
										} else {
                                            Toast.makeText(Settings.this, R.string.cpos_pw_err, Toast.LENGTH_LONG).show();
                                        }

									}
								}
							})
					.setNegativeButton(R.string.admin_login_cancel,
							new DialogInterface.OnClickListener() { 
                                   @Override
                                   public void onClick(DialogInterface dialog,
						                 int which) {
						                         // TODO Auto-generated method stub
						           }
						                                                       
					}).create();
		}
		adminDialog.show();
    }

    void showChangePwDialog(){
        if (changePwDialog == null) {
            View view = getLayoutInflater().inflate(R.layout.admin_change_pw, null);
            oldPwEt = (EditText) view.findViewById(R.id.old_password_et);
            newPwEt = (EditText) view
                    .findViewById(R.id.new_password_et);
            changePwDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.admin_login)
                    .setView(view)
                    .setPositiveButton(R.string.admin_login_ok,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // TODO Auto-generated method stub
                                    if(mSystemManager != null){
                                        Toast.makeText(Settings.this, mSystemManager.changePassword(oldPwEt.getText().toString(), newPwEt.getText().toString())?
                                                R.string.cpos_pw_change_success : R.string.cpos_pw_change_failed, Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                    .setNegativeButton(R.string.admin_login_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    // TODO Auto-generated method stub
                                }

                            }).create();
        }
        changePwDialog.show();
    }
    
    private void updateAdminNotification(){

    	NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	final int id = R.string.menu_admin_notification_title;
        if (isCPOSAdmin) {
            if (!isAdminNotificationShown) {
                Resources r = this.getResources();
                CharSequence title = r.getText(id);
                CharSequence message = "";
                Notification notification = new Notification();
                notification.icon = R.drawable.avatar_default_1;
                notification.when = 0;
                notification.flags = Notification.FLAG_ONGOING_EVENT;
                notification.tickerText = title;
                notification.defaults = 0; 
                notification.sound = null;
                notification.vibrate = null;
                notification.priority = Notification.PRIORITY_LOW;

                Intent intent = Intent.makeRestartActivityTask(
                        new ComponentName("com.android.settings",
                                "com.android.settings.Settings"));
                PendingIntent pi = PendingIntent.getActivityAsUser(this, 0,
                        intent, 0, null, UserHandle.CURRENT);
                notification.setLatestEventInfo(this, title, message, pi);
                isAdminNotificationShown = true;
                mNotificationManager.notifyAsUser(null, id, notification,
                        UserHandle.ALL);
            }
        } else if (isAdminNotificationShown) {
        	isAdminNotificationShown = false;
            mNotificationManager.cancelAsUser(null, id, UserHandle.ALL);
        }
    	
    
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if(mServiceInvoker != null && mServiceInvoker.keyIn(keyCode)) {
            return super.onKeyDown(keyCode, event);
        }
        if(keyCode == KeyEvent.KEYCODE_MENU) {
            if (isCPOS && getLocalClassName().equals("Settings")) {
                if (menuItems == null) {
                    menuItems = new ArrayList<Settings.MenuItemView>();
                    menuItems.add(new MenuItemView(MENU_ADMIN, 0, getText(R.string.menu_admin_login_title).toString(), getText(R.string.menu_admin_login_msg).toString()));
                    menuItems.add(new MenuItemView(MENU_PASSWORD, 0, getText(R.string.menu_password_title).toString(), null));
                    menuItems.add(new MenuItemView(MENU_CERT, 0, getText(R.string.menu_certificate_title).toString(), null));
                    String sn = android.device.provider.Settings.System.getString(getContentResolver(), android.provider.Settings.System.DEVICE_SN);
                    menuItems.add(new MenuItemView(MENU_SN, 0, getText(R.string.menu_sn_title).toString(),
                            sn == null || sn.equals("") ? getText(R.string.summary_device_sn).toString() : sn));
                }
                if (isCPOSAdmin) {
                    menuItems.get(0).setTitle(getText(R.string.menu_admin_logout_title).toString()).setMsg(getText(R.string.menu_admin_logout_msg).toString());
                } else {
                    menuItems.get(0).setTitle(getText(R.string.menu_admin_login_title).toString()).setMsg(getText(R.string.menu_admin_login_msg).toString());
                }
                if (menuDialog == null) {
                    ListView list = new ListView(Settings.this);
                    list.setAdapter(new MyAdapter(MyAdapter.TYPE_MENU));
                    list.setOnItemClickListener(new OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1,
                                                int position, long arg3) {
                            // TODO Auto-generated method stub
                            menuItems.get(position).onClick();
                            menuDialog.dismiss();
                        }
                    });
                    menuDialog = new AlertDialog.Builder(Settings.this).setView(list)
                            .setTitle(getText(R.string.settings_label).toString()).create();
                }
                menuDialog.show();
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public class MenuItemView{
    	private int menuId;
    	private int iconId;
    	private String title;
    	private String msg;
    	
    	public MenuItemView(int menuId, int iconId, String title, String msg){
    		this.menuId = menuId;
    		this.iconId = iconId;
    		this.title = title;
    		this.msg = msg;
    	}
    	
    	public View create(Context context, ViewGroup parent, boolean isEnable){
    		LayoutInflater inflater = LayoutInflater.from(context);
    		View v = inflater.inflate(R.layout.admin_menu, parent, false);
    		ImageView iv = (ImageView)v.findViewById(R.id.icon);
    		TextView tvTitle = (TextView)v.findViewById(R.id.title);
    		TextView tvMsg = (TextView)v.findViewById(R.id.message);
    		if(iconId != 0)
    			iv.setImageResource(iconId);
    		if(title != null)
    			tvTitle.setText(title);
    		if(msg != null)
    			tvMsg.setText(msg);
            if(isEnable){
                tvTitle.setTextColor(Color.WHITE);
                tvMsg.setTextColor(Color.WHITE);
            } else {
                tvTitle.setTextColor(Color.GRAY);
                tvMsg.setTextColor(Color.GRAY);
            }
    		return v;
    	}
    	
    	public MenuItemView setTitle(String title){
    		this.title = title;
    		return this;
    	}
    	
    	public MenuItemView setMsg(String msg){
    		this.msg = msg;
    		return this;
    	}
    	
    	public void onClick(){
    		switch (menuId) {
			case MENU_ADMIN:
				if(!isCPOSAdmin)
					showAdminLoginDialog();
				else{
					isCPOSAdmin = false;
                    updateAdminNotification();
                    refreshSettings();
                }
				break;
			case MENU_PASSWORD:
				showChangePwDialog();
				break;
			case MENU_CERT:
				certsList = new ArrayList<Settings.MenuItemView>();
                Map<String, String > certs = mSystemManager.getHwSecurityManager().getCertList();
                if(certs != null && certs.size() > 0){
                    for(String key: certs.keySet()){
                        certsList.add(new MenuItemView(MENU_CERTS_MANAGER, 0,
                                getText(R.string.cert_alia).toString()+key,
                                getText(R.string.cert_type).toString()+certs.get(key)));
                    }

                }
				if(certDialog == null){
					ListView list = new ListView(Settings.this);
        			list.setAdapter(new MyAdapter(MyAdapter.TYPE_CERT));
        			list.setOnItemClickListener(new OnItemClickListener() {
        				@Override
        				public void onItemClick(AdapterView<?> arg0, View arg1,
        						int position, long arg3) {
        					// TODO Auto-generated method stub
        				}
        			});
        			certDialog = new AlertDialog.Builder(Settings.this).setView(list)
        					.setTitle(getText(R.string.menu_certificate_title).toString()).create();
    			}
    			certDialog.show();
        	
				break;
			case MENU_SN:
				
				break;

			default:
				break;
			}
    	}
    }  
    
    class MyAdapter extends BaseAdapter{
    	
    	public static final int TYPE_MENU = 0;
    	public static final int TYPE_CERT = 1;
    	private final int type;
    	
    	public MyAdapter(int type){
    		this.type = type;
    	}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			if (type == TYPE_MENU)
				return menuItems.size();
			else 
				return certsList.size();
		}

		@Override
		public MenuItemView getItem(int position) {
			// TODO Auto-generated method stub
			if(type == TYPE_MENU)
				return menuItems.get(position);
			else
				return certsList.get(position);
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

        @Override
        public boolean isEnabled(int position) {
            if(type == TYPE_MENU && !isCPOSAdmin && position > MENU_ADMIN){
                return false;
            }
            return true;
        }

        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			MenuItemView item = getItem(position);
			return item.create(Settings.this, parent, isEnabled(position));
		}
    	
    }
    
    /*
     * Settings subclasses for launching independently.
     */
    public static class BluetoothSettingsActivity extends Settings { /* empty */ }
    public static class WirelessSettingsActivity extends Settings { /* empty */ }
    public static class TetherSettingsActivity extends Settings { /* empty */ }
    public static class VpnSettingsActivity extends Settings { /* empty */ }
    public static class DateTimeSettingsActivity extends Settings { /* empty */ }
    public static class StorageSettingsActivity extends Settings { /* empty */ }
    public static class WifiSettingsActivity extends Settings { /* empty */ }
    public static class WifiP2pSettingsActivity extends Settings { /* empty */ }
    public static class InputMethodAndLanguageSettingsActivity extends Settings { /* empty */ }
    public static class KeyboardLayoutPickerActivity extends Settings { /* empty */ }
    public static class InputMethodAndSubtypeEnablerActivity extends Settings { /* empty */ }
    public static class SpellCheckersSettingsActivity extends Settings { /* empty */ }
    public static class LocalePickerActivity extends Settings { /* empty */ }
    public static class UserDictionarySettingsActivity extends Settings { /* empty */ }
    public static class SoundSettingsActivity extends Settings { /* empty */ }
    public static class DisplaySettingsActivity extends Settings { /* empty */ }
    public static class DeviceInfoSettingsActivity extends Settings { /* empty */ }
    public static class ApplicationSettingsActivity extends Settings { /* empty */ }
    public static class ManageApplicationsActivity extends Settings { /* empty */ }
    public static class AppOpsSummaryActivity extends Settings { /* empty */ }
    public static class StorageUseActivity extends Settings { /* empty */ }
    public static class DevelopmentSettingsActivity extends Settings { /* empty */ }
    public static class DockerSettingsActivity extends Settings { /* empty */ }
    public static class AccessibilitySettingsActivity extends Settings { /* empty */ }
    public static class SecuritySettingsActivity extends Settings { /* empty */ }
    public static class LocationSettingsActivity extends Settings { /* empty */ }
    public static class PrivacySettingsActivity extends Settings { /* empty */ }
    public static class RunningServicesActivity extends Settings { /* empty */ }
    public static class ManageAccountsSettingsActivity extends Settings { /* empty */ }
    public static class PowerUsageSummaryActivity extends Settings { /* empty */ }
    public static class AccountSyncSettingsActivity extends Settings { /* empty */ }
    public static class AccountSyncSettingsInAddAccountActivity extends Settings { /* empty */ }
    public static class CryptKeeperSettingsActivity extends Settings { /* empty */ }
    public static class DeviceAdminSettingsActivity extends Settings { /* empty */ }
    public static class DataUsageSummaryActivity extends Settings { /* empty */ }
    public static class AdvancedWifiSettingsActivity extends Settings { /* empty */ }
    public static class TextToSpeechSettingsActivity extends Settings { /* empty */ }
    public static class AndroidBeamSettingsActivity extends Settings { /* empty */ }
    public static class WifiDisplaySettingsActivity extends Settings { /* empty */ }
    public static class DreamSettingsActivity extends Settings { /* empty */ }
    public static class NotificationStationActivity extends Settings { /* empty */ }
    public static class UserSettingsActivity extends Settings { /* empty */ }
    public static class NotificationAccessSettingsActivity extends Settings { /* empty */ }
}
