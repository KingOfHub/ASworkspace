/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/urovo-dotorom/AndroidStudioProjects/Settings/app/src/main/aidl/com/android/cabl/ICABLService.aidl
 */
package com.android.cabl;
public interface ICABLService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.android.cabl.ICABLService
{
private static final java.lang.String DESCRIPTOR = "com.android.cabl.ICABLService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.android.cabl.ICABLService interface,
 * generating a proxy if needed.
 */
public static com.android.cabl.ICABLService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.android.cabl.ICABLService))) {
return ((com.android.cabl.ICABLService)iin);
}
return new com.android.cabl.ICABLService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_control:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.control(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setVisualQualityLevel:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _result = this.setVisualQualityLevel(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.android.cabl.ICABLService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public boolean control(int controlType) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(controlType);
mRemote.transact(Stub.TRANSACTION_control, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public boolean setVisualQualityLevel(java.lang.String level) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(level);
mRemote.transact(Stub.TRANSACTION_setVisualQualityLevel, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_control = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_setVisualQualityLevel = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public boolean control(int controlType) throws android.os.RemoteException;
public boolean setVisualQualityLevel(java.lang.String level) throws android.os.RemoteException;
}
