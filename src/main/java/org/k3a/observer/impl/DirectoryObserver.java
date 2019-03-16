package org.k3a.observer.impl;

import org.k3a.observer.LocalFileSystemObserver;
import org.k3a.observer.RejectObserving;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by  k3a
 * on 2018/7/27  1:24
 * <p>
 * Watch the change of Directory
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class DirectoryObserver extends LocalFileSystemObserver {

    protected final Set<Path> recursively = new ConcurrentSkipListSet<>();

    protected final AtomicInteger eventNum = new AtomicInteger(0);

    protected DirectoryObserver() {
    }

    public static DirectoryObserver get() {
        return new DirectoryObserver();
    }

    protected void recursiveRegister(Path path, RejectObserving<Path> reject) {
        try {
            if (!Files.readAttributes(path, BasicFileAttributes.class).isDirectory())
                return;

            CompletableFuture<Void> f = CompletableFuture.runAsync(() -> register(path, reject));
            Files.list(path).parallel().forEach(e -> recursiveRegister(e, reject));
            f.join();
            recursively.add(path);
        } catch (Exception e) {
            LOGGER.warn("recursiveRegister error", e);
            reject.reject(path);
        }
    }

    public DirectoryObserver registerRecursively(Path path, RejectObserving<Path> reject) {
        tasks.add(CompletableFuture.runAsync(() -> recursiveRegister(path, reject)));
        return this;
    }

    public DirectoryObserver registerRecursively(Path path) {
        registerRecursively(path, defaultRejection());
        return this;
    }

    @Override
    public void start() throws InterruptedException {
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[]{})).join();
        super.start();
        tasks.clear();
    }

    @Override
    public void startAsync() {
        regNewDirExecutor.execute(() -> {
            try {
                start();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
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
            WatchKey take = null;
            Path path;
            do {
                try {
                    take = watchService.take();
                    path = (Path) take.watchable();

                    if (!take.isValid() || !TIMESTAMP.containsKey(path)) {
                        take.cancel();
                        unRegister(path);
                        LOGGER.warn("cancel invalid watchKey {}", take);
                        continue;
                    }

                    //ignore double update
                    Thread.sleep(minInterval);

                    //handle events
                    Long lastModified;
                    for (WatchEvent<?> event : take.pollEvents()) {
                        final Long[] lastModifiedArr = TIMESTAMP.get(path);
                        lastModified = lastModifiedArr[getEventOrder(event.kind())];
                        final long thisModified = Files.readAttributes(path, BasicFileAttributes.class).lastModifiedTime().toMillis();
                        //ignore double update
                        if (lastModified != null && lastModified < thisModified && eventNum.incrementAndGet() == bathSize) {
                            try {
                                commonOnChangeHandler().accept(path, event);
                            } finally {
                                lastModifiedArr[getEventOrder(event.kind())] = thisModified;
                                eventNum.set(0);
                                postEventHandler(take, event, path, path.resolve((Path) event.context()));
                            }
                        }
                    }
                } catch (ClosedWatchServiceException | InterruptedException e) {
                    //break this round observing and return from this Thread
                    break;
                } catch (NoSuchFileException e) {
                    //usually happens when watched path is deleted
                    LOGGER.warn("registered file not found,maybe deleted?",e);
                    take.cancel();
                    unRegister((Path) take.watchable());
                } catch (Exception e) {
                    LOGGER.warn("notify error", e);
                } finally {
                    if (take != null)
                        take.reset();
                }
            } while (true);
        };
    }

    /**
     * register new path or cancel deleted key
     */
    @Override
    protected void postEventHandler(WatchKey take, WatchEvent<?> event, Path path, Path contextPath) {
        if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind()) && contextPath.toFile().isDirectory()) {
            if (recursively.contains(path))
                regNewDirExecutor.execute(() -> registerRecursively(contextPath));
            else
                regNewDirExecutor.execute(() -> register(contextPath));
        } else if (StandardWatchEventKinds.ENTRY_DELETE.equals(event.kind())) {
            take.cancel();
            unRegister(contextPath);
        }
    }

    @Override
    protected RejectObserving<Path> defaultRejection() {
        //noinspection unchecked
        return RejectObserving.SILENTLY;
    }

    @Override
    public BiConsumer<Path, RejectObserving<Path>> defaultRegistry() {
        return (path, reject) -> {
            try {
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                if (!attributes.isDirectory()) {
                    reject.reject(path);
                    return;
                }
                path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE);
                long lastModifiedTime = attributes.lastModifiedTime().toMillis();
                TIMESTAMP.put(path, new Long[]{lastModifiedTime, lastModifiedTime, lastModifiedTime});
            } catch (IOException e) {
                LOGGER.warn("register error",e);
                reject.reject(path);
            }
        };
    }

    @Override
    public Consumer<Path> defaultCancel() {
        return path -> {
            recursively.remove(path);
            TIMESTAMP.remove(path);
        };
    }
}
