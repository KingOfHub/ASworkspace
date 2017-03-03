package com.example.urovo_dotorom.netcert_check_apk;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class Settings {
    static final String TAG = "Superuser";
    SQLiteDatabase mDatabase;
    Context mContext;

     

     

     
    
     

    private static final String KEY_NOTIFICATION = "notification";
    public static final int NOTIFICATION_TYPE_NONE = 0;
    public static final int NOTIFICATION_TYPE_TOAST = 1;
    public static final int NOTIFICATION_TYPE_NOTIFICATION = 2;
    public static final int NOTIFICATION_TYPE_DEFAULT = NOTIFICATION_TYPE_TOAST;
     
    
    private static String digest(String value) {
        // ok, there's honestly no point in digesting the pin.
        // if someone gets a hold of the hash, there's really only like
        // 10^n possible values to brute force, where N is generally
        // 4. Ie, 10000. Yay, security theater. This really ought
        // to be a password.
        if (TextUtils.isEmpty(value))
            return null;
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            return Base64.encodeToString(digester.digest(value.getBytes()), Base64.DEFAULT);
        }
        catch (Exception e) {
            return value;
        }
    }
    
     

    private static final String KEY_REQUIRE_PREMISSION = "require_permission";
    
    
    private static final String KEY_AUTOMATIC_RESPONSE = "automatic_response";
    public static final int AUTOMATIC_RESPONSE_PROMPT = 0;
    public static final int AUTOMATIC_RESPONSE_ALLOW = 1;
    public static final int AUTOMATIC_RESPONSE_DENY = 2;
    public static final int AUTOMATIC_RESPONSE_DEFAULT = AUTOMATIC_RESPONSE_PROMPT;
    
    static public String readFile(String filename) throws IOException {
        return readFile(new File(filename));
    }
    
    static public String readFile(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        DataInputStream input = new DataInputStream(new FileInputStream(file));
        input.readFully(buffer);
        input.close();
        return new String(buffer);
    }
    
    public static void writeFile(File file, String string) throws IOException {
        writeFile(file.getAbsolutePath(), string);
    }
    
    public static void writeFile(String file, String string) throws IOException {
        File f = new File(file);
        f.getParentFile().mkdirs();
        DataOutputStream dout = new DataOutputStream(new FileOutputStream(f));
        dout.write(string.getBytes());
        dout.close();
    }
    
    public static byte[] readToEndAsArray(InputStream input) throws IOException {
        DataInputStream dis = new DataInputStream(input);
        byte[] stuff = new byte[1024];
        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        int read = 0;
        while ((read = dis.read(stuff)) != -1)
        {
            buff.write(stuff, 0, read);
        }
        input.close();
        return buff.toByteArray();
    }

    public static String readToEnd(InputStream input) throws IOException {
        return new String(readToEndAsArray(input));
    }

    public static final int MULTIUSER_MODE_OWNER_ONLY = 0;
    public static final int MULTIUSER_MODE_OWNER_MANAGED = 1;
    public static final int MULTIUSER_MODE_USER = 2;
    public static final int MULTIUSER_MODE_NONE = 3;
    
    private static final String MULTIUSER_VALUE_OWNER_ONLY  = "owner";
    private static final String MULTIUSER_VALUE_OWNER_MANAGED = "managed";
    private static final String MULTIUSER_VALUE_USER = "user";

    public static final int getMultiuserMode(Context context) {
        if (Build.VERSION.SDK_INT < 17)
            return MULTIUSER_MODE_NONE;

        if (!Helper.supportsMultipleUsers(context))
            return MULTIUSER_MODE_NONE;
        
        try {
            String mode;
            if (Helper.isAdminUser(context)) {
                File file = context.getFileStreamPath("multiuser_mode");
                mode = readFile(file);
            }
            else {
                Process p = Runtime.getRuntime().exec("su -u");
                mode = readToEnd(p.getInputStream()).trim();
            }
            
            if (MULTIUSER_VALUE_OWNER_MANAGED.equals(mode))
                return MULTIUSER_MODE_OWNER_MANAGED;
            if (MULTIUSER_VALUE_USER.equals(mode))
                return MULTIUSER_MODE_USER;
            if (MULTIUSER_VALUE_OWNER_ONLY.equals(mode))
                return MULTIUSER_MODE_OWNER_ONLY;
        }
        catch (Exception e) {
        }
        return MULTIUSER_MODE_OWNER_ONLY;
    }
    
    public static void setMultiuserMode(Context context, int mode) {
        if (!Helper.isAdminUser(context))
            return;
        try {
            File file = context.getFileStreamPath("multiuser_mode");
            switch (mode) {
            case MULTIUSER_MODE_OWNER_MANAGED:
                writeFile(file, MULTIUSER_VALUE_OWNER_MANAGED);
                break;
            case MULTIUSER_MODE_USER:
                writeFile(file, MULTIUSER_VALUE_USER);
                break;
            case MULTIUSER_MODE_NONE:
                file.delete();
                break;
            default:
                writeFile(file, MULTIUSER_VALUE_OWNER_ONLY);
                break;
            }
        }
        catch (Exception ex) {
        }
    }
    
    
    public static final int SUPERUSER_ACCESS_DISABLED = 0;
    public static final int SUPERUSER_ACCESS_APPS_ONLY = 1;
    public static final int SUPERUSER_ACCESS_ADB_ONLY = 2;
    public static final int SUPERUSER_ACCESS_APPS_AND_ADB = 3;
    public static int getSuperuserAccess() {
        try {
            Class c = Class.forName("android.os.SystemProperties");
            Method m = c.getMethod("get", String.class);
            String value = (String)m.invoke(null, "persist.sys.root_access");
            int val = Integer.valueOf(value);
            switch (val) {
            case SUPERUSER_ACCESS_DISABLED:
            case SUPERUSER_ACCESS_APPS_ONLY:
            case SUPERUSER_ACCESS_ADB_ONLY:
            case SUPERUSER_ACCESS_APPS_AND_ADB:
                return val;
            default:
                return SUPERUSER_ACCESS_APPS_AND_ADB;
            }
        }
        catch (Exception e) {
            return SUPERUSER_ACCESS_APPS_AND_ADB;
        }
    }
    
    public static void setSuperuserAccess(int mode) {
        try {
            if (android.os.Process.myUid() == android.os.Process.SYSTEM_UID) {
                Class c = Class.forName("android.os.SystemProperties");
                Method m = c.getMethod("set", String.class, String.class);
                m.invoke(null, "persist.sys.root_access", String.valueOf(mode));
                if (mode == getSuperuserAccess()) return;
            }
            String command = "setprop persist.sys.root_access " + mode;
            Process p = Runtime.getRuntime().exec("su");
            p.getOutputStream().write(command.getBytes());
            p.getOutputStream().close();
            int ret = p.waitFor();
            if (ret != 0) Log.w(TAG, "su failed: " + ret);
        }
        catch (Exception ex) {
            Log.w(TAG, "got exception: ", ex);
        }
    }
    
    private static final String CHECK_SU_QUIET = "check_su_quiet";
    
}
