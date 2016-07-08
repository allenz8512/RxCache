package me.allenzjl.rxcache.library;

import android.content.Context;

/**
 * Created by Allen on 2016/6/29.
 */

public class CacheRepository extends CacheStorage {

    protected MemoryCacheStorage mMemoryCache;

    protected SQLiteCacheStorage mDatabaseCache;

    protected final Object mLock;

    protected int mGetAccess;

    protected int mPutAccess;

    protected int mRemoveAccess;

    protected int mHit;

    protected int mMiss;

    public CacheRepository(int memoryCacheMaxSize) {
        this(memoryCacheMaxSize, null, 0);
    }

    public CacheRepository(Context context, int databaseCacheMaxSize) {
        this(0, context, databaseCacheMaxSize);
    }

    public CacheRepository(int memoryCacheSize, Context context, int databaseCacheSize) {
        if (memoryCacheSize < 0) {
            throw new IllegalArgumentException("Parameter 'memoryCacheSize' should not be below zero.");
        }
        if (databaseCacheSize < 0) {
            throw new IllegalArgumentException("Parameter 'databaseCacheSize' should not be below zero.");
        }
        if (memoryCacheSize == 0 && (databaseCacheSize == 0 || context == null)) {
            throw new IllegalArgumentException("No cache will be create dude.");
        }
        if (memoryCacheSize > 0) {
            mMemoryCache = new MemoryCacheStorage(memoryCacheSize);
        }
        if (databaseCacheSize > 0) {
            if (context == null) {
                throw new IllegalArgumentException("Parameter 'context' should not be null.");
            }
            mDatabaseCache = new SQLiteCacheStorage(context, databaseCacheSize);
        }
        mLock = new Object();
    }

    @Override
    public int getSize() {
        throw new UnsupportedOperationException("Method getSize() is unsupported.");
    }

    @Override
    public int getMaxSize() {
        throw new UnsupportedOperationException("Method getMaxSize() is unsupported.");
    }

    @Override
    protected <T> T doGet(String type, String key) {
        T value;
        if (mMemoryCache == null) {
            value = mDatabaseCache.doGet(type, key);
        } else if (mDatabaseCache == null) {
            value = mMemoryCache.doGet(type, key);
        } else {
            value = mMemoryCache.doGet(type, key);
            if (value == null) {
                value = mDatabaseCache.doGet(type, key);
                if (value != null) {
                    mMemoryCache.putNow(type, key, value);
                }
            }
        }
        synchronized (mLock) {
            mGetAccess++;
            if (value == null) {
                mMiss++;
            } else {
                mHit++;
            }
        }
        return value;
    }

    @Override
    protected <T> void doPut(String type, String key, T object, long expired) {
        if (mMemoryCache == null) {
            mDatabaseCache.doPut(type, key, object, expired);
        } else if (mDatabaseCache == null) {
            mMemoryCache.doPut(type, key, object, expired);
        } else {
            mMemoryCache.doPut(type, key, object, expired);
            mDatabaseCache.doPut(type, key, object, expired);
        }
        synchronized (mLock) {
            mPutAccess++;
        }
    }

    @Override
    protected void doRemove(String type, String key) {
        if (mMemoryCache == null) {
            mDatabaseCache.doRemove(type, key);
        } else if (mDatabaseCache == null) {
            mMemoryCache.doRemove(type, key);
        } else {
            mMemoryCache.doRemove(type, key);
            mDatabaseCache.doRemove(type, key);
        }
        synchronized (mLock) {
            mRemoveAccess++;
        }
    }

    @Override
    protected <T> int sizeOf(String type, String key, T value) {
        throw new UnsupportedOperationException("Method sizeOf() is unsupported.");
    }

    @Override
    protected void clear() {
        synchronized (mLock) {
            if (mMemoryCache != null) {
                mMemoryCache.clear();
            }
            if (mDatabaseCache != null) {
                mDatabaseCache.clear();
            }
        }
    }

    public CacheStatus getCacheStatus() {
        synchronized (mLock) {
            CacheStatus cacheStatus = new CacheStatus();
            cacheStatus.getAccess = mGetAccess;
            cacheStatus.putAccess = mPutAccess;
            cacheStatus.removeAccess = mRemoveAccess;
            cacheStatus.hit = mHit;
            cacheStatus.miss = mMiss;
            if (mMemoryCache != null) {
                cacheStatus.memorySize = mMemoryCache.getSize();
                cacheStatus.memoryMaxSize = mMemoryCache.getMaxSize();
            }
            if (mDatabaseCache != null) {
                cacheStatus.databaseSize = mDatabaseCache.getSize();
                cacheStatus.databaseMaxSize = mDatabaseCache.getMaxSize();
            }
            return cacheStatus;
        }
    }

    public static class CacheStatus {

        public int getAccess;

        public int putAccess;

        public int removeAccess;

        public int hit;

        public int miss;

        public int memorySize;

        public int memoryMaxSize;

        public int databaseSize;

        public int databaseMaxSize;

        @Override
        public String toString() {
            return "CacheStatus{" +
                    "getAccess=" + getAccess +
                    ", putAccess=" + putAccess +
                    ", removeAccess=" + removeAccess +
                    ", hit=" + hit +
                    ", miss=" + miss +
                    ", hitRate=" + ((float) hit / getAccess * 100) + "%" +
                    ", memorySize=" + memorySize +
                    ", memoryMaxSize=" + memoryMaxSize +
                    ", databaseSize=" + databaseSize +
                    ", databaseMaxSize=" + databaseMaxSize +
                    '}';
        }
    }
}
