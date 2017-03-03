package com.example.urovo_dotorom.jnitest;


import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    static {
        System.loadLibrary("MyLib");
    }

    public native String getStringFromNative();

    public native int NumAdd(int a,int b);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView)findViewById(R.id.textView01);
        tv.setText(getStringFromNative());

        TextView tv2 = (TextView)findViewById(R.id.textView02);
        tv2.setText(Integer.toString(NumAdd(3,3)));

    }


}
