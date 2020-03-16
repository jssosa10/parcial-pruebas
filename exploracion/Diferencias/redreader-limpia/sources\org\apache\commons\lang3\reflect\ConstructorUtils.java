package org.apache.commons.lang3.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public class ConstructorUtils {
    public static <T> T invokeConstructor(Class<T> cls, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] args2 = ArrayUtils.nullToEmpty(args);
        return invokeConstructor(cls, args2, ClassUtils.toClass(args2));
    }

    public static <T> T invokeConstructor(Class<T> cls, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] args2 = ArrayUtils.nullToEmpty(args);
        Constructor<T> ctor = getMatchingAccessibleConstructor(cls, ArrayUtils.nullToEmpty(parameterTypes));
        if (ctor != null) {
            if (ctor.isVarArgs()) {
                args2 = MethodUtils.getVarArgs(args2, ctor.getParameterTypes());
            }
            return ctor.newInstance(args2);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("No such accessible constructor on object: ");
        sb.append(cls.getName());
        throw new NoSuchMethodException(sb.toString());
    }

    public static <T> T invokeExactConstructor(Class<T> cls, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] args2 = ArrayUtils.nullToEmpty(args);
        return invokeExactConstructor(cls, args2, ClassUtils.toClass(args2));
    }

    public static <T> T invokeExactConstructor(Class<T> cls, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] args2 = ArrayUtils.nullToEmpty(args);
        Constructor<T> ctor = getAccessibleConstructor(cls, ArrayUtils.nullToEmpty(parameterTypes));
        if (ctor != null) {
            return ctor.newInstance(args2);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("No such accessible constructor on object: ");
        sb.append(cls.getName());
        throw new NoSuchMethodException(sb.toString());
    }

    public static <T> Constructor<T> getAccessibleConstructor(Class<T> cls, Class<?>... parameterTypes) {
        Validate.notNull(cls, "class cannot be null", new Object[0]);
        try {
            return getAccessibleConstructor(cls.getConstructor(parameterTypes));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static <T> Constructor<T> getAccessibleConstructor(Constructor<T> ctor) {
        Validate.notNull(ctor, "constructor cannot be null", new Object[0]);
        if (!MemberUtils.isAccessible(ctor) || !isAccessible(ctor.getDeclaringClass())) {
            return null;
        }
        return ctor;
    }

    public static <T> Constructor<T> getMatchingAccessibleConstructor(Class<T> cls, Class<?>... parameterTypes) {
        Constructor<?>[] arr$;
        Validate.notNull(cls, "class cannot be null", new Object[0]);
        try {
            Constructor<T> ctor = cls.getConstructor(parameterTypes);
            MemberUtils.setAccessibleWorkaround(ctor);
            return ctor;
        } catch (NoSuchMethodException e) {
            Constructor<T> result = null;
            for (Constructor<?> ctor2 : cls.getConstructors()) {
                if (MemberUtils.isMatchingConstructor(ctor2, parameterTypes)) {
                    Constructor<?> ctor3 = getAccessibleConstructor(ctor2);
                    if (ctor3 != null) {
                        MemberUtils.setAccessibleWorkaround(ctor3);
                        if (result == null || MemberUtils.compareConstructorFit(ctor3, result, parameterTypes) < 0) {
                            result = ctor3;
                        }
                    }
                }
            }
            return result;
        }
    }

    private static boolean isAccessible(Class<?> type) {
        for (Class<?> cls = type; cls != null; cls = cls.getEnclosingClass()) {
            if (!Modifier.isPublic(cls.getModifiers())) {
                return false;
            }
        }
        return true;
    }
}
