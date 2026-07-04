package de.robv.android.xposed;

import android.util.Log;

public class XposedBridge {
    private static final String TAG = "Xposed";

    public static void log(String text) {
        Log.d(TAG, text);
    }

    public static void log(Throwable t) {
        Log.e(TAG, "Error", t);
    }
}