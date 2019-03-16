package org.k3a.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by  k3a
 * on 2018/6/29  14:17
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Observer<O, W extends Closeable> {

    protected final AtomicBoolean running = new AtomicBoolean(false);

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected volatile long minInterval = 50L;

    protected volatile int bathSize = 1;

    protected final ConcurrentMap<O, Long[]> TIMESTAMP = new ConcurrentHashMap<>();

    protected final Supplier<ExecutorService> notifierExecutorSupplier = () ->
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(Integer.MAX_VALUE),
                    new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);
                        private final String namePrefix = "Observer-notifier-";

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                            t.setDaemon(false);
                            return t;
                        }
                    },
                    (r, p) -> LOGGER.info("runnable: " + r + " is discarded by pool:" + p));

    protected volatile ExecutorService notifierExecutor = notifierExecutorSupplier.get();

    protected final int coreSize = Runtime.getRuntime().availableProcessors();

    protected final List<CompletableFuture<?>> tasks = Collections.synchronizedList(new LinkedList<>());

    protected final Supplier<ExecutorService> regNewDirSupplier = () ->
            new ThreadPoolExecutor(coreSize, coreSize, 60L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(Integer.MAX_VALUE),
                    new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);
                        private final String namePrefix = "DirectoryObserver-register-";

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                            t.setDaemon(true);
                            return t;
                        }
                    },
                    (r, p) -> LOGGER.info("runnable: " + r + " is discarded by pool:" + p));

    protected volatile ExecutorService regNewDirExecutor = regNewDirSupplier.get();

    protected final Supplier<ExecutorService> bootExecutorSupplier = () ->
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(Byte.MAX_VALUE),
                    new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);
                        private final String namePrefix = "StartAsync-";

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                            t.setDaemon(true);
                            return t;
                        }
                    },
                    (r, p) -> LOGGER.info("runnable: " + r + " is discarded by pool:" + p));

    protected volatile ExecutorService bootExecutor = bootExecutorSupplier.get();

    protected final ConcurrentMap<O, Consumer<O>> modifyHandlers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<O, Consumer<O>> deleteHandlers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<O, Consumer<O>> createHandlers = new ConcurrentHashMap<>();

    protected volatile Consumer<O> commonModifyHandler = o -> LOGGER.info("NOTICE:this message is printed due to no modify handler was set, event source:" + o);

    protected volatile Consumer<O> commonDeleteHandler = o -> LOGGER.info("NOTICE:this message is printed due to no delete handler was set, event source:" + o);

    protected volatile Consumer<O> commonCreateHandler = o -> LOGGER.info("NOTICE:this message is printed due to no create handler was set, event source:" + o);

    protected volatile RejectObserving<O> rejection = defaultRejection();

    protected volatile W watchService = defaultWatchServiceSupplier().get();

    protected volatile Supplier<W> watchServiceSupplier = null;

    protected volatile Runnable notifier = null;

    protected volatile BiConsumer<O, RejectObserving<O>> register = null;

    protected volatile Consumer<O> unRegister = null;

    /**
     * mill second
     */
    public Observer<O, W> setMinInterval(long ms) {
        this.minInterval = ms;
        return this;
    }

    /**
     * batch notify
     */
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
        return register(reject, Arrays.asList(observables));
    }

    public Observer<O, W> register(Collection<O> paths) {
        return register(rejection, paths);
    }

    public Observer<O, W> register(RejectObserving<O> reject, Collection<O> observables) {
        for (O o : observables)
            register(o, reject);
        return this;
    }

    public Observer<O, W> register(O observable, RejectObserving<O> reject) {
        tasks.add(CompletableFuture.runAsync(() -> (register == null ? register = defaultRegistry() : register).accept(observable, reject)));
        return this;
    }

    public Observer<O, W> unRegister(O observable) {
        (unRegister == null ? unRegister = defaultCancel() : unRegister).accept(observable);
        return this;
    }

    public void start() throws InterruptedException {
        if (running.compareAndSet(false, true)) {
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture[]{})).join();
            tasks.clear();
            //start this round
            notifierExecutor.execute(notifier == null ? notifier = defaultNotifier() : notifier);
        }
    }

    public void startAsync() {
        bootExecutor.execute(() -> {
            try {
                start();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void stop() throws IOException {
        if (running.compareAndSet(true, false)) {
            if (watchService != null) {
                watchService.close();
            }
            notifierExecutor.shutdown();
            regNewDirExecutor.shutdown();
            bootExecutor.shutdown();
            TIMESTAMP.clear();
        }
    }

    public void reset() throws IOException {
        stop();
        notifierExecutor = notifierExecutorSupplier.get();
        regNewDirExecutor = regNewDirSupplier.get();
        bootExecutor = bootExecutorSupplier.get();
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
     * use this to change link or override {@link Observer#defaultCancel()} ()}
     */
    public Observer<O, W> onUnRegister(Consumer<O> unRegister) {
        this.unRegister = unRegister;
        return this;
    }

    /**
     * handler event after customized action execute
     */
    protected void postEventHandler(WatchKey take, WatchEvent<?> event, Path path, Path contextPath) {
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

    /**
     * override this to change link or use {@link Observer#onUnRegister(Consumer)}
     */
    protected Consumer<O> defaultCancel() {
        throw new IllegalStateException("UnRegister is required but has not been set up yet!");
    }

}
