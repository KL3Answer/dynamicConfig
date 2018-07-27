package org.k3a.observer;

import java.nio.file.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by HQ.XPS15
 * on 2018/6/22  12:03
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FileObserver extends Observer<Path, WatchService> {

    private FileObserver() {
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
                    Thread.sleep(50);
                    for (WatchEvent<?> event : take.pollEvents()) {
                        final Path fullPath = parent.resolve((Path) event.context());
                        //ignore non-registered
                        if (!FILE_TIMESTAMP.keySet().contains(fullPath)) continue;
                        //ignore double update
                        final Long lastModified = FILE_TIMESTAMP.get(fullPath);
                        final long thisModified = fullPath.toFile().lastModified();
                        if (lastModified != null && lastModified < thisModified) {
                            try {
                                final WatchEvent.Kind<?> kind = event.kind();
                                final Consumer<Path> consumer;
                                if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind))
                                    ((consumer = modifyHandlers.get(fullPath)) == null ? commonModifyHandler : consumer).accept(fullPath);
                                else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind))
                                    ((consumer = deleteHandlers.get(fullPath)) == null ? commonDeleteHandler : consumer).accept(fullPath);
                                else if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind))
                                    ((consumer = createHandlers.get(fullPath)) == null ? commonCreateHandler : consumer).accept(fullPath);
                            } finally {
                                FILE_TIMESTAMP.put(fullPath, thisModified);
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
            try {
                Path parent = path.getParent();
                if (parent == null || !path.toFile().isFile()) {
                    reject.reject(path);
                } else {
                    WATCHED_PATH.put(parent.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), parent);
                    FILE_TIMESTAMP.put(path, path.toFile().lastModified());
                }
            } catch (Exception e) {
                reject.reject(path);
            }
        };
    }

}