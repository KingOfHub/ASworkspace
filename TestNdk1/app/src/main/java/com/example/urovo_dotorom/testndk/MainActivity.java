package com.example.urovo_dotorom.testndk;


import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    static{
        System.loadLibrary("MyLibrary");
    }

    public native String getString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

}
