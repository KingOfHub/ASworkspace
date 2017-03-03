package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.device.ScanManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class ScannerEnable  implements CompoundButton.OnCheckedChangeListener {

	private static final String TAG = "ScannerEnable";

	private ScanManager mScanManager;

    private final Context mContext;
	private Switch mSwitch;
    public static final String RADIO_AVALIABLE_ACTION =
            "radio.avaliable.message";
//	private NVAccess NVm;
	Intent intent = new Intent("scan.setting.change");
//    private final IntentFilter mIntentFilter;
    
/*	private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			Log.i(TAG, "action == " + action);
			if (RADIO_AVALIABLE_ACTION.equals(action)) {
				SetPowerSwitch();
			}
		}

	};*/

	private int syncScantype() {
		int typeInit = mScanManager.getOutputParameter(7);
		if (typeInit == 0) {
/*			int tmp = NVm.getScanType();
			Log.i(TAG, "scan type " + tmp);
			if (tmp > 0) {
				mScanManager.setOutputParameter(7, tmp);
				mSwitch.setEnabled(true);
				return tmp;
			}*/
			mSwitch.setEnabled(false);
		}
		mSwitch.setEnabled(true);		
		return typeInit;
	}

	private void SetPowerSwitch(){
		int isenabled = mScanManager.getOutputParameter(4);
		if (isenabled == 1) {
			mSwitch.setChecked(true);
		} else {
			mSwitch.setChecked(false);
		}
		syncScantype();
	}

	public ScannerEnable(Context context, Switch switch_) {
		mContext = context;
		mSwitch = switch_;
		mScanManager = new ScanManager();
//		mIntentFilter = new IntentFilter(RADIO_AVALIABLE_ACTION);
//		NVm = new NVAccess();
		syncScantype();
	}

	public void setSwitch(Switch switch_) {
		if (mSwitch == switch_){
			SetPowerSwitch();
			return;
		}

		mSwitch.setOnCheckedChangeListener(null);
		mSwitch = switch_;
		mSwitch.setOnCheckedChangeListener(this);
		SetPowerSwitch();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onCheckedChanged= " + isChecked);
/*		if (mScanManager.getOutputParameter(7) == 0) {
			Toast.makeText(mContext, R.string.scanner_type_unknown,
					Toast.LENGTH_SHORT).show();
			return;
		}*/
		if (isChecked) {
			mScanManager.openScanner();
//			mScanManager.setOutputParameter(4, 1);
		} else {
			mScanManager.closeScanner();
//			mScanManager.setOutputParameter(4, 0);
		}
		intent.putExtra("checked", isChecked);
		mContext.sendBroadcast(intent);
	}

	public void resume() {
//		mContext.registerReceiver(mScanReceiver, mIntentFilter);
		mSwitch.setOnCheckedChangeListener(this);
	}

	public void pause() {
		mSwitch.setOnCheckedChangeListener(null);
//		mContext.unregisterReceiver(mScanReceiver);
	}
}


