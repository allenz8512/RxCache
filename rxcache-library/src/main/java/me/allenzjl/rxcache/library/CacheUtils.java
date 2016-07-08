package me.allenzjl.rxcache.library;

import java.util.List;
import java.util.Map;

import me.allenzjl.rxcache.annotation.Cacheable;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by Allen on 2016/7/2.
 */

public class CacheUtils {

    private CacheUtils() {
        throw new UnsupportedOperationException();
    }

    public static <T> Observable<T> getObjectCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                 long expired, int strategy, Observable<T> source) {
        Observable<T> storeSource = source.doOnNext(value -> {
            long expiredOn = expired == 0 ? 0 : System.currentTimeMillis() + expired;
            cacheRepository.putNow(type, key, value, expiredOn);
        });
        return getCacheableObservable(cacheRepository, type, key, strategy, storeSource);
    }

    public static <T> Observable<T[]> getArrayCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                  long expired, int strategy, String subType,
                                                                  Func1<T, String> keyMapper, Observable<T[]> source) {
        Observable<T[]> storeSource = source.doOnNext(entities -> {
            long expiredOn = expired == 0 ? 0 : System.currentTimeMillis() + expired;
            cacheRepository.putNow(type, key, entities, expiredOn);
            for (T entity : entities) {
                String subKey = keyMapper.call(entity);
                cacheRepository.putNow(subType, subKey, entity, expiredOn);
            }
        });
        return getCacheableObservable(cacheRepository, type, key, strategy, storeSource);
    }

    public static <T> Observable<List<T>> getListCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                     long expired, int strategy, String subType,
                                                                     Func1<T, String> keyMapper, Observable<List<T>> source) {
        Observable<List<T>> storeSource = source.doOnNext(entities -> {
            long expiredOn = expired == 0 ? 0 : System.currentTimeMillis() + expired;
            cacheRepository.putNow(type, key, entities, expiredOn);
            for (T entity : entities) {
                String subKey = keyMapper.call(entity);
                cacheRepository.putNow(subType, subKey, entity, expiredOn);
            }
        });
        return getCacheableObservable(cacheRepository, type, key, strategy, storeSource);
    }

    public static <K, V> Observable<Map<K, V>> getMapCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                                         long expired, int strategy, String subType,
                                                                         Func1<V, String> keyMapper,
                                                                         Observable<Map<K, V>> source) {
        Observable<Map<K, V>> storeSource = source.doOnNext(map -> {
            long expiredOn = expired == 0 ? 0 : System.currentTimeMillis() + expired;
            cacheRepository.putNow(type, key, map, expiredOn);
            for (V entity : map.values()) {
                String subKey = keyMapper.call(entity);
                cacheRepository.putNow(subType, subKey, entity, expiredOn);
            }
        });
        return getCacheableObservable(cacheRepository, type, key, strategy, storeSource);
    }

    private static <T> Observable<T> getCacheableObservable(CacheRepository cacheRepository, String type, String key,
                                                            int strategy, Observable<T> storeSource) {
        return cacheRepository.<T>get(type, key).flatMap(cache -> {
            if (cache != null) {
                if (strategy == Cacheable.STRATEGY_LOAD_SOURCE_AFTER_READ_CACHE) {
                    return Observable.concat(Observable.just(cache), storeSource);
                } else {
                    return Observable.just(cache);
                }
            } else {
                return storeSource;
            }
        });
    }

}
