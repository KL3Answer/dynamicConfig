
package org.k3a.utils;

import java.util.Objects;

@SuppressWarnings("unused")
@FunctionalInterface
public interface TriConsumer<T, U, S> {

    void accept(T t, U u, S s) throws Exception;

    default TriConsumer<T, U, S> andThen(TriConsumer<? super T, ? super U, ? super S> after) throws Exception{
        Objects.requireNonNull(after);

        return (l, r, s) -> {
            accept(l, r, s);
            after.accept(l, r, s);
        };
    }
}
