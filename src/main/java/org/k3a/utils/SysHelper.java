package org.k3a.utils;

/**
 * Created by k3a
 * on 19-1-30  上午9:36
 */
public class SysHelper {

    public static final String osName = System.getProperty("os.name");

    private static boolean isMacOs = osName.equalsIgnoreCase("Mac OS X");

    public static boolean isMacOs() {
        return isMacOs;
    }
}
