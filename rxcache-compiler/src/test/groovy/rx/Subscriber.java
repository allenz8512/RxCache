package rx;

/**
 * Created by Allen on 2016/7/5.
 */

public interface Subscriber<T> {

    void onCompleted();

    void onError(Throwable e);

    void onNext(T t);
}
