package com.android.settings.ytoevent;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * http://developer.android.com/reference/android/speech/tts/TextToSpeech.html
 * http://developer.android.com/reference/android/speech/RecognizerIntent.html
 * */
public class TextToSpeechService extends Service {

    public TextToSpeechService() {
        // TODO Auto-generated constructor stub
    }

    private String             mDefaultLanguage = null;
    private String             mDefaultCountry = null;
    private String             mDefaultLocVariant = null;
    private String             mDefaultEng = "";
    private int                mDefaultRate = 140;//TextToSpeech.Engine.DEFAULT_RATE;
    
    // TODO move default Locale values to TextToSpeech.Engine
    private static final String DEFAULT_LANG_VAL = "eng";
    private static final String DEFAULT_COUNTRY_VAL = "USA";
    private static final String DEFAULT_VARIANT_VAL = "";

    private static final String LOCALE_DELIMITER = "-";

    // Array of strings used to demonstrate TTS in the different languages.
    private String[] mDemoStrings;
    // Index of the current string to use for the demo.
    private int      mDemoStringIndex = 0;

    private boolean mEnableDemo = false;
    private boolean mVoicesMissing = false;

    private TextToSpeech mTts = null;
    private boolean mTtsStarted = false;
    
    /**
     * Request code (arbitrary value) for voice data check through
     * startActivityForResult.
     */
    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;
    private static final int GET_SAMPLE_TEXT = 1983;
    
    private static String TAG = "TextToSpeechService";
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String content = intent.getStringExtra("content");
            speakText(content);
        }
    };
    
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Locale currentLocale = Locale.getDefault();
        mDefaultLanguage = currentLocale.getISO3Language();
        mDefaultCountry = currentLocale.getISO3Country();
        mDefaultLocVariant = currentLocale.getVariant();
        mTts = new TextToSpeech(this, speechListener);
        IntentFilter inFilter = new IntentFilter();
        inFilter.addAction("com.yto.action.TEXT_TO_SPEECH");
        registerReceiver(mReceiver, inFilter);
    }
    
    private TextToSpeech.OnInitListener speechListener = new TextToSpeech.OnInitListener() {
        
        @Override
        public void onInit(int status) {
            // TODO Auto-generated method stub
            if (status == TextToSpeech.SUCCESS) {
                mTts.setLanguage(Locale.CHINA);
            }
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        /*String content = intent.getStringExtra("content");
        speakText(content);*/
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mTts.stop();
        if (mTts != null) {
            mTts.shutdown();
        }
    }

    protected void speakText(String content) {
        // TODO Auto-generated method stub
        
        int result = -1;
        if(content != null && !content.equals("")) {
            if (mTts != null) {
                    result = mTts.speak(content, TextToSpeech.QUEUE_FLUSH, null);
                
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

}
