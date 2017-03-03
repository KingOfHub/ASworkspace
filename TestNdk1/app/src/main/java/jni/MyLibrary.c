#include <jni.h>
#include "com_example_urovo_dotorom_testndk_MainActivity.h"

JNIEXPORT jstring JNICALL Java_com_example_urovo_dotorom_ndksample_MainActivity_testndk_getString
  (JNIEnv * env, jobject obj){
   return (*env)->NewStringUTF("This is mylibrary !!!");
  }
