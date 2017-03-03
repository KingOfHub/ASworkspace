package com.android.settings;

import android.app.DockerManager;
import android.content.Context;
import android.content.Intent;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;

public class DockerEnable implements OnCheckedChangeListener {
	
	public static final Intent intent = new Intent("docker.setting.change");
	
	private Context mContext;
	private Switch mSwitch;
	
	public DockerEnable(Context context, Switch switch_) {
		mContext = context;
		mSwitch = switch_;
		
		syncSwitchStat();
	}
	
	private void syncSwitchStat(){
		DockerManager mDockerManager = DockerManager.getInstance();
		mSwitch.setChecked(mDockerManager.isDockerEnabled());
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
		// TODO Auto-generated method stub
		DockerManager.getInstance().setDockerEnabled(isChecked);
		intent.putExtra("checked", isChecked);
		mContext.sendBroadcast(intent);
	}
	
	public void setSwitch(Switch switch_) {
		if (mSwitch == switch_){
			syncSwitchStat();
			return;
		}

		mSwitch.setOnCheckedChangeListener(null);
		mSwitch = switch_;
		mSwitch.setOnCheckedChangeListener(this);
		syncSwitchStat();
	}

	public void resume() {
		mSwitch.setOnCheckedChangeListener(this);
	}

	public void pause() {
		mSwitch.setOnCheckedChangeListener(null);
	}
	
	public Switch getSwitch(){
		return mSwitch;
	}

}
