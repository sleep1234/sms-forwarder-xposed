package de.robv.android.xposed;

import java.lang.reflect.Method;

public class XposedHelpers {
    public static Object callMethod(Object obj, String methodName, Object... args) throws Throwable {
        Class<?> clazz = obj.getClass();
        Method method = findMethodExact(clazz, methodName, getParameterTypes(args));
        return method.invoke(obj, args);
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) throws Throwable {
        Method method = findMethodExact(clazz, methodName, getParameterTypes(args));
        return method.invoke(null, args);
    }

    public static Object findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) throws Throwable {
        Class<?> clazz = Class.forName(className, false, classLoader);
        return findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
    }

    public static Object findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        // Stub - returns null
        return null;
    }

    private static Class<?>[] getParameterTypes(Object[] args) {
        // Simple implementation
        return new Class<?>[0];
    }

    private static Method findMethodExact(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, parameterTypes);
    }
}