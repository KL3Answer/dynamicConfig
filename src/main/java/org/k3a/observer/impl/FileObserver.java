package org.k3a.observer.impl;

import org.k3a.observer.LocalFileSystemObserver;
import org.k3a.observer.Observer;
import org.k3a.observer.RejectObserving;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Created by HQ.XPS15
 * on 2018/6/22  12:03
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FileObserver extends LocalFileSystemObserver {

    protected FileObserver() {
    }

    public FileObserver(Supplier<WatchService> s) {
        this.watchServiceSupplier = s;
    }

    public static Observer<Path, WatchService> get() {
        return new FileObserver();
    }

    @Override
    public Supplier<WatchService> defaultWatchServiceSupplier() {
        return () -> {
            try {
                return FileSystems.getDefault().newWatchService();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };
    }

    /**
     * ignore System which is defined as the name of system properties
     */
    @Override
    protected RejectObserving<Path> defaultRejection() {
        //noinspection unchecked
        return p -> {
            if (!"System".equals(p.toString())) {
                //noinspection unchecked
                RejectObserving.EXCEPTION.reject(p);
            }
        };
    }

    @Override
    protected Runnable defaultNotifier() {
        return () -> {
            do {
                WatchKey take = null;
                try {
                    take = watchService.take();
                    final Path parent = WATCHED_PATH.get(take);
                    //ignore double update
                    Thread.sleep(minInterval);
                    for (WatchEvent<?> event : take.pollEvents()) {
                        final Path fullPath = parent.resolve((Path) event.context());
                        //ignore non-registered
                        if (!TIMESTAMP.keySet().contains(fullPath)) continue;
                        //ignore double update
                        final Long lastModified = TIMESTAMP.get(fullPath);
                        final long thisModified = Files.getLastModifiedTime(fullPath).toMillis();
                        if (lastModified < thisModified) {
                            try {
                                commonOnChangeHandler().accept(fullPath, event);
                            } finally {
                                TIMESTAMP.put(fullPath, thisModified);
                            }
                        }
                    }
                } catch (ClosedWatchServiceException | InterruptedException e) {
                    //break observing and return from this Thread
                    break;
                } catch (Exception ignore) {
                } finally {
                    if (take != null)
                        take.reset();
                }
            } while (true);
        };
    }

    @Override
    public BiConsumer<Path, RejectObserving<Path>> defaultRegistry() {
        return (path, reject) -> {
            try {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                if (!attributes.isRegularFile()) {
                    reject.reject(path);
                    return;
                }

                final Path parent = path.getParent();
                WATCHED_PATH.put(parent.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), parent);
                TIMESTAMP.put(path, attributes.lastModifiedTime().toMillis());
            } catch (Exception e) {
                reject.reject(path);
            }
        };
    }

}