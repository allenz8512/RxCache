package me.allenzjl.rxcache.library;

import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by Allen on 2016/7/2.
 */

public class CacheUtils {

    public static <T> Observable<T> getObjectCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                 long expired, int strategy, Observable<T> source) {
        return null;
    }

    public static <T> Observable<T[]> getArrayCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                  long expired, int strategy, String subType,
                                                                  Func1<T, String> keyMapper, Observable<T[]> source) {
        return null;
    }

    public static <T> Observable<List<T>> getListCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                     long expired, int strategy, String subType,
                                                                     Func1<T, String> keyMapper, Observable<List<T>> source) {
        return null;
    }

    public static <K, V> Observable<Map<K, V>> getMapCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                         long expired, int strategy, String subType,
                                                                         Func1<V, String> keyMapper,
                                                                         Observable<Map<K, V>> source) {
        return null;
    }

}
