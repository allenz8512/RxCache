package me.allenzjl.rxcache.library;

import android.support.v4.util.LruCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Allen on 2016/6/22.
 */

public class MemoryCacheStorage extends CacheStorage {

    protected LruCache<String, Object> mCache;

    protected Map<String, Long> mExpires;

    protected final Object mLock;

    public MemoryCacheStorage(int maxSize) {
        mCache = new Cache(maxSize);
        mExpires = new HashMap<>();
        mLock = new Object();
    }

    @Override
    public int getSize() {
        return mCache.size();
    }

    @Override
    public int getMaxSize() {
        return mCache.maxSize();
    }

    @Override
    protected <T> T doGet(String type, String key) {
        //noinspection unchecked
        return (T) getInternal(type, key);
    }

    @Override
    protected <T> void doPut(String type, String key, T object, long expired) {
        putInternal(type, key, object, expired);
    }

    @Override
    protected void doRemove(String type, String key) {
        removeInternal(type, key);
    }

    @Override
    protected <T> int sizeOf(String type, String key, T value) {
        return JSONProcessor.getInstance().toJSON(value).length();
    }

    @Override
    protected void clear() {
        synchronized (mLock) {
            mCache.evictAll();
            mExpires.clear();
        }
    }

    protected Object getInternal(String type, String key) {
        String realKey = realKey(type, key);
        Object value;
        synchronized (mLock) {
            if (mExpires.containsKey(realKey)) {
                long expired = mExpires.get(realKey);
                if (expired > 0 && System.currentTimeMillis() > expired) {
                    value = null;
                    mCache.remove(realKey);
                    mExpires.remove(realKey);
                } else {
                    value = mCache.get(realKey);
                }
            } else {
                value = mCache.get(realKey);
            }
        }
        return value;
    }

    protected void putInternal(String type, String key, Object object, long expired) {
        String realKey = realKey(type, key);
        synchronized (mLock) {
            if (object == null) {
                mCache.remove(realKey);
                mExpires.remove(realKey);
            } else {
                mCache.put(realKey, object);
                mExpires.put(realKey, expired);
            }
        }
    }

    protected void removeInternal(String type, String key) {
        String realKey = realKey(type, key);
        synchronized (mLock) {
            mCache.remove(realKey);
            mExpires.remove(realKey);
        }
    }

    protected String realKey(String type, String key) {
        return type.concat(key);
    }

    protected class Cache extends LruCache<String, Object> {

        public Cache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, Object value) {
            return MemoryCacheStorage.this.sizeOf(null, null, value);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Object oldValue, Object newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
        }
    }

}
