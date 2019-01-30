package org.k3a.observer.impl;

import org.k3a.observer.LocalFileSystemObserver;
import org.k3a.observer.Observer;
import org.k3a.observer.RejectObserving;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Created by  k3a
 * on 2018/6/22  12:03
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FileObserver extends LocalFileSystemObserver {

    private final List<Path> abandon = Collections.synchronizedList(new LinkedList<>());

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
            WatchKey take = null;
            Path parent;
            do {
                try {
                    take = watchService.take();
                    parent = (Path) take.watchable();
                    if (!take.isValid()) {
                        unRegister(parent);
                        LOGGER.log(Level.WARNING, "cancel invalid watchKey :" + take);
                        continue;
                    }

                    //ignore double update
                    Thread.sleep(minInterval);
                    for (WatchEvent<?> event : take.pollEvents()) {
                        final Path fullPath = parent.resolve((Path) event.context());
                        //unRegister
                        if (!abandon.isEmpty() && (abandon.contains(fullPath) || abandon.contains(parent))) {
                            take.cancel();
                            continue;
                        }
                        //ignore non-registered
                        if (!TIMESTAMP.keySet().contains(fullPath)) continue;
                        //ignore double update
                        final Long lastModified = TIMESTAMP.get(fullPath)[getEventOrder(event.kind())];
                        final long thisModified = Files.getLastModifiedTime(fullPath).toMillis();
                        if (lastModified < thisModified) {
                            try {
                                commonOnChangeHandler().accept(fullPath, event);
                            } finally {
                                TIMESTAMP.get(fullPath)[getEventOrder(event.kind())] = thisModified;
                            }
                        }
                    }
                } catch (ClosedWatchServiceException | InterruptedException e) {
                    //break observing and return from this Thread
                    break;
                } catch (NoSuchFileException e) {
                    //usually happens when watched path is deleted
                    LOGGER.log(Level.WARNING, "\tat " + e.getStackTrace()[0]);
                    take.cancel();
                    unRegister((Path) take.watchable());
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "\tat " + e.getStackTrace()[0]);
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

                parent.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                long lastModifiedTime = attributes.lastModifiedTime().toMillis();
                TIMESTAMP.put(path, new Long[]{lastModifiedTime, lastModifiedTime, lastModifiedTime});
            } catch (Exception e) {
                reject.reject(path);
            }
        };
    }

    @Override
    public Consumer<Path> defaultCancel() {
        return path -> {
            try {
                BasicFileAttributes fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
                if (fileAttributes.isDirectory()) {
                    TIMESTAMP.keySet().removeIf(k -> k.getParent().equals(path));
                    abandon.add(path);
                } else if (fileAttributes.isRegularFile()) {
                    TIMESTAMP.remove(path);
                    //unRegister the directory only if no files under this directory is registered
                    Path parent = path.getParent();
                    if (TIMESTAMP.keySet().stream().noneMatch(p -> p.getParent().equals(parent))) {
                        abandon.add(path);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "\tat " + e.getStackTrace()[0]);
            }
        };
    }

}