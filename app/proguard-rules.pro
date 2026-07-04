# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Xposed entry points
-keep class com.example.smsforwarder.XposedHook { *; }
-keep class com.example.smsforwarder.SmsForwarderModule { *; }

# Keep the module class
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep class * implements de.robv.android.xposed.IXposedHookZygoteInit { *; }
-keep class * implements de.robv.android.xposed.IXposedHookInitPackageResources { *; }
-keep class * implements de.robv.android.xposed.IXposedHookPostInit { *; }