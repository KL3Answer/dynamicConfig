package org.k3a.dynamicConfig;

import org.k3a.utils.TriConsumer;
import org.k3a.observer.Observer;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by HQ.XPS15
 * on 2018/6/29  13:18
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class Dynamically<S, B, K, V> {

    protected final Map<S, B> VALUES = new ConcurrentHashMap<>();

    protected volatile Consumer<Throwable> commonErrorHandler = defaultErrorHandler();

    protected volatile Function<S, B> convertor = null;

    protected volatile BiFunction<K, B, V> searcher = null;

    protected volatile Observer<S, ? extends Closeable> observer = null;

    protected volatile TriConsumer<K, V, S> appender = null;

    protected volatile BiConsumer<K, S> eraser = null;

    protected volatile AtomicBoolean isDynamic = new AtomicBoolean(false);

    /**
     * link sources
     */
    public Dynamically<S, B, K, V> link(S s, Consumer<Throwable> errorHandler) {
        try {
            B b = (convertor == null ? convertor = defaultConvertor() : convertor).apply(s);
            VALUES.put(s, b);
        } catch (Throwable t) {
            VALUES.remove(s);
            errorHandler.accept(t);
        }
        return this;
    }

    /**
     * link sources
     */
    @SuppressWarnings("unchecked")
    public final Dynamically<S, B, K, V> link(S... sources) {
        return link(commonErrorHandler, sources);
    }

    /**
     * link sources
     */
    public final Dynamically<S, B, K, V> link(Collection<S> sources) {
        return link(commonErrorHandler, sources);
    }

    /**
     * link sources
     */
    @SuppressWarnings("unchecked")
    public Dynamically<S, B, K, V> link(Consumer<Throwable> errorHandler, S... sources) {
        link(errorHandler, Arrays.asList(sources));
        return this;
    }

    /**
     * link sources
     */
    public Dynamically<S, B, K, V> link(Consumer<Throwable> errorHandler, Collection<S> sources) {
        for (S file : sources)
            link(file, errorHandler);
        return this;
    }

    /**
     * start isDynamic registered sources
     */
    public Dynamically<S, B, K, V> activate(Observer<S, ? extends Closeable> observer) {
        try {
            // stop previous round
            if (!isDynamic.compareAndSet(false, true))
                inactivate();

            //start a new round
            //noinspection unchecked
            (this.observer = observer).register(VALUES.keySet()).onModify(this::link).start();
            isDynamic.set(true);
        } catch (Throwable t) {
            commonErrorHandler.accept(t);
        }
        return this;
    }

    /**
     * start isDynamic registered paths
     */
    public Dynamically<S, B, K, V> activate() {
        return activate(observer == null ? observer = defaultObserver() : observer);
    }

    /**
     * stop isDynamic paths
     */
    public Dynamically<S, B, K, V> inactivate(Consumer<Throwable> errorHandler) {
        try {
            if (observer != null) {
                observer.reset();
                isDynamic.set(false);
            }
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
        return this;
    }

    /**
     * stop isDynamic paths
     */
    public Dynamically<S, B, K, V> inactivate() {
        return inactivate(commonErrorHandler);
    }

    /**
     * get value from watched files
     */
    public V getValue(K key, Consumer<Throwable> errorHandler) {
        try {
            V v;
            for (S s : VALUES.keySet()) {
                v = getExplicitValue(key, s, errorHandler);
                if (v != null)
                    return v;
            }
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
        return null;
    }

    /**
     * get value from watched files
     */
    public V getValue(K key) {
        return getValue(key, commonErrorHandler);
    }

    /**
     * get value from explicit watched files
     */
    public V getExplicitValue(K key, S source) {
        return getExplicitValue(key, source, commonErrorHandler);
    }

    /**
     * get value from explicit watched files
     */
    public V getExplicitValue(K key, S source, Consumer<Throwable> errorHandler) {
        try {
            B b = VALUES.get(source);
            return (searcher == null ? searcher = defaultSearcher() : searcher).apply(key, b);
        } catch (Throwable t) {
            errorHandler.accept(t);
        }
        return null;
    }

    /**
     *
     */
    public Dynamically<S, B, K, V> append(K k, V v, S s) {
        try {
            if (isDynamic.get() && VALUES.get(s) != null)
                (appender == null ? appender = defaultAppender() : appender).accept(k, v, s);
        } catch (Throwable t) {
            commonErrorHandler.accept(t);
        }
        return this;
    }

    /**
     *
     */
    public Dynamically<S, B, K, V> remove(K k, S s) {
        try {
            if (isDynamic.get() && VALUES.get(s) != null)
                (eraser == null ? eraser = defaultEraser() : eraser).accept(k, s);
        } catch (Throwable t) {
            commonErrorHandler.accept(t);
        }
        return this;
    }

    /**
     * use this to decide how to covert file to B
     * or override {@link Dynamically#defaultConvertor()}
     */
    public Dynamically<S, B, K, V> onConvert(Function<S, B> convertor) {
        this.convertor = convertor;
        return this;
    }

    /**
     * use this to change default error handler,should return null when no suitable value was found
     * or override  {@link Dynamically#defaultSearcher()}
     */
    public Dynamically<S, B, K, V> onSearch(BiFunction<K, B, V> searcher) {
        this.searcher = searcher;
        return this;
    }

    /**
     * use this to change default error handler
     * or override  {@link Dynamically#defaultErrorHandler()}
     */
    public Dynamically<S, B, K, V> onError(Consumer<Throwable> errorHandler) {
        this.commonErrorHandler = errorHandler;
        return this;
    }

    /**
     * use this to change default file Observer
     * or override  {@link Dynamically#defaultObserver()}
     */
    public Dynamically<S, B, K, V> onObserve(Observer<S, ? extends Closeable> observer) {
        this.observer = observer;
        return this;
    }

    /**
     * use this to change default file Observer
     * or override  {@link Dynamically#defaultObserver()}
     */
    public Dynamically<S, B, K, V> onAppend(TriConsumer<K, V, S> appender) {
        this.appender = appender;
        return this;
    }

    /**
     * use this to change default file Observer
     * or override  {@link Dynamically#defaultObserver()}
     */
    public Dynamically<S, B, K, V> onErase(BiConsumer<K, S> eraser) {
        this.eraser = eraser;
        return this;
    }

    /**
     * override this to change default error handler
     * or use {@link Dynamically#onError(Consumer)}
     */
    protected Consumer<Throwable> defaultErrorHandler() {
        return Throwable::printStackTrace;
    }

    /**
     * override this to decide how to covert file to B
     * or use {@link Dynamically#onConvert(Function)}
     */
    protected Function<S, B> defaultConvertor() {
        throw new IllegalStateException("convertor is required but has not been set up yet!");
    }

    /**
     * override this to change default error handler,should return null when no suitable value was found
     * or use {@link Dynamically#onSearch(BiFunction)}
     */
    protected BiFunction<K, B, V> defaultSearcher() {
        throw new IllegalStateException("searcher is required but has not been set up yet!");
    }

    /**
     * override this to change default file Observer
     * or use {@link Dynamically#onObserve(Observer)}
     */
    protected Observer<S, ? extends Closeable> defaultObserver() {
        throw new IllegalStateException("observer is required but has not been set up yet!");
    }

    /**
     * override this to change default appender
     * or use {@link Dynamically#onAppend(TriConsumer)}
     */
    protected TriConsumer<K, V, S> defaultAppender() {
        throw new IllegalStateException("appender is required but has not been set up yet!");
    }

    /**
     * override this to change default eraser
     * or use {@link Dynamically#onErase(BiConsumer)}
     */
    protected BiConsumer<K, S> defaultEraser() {
        throw new IllegalStateException("eraser is required but has not been set up yet!");
    }
}
