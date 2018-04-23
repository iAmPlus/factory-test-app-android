package com.iamplus.earin.util;

import android.util.Log;

public class SeekBarInputWrapper {

    private int mCurrentValue;
    private int mMaxValue;
    private int mMinValue;
    private int mOffsetValue;

    private static SeekBarInputWrapper sFirstInstance;
    private static SeekBarInputWrapper sSecondInstance;
    private static OnFinishEditingListener sOnFinishEditingListener;
    private static OnModeChangeListener sOnModeChangeListener;
    private static OnExitListener sOnExitListener;

    private SeekBarInputWrapper(int currentValue, int minValue, int maxValue, int offset) {
        this.mCurrentValue = currentValue;
        this.mMinValue = minValue;
        this.mMaxValue = maxValue;
        this.mOffsetValue = offset;
    }

    public static void createFirst(int current, int min, int max, int offset) {
        sFirstInstance = new SeekBarInputWrapper(current, min, max, offset);
        Log.w("SeekBarInputWrapper", "Create first instance!");
    }

    public static void createSecond(int current, int min, int max, int offset) {
        sSecondInstance = new SeekBarInputWrapper(current, min, max, offset);
    }

    public static void setOnModeChangeListener(OnModeChangeListener onModeChangeListener) {
        SeekBarInputWrapper.sOnModeChangeListener = onModeChangeListener;
    }

    public static void setOnFinishEditingListener(OnFinishEditingListener onFinishEditingListener) {
        SeekBarInputWrapper.sOnFinishEditingListener = onFinishEditingListener;
    }

    public static void setOnExitListener(OnExitListener onExitListener) {
        SeekBarInputWrapper.sOnExitListener = onExitListener;
    }

    public static SeekBarInputWrapper getFirstInstance() {
        Log.w("SeekBarInputWrapper", "get first instance!");
        return sFirstInstance;
    }

    public static SeekBarInputWrapper getSecondInstance() {
        return sSecondInstance;
    }

    public int getCurrentDisplayValue() {
        return mCurrentValue + mOffsetValue;
    }

    public void setCurrentDisplayValue(int currentValue) {
        this.mCurrentValue = currentValue - mOffsetValue;
    }

    public int getMinValue() {
        return mMinValue;
    }

    public int getMinDisplayValue() {
        return mMinValue + mOffsetValue;
    }

    public int getMaxDisplayValue() {
        return mMaxValue + mOffsetValue;
    }

    public static void save() {
        int firstValue = sFirstInstance != null ? sFirstInstance.mCurrentValue : 0;
        int secondValue = sSecondInstance != null ? sSecondInstance.mCurrentValue : 0;
        sOnFinishEditingListener.onFinish(firstValue, secondValue);
    }


    public static void changeMode(int mode) {
        if(sOnModeChangeListener != null) {
            sOnModeChangeListener.onChange(mode);
        }
    }
    public static void exit() {
        sOnExitListener.onExit();
    }

    public interface OnFinishEditingListener {
        void onFinish(int firstValue, int secondValue);
    }

    public interface OnModeChangeListener {
        void onChange(int mode);
    }
    public interface OnExitListener {
        void onExit();
    }

}
