package org.k3a.observer;

import java.nio.file.*;
import java.util.function.Consumer;

/**
 * Created by  k3a
 * on 2018/7/27  0:14
 */
public class ObserverFactory {

    @SuppressWarnings("Duplicates")
    protected static int getEventOrder(WatchEvent.Kind<?> kind) {
        switch (kind.name()) {
            case "ENTRY_MODIFY":
                return 0;
            case "ENTRY_DELETE":
                return 1;
            case "ENTRY_CREATE":
                return 2;
            default:
                return 3;
        }
    }

    public static Observer<Path, WatchService> createFileObserver() {
        Observer<Path, WatchService> observer = new Observer<>();
        return observer
                .onRegister((p, r) -> {
                    try {
                        Path parent = p.getParent();
                        if (parent == null || !p.toFile().isFile()) {
                            r.reject(p);
                        } else {
                            parent.register(observer.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                            long lastModified = p.toFile().lastModified();
                            observer.TIMESTAMP.put(p, new Long[]{lastModified, lastModified, lastModified});
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
                            Path parent = (Path) take.watchable();
                            //ignore double update
                            Thread.sleep(50);
                            for (WatchEvent<?> event : take.pollEvents()) {
                                final Path fullPath = parent.resolve((Path) event.context());
                                if (!observer.TIMESTAMP.keySet().contains(fullPath)) continue;
                                //ignore double update
                                final Long lastModified = observer.TIMESTAMP.get(fullPath)[getEventOrder(event.kind())];
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
                                        observer.TIMESTAMP.get(fullPath)[getEventOrder(event.kind())] = thisModified;
                                    }
                                }
                            }
                        } catch (ClosedWatchServiceException e) {
                            //break this round observing and return from this Thread because this key is invalid now
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
