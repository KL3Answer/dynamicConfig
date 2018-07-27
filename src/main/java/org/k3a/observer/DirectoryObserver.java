package org.k3a.observer;

import java.io.File;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Created by HQ.XPS15
 * on 2018/7/27  1:24
 * <p>
 * Watch the change of Directory
 */
public class DirectoryObserver extends LocalFileSystemObserver {

    private final AtomicInteger eventNum = new AtomicInteger(0);

    private DirectoryObserver() {
    }

    public static Observer<Path, WatchService> get() {
        return new DirectoryObserver();
    }

    @Override
    public Supplier<WatchService> defaultWatchServiceSupplier() {
        return () -> {
            try {
                return FileSystems.getDefault().newWatchService();
            } catch (Exception t) {
                throw new RuntimeException(t);
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
                    final Path path = WATCHED_PATH.get(take);
                    //ignore double update
                    Thread.sleep(minInterval);
                    for (WatchEvent<?> event : take.pollEvents()) {
                        //no need to get event full path
//                        final Path fullPath = path.resolve((Path) event.context());
                        //ignore non-registered
                        if (!TIMESTAMP.keySet().contains(path)) continue;
                        //ignore double update
                        final Long lastModified = TIMESTAMP.get(path);
                        final long thisModified = path.toFile().lastModified();
                        if (lastModified != null && lastModified < thisModified && eventNum.incrementAndGet() == bathSize) {
                            try {
                                commonOnChangeHandler().accept(path, event);
                            } finally {
                                TIMESTAMP.put(path, thisModified);
                                eventNum.set(0);
                                //register new path
                                if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind()) && path.toFile().isDirectory())
                                    register(path);
                            }
                        }
                    }
                } catch (ClosedWatchServiceException e) {
                    //break this round observing and return from this Thread
                    break;
                } catch (InterruptedException ignore) {
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
            if (path == null || !path.toFile().isDirectory()) {
                reject.reject(path);
                return;
            }

            try {
                WATCHED_PATH.put(path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), path);
                TIMESTAMP.put(path, path.toFile().lastModified());
                //sub dirs
                File[] subs = path.toFile().listFiles();
                if (subs == null) {
                    return;
                }
                for (File file : subs) {
                    if (file.isDirectory())
                        register(file.toPath(), reject);
                }
            } catch (Exception e) {
                reject.reject(path);
            }
        };
    }
}
