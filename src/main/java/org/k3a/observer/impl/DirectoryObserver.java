package org.k3a.observer.impl;

import org.k3a.observer.LocalFileSystemObserver;
import org.k3a.observer.RejectObserving;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Created by HQ.XPS15
 * on 2018/7/27  1:24
 * <p>
 * Watch the change of Directory
 */
@SuppressWarnings({"WeakerAccess", "UnusedReturnValue"})
public class DirectoryObserver extends LocalFileSystemObserver {

    protected final int coreSize = Runtime.getRuntime().availableProcessors();

    protected final Set<Path> recursively = new ConcurrentSkipListSet<>();

    protected final List<CompletableFuture<?>> tasks = Collections.synchronizedList(new LinkedList<>());

    protected ExecutorService regNewDir = new ThreadPoolExecutor(coreSize, coreSize, 60L, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<>(Integer.MAX_VALUE),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                private final String namePrefix = "DirectoryObserver-register-";

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }, (r, p) -> Logger.getLogger(this.getClass().getName()).info("runnable: " + r + " is discarded by pool:" + p));

    protected final AtomicInteger eventNum = new AtomicInteger(0);

    protected DirectoryObserver() {
        //if FJP is already init then this will not work
//        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", Integer.toString(coreSize /** 2*/));
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
            reject.reject(path);
        }
    }

    public DirectoryObserver registerRecursively(Path path, RejectObserving<Path> reject) {
        tasks.add(CompletableFuture.runAsync(() -> recursiveRegister(path, reject)));
        return this;
    }

    public DirectoryObserver registerRecursively(Path path) {
        //noinspection unchecked
        registerRecursively(path, defaultRejection());
        return this;
    }

    @Override
    public void start() throws InterruptedException {
        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[]{})).join();
        super.start();
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
                        final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                        final long thisModified = attributes.lastModifiedTime().toMillis();
                        if (lastModified != null && lastModified < thisModified && eventNum.incrementAndGet() == bathSize) {
                            try {
                                commonOnChangeHandler().accept(path, event);
                            } finally {
                                TIMESTAMP.put(path, thisModified);
                                eventNum.set(0);
                                //register new path
                                if (StandardWatchEventKinds.ENTRY_CREATE.equals(event.kind()) && attributes.isDirectory())
                                    if (recursively.contains(path))
                                        regNewDir.execute(() -> registerRecursively(path));
                                    else
                                        regNewDir.execute(() -> register(path));
                            }
                        }
                    }
                } catch (ClosedWatchServiceException | InterruptedException e) {
                    //break this round observing and return from this Thread
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
                WATCHED_PATH.put(path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE), path);
                TIMESTAMP.put(path, attributes.lastModifiedTime().toMillis());
            } catch (IOException e) {
                reject.reject(path);
            }
        };
    }
}
