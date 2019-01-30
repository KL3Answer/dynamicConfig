package org.k3a.observer;

import java.util.logging.Logger;

/**
 * Created by  k3a
 * on 2018/6/22  14:51
 */
@SuppressWarnings("unused")
public interface RejectObserving<T> {

    Logger LOGGER = Logger.getLogger(RejectObserving.class.getName());

    void reject(T t);

    RejectObserving SILENTLY = t -> {
    };
    RejectObserving EXCEPTION = t -> {
        throw new IllegalArgumentException("error handling " + t + ",please check your parameter");
    };
    RejectObserving IGNORE = t -> LOGGER.info("NOTICE:an error occur while registering " + t + " and it won't be observed\n");
}

