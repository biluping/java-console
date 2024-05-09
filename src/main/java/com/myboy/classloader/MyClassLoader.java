package com.myboy.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Frank
 * @date 2024/4/4 17:09
 */
public class MyClassLoader extends URLClassLoader {

    public static String namePrefix = "WClassLoader";

    public MyClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public String toString() {
        return namePrefix + super.toString();
    }
}