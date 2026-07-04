package de.robv.android.xposed;

public class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static class MethodHookParam {
        public Object method;
        public Object[] args;
        public Object result;
        public Throwable throwable;
    }
}