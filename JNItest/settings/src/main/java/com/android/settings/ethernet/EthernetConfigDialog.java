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


import java.net.InetAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Slog;

public class EthernetConfigDialog extends AlertDialog implements
        DialogInterface.OnClickListener, AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher {
    private final String TAG = "EthernetSettings";
    private static final boolean localLOGV = true;

    private EthernetEnabler mEthEnabler;
    private View mView;
    private Spinner mDevList;
    private TextView mDevs;
    private RadioButton mConTypeDhcp;
    private RadioButton mConTypeManual;
    private EditText mIpaddr;
    private EditText mDns;
    private EditText mGw;
    private EditText mMask;

    private EthernetLayer mEthLayer;
    private EthernetDevInfo mEthInfo;
    private boolean mEnablePending;
    private EthernetManager mEthManager;
    private ConnectivityManager connManager;
    private Preference mRelationPref;

    public EthernetConfigDialog(Context context, EthernetEnabler Enabler, Preference relationPref) {
        super(context);
        mEthLayer = new EthernetLayer(this);
        mEthEnabler = Enabler;
        mEthManager = (EthernetManager)context.getSystemService(Context.ETHERNET_SERVICE);
        connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        buildDialogContent(context);
        mRelationPref = relationPref;
    }

    public int buildDialogContent(Context context) {
    	NetworkInfo info = connManager.getActiveNetworkInfo();
    	boolean ethConnect = info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_ETHERNET;
        this.setTitle(R.string.eth_config_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.eth_configure, null));
        mDevs = (TextView) mView.findViewById(R.id.eth_dev_list_text);
        mDevList = (Spinner) mView.findViewById(R.id.eth_dev_spinner);
        mConTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mConTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        mIpaddr = (EditText)mView.findViewById(R.id.ipaddr_edit);
        mMask = (EditText)mView.findViewById(R.id.netmask_edit);
        mDns = (EditText)mView.findViewById(R.id.eth_dns_edit);
        mGw = (EditText)mView.findViewById(R.id.eth_gw_edit);
        mIpaddr.addTextChangedListener(this);
        mMask.addTextChangedListener(this);
        mDns.addTextChangedListener(this);
        mGw.addTextChangedListener(this);
        
        if(ethConnect){
        	String ip = connManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET).getAddresses().toString();
        	if (ip != "[]" )
        		mIpaddr.setText(ip.substring(2, ip.length()-1));
        	String dns = "";
        	for( InetAddress d : connManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET).getDnses()) {
                String temp = d.toString();
                if (temp != null)
                    dns = temp.substring(1, temp.length()-1);
                break;
            }
        	mDns.setText(dns);
        }

        mConTypeDhcp.setChecked(true);
        mConTypeManual.setChecked(false);
        mIpaddr.setEnabled(false);
        mMask.setEnabled(false);
        mDns.setEnabled(false);
        mGw.setEnabled(false);
        mConTypeManual.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mIpaddr.setEnabled(true);
                mDns.setEnabled(true);
                mGw.setEnabled(true);
                mMask.setEnabled(true);
            }
        });

        mConTypeDhcp.setOnClickListener(new RadioButton.OnClickListener() {
           public void onClick(View v) {
                mIpaddr.setEnabled(false);
                mDns.setEnabled(false);
                mGw.setEnabled(false);
                mMask.setEnabled(false);
            }
        });

        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
