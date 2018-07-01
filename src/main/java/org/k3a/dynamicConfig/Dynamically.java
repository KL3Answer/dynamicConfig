package org.k3a.dynamicConfig;

import org.k3a.utils.TriConsumer;
import org.k3a.observer.Observer;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    protected Function<S, B> convertor = defaultConvertor();

    protected BiFunction<K, B, V> searcher = defaultSearcher();

    protected Observer<S, ? extends Closeable> observer = defaultObserver();

    protected TriConsumer<K, V, S> appender = defaultAppender();

    protected BiConsumer<K, S> eraser = defaultEraser();

    protected Consumer<Throwable> commonErrorHandler = defaultErrorHandler();

    protected volatile boolean isDynamic = false;

    /**
     * add sources
     */
    public Dynamically<S, B, K, V> register(S s, Consumer<Throwable> errorHandler) {
        try {
            B b = convertor.apply(s);
            VALUES.put(s, b);
        } catch (Throwable t) {
            VALUES.remove(s);
            errorHandler.accept(t);
        }
        return this;
    }

    /**
     * add sources
     */
    @SuppressWarnings("unchecked")
    public final Dynamically<S, B, K, V> register(S... sources) {
        return register(commonErrorHandler, sources);
    }

    /**
     * add sources
     */
    public final Dynamically<S, B, K, V> register(Collection<S> sources) {
        return register(commonErrorHandler, sources);
    }

    /**
     * add sources
     */
    @SuppressWarnings("unchecked")
    public Dynamically<S, B, K, V> register(Consumer<Throwable> errorHandler, S... sources) {
        register(errorHandler, Arrays.asList(sources));
        return this;
    }

    /**
     * add sources
     */
    public Dynamically<S, B, K, V> register(Consumer<Throwable> errorHandler, Collection<S> sources) {
        if (sources != null && sources.size() != 0)
            for (S file : sources)
                register(file, errorHandler);
        return this;
    }

    /**
     * start isDynamic registered sources
     */
    public Dynamically<S, B, K, V> activate(Observer<S, ? extends Closeable> observer) {
        try {
            // stop previous round
            if (isDynamic)
                inactivate();

            //start a new round
            final Collection<S> sources = VALUES.keySet();
            //noinspection unchecked
            (this.observer = observer).onModify(this::register).watch(sources);
            isDynamic = true;
        } catch (Throwable t) {
            commonErrorHandler.accept(t);
        }
        return this;
    }

    /**
     * start isDynamic registered paths
     */
    public Dynamically<S, B, K, V> activate() {
        return activate(observer);
    }

    /**
     * stop isDynamic paths
     */
    public Dynamically<S, B, K, V> inactivate(Consumer<Throwable> errorHandler) {
        try {
            if (observer != null) {
                observer.stop();
                isDynamic = false;
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
            return searcher.apply(key, b);
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
            if (isDynamic && VALUES.get(s) != null)
                appender.accept(k, v, s);
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
            if (isDynamic && VALUES.get(s) != null)
                eraser.accept(k, s);
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
        if (convertor != null)
            this.convertor = convertor;
        return this;
    }

    /**
     * use this to change default error handler,should return null when no suitable value was found
     * or override  {@link Dynamically#defaultSearcher()}
     */
    public Dynamically<S, B, K, V> onSearch(BiFunction<K, B, V> searcher) {
        if (searcher != null)
            this.searcher = searcher;
        return this;
    }

    /**
     * use this to change default error handler
     * or override  {@link Dynamically#defaultErrorHandler()}
     */
    public Dynamically<S, B, K, V> onError(Consumer<Throwable> errorHandler) {
        if (errorHandler != null)
            this.commonErrorHandler = errorHandler;
        return this;
    }

    /**
     * use this to change default file Observer
     * or override  {@link Dynamically#defaultObserver()}
     */
    public Dynamically<S, B, K, V> onObserver(Observer<S, ? extends Closeable> observer) {
        if (observer != null)
            this.observer = observer;
        return this;
    }

    /**
     * use this to change default file Observer
     * or override  {@link Dynamically#defaultObserver()}
     */
    public Dynamically<S, B, K, V> onAppend(TriConsumer<K, V, S> appender) {
        if (appender != null)
            this.appender = appender;
        return this;
    }

    /**
     * use this to change default file Observer
     * or override  {@link Dynamically#defaultObserver()}
     */
    public Dynamically<S, B, K, V> onErase(BiConsumer<K, S> eraser) {
        if (eraser != null)
            this.eraser = eraser;
        return this;
    }

    /**
     * override this to decide how to covert file to B
     * or use {@link Dynamically#onConvert(Function)}
     */
    protected Function<S, B> defaultConvertor() {
        return s -> null;
    }

    /**
     * override this to change default error handler,should return null when no suitable value was found
     * or use {@link Dynamically#onSearch(BiFunction)}
     */
    protected BiFunction<K, B, V> defaultSearcher() {
        return (k, b) -> null;
    }

    /**
     * override this to change default error handler
     * or use {@link Dynamically#onError(Consumer)}
     */
    protected Consumer<Throwable> defaultErrorHandler() {
        return Throwable::printStackTrace;
    }

    /**
     * override this to change default file Observer
     * or use {@link Dynamically#onObserver(Observer)}
     */
    protected Observer<S, ? extends Closeable> defaultObserver() {
        return null;
    }

    /**
     * override this to change default appender
     * or use {@link Dynamically#onAppend(TriConsumer)}
     */
    protected TriConsumer<K, V, S> defaultAppender() {
        return null;
    }

    /**
     * override this to change default eraser
     * or use {@link Dynamically#onErase(BiConsumer)}
     */
    protected BiConsumer<K, S> defaultEraser() {
        return null;
    }
}
