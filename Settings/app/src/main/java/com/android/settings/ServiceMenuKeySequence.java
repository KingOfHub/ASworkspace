package com.android.settings;

import java.util.EventListener;
import android.view.KeyEvent;
import android.util.Log;

public class ServiceMenuKeySequence{
    private static final String TAG = "ServiceMenuKeySequence.java";

    public interface OnInvokeServiceMenuListener extends EventListener{
        public void onServiceMenu();
    }
    OnInvokeServiceMenuListener mrListener;

    long mlLastTime;
    int mnCurPos=0;
    final int[] mrnPreset= {
            KeyEvent.KEYCODE_VOLUME_UP, 
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP, 
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP, 
            KeyEvent.KEYCODE_VOLUME_UP, 
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            };
    final int[] mrnPresetNumber= {
            KeyEvent.KEYCODE_1, 
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3, 
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_1,
            };

    public ServiceMenuKeySequence(){
        setOnInvokeServiceMenuListener(null);
    }

    public boolean keyIn(int inKeyCode){
        //Log.i(TAG,"inKeyCode = "+ inKeyCode);
        if(mnCurPos < mrnPreset.length && (inKeyCode==mrnPreset[mnCurPos] || inKeyCode==mrnPresetNumber[mnCurPos])){
            if(mnCurPos==0){
                mlLastTime=System.currentTimeMillis();
                ++mnCurPos;
            } else {
                if(System.currentTimeMillis()-mlLastTime<10000){
                    ++mnCurPos;
                    if(mnCurPos>=mrnPreset.length){
                        mnCurPos=0;
                        mrListener.onServiceMenu();
                        return true;
                    }
                    mlLastTime=System.currentTimeMillis();
                } else {
                    mnCurPos=0;
                }
            }
        } else {
            mnCurPos=0;
        }
        return false;
    }

    public void setOnInvokeServiceMenuListener(OnInvokeServiceMenuListener irListener){
        if(irListener==null){
            mrListener = new OnInvokeServiceMenuListener(){
                public void onServiceMenu() {}
            };
        } else {
            mrListener = irListener;
        }
    }
}
