package org.k3a.observer;

import java.nio.file.*;
import java.util.function.Consumer;

/**
 * Created by HQ.XPS15
 * on 2018/7/27  0:14
 */
public class ObserverFactory {

    public static Observer<Path, WatchService> createFileObserver() {
        Observer<Path, WatchService> observer = new Observer<>();
        return observer
                .onRegister((p, r) -> {
                    try {
                        Path parent = p.getParent();
                        if (parent == null || !p.toFile().isFile()) {
                            r.reject(p);
                        } else {
                            observer.WATCHED_PATH.put(parent.register(observer.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), parent);
                            observer.FILE_TIMESTAMP.put(p, p.toFile().lastModified());
                        }
                    } catch (Exception e) {
                        r.reject(p);
                    }
                }).onWatch(() -> {
                    try {
                        return FileSystems.getDefault().newWatchService();
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }).onNotify(() -> {
                    do {
                        WatchKey take = null;
                        try {
                            take = observer.watchService.take();
                            Path parent = observer.WATCHED_PATH.get(take);
                            //ignore double update
                            Thread.sleep(50);
                            for (WatchEvent<?> event : take.pollEvents()) {
                                final Path fullPath = parent.resolve((Path) event.context());
                                if (!observer.FILE_TIMESTAMP.keySet().contains(fullPath)) continue;
                                //ignore double update
                                final Long lastModified = observer.FILE_TIMESTAMP.get(fullPath);
                                final long thisModified = fullPath.toFile().lastModified();
                                if (lastModified != null && lastModified < thisModified) {
                                    try {
                                        final WatchEvent.Kind<?> kind = event.kind();
                                        final Consumer<Path> consumer;
                                        if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind))
                                            ((consumer = observer.modifyHandlers.get(fullPath)) == null ? observer.commonModifyHandler : consumer).accept(fullPath);
                                        else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind))
                                            ((consumer = observer.deleteHandlers.get(fullPath)) == null ? observer.commonDeleteHandler : consumer).accept(fullPath);
                                        else if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind))
                                            ((consumer = observer.createHandlers.get(fullPath)) == null ? observer.commonCreateHandler : consumer).accept(fullPath);
                                    } finally {
                                        observer.FILE_TIMESTAMP.put(fullPath, thisModified);
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
                });
    }
}
