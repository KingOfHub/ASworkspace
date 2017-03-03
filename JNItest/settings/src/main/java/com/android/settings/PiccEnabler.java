package com.android.settings;

import android.content.Context;
import android.content.Intent;
//import android.device.ScanManager;
import android.device.PiccReader;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import android.content.SharedPreferences;

public class PiccEnabler  implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "ScannerEnable";
    private final PiccReader mPiccReader;
    private final Context mContext;
    private Switch mSwitch;
    public static final String RADIO_AVALIABLE_ACTION =
            "radio.avaliable.message";
    public PiccEnabler(Context context, Switch switch_) {
		mContext = context;
		mSwitch = switch_;
                mPiccReader=new PiccReader();
 
    }
    private void SetInitSwitch(){
               SharedPreferences settings = mContext.getSharedPreferences("picc_check", mContext.MODE_PRIVATE);
               int state = settings.getInt("check", 0);
	       if (state == 1) {
			mSwitch.setChecked(true);
		} else {
			mSwitch.setChecked(false);
		}
	}
    public void setSwitch(Switch switch_) {
		if (mSwitch == switch_){
			return;
		}
		mSwitch.setOnCheckedChangeListener(null);
		mSwitch = switch_;
		mSwitch.setOnCheckedChangeListener(this);
                
                SetInitSwitch();
    }

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if (isChecked) {
                        mPiccReader.Open();
		} else {
                        mPiccReader.Close();
		}
                SharedPreferences mySharedPreferences= mContext.getSharedPreferences("picc_check",mContext.MODE_PRIVATE);
                SharedPreferences.Editor editor = mySharedPreferences.edit(); 
                if(isChecked){
                     editor.putInt("check",1);
                }else {
                     editor.putInt("check",0);
                }
                editor.commit();
	}

	public void resume() {
		mSwitch.setOnCheckedChangeListener(this);
	}

	public void pause() {
		mSwitch.setOnCheckedChangeListener(null);
	}
}


