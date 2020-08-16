package com.eszdman.photoncamera.Parameters;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.util.Range;

import com.eszdman.photoncamera.api.Interface;
import com.eszdman.photoncamera.ui.CameraFragment;

public class IsoExpoSelector {
    private static String TAG = "IsoExpoSelector";
    public static final int baseFrame = 1;
    public static boolean HDR = false;
    public static void setExpo(CaptureRequest.Builder builder, int step) {
        double mpy = 0.8;
        ExpoPair pair = new ExpoPair(CameraFragment.context.mPreviewExposuretime,getEXPLOW(),getEXPHIGH(),
                CameraFragment.context.mPreviewIso,getISOLOW(),getISOHIGH());
        ExpoPair startPair = new ExpoPair(pair);
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        pair.normalizeiso100();
        Log.d(TAG, "InputParams: expo time:" + ExposureIndex.sec2string(ExposureIndex.time2sec(pair.exposure)) + " iso:" + pair.iso);
        if(Interface.i.settings.nightMode) mpy = 2.0;
        if (pair.exposure < ExposureIndex.sec / 40 && pair.iso > 90) {
            pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 13 && pair.iso > 750) {
            pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 8 && pair.iso > 1500) {
            if(step != baseFrame || !Interface.i.settings.eisPhoto) pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 8 && pair.iso > 1500) {
            if(step != baseFrame || !Interface.i.settings.eisPhoto) pair.ReduceIso(1.25);
        }
        if (pair.iso >= 12700) {
            pair.ReduceIso();
        }
        if (CameraFragment.mTargetFormat == CameraFragment.rawFormat){
        if (pair.iso >= 100/0.65) pair.iso *= mpy;
            else {
                pair.exposure *= mpy;
            }
        }
        if(Interface.i.settings.ManualMode && Interface.i.manual.exposure){
            pair.exposure = (long)(ExposureIndex.sec*Interface.i.manual.expvalue);
            pair.iso = (int)(Interface.i.manual.isovalue/getMPY());
        }
        if(step == 3 && HDR){
            pair.ExpoCompensateLower(1.0/6.0);
        }
        if(step == 2 && HDR){
            pair.ExpoCompensateLower(6.0);
        }
        if(pair.exposure < ExposureIndex.sec/90 && Interface.i.settings.eisPhoto){
            //HDR = true;
        }
        if(step == baseFrame){
            if(pair.iso <= 120 && pair.exposure > ExposureIndex.sec/70 && Interface.i.settings.eisPhoto){
                pair.ReduceExpo();
            }
            if(pair.iso <= 245 && pair.exposure > ExposureIndex.sec/50 && Interface.i.settings.eisPhoto){
                pair.ReduceExpo();
            }
            if(pair.exposure < ExposureIndex.sec*3.00 && pair.exposure > ExposureIndex.sec/3 && pair.iso < 3200 && Interface.i.settings.eisPhoto){
                pair.FixedExpo(1.0/8);
                if(pair.normalizeCheck()) Interface.i.camera.showToast("Wrong parameters: iso:"+pair.iso+ " exp:"+pair.exposure);
            }
        }
        pair.denormalizeSystem();
        Log.d(TAG, "IsoSelected:" + pair.iso + " ExpoSelected:" + ExposureIndex.sec2string(ExposureIndex.time2sec(pair.exposure)) + " sec step:"+step+" HDR:"+HDR);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, pair.exposure);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, pair.iso);
    }


    public static double getMPY(){
        return 50.0/getISOLOW();
    }
    private static int mpyIso(int in){
        return (int)(in*getMPY());
    }
    private static int getISOHIGH() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if (key == null) return 3200;
        else {
            return (int) ((Range) (key)).getUpper();
        }
    }
    public static int getISOHIGHExt() {
        return mpyIso(getISOHIGH());
    }
    private static int getISOLOW() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if (key == null) return 100;
        else {
            return (int) ((Range) (key)).getLower();
        }
    }
    public static int getISOLOWExt() {
        return mpyIso(getISOLOW());
    }
    public static long getEXPHIGH() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        if (key == null) return ExposureIndex.sec;
        else {
            return (long) ((Range) (key)).getUpper();
        }
    }
    public static long getEXPLOW() {
        Object key = CameraFragment.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        if (key == null) return ExposureIndex.sec/1000;
        else {
            return (long) ((Range) (key)).getLower();
        }
    }
    static class ExpoPair {
        long exposure;
        long exposurehigh,exposurelow;
        int iso;
        int isolow,isohigh;
        public ExpoPair(ExpoPair pair){
            copyfrom(pair);
        }
        public ExpoPair(long expo,long expl,long exph,int is,int islow,int ishigh){
            exposure = expo;
            iso = is;
            exposurehigh = exph;
            exposurelow = expl;
            isolow = islow;
            isohigh = ishigh;
        }
        public void copyfrom(ExpoPair pair){
            exposure = pair.exposure;
            exposurelow = pair.exposurelow;
            exposurehigh = pair.exposurehigh;
            iso = pair.iso;
            isolow = pair.isolow;
            isohigh = pair.isohigh;
        }
        public void normalizeiso100(){
            double mpy = 100.0/isolow;
            iso*=mpy;
        }
        public void denormalizeSystem(){
            double div = 100.0/isolow;
            iso/=div;
        }
        public void normalize(){
            double div = 100.0/isolow;
            if(iso/div > isohigh) iso = isohigh;
            if(iso/div < isolow) iso = isolow;
            if(exposure > exposurehigh) exposure = exposurehigh;
            if(exposure < exposurelow) exposure = exposurelow;
        }
        public boolean normalizeCheck(){
            double div = 100.0/isolow;
            boolean wrongparams = false;
            if(iso/div > isohigh) wrongparams = true;
            if(iso/div < isolow) wrongparams = true;
            if(exposure > exposurehigh) wrongparams = true;
            if(exposure < exposurelow) wrongparams = true;
            return wrongparams;
        }
        public void ExpoCompensateLower(double k){
            iso/=k;
            if(normalizeCheck()){
                iso*=k;
                exposure/=k;
                if(normalizeCheck()){
                    exposure*=k;
                }
            }
        }
        public void ReduceIso(){
            ReduceIso(2.0);
            if(normalizeCheck()){
                ReduceIso(1.0/2);
            }
        }
        public void ReduceIso(double k){
            iso/=k;
            exposure*=k;
        }
        public void ReduceExpo(){
            ReduceExpo(2.0);
            if(normalizeCheck()) ReduceExpo(1.0/2);
        }
        public void ReduceExpo(double k){
            Log.d(TAG,"ExpoReducing iso:"+iso+" expo:"+ ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
            iso*=k;
            exposure/=k;
            Log.d(TAG,"ExpoReducing done iso:"+iso+" expo:"+ ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
        }
        public void FixedExpo(double expo){

            long expol = ExposureIndex.sec2time(expo);
            double k = (double)exposure/expol;
            ReduceExpo(k);
            Log.d(TAG,"ExpoFixating iso:"+iso+" expo:"+ ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
            if(normalizeCheck()) ReduceExpo(1/k);
        }
    }
}