//        String[] Devs = mEthManager.getDeviceNameList();
        
        return 0;
    }
    
    @Override
    public void show() {
    	// TODO Auto-generated method stub
    	String[] Devs = new String[]{"eth0"};
		if (Devs != null) {
			if (localLOGV)
				Slog.v(TAG, "found device: " + Devs[0]);
			updateDevNameList(Devs);
			if (mEthManager.isConfigured()) {
				mEthInfo = mEthManager.getSavedConfig();
				for (int i = 0; i < Devs.length; i++) {
					if (Devs[i].equals(mEthInfo.getIfName())) {
						mDevList.setSelection(i);
						break;
					}
				}
				mIpaddr.setText(mEthInfo.getIpAddress());
				mGw.setText(mEthInfo.getRouteAddr());
				mDns.setText(mEthInfo.getDnsAddr());
				mMask.setText(mEthInfo.getNetMask());
				boolean isDHCP = mEthInfo.getConnectMode().equals(
						EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
				mConTypeDhcp.setChecked(isDHCP);
				mConTypeManual.setChecked(!isDHCP);
				mIpaddr.setEnabled(!isDHCP);
				mDns.setEnabled(!isDHCP);
				mGw.setEnabled(!isDHCP);
				mMask.setEnabled(!isDHCP);
			} else {
				for (int i = 0; i < Devs.length; i++) {
					if (Devs[i].equals("eth0")) {
						mDevList.setSelection(i);
						break;
					}
				}
			}
		}
    	super.show();
    }
    
    private void setConfigEnable(boolean enabled){
    	mConTypeDhcp.setEnabled(enabled);
    	mConTypeManual.setEnabled(enabled);
    	this.getButton(BUTTON_POSITIVE).setEnabled(enabled);
    	mRelationPref.setEnabled(enabled);
    	mRelationPref.setSummary(enabled? R.string.eth_conf_summary : mConTypeDhcp.isChecked()? R.string.getting_ip_address : R.string.configuring_ethernet);
    }

    private void handle_saveconf() {
        EthernetDevInfo info = new EthernetDevInfo();
        info.setIfName(mDevList.getSelectedItem().toString());
        if (localLOGV)
            Slog.v(TAG, "Config device for " + mDevList.getSelectedItem().toString());
        if (mConTypeDhcp.isChecked()) {
            Slog.v(TAG, "Config device for DHCP ");
            info.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
            info.setIpAddress(null);
            info.setRouteAddr(null);
            info.setDnsAddr(null);
            info.setNetMask(null);
        } else {
            Slog.v(TAG, "Config device for static " + mIpaddr.getText().toString() + mGw.getText().toString() + mDns.getText().toString() + mMask.getText().toString());
            info.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
            info.setIpAddress(mIpaddr.getText().toString());
            info.setRouteAddr(mGw.getText().toString());
            info.setDnsAddr(mDns.getText().toString());
            info.setNetMask(mMask.getText().toString());
        }
        mEthManager.updateDevInfo(info);
        mEthManager.resetInterface();
        if (mEnablePending) {
            mEthManager.setEnabled(true);
            mEnablePending = false;
        }
    }
    
    public static boolean isIPAddress(String ipaddr) {
    	boolean flag = false;
    	Pattern pattern = Pattern.compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
    	Matcher m = pattern.matcher(ipaddr);
    	flag = m.matches();
    	return flag;
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
            	new SaveConfigTask().execute();
                break;
            case BUTTON_NEGATIVE:
                //Don't need to do anything
                break;
            default:
                Slog.e(TAG,"Unknow button");
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onClick(View v) {

    }

    public void updateDevNameList(String[] DevList) {
        if (DevList != null) {
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                    getContext(), android.R.layout.simple_spinner_item, DevList);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            mDevList.setAdapter(adapter);
        }

    }

    public void enableAfterConfig() {
        mEnablePending = true;
    }
    
    class SaveConfigTask extends AsyncTask<Void, Void, Void>{

    	@Override
    	protected void onPreExecute() {
    		// TODO Auto-generated method stub
    		setConfigEnable(false);
    	}
    	
    	@Override
    	protected void onPostExecute(Void result) {
    		// TODO Auto-generated method stub
    		setConfigEnable(true);
    	}
    	
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			handle_saveconf();
			return null;
		}
    	
    }
    
    public boolean isIpAvalid(EditText et){
    	String text = et.getText().toString();
    	return text != null && isIPAddress(text);
    }
    
    public boolean checkSaveEnable(){
    	return mConTypeManual.isChecked() && isIpAvalid(mIpaddr) 
    			&& isIpAvalid(mMask) && isIpAvalid(mDns) && isIpAvalid(mGw);
    }

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		if(this.getButton(BUTTON_POSITIVE) != null)
			this.getButton(BUTTON_POSITIVE).setEnabled(checkSaveEnable());
	}
}

