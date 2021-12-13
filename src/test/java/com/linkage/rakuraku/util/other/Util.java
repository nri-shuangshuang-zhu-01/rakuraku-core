package com.linkage.rakuraku.util.other;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

public class Util {

    public static Object getObject(Object o, Object key)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        Field f = null;
        f = getClassField(o.getClass(), key.toString());
        if (f != null) {
            f.setAccessible(true);
            return f.get(o);
        } else {
            throw new NoSuchFieldException();
        }
    }

    public static void setObject(Object o, Object key, Object val)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
        Field f = null;
        f = getClassField(o.getClass(), key.toString());
        if (f != null) {
            f.setAccessible(true);
            f.set(o, val);
        } else {
            throw new NoSuchFieldException();
        }
    }

    @SuppressWarnings("rawtypes")
    private static Field getClassField(Class cls, String key) {
        Field f = null;

        try {
            f = cls.getDeclaredField(key);
        } catch (SecurityException e) {
        } catch (NoSuchFieldException e) {
        }

        if (f != null) {
            return f;
        } else {
            Class superCls = cls.getSuperclass();
            if (!superCls.getName().equals("java.lang.Object")) {
                f = getClassField(superCls, key);
            } else {
                return null;
            }
        }
        return f;
    }

    public static byte[] readAsByteArray(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        byte[] ret = readAsByteArray(in);
        in.close();
        return ret;
    }

    public static byte[] readAsByteArray(InputStream inStream) throws IOException {
        int size = 1024;
        byte[] ba = new byte[size];
        int readSoFar = 0;
        while (true) {
            int nRead = inStream.read(ba, readSoFar, size - readSoFar);
            if (nRead == -1)
                break;
            readSoFar += nRead;
            if (readSoFar == size) {
                int newSize = size * 2;
                byte[] newBa = new byte[newSize];
                System.arraycopy(ba, 0, newBa, 0, size);
                ba = newBa;
                size = newSize;
            }
        }

        byte[] newBa = new byte[readSoFar];
        System.arraycopy(ba, 0, newBa, 0, readSoFar);
        return newBa;
    }

    public static void main(String args[]) {

    }
}
