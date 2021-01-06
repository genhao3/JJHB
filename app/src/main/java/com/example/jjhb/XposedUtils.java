package com.example.jjhb;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static de.robv.android.xposed.XposedHelpers.getParameterTypes;

public class XposedUtils {

    public static Object findResultByMethodNameAndReturnTypeAndParams(Object targetObject, String methodName, String returnType, Object... params) throws InvocationTargetException, IllegalAccessException {
        return findMethodByNameAndReturnType(targetObject.getClass(), methodName, returnType, getParameterTypes(params)).invoke(targetObject, params);
    }


    public static Method findMethodByNameAndReturnType(Class<?> targetObject, String methodName, String returnType, Class<?>... params) {
        for (Method method : targetObject.getDeclaredMethods()) {
            if (method.getReturnType().getName().equals(returnType) && method.getName().equals(methodName)) {
                Class[] parameterTypes = method.getParameterTypes();
                if (params.length != parameterTypes.length) {
                    continue;
                }
                for (int i = 0; i < params.length; i++) {
                    if (params[i] != parameterTypes[i]) {
                        break;
                    }
                }
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodError();
    }

    public static Field findFieldByClassAndTypeAndName(Class<?> targetObject, Class<?> fieldType, String fieldName) {
        Class clazz = targetObject;
        do {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == fieldType && field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    return field;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        throw new NoSuchFieldError("Field of type " + fieldType.getName() + " in class " + targetObject.getName());
    }

}
