package org.k3a.observer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.WatchKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by HQ.XPS15
 * on 2018/6/29  14:17
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Observer<O, W extends Closeable> {

    protected volatile long minInterval = 50L;

    protected volatile int bathSize = 1;

    protected final ConcurrentMap<WatchKey, O> WATCHED_PATH = new ConcurrentHashMap<>();

    protected final ConcurrentMap<O, Long> TIMESTAMP = new ConcurrentHashMap<>();

    protected final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    protected final ConcurrentMap<O, Consumer<O>> modifyHandlers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<O, Consumer<O>> deleteHandlers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<O, Consumer<O>> createHandlers = new ConcurrentHashMap<>();

    protected volatile Consumer<O> commonModifyHandler = o -> System.out.printf("NOTICE:this message is printed due to no modify handler was set, event source:%s\n", o.toString());

    protected volatile Consumer<O> commonDeleteHandler = o -> System.out.printf("NOTICE:this message is printed due to no delete handler was set, event source:%s\n", o.toString());

    protected volatile Consumer<O> commonCreateHandler = o -> System.out.printf("NOTICE:this message is printed due to no create handler was set, event source:%s\n", o.toString());

    protected volatile RejectObserving<O> rejection = defaultRejection();

    protected volatile W watchService = defaultWatchServiceSupplier().get();

    protected volatile Supplier<W> watchServiceSupplier = null;

    protected volatile Runnable notifier = null;

    protected volatile BiConsumer<O, RejectObserving<O>> register = null;

    //mill second
    public Observer<O, W> setMinInterval(long ms) {
        this.minInterval = ms;
        return this;
    }

    public Observer<O, W> setBatchSize(int batch) {
        this.bathSize = batch;
        return this;
    }

    @SuppressWarnings("unchecked")
    public Observer<O, W> register(O... observables) {
        return register(rejection, observables);
    }

    @SuppressWarnings("unchecked")
    public Observer<O, W> register(RejectObserving<O> reject, O... observables) {
        if (observables == null || observables.length == 0)
            return this;
        return register(reject, Arrays.asList(observables));
    }

    public Observer<O, W> register(Collection<O> paths) {
        return register(rejection, paths);
    }

    public Observer<O, W> register(RejectObserving<O> reject, Collection<O> observables) {
        if (observables == null || observables.size() == 0)
            return this;
        for (O o : observables)
            register(o, reject);
        return this;
    }

    public Observer<O, W> register(O observable, RejectObserving<O> reject) {
        (register == null ? register = defaultRegistry() : register).accept(observable, reject);
        return this;
    }

    public void start() {
        //start this round
        EXECUTOR.execute(notifier == null ? notifier = defaultNotifier() : notifier);
    }

    public void stop() throws IOException {
        if (watchService != null)
            watchService.close();
    }

    public void suspend() throws IOException {
        stop();
        //so that it can be recovered
        watchService = watchServiceSupplier == null ? (watchServiceSupplier = defaultWatchServiceSupplier()).get() : watchServiceSupplier.get();
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
     * use this to change link service or override {@link Observer#defaultWatchServiceSupplier}
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
     * use this to change link or override {@link Observer#defaultRegistry()} ()}
     */
    public Observer<O, W> onRegister(BiConsumer<O, RejectObserving<O>> register) {
        this.register = register;
        return this;
    }

    /**
     * override this to change Rejection or use {@link Observer#onWatch}
     */
    protected RejectObserving<O> defaultRejection() {
        //noinspection unchecked
        return RejectObserving.EXCEPTION;
    }

    /**
     * override this to change link service or use {@link Observer#onWatch}
     */
    protected Supplier<W> defaultWatchServiceSupplier() {
        throw new IllegalStateException("watchService is required but has not been set up yet!");
    }

    /**
     * override this to change notifier or use {@link Observer#onNotify(Runnable)}
     */
    protected Runnable defaultNotifier() {
        throw new IllegalStateException("notifier is required but has not been set up yet!");
    }

    /**
     * override this to change link or use {@link Observer#onRegister(BiConsumer)}
     */
    protected BiConsumer<O, RejectObserving<O>> defaultRegistry() {
        throw new IllegalStateException("Register is required but has not been set up yet!");
    }
}
