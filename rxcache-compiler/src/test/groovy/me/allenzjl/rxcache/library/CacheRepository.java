package me.allenzjl.rxcache.library;

import rx.Observable;

/**
 * Created by Allen on 2016/7/2.
 */

public interface CacheRepository {

    <T> Observable<T> get(String type, String key);

    <T> Observable<T> put(String type, String key, T value, long expired);
}
