package rx;

import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.BlockingObservable;

/**
 * Created by Allen on 2016/7/2.
 */

public class Observable<T> {

    public static <T> Observable<T> just(final T value) {
        return null;
    }

    public <R> Observable<R> flatMap(Func1<? super T, ? extends Observable<? extends R>> func) {
        return null;
    }

    public final Observable<T> doOnNext(final Action1<? super T> onNext) {
        return null;
    }

    public static <T> Observable<T> concat(Observable<? extends Observable<? extends T>> observables) {
        return null;
    }

    public final BlockingObservable<T> toBlocking() {
        return null;
    }
}
