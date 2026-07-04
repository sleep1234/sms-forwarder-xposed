package de.robv.android.xposed.callbacks;

import android.app.Application;

public class XC_LoadPackage {
    public static class LoadPackageParam {
        public String packageName;
        public Application app;
        public ClassLoader classLoader;
        public PackageInfo appInfo;
        public boolean isFirstApplication;
    }

    public static class PackageInfo {
        public String packageName;
        public String processName;
    }
}