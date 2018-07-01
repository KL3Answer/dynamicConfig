package org.k3a.observer;

/**
 * Created by HQ.XPS15
 * on 2018/6/22  14:51
 */
@SuppressWarnings("unused")
public interface RejectObserving<T> {

    void reject(T t);

    final RejectObserving SILENTLY = t -> {
    };
    final RejectObserving EXCEPTION = t -> {
        throw new IllegalArgumentException("error handling " + t + ",please check your parameter");
    };
}

