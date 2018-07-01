package org.k3a.dynamicConfig;

import org.k3a.observer.FileObserver;
import org.k3a.observer.Observer;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * Created by HQ.XPS15
 * on 2018/6/26  20:12
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DynamicFile<U, K, V> extends Dynamically<Path, U, K, V> {
    @Override
    protected Observer<Path, ? extends Closeable> defaultObserver() {
        return FileObserver.get();
    }
}