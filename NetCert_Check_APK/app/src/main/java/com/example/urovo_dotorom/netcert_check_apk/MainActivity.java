package com.example.urovo_dotorom.netcert_check_apk;

import java.io.DataOutputStream;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private ConnectivityManager mCM;

    private Button rootButton;
    private Button openButton;
    private Button shutButton;
    private Button bt_open_wifi;
    private Button bt_close_wifi;
    private Button localButton;
    private WifiManager wifiManager;
    private TelephonyManager telephonyManager;
    private ConnectivityManager connectivityManager;
    // 这个是查看一下，现在有没有联网，如果有联网就为true，没有就为false
    private boolean isConnect = false;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*  final ProgressDialog dlg = new ProgressDialog(this);
        dlg.setTitle(R.string.superuser);
        dlg.setMessage(getString(R.string.checking_superuser));
        dlg.setIndeterminate(true);
        dlg.show();
       new Thread() {
            public void run() {
                boolean _error = false;
                try {
                    SuHelper.checkSu(MainActivity.this);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    _error = true;
                }
                final boolean error = _error;
                dlg.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (error) {
                            doInstall();
                        }
                        else {
                            doWhatsNew();
                        }
                    }
                });
            };
        }.start();*/

        // 拿到一个wifi管理器，用来管理wifi的开关
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // 拿到一个telphonyManager，用来判断我们现在有没有联网的
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getDataState() == TelephonyManager.DATA_CONNECTED)
        {
            isConnect = true;
            android.util.Log.v("=======@@@@@======",isConnect+"");
        }
        // 拿到一个链接管理器，打开和关闭数据链接都通过它控制了
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null)
        {
            android.util.Log.v("=======@@@@@======","当前没有联网");
        }
        else
        {
            android.util.Log.v("=======@@@@@======",networkInfo.isAvailable()+"");
            android.util.Log.v("=======@@@@@======",networkInfo.getTypeName());
        }



        mCM = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        openButton = (Button)findViewById(R.id.btnOpen);
        shutButton = (Button)findViewById(R.id.btnShut);
        bt_open_wifi = (Button) findViewById(R.id.bt_open_wifi);
        bt_close_wifi = (Button) findViewById(R.id.bt_close_wifi);
        localButton = (Button) findViewById(R.id.access_location);
        rootButton =  (Button) findViewById(R.id.root_check);

        openButton.setOnClickListener(btnListener);
        shutButton.setOnClickListener(btnListener);
        bt_open_wifi.setOnClickListener(btnListener);
        bt_close_wifi.setOnClickListener(btnListener);
        localButton.setOnClickListener(btnListener);
        rootButton.setOnClickListener(btnListener);
    }


    private Button.OnClickListener btnListener = new Button.OnClickListener()
    {

        @Override
        public void onClick(View v)
        {
            switch (v.getId())
            {
                case R.id.root_check:
                    try
                    {
                        Boolean su = SuperUserUtils.canExecuteSU();
                        Toast t = Toast.makeText(getApplicationContext(), su?"root success!":"root failed!", Toast.LENGTH_LONG);
                        t.show();
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                    }

                case R.id.btnOpen:

                    setGprsEnabled(true,getApplication());
                    gprsEnable(true);

                    break;
                case R.id.btnShut:

                    setGprsEnabled(false,getApplication());
                    gprsEnable(false);

                    break;

                case R.id.bt_open_wifi:
                    if (!wifiManager.isWifiEnabled())
                    {
                        wifiManager.setWifiEnabled(true);
                        android.util.Log.v("=======@@@@@======","正在打开wifi");
                    }
                    break;

                case R.id.bt_close_wifi:
                    if (wifiManager.isWifiEnabled())
                    {
                        wifiManager.setWifiEnabled(false);
                        android.util.Log.v("=======@@@@@======","正在关闭wifi");
                    }
                    break;

                case R.id.access_location:

                    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    break;

                default:
                    break;
            }
        }

    };

    //�򿪻�ر�GPRS
    private boolean gprsEnable(boolean bEnable)
    {
        Object[] argObjects = null;

        boolean isOpen = gprsIsOpenMethod("getMobileDataEnabled");
        if(isOpen == !bEnable)
        {
            setGprsEnable("setMobileDataEnabled", bEnable);
            Log.v("==========", isOpen+"");
        }

        return isOpen;
    }

    //���GPRS�Ƿ��
    private boolean gprsIsOpenMethod(String methodName)
    {
        Class cmClass 		= mCM.getClass();
        Class[] argClasses 	= null;
        Object[] argObject 	= null;

        Boolean isOpen = false;
        try
        {
            Method method = cmClass.getMethod(methodName, argClasses);

            isOpen = (Boolean) method.invoke(mCM, argObject);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return isOpen;
    }

    //����/�ر�GPRS
    private void setGprsEnable(String methodName, boolean isEnable)
    {
        Class cmClass 		= mCM.getClass();
        Class[] argClasses 	= new Class[1];
        argClasses[0] 		= boolean.class;

        try
        {
            Method method = cmClass.getMethod(methodName, argClasses);
            method.invoke(mCM, isEnable);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    private final static String COMMAND_L_ON = "svc data enable\n ";
    private final static String COMMAND_L_OFF = "svc data disable\n ";
    private final static String COMMAND_SU = "su\n";
    public static void setGprsEnabled(boolean enable,Context context){
        String command;
        if(enable)
            command = COMMAND_L_ON;
        else
            command = COMMAND_L_OFF;
        try{
            Process su = Runtime.getRuntime().exec(COMMAND_SU);
            Log.v("=====dotorom=====", "su over");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
            outputStream.writeBytes(command);
            Log.v("=====dotorom=====", "command over"+"   "+command);
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            Log.v("=====dotorom=====", "exit");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (Exception e) {
                e.printStackTrace();
            }
            outputStream.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private static final String WHATS_NEW = "Added support for Android 4.3.";
    protected void doWhatsNew() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.whats_new);
        /*builder.setIcon(R.drawable.ic_launcher);*/
        builder.setMessage(WHATS_NEW);
        builder.setPositiveButton(R.string.rate, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }


    void doInstall() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.install);
        builder.setMessage(R.string.install_superuser_info);
        if (Build.VERSION.SDK_INT < 18) {
            builder.setPositiveButton(R.string.install, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getExternalStoragePath();
                }
            });
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setNeutralButton(R.string.recovery_install, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }
/*
 * Environment 是一个提供访问环境变量的类。
Environment 包含常量：
MEDIA_BAD_REMOVAL
解释：返回getExternalStorageState() ，表明SDCard 被卸载前己被移除
MEDIA_CHECKING
解释：返回getExternalStorageState() ，表明对象正在磁盘检查。
MEDIA_MOUNTED
解释：返回getExternalStorageState() ，表明对象是否存在并具有读/写权限
MEDIA_MOUNTED_READ_ONLY
解释：返回getExternalStorageState() ，表明对象权限为只读
MEDIA_NOFS
解释：返回getExternalStorageState() ，表明对象为空白或正在使用不受支持的文件系统。
MEDIA_REMOVED
解释：返回getExternalStorageState() ，如果不存在 SDCard 返回
MEDIA_SHARED
解释：返回getExternalStorageState() ，如果 SDCard 未安装 ，并通过 USB 大容量存储共享 返回
MEDIA_UNMOUNTABLE
解释：返回getExternalStorageState() ，返回 SDCard 不可被安装 如果 SDCard 是存在但不可以被安装
MEDIA_UNMOUNTED
解释：返回getExternalStorageState() ，返回 SDCard 已卸掉如果 SDCard   是存在但是没有被安装
Environment 常用方法：
方法：getDataDirectory()
解释：返回 File ，获取 Android 数据目录。
方法：getDownloadCacheDirectory()
解释：返回 File ，获取 Android 下载/缓存内容目录。
方法：getExternalStorageDirectory()
解释：返回 File ，获取外部存储目录即 SDCard
方法：getExternalStoragePublicDirectory(String type)
解释：返回 File ，取一个高端的公用的外部存储器目录来摆放某些类型的文件
方法：getExternalStorageState()
解释：返回 File ，获取外部存储设备的当前状态
方法：getRootDirectory()
解释：返回 File ，获取 Android 的根目录
2、讲述 StatFs 类
StatFs 一个模拟linux的df命令的一个类,获得SD卡和手机内存的使用情况
StatFs 常用方法:
getAvailableBlocks()
解释：返回 Int ，获取当前可用的存储空间
getBlockCount()
解释：返回 Int ，获取该区域可用的文件系统数
getBlockSize()
解释：返回 Int ，大小，以字节为单位，一个文件系统
getFreeBlocks()
解释：返回 Int ，该块区域剩余的空间
restat(String path)
解释：执行一个由该对象所引用的文件系统
 */

    public String getExternalStoragePath() {
        // 获取SdCard状态
        String state = android.os.Environment.getExternalStorageState();
        // 判断SdCard是否存在并且是可用的
        if (android.os.Environment.MEDIA_MOUNTED.equals(state)) {
            if (android.os.Environment.getExternalStorageDirectory().canWrite()) {
                getAvailableStore(android.os.Environment.getExternalStorageDirectory().getPath());
                Toast.makeText(MainActivity.this, "getExternalStorageDirectory " + android.os.Environment.getExternalStorageDirectory().getPath(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "getExternalStorageState " + state, Toast.LENGTH_LONG).show();
        }
        return state;
    }

    public static long getAvailableStore(String filePath) {
        // 取得sdcard文件路径
        StatFs statFs = new StatFs(filePath);
        // 获取block的SIZE
        long blocSize = statFs.getBlockSize();
        // 获取BLOCK数量
        // long totalBlocks = statFs.getBlockCount();
        // 可使用的Block的数量
        long availaBlock = statFs.getAvailableBlocks();
        // long total = totalBlocks * blocSize;
        long availableSpare = availaBlock * blocSize;
        return availableSpare;
    }

    /*private static ArrayList<String> getDevMountList() {
        String[] toSearch = android.text.FileUtils.readFile("/etc/vold.fstab").split(" ");
        ArrayList<String> out = new ArrayList<String>();
        for (int i = 0; i < toSearch.length; i++) {
            if (toSearch[i].contains("dev_mount")) {
                if (new File(toSearch[i + 2]).exists()) {
                    out.add(toSearch[i + 2]);
                }
            }
        }
        return out;
    }*/
}