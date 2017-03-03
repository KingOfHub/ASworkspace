package com.android.settings.wifi;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.wifi.AccessPoint;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.IpAssignment;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.ProxySettings;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.List;

import static com.android.internal.telephony.MSimConstants.MAX_PHONE_COUNT_TRI_SIM;

public class SystemSettingReceiver extends BroadcastReceiver {
    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private LinkProperties mLinkProperties = new LinkProperties();

    public static final String WIFI_SSID = "wifi_ssid";
    public static final String WIFI_AUTHTYPE = "wifi_authtype";
    public static final String WIFI_PASSWORD = "wifi_password";
    public static final String WIFI_ISHIDDEN = "wifi_ishidden";
    public static final String WIFI_ISDHCP = "wifi_isDHCP";
    public static final String WIFI_LOCAL_IP = "wifi_localip";
    public static final String WIFI_NET_GATE = "wifi_net_gate";
    public static final String WIFI_DNS1 = "wifi_DNS1";
    public static final String WIFI_DNS2 = "wifi_DNS2";
    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    WifiManager mWifiManager =null;
    private int mRequestedQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        android.util.Log.d("SystemSettingReceiver", action);

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(Build.PROJECT.equals("ID86")&& Build.PWV_CUSTOM_CUSTOM.equals("BELLE")) {
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

                String pin_install = android.os.SystemProperties.get("persist.sys.pin_install");
                if (pin_install.equals("off") || pin_install.equals("on")) {

                } else {
                    android.os.SystemProperties.set("persist.sys.pin_install", "on");
                }

                String pin_flag=android.os.SystemProperties.get("persist.sys.pin_set");
                if (pin_flag.equals("1") || pin_flag.equals("0")) {

                } else {
                    android.os.SystemProperties.set("persist.sys.pin_set", "0");
                }
                if(android.os.SystemProperties.get("persist.sys.pin_set").equals("0")) {
                    LockPatternUtils mLockPatternUtils = new LockPatternUtils(context);
                    mLockPatternUtils.clearLock(false);
                    mLockPatternUtils.setPowerButtonInstantlyLocks(false);
                    mLockPatternUtils.saveLockPassword("587412", mRequestedQuality, false);
                }
                mWifiManager.setWifiEnabled(false);
                mWifiManager.setWifiEnabled(true);
            }

            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {//wifi打开与否
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    WifiConfiguration tempConfig1 = isExsits("BAROQUE");
                    WifiConfiguration tempConfig2 = isExsits("MOUSSY");
                    WifiConfiguration tempConfig3 = isExsits("REPLAY");
                    if (tempConfig1 == null) {
                        WifiConfiguration wifiConfig = createWifiInfo("BAROQUE", "BARoqueP@ssw0rd", 3);
                        if (wifiConfig == null) {
                            Log.i("SystemSettingReceiver", "wifiConfig  :" +wifiConfig);
                        }
                        int netID = mWifiManager.addNetwork(wifiConfig);
                        mWifiManager.enableNetwork(netID, true);
                    }
                    if (tempConfig2 == null) {
                        WifiConfiguration wifiConfig = createWifiInfo("MOUSSY", "BARoqueP@ssw0rd", 3);
                        if (wifiConfig == null) {
                            Log.i("SystemSettingReceiver", "wifiConfig  :" +wifiConfig);
                        }
                        int netID = mWifiManager.addNetwork(wifiConfig);
                        mWifiManager.enableNetwork(netID, true);
                    }
                    if (tempConfig3 == null) {
                        WifiConfiguration wifiConfig = createWifiInfo("REPLAY", "BARoqueP@ssw0rd", 3);
                        if (wifiConfig == null) {
                            Log.i("SystemSettingReceiver", "wifiConfig  :" +wifiConfig);
                        }
                        int netID = mWifiManager.addNetwork(wifiConfig);
                        mWifiManager.enableNetwork(netID, true);
                    }


                }

            }
        }

        if(action.equals("android.action.CHANGE_WIFI_NETWORK_STATE")) {
            Bundle bundle = intent.getExtras();
            WifiConfiguration config = getWifiConfig(bundle);

            /*mWifiManager.save(config, new WifiManager.ActionListener() {
                @Override
                public void onSuccess() {
                    android.util.Log.d("SystemSettingReceiver", "onSuccess");
                    final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
                    for (WifiConfiguration config : configs) {
                        String confSSID = config.SSID;
                        try{
                            if(confSSID != null && confSSID.length() >2) {
                                int len = config.SSID.length();
                                confSSID = confSSID.substring(1, len-1);
                            }
                        } catch (IndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }
                            mWifiManager.enableNetwork(config.networkId, false);//启用 符合的AP
                            mWifiManager.reconnect();
                           
                    }
                }
                @Override
                public void onFailure(int reason) {
                    android.util.Log.d("SystemSettingReceiver", "onFailure" + reason);
                }
        });*/
            int wcgID = mWifiManager.addNetwork(config);
            android.util.Log.d("SystemSettingReceiver", "addNetwork id: " + wcgID);
            if(wcgID != WifiConfiguration.INVALID_NETWORK_ID) {
                boolean b = mWifiManager.enableNetwork(wcgID, false);
                mWifiManager.connect(wcgID, null);
            } else {
                mWifiManager.reconnect();
            }
            //mWifiManager.startScan();
        } else if(action.equals("android.action.CHANGE_NETWORK_STATE")) {
            String type = intent.getStringExtra("type");
            boolean enable = intent.getBooleanExtra("enable", false);
            if(type != null) {
                if (type.equals("mobile")) {
                    ConnectivityManager cm =
                            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    boolean mobileState = cm.getMobileDataEnabled();
                    if(!(mobileState == enable)) {
                        cm.setMobileDataEnabled(enable);
                    }
                } else if (type.equals("wifi")) {
                    //WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    boolean state = mWifiManager.isWifiEnabled();
                    if(!(state == enable)) {
                        mWifiManager.setWifiEnabled(enable);
                    }
                }
            }
        }
    }

    private WifiConfiguration createWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";
        if (Type == 1) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 2) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type ==3) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            // 此处需要修改否则不能自动重联
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } else {
            return null;
        }
        return config;
    }

    private WifiConfiguration isExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration getWifiConfig(Bundle bundle){

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = AccessPoint.convertToQuotedString(
                bundle.getString("wifi_ssid"));
        // If the user adds a network manually, assume that it is hidden.
        config.hiddenSSID = bundle.getBoolean("wifi_ishidden");
        String password = bundle.getString("wifi_password", "");
        int  mAccessPointSecurity = bundle.getInt("wifi_authtype", 1) -1;
        switch (mAccessPointSecurity) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (password.length() != 0) {
                    int length = password.length();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case SECURITY_PSK:
            case 3://CIPHERTYPE_WPA2
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (password.length() != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            /*case AccessPoint.SECURITY_EAP:
                config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = mEapMethodSpinner.getSelectedItemPosition();
                int phase2Method = mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the PHASE2_PEAP_ADAPTER to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_NONE:
                                config.enterpriseConfig.setPhase2Method(Phase2.NONE);
                                break;
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        // The default index from PHASE2_FULL_ADAPTER maps to the API
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                String caCert = (String) mEapCaCertSpinner.getSelectedItem();
                if (caCert.equals(unspecifiedCert)) caCert = "";
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                String clientCert = (String) mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(unspecifiedCert)) clientCert = "";
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                config.enterpriseConfig.setAnonymousIdentity(
                        mEapAnonymousView.getText().toString());

                if (mPasswordView.isShown()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                }
                break;*/
            default:
                return null;
        }

        config.proxySettings = ProxySettings.NONE;//mProxySettings;
        mIpAssignment = bundle.getBoolean(WIFI_ISDHCP) ? IpAssignment.STATIC : IpAssignment.DHCP ;
        config.ipAssignment = mIpAssignment;
        if(mIpAssignment == IpAssignment.STATIC) {
            validateIpConfigFields(bundle);
        }
        config.linkProperties = new LinkProperties(mLinkProperties);
        android.util.Log.d("SystemSettingReceiver", config.toString());
        return config;
    }

    private void validateIpConfigFields(Bundle bundle) {

        InetAddress inetAddr = null;
        String ipAddr = bundle.getString(WIFI_LOCAL_IP);
        try {
            inetAddr = NetworkUtils.numericToInetAddress(ipAddr);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        mLinkProperties.addLinkAddress(new LinkAddress(inetAddr, 24));

        String gateway = bundle.getString(WIFI_NET_GATE);
        if (TextUtils.isEmpty(gateway)) {
            try {
                //Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, 24);
                byte[] addr = netPart.getAddress();
                addr[addr.length-1] = 1;
                try {
                    netPart = NetworkUtils.numericToInetAddress(InetAddress.getByAddress(addr).getHostAddress());
                } catch (IllegalArgumentException e) {
                }
                mLinkProperties.addRoute(new RouteInfo(netPart));
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = null;
            try {
                gatewayAddr = NetworkUtils.numericToInetAddress(gateway);
            } catch (IllegalArgumentException e) {
            }
            mLinkProperties.addRoute(new RouteInfo(gatewayAddr));
        }

        InetAddress dnsAddr = null;
        String dns = bundle.getString(WIFI_DNS1);
        if (TextUtils.isEmpty(dns)) {
            //If everything else is valid, provide hint as a default option
        } else {
            try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns);
            } catch (IllegalArgumentException e) {
            }
            mLinkProperties.addDns(dnsAddr);
        }
        String dns2 = bundle.getString(WIFI_DNS2);
        if (!TextUtils.isEmpty(dns)) {
            try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns2);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mLinkProperties.addDns(dnsAddr);
        }
    }
}
