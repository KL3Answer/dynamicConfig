package org.k3a.observer;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by  k3a
 * on 2018/7/27  9:39
 */
@SuppressWarnings("WeakerAccess")
public abstract class LocalFileSystemObserver extends Observer<Path, WatchService> {

    @SuppressWarnings("Duplicates")
    protected int getEventOrder(WatchEvent.Kind<?> kind) {
        switch (kind.name()) {
            case "ENTRY_MODIFY":
                return 0;
            case "ENTRY_DELETE":
                return 1;
            case "ENTRY_CREATE":
                return 2;
            default:
                return 3;
        }
    }

    @Override
    protected RejectObserving<Path> defaultRejection() {
        //noinspection unchecked
        return RejectObserving.IGNORE;
    }

    protected BiConsumer<Path, WatchEvent<?>> commonOnChangeHandler() {
        return (path, event) -> {
            final WatchEvent.Kind<?> kind = event.kind();
            final Consumer<Path> consumer;
            if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind))
                ((consumer = modifyHandlers.get(path)) == null ? commonModifyHandler : consumer).accept(path);
            else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind))
                ((consumer = deleteHandlers.get(path)) == null ? commonDeleteHandler : consumer).accept(path);
            else if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind))
                ((consumer = createHandlers.get(path)) == null ? commonCreateHandler : consumer).accept(path);
        };
    }


}
