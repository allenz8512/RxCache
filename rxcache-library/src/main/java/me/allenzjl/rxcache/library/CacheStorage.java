package me.allenzjl.rxcache.library;

import rx.Observable;

/**
 * Created by Allen on 2016/6/22.
 */

public abstract class CacheStorage {

    public <T> Observable<T> get(String type, String key) {
        checkTypeAndKey(type, key);
        return Observable.create(s -> {
            T cache = getNow(type, key);
            s.onNext(cache);
            s.onCompleted();
        });
    }

    public <T> Observable<T> put(String type, String key, T value) {
        checkTypeAndKey(type, key);
        return Observable.create(s -> {
            putNow(type, key, value, 0);
            s.onNext(value);
            s.onCompleted();
        });
    }

    public <T> Observable<T> put(String type, String key, T value, long expired) {
        checkTypeAndKey(type, key);
        checkExpired(expired);
        return Observable.create(s -> {
            putNow(type, key, value, expired);
            s.onNext(value);
            s.onCompleted();
        });
    }

    public Observable<Void> remove(String type, String key) {
        checkTypeAndKey(type, key);
        return Observable.create(s -> {
            removeNow(type, key);
            s.onNext(null);
            s.onCompleted();
        });
    }

    public <T> T getNow(String type, String key) {
        return doGet(type, key);
    }

    public <T> void putNow(String type, String key, T value) {
        doPut(type, key, value, 0);
    }

    public <T> void putNow(String type, String key, T value, long expired) {
        doPut(type, key, value, expired);
    }

    public void removeNow(String type, String key) {
        doRemove(type, key);
    }

    public abstract int getSize();

    public abstract int getMaxSize();

    protected abstract <T> T doGet(String type, String key);

    protected abstract <T> void doPut(String type, String key, T object, long expired);

    protected abstract void doRemove(String type, String key);

    protected abstract <T> int sizeOf(String type, String key, T value);

    protected abstract void clear();

    protected void checkTypeAndKey(String type, String key) {
        if (isStringEmpty(type)) {
            throw new IllegalArgumentException("Value of 'type' should not be empty");
        }
        if (isStringEmpty(key)) {
            throw new IllegalArgumentException("Value of 'key' should not be empty");
        }
    }

    protected void checkExpired(long expired) {
        if (expired < 0) {
            throw new IllegalArgumentException("Value of 'expired' should not be below zero");
        }
    }

    protected boolean isStringEmpty(String str) {
        return str == null || str.equals("");
    }
}
