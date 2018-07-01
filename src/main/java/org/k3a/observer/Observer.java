package org.k3a.observer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.WatchKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by HQ.XPS15
 * on 2018/6/29  14:17
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public abstract class Observer<O, W extends Closeable> {

    protected final Map<WatchKey, O> WATCHED_PATH = new ConcurrentHashMap<>();

    protected final Map<O, Long> FILE_TIMESTAMP = new ConcurrentHashMap<>();

    protected final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    protected final Map<O, Consumer<O>> modifyHandlers = new ConcurrentHashMap<>();
    protected final Map<O, Consumer<O>> deleteHandlers = new ConcurrentHashMap<>();
    protected final Map<O, Consumer<O>> createHandlers = new ConcurrentHashMap<>();

    protected Consumer<O> commonModifyHandler;
    protected Consumer<O> commonDeleteHandler;
    protected Consumer<O> commonCreateHandler;

    protected W watchService;

    protected Supplier<W> watchServiceSupplier = defaultWatchServiceSupplier();

    protected RejectObserving<O> rejection = defaultRejection();

    protected Runnable notifier = defaultNotifier();

    @SuppressWarnings("unchecked")
    public void watch(O... observables) throws Exception {
        watch(rejection, observables);
    }

    @SuppressWarnings("unchecked")
    public void watch(RejectObserving<O> reject, O... observables) throws Exception {
        if (observables == null || observables.length == 0)
            return;
        watch(reject, Arrays.asList(observables));
    }

    public void watch(Collection<O> paths) throws Exception {
        watch(rejection, paths);
    }

    public void watch(RejectObserving<O> reject, Collection<O> observables) throws Exception {
        if (observables == null || observables.size() == 0)
            return;
        //stop previous round
        if (watchService != null)
            watchService.close();
        //register paths
        watchService = watchServiceSupplier.get();
        for (O o : observables)
            addObservable(o, reject);
        //start this round
        EXECUTOR.execute(notifier);
    }

    protected abstract void addObservable(O observable, RejectObserving<O> reject) throws Exception;

    public void stop() throws IOException {
        if (watchService != null)
            watchService.close();
    }

    public Observer<O, W> onModify(Consumer<O> consumer) {
        this.commonModifyHandler = consumer;
        return this;
    }

    public Observer<O, W> onModify(O path, Consumer<O> consumer) {
        modifyHandlers.put(path, consumer);
        return this;
    }

    public Observer<O, W> onDelete(Consumer<O> consumer) {
        this.commonDeleteHandler = consumer;
        return this;
    }

    public Observer<O, W> onDelete(O path, Consumer<O> consumer) {
        this.deleteHandlers.put(path, consumer);
        return this;
    }

    public Observer<O, W> onCreate(Consumer<O> consumer) {
        this.commonCreateHandler = consumer;
        return this;
    }

    public Observer<O, W> onCreate(O path, Consumer<O> consumer) {
        this.createHandlers.put(path, consumer);
        return this;
    }

    /**
     * use this to change watch service or override {@link Observer#defaultWatchServiceSupplier}
     */
    public Observer<O, W> onWatch(Supplier<W> watchServiceSupplier) {
        this.watchServiceSupplier = watchServiceSupplier;
        return this;
    }

    /**
     * use this to change Rejection or override {@link Observer#defaultRejection()}
     */
    public Observer<O, W> onReject(RejectObserving<O> reject) {
        this.rejection = reject;
        return this;
    }

    /**
     * use this to change notifier or override {@link Observer#defaultNotifier()}
     */
    public Observer<O, W> onNotify(Runnable notifier) {
        this.notifier = notifier;
        return this;
    }

    /**
     * override this to change watch service or use {@link Observer#onWatch}
     */
    protected Supplier<W> defaultWatchServiceSupplier() {
        return null;
    }

    /**
     * override this to change Rejection or use {@link Observer#onWatch}
     */
    protected RejectObserving<O> defaultRejection() {
        //noinspection unchecked
        return RejectObserving.EXCEPTION;
    }

    /**
     * override this to change notifier or use {@link Observer#onNotify(Runnable)}
     */
    protected Runnable defaultNotifier() {
        return null;
    }
}
