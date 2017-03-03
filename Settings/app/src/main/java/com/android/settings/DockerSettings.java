package com.android.settings;

import com.android.settings.ethernet.EthernetConfigDialog;
import com.android.settings.ethernet.EthernetEnabler;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DockerManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.widget.Switch;

public class DockerSettings extends SettingsPreferenceFragment {

	private static final String KEY_USBHOST_TOGGLE = "host_toggle";

	private static final String KEY_ETH_TOGGLE = "eth_toggle";
	private static final String KEY_ETH_CONFIG = "eth_config";
	private static final String KEY_ETH_PROXY = "eth_proxy";
	
	private IntentFilter intentFilter;

	CheckBoxPreference hostToggle;
	
	CheckBoxPreference ethToggle;
	Preference ethConfig;
	PreferenceScreen ethProxy;
	
    private EthernetEnabler mEthEnabler;
    private EthernetConfigDialog mEthConfigDialog;
	private DockerEnable mDockerEnable;
    
	@Override
	public void onCreate(Bundle icicle) {
		// TODO Auto-generated method stub
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.docker_settings);
		hostToggle = (CheckBoxPreference) findPreference(KEY_USBHOST_TOGGLE);
		ethToggle = (CheckBoxPreference) findPreference(KEY_ETH_TOGGLE);
		ethConfig = findPreference(KEY_ETH_CONFIG);
		ethProxy = (PreferenceScreen) findPreference(KEY_ETH_PROXY);
		intentFilter = new IntentFilter("docker.setting.change");
		final Activity activity = getActivity();
		if(!isUSBHost()) {
			Switch actionBarSwitch = new Switch(activity);

			if (activity instanceof PreferenceActivity) {
				PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
				if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
					final int padding = activity.getResources().getDimensionPixelSize(
							R.dimen.action_bar_switch_padding);
					actionBarSwitch.setPadding(0, 0, padding, 0);
					activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
							ActionBar.DISPLAY_SHOW_CUSTOM);
					activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
							ActionBar.LayoutParams.WRAP_CONTENT,
							ActionBar.LayoutParams.WRAP_CONTENT,
							Gravity.CENTER_VERTICAL | Gravity.RIGHT));
				}
			}

			getPreferenceScreen().removePreference(hostToggle);
			mDockerEnable = new DockerEnable(activity, actionBarSwitch);
			mDockerEnable.setSwitch(actionBarSwitch);
		}
		initToggles();
	}
	
	private void initToggles() {
        mEthEnabler = new EthernetEnabler((CheckBoxPreference) findPreference(KEY_ETH_TOGGLE), getActivity());
        mEthConfigDialog = new EthernetConfigDialog(getActivity(), mEthEnabler, ethConfig);
        mEthEnabler.setConfigDialog(mEthConfigDialog);
    }
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
			if (!isUSBHost()) {
				if(mDockerEnable != null) {
					mDockerEnable.resume();
					syscWedigetStat(mDockerEnable.getSwitch().isChecked());
				}
			} else {
				hostToggle.setChecked(DockerManager.getInstance().isDockerEnabled());
				syscWedigetStat(hostToggle.isChecked());
			}
			mEthEnabler.resume();
			getActivity().registerReceiver(mReceiver, intentFilter);
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
			if (mDockerEnable != null) {
				mDockerEnable.pause();
			}
			mEthEnabler.pause();
			getActivity().unregisterReceiver(mReceiver);
	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference == ethConfig) {
            mEthConfigDialog.show();
        } else if(preference == ethToggle) {
        	syscWedigetStat(true);
        } else if(preference == hostToggle) {
			DockerManager.getInstance().setDockerEnabled(hostToggle.isChecked());
		}
        return false;
	}
	
	public void syscWedigetStat(boolean dockerEnable){
		if(isUSBHost()) {
			ethConfig.setEnabled(ethToggle.isChecked());
			ethProxy.setEnabled(ethToggle.isChecked());
		} else {
			ethToggle.setEnabled(dockerEnable);
			ethConfig.setEnabled(dockerEnable && ethToggle.isChecked());
			ethProxy.setEnabled(dockerEnable && ethToggle.isChecked());
		}
	}
	
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			boolean state = intent.getBooleanExtra("checked", false);
			syscWedigetStat(state);
		}
	};

	public static boolean isUSBHost(){
		return DockerManager.getInstance().isUSBHost();
	}


}
