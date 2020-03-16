package org.apache.commons.lang3.event;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.reflect.MethodUtils;

public class EventUtils {

    private static class EventBindingInvocationHandler implements InvocationHandler {
        private final Set<String> eventTypes;
        private final String methodName;
        private final Object target;

        EventBindingInvocationHandler(Object target2, String methodName2, String[] eventTypes2) {
            this.target = target2;
            this.methodName = methodName2;
            this.eventTypes = new HashSet(Arrays.asList(eventTypes2));
        }

        public Object invoke(Object proxy, Method method, Object[] parameters) throws Throwable {
            if (!this.eventTypes.isEmpty() && !this.eventTypes.contains(method.getName())) {
                return null;
            }
            if (hasMatchingParametersMethod(method)) {
                return MethodUtils.invokeMethod(this.target, this.methodName, parameters);
            }
            return MethodUtils.invokeMethod(this.target, this.methodName);
        }

        private boolean hasMatchingParametersMethod(Method method) {
            return MethodUtils.getAccessibleMethod(this.target.getClass(), this.methodName, method.getParameterTypes()) != null;
        }
    }

    public static <L> void addEventListener(Object eventSource, Class<L> listenerType, L listener) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("add");
            sb.append(listenerType.getSimpleName());
            MethodUtils.invokeMethod(eventSource, sb.toString(), listener);
        } catch (NoSuchMethodException e) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Class ");
            sb2.append(eventSource.getClass().getName());
            sb2.append(" does not have a public add");
            sb2.append(listenerType.getSimpleName());
            sb2.append(" method which takes a parameter of type ");
            sb2.append(listenerType.getName());
            sb2.append(".");
            throw new IllegalArgumentException(sb2.toString());
        } catch (IllegalAccessException e2) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Class ");
            sb3.append(eventSource.getClass().getName());
            sb3.append(" does not have an accessible add");
            sb3.append(listenerType.getSimpleName());
            sb3.append(" method which takes a parameter of type ");
            sb3.append(listenerType.getName());
            sb3.append(".");
            throw new IllegalArgumentException(sb3.toString());
        } catch (InvocationTargetException e3) {
            throw new RuntimeException("Unable to add listener.", e3.getCause());
        }
    }

    public static <L> void bindEventsToMethod(Object target, String methodName, Object eventSource, Class<L> listenerType, String... eventTypes) {
        addEventListener(eventSource, listenerType, listenerType.cast(Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[]{listenerType}, new EventBindingInvocationHandler(target, methodName, eventTypes))));
    }
}
