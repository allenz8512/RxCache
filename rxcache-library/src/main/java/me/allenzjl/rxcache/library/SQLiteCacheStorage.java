package me.allenzjl.rxcache.library;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.EXPIRE_TIMESTAMP;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.ID;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.KEY;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.TABLE_NAME;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.TIMESTAMP;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.TYPE;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.VALUE;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.VALUE_CLASS_NAME;
import static me.allenzjl.rxcache.library.SQLiteCacheStorage.SQLiteCacheTable.VALUE_TYPE;

/**
 * Created by Allen on 2016/6/22.
 */

public class SQLiteCacheStorage extends CacheStorage {

    public static final int VALUE_TYPE_OBJECT = 0;

    public static final int VALUE_TYPE_ARRAY = 1;

    public static final int VALUE_TYPE_LIST = 2;

    public static final int VALUE_TYPE_MAP = 3;

    protected final int mMaxSize;

    protected SQLiteDatabase mReadableDB;

    protected SQLiteDatabase mWritableDB;

    protected final Object mLock;

    protected volatile int mSize;

    public SQLiteCacheStorage(Context context, int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Value of 'maxSize' should be above zero.");
        }
        mMaxSize = maxSize;
        SQLiteCacheStorageHelper sqLiteCacheStorageHelper = new SQLiteCacheStorageHelper(context);
        mReadableDB = sqLiteCacheStorageHelper.getReadableDatabase();
        mWritableDB = sqLiteCacheStorageHelper.getWritableDatabase();
        mLock = new Object();
        mSize = count();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public int getSize() {
        return mSize;
    }

    @Override
    public int getMaxSize() {
        return mMaxSize;
    }

    @Override
    protected <T> T doGet(String type, String key) {
        Cache cache = loadInternal(type, key);
        Object result;
        if (cache == null) {
            result = null;
        } else {
            if (TextUtils.isEmpty(cache.valueClassname)) {
                if (cache.valueType == VALUE_TYPE_LIST) {
                    result = new ArrayList();
                } else if (cache.valueType == VALUE_TYPE_MAP) {
                    result = new HashMap();
                } else {
                    throw new IllegalStateException("Error cache data");
                }
            } else {
                if (cache.valueType == VALUE_TYPE_MAP) {
                    String[] classNames = cache.valueClassname.split(",");
                    Class<?> keyClass = getClassByName(classNames[0]);
                    Class<?> valueClass = getClassByName(classNames[1]);
                    result = JSONProcessor.getInstance().parseMap(cache.json, keyClass, valueClass);
                } else {
                    Class<?> valueClass = getClassByName(cache.valueClassname);
                    switch (cache.valueType) {
                        case VALUE_TYPE_OBJECT:
                            result = JSONProcessor.getInstance().parse(cache.json, valueClass);
                            break;
                        case VALUE_TYPE_ARRAY:
                            result = JSONProcessor.getInstance().parseArray(cache.json, valueClass);
                            break;
                        case VALUE_TYPE_LIST:
                            result = JSONProcessor.getInstance().parseList(cache.json, valueClass);
                            break;
                        default:
                            throw new IllegalStateException("Unknown value type: " + cache.valueType);
                    }
                }
            }
        }
        //noinspection unchecked
        return (T) result;
    }

    @Override
    protected <T> void doPut(String type, String key, T object, long expired) {
        if (object.getClass().isArray()) {
            //noinspection unchecked
            storeInternal(type, key, object, VALUE_TYPE_ARRAY, object.getClass().getComponentType().getName(), expired);
        } else if (object instanceof List) {
            if (((List) object).isEmpty()) {
                storeInternal(type, key, object, VALUE_TYPE_LIST, "", expired);
            } else {
                storeInternal(type, key, object, VALUE_TYPE_LIST, ((List) object).get(0).getClass().getName(), expired);
            }
        } else if (object instanceof Map) {
            if (((Map) object).isEmpty()) {
                storeInternal(type, key, object, VALUE_TYPE_MAP, "", expired);
            } else {
                Map.Entry entry = (Map.Entry) ((Map) object).entrySet().iterator().next();
                Class<?> keyClass = entry.getKey().getClass();
                checkKeyClass(keyClass);
                String valueClassname = keyClass.getName() + "," + entry.getValue().getClass().getName();
                storeInternal(type, key, object, VALUE_TYPE_MAP, valueClassname, expired);
            }
        } else {
            storeInternal(type, key, object, VALUE_TYPE_OBJECT, object.getClass().getName(), expired);
        }
    }


    @Override
    protected void doRemove(String type, String key) {
        delete(type, key);
    }

    @Override
    protected <T> int sizeOf(String type, String key, T value) {
        return 1;
    }

    @Override
    protected void clear() {
        mWritableDB.execSQL("DELETE FROM " + TABLE_NAME);
        synchronized (mLock) {
            mSize = 0;
        }
    }

    protected Cache loadInternal(String type, String key) {
        Cursor cursor = null;
        String json;
        int valueType;
        String valueClassname;
        long expired;
        try {
            cursor =
                    mReadableDB.query(TABLE_NAME, null, TYPE + "=? and " + KEY + "=?", new String[]{type, key}, null, null, null);
            int count = cursor.getCount();
            if (count == 0) {
                return null;
            } else if (count > 1) {
                throw new IllegalStateException("More than one column under type '" + type + "' and key '" + key + "' found.");
            } else {
                cursor.moveToFirst();
                json = cursor.getString(3);
                valueType = cursor.getInt(4);
                valueClassname = cursor.getString(5);
                expired = cursor.getLong(6);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        if (expired != 0 && System.currentTimeMillis() > expired) {
            delete(type, key);
            return null;
        } else {
            return new Cache(json, valueType, valueClassname);
        }
    }

    protected void storeInternal(String type, String key, Object value, int valueType, String valueClassname, long expired) {
        ContentValues values = new ContentValues();
        values.put(TYPE, type);
        values.put(KEY, key);
        values.put(VALUE, JSONProcessor.getInstance().toJSON(value));
        values.put(VALUE_TYPE, valueType);
        values.put(VALUE_CLASS_NAME, valueClassname);
        values.put(EXPIRE_TIMESTAMP, expired);
        Cursor cursor = null;
        boolean exist = false;
        try {
            cursor =
                    mReadableDB.query(TABLE_NAME, null, TYPE + "=? and " + KEY + "=?", new String[]{type, key}, null, null, null);
            int count = cursor.getCount();
            if (count == 1) {
                cursor.moveToFirst();
                values.put(ID, cursor.getInt(0));
                exist = true;
            } else if (count > 1) {
                throw new IllegalStateException("More than one column under type '" + type + "' and key '" + key + "' found.");
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        values.put(TIMESTAMP, System.currentTimeMillis());
        long rowId = mWritableDB.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            throw new RuntimeException("Insert cache with type:'" + type + "' and key:'" + key + "' failed.");
        }
        synchronized (mLock) {
            if (!exist) {
                mSize++;
                if (mSize > mMaxSize) {
                    int deleteCount = mSize - mMaxSize;
                    deleteOldest(deleteCount);
                }
            }
        }
    }

    protected void checkKeyClass(Class<?> keyClass) {
        if (keyClass == String.class || keyClass == Integer.class || keyClass == Long.class) {
            return;
        }
        throw new IllegalArgumentException("Unsupported key class type:'" + keyClass.getName() + "'.");
    }

    protected void delete(String type, String key) {
        if (mWritableDB.delete(TABLE_NAME, TYPE + "=? and " + KEY + "=?", new String[]{type, key}) > 0) {
            synchronized (mLock) {
                mSize--;
                if (mSize < 0) {
                    throw new IllegalStateException("Size is below zero after delete cache");
                }
            }
        }
    }

    protected Class<?> getClassByName(String valueClassname) {
        Class<?> valueClass;
        try {
            valueClass = Class.forName(valueClassname);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return valueClass;
    }

    protected void deleteOldest(int deleteCount) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE rowid IN(SELECT rowid from " + TABLE_NAME + " ORDER BY " + TIMESTAMP +
                " LIMIT " + deleteCount + ");";
        mWritableDB.execSQL(sql);
        mSize -= deleteCount;
        if (mSize < 0) {
            throw new IllegalStateException("Size is below zero after delete cache");
        }
    }

    protected int count() {
        Cursor cursor = null;
        try {
            cursor = mReadableDB.query(TABLE_NAME, null, null, null, null, null, null);
            return cursor.getCount();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    protected void close() {
        mReadableDB.close();
        mWritableDB.close();
    }

    protected static class SQLiteCacheStorageHelper extends SQLiteOpenHelper {

        public static final int DB_VERSION = 1;

        public static final String DB_NAME = "CACHE_STORAGE";

        public SQLiteCacheStorageHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + TYPE +
                    " TEXT," + KEY + " TEXT," + VALUE + " TEXT," + VALUE_TYPE + " INTEGER," + VALUE_CLASS_NAME + " TEXT," +
                    EXPIRE_TIMESTAMP + " INTEGER," + TIMESTAMP + " INTEGER);";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    protected static class SQLiteCacheTable {

        public static final String TABLE_NAME = "cache";

        public static final String ID = "_id";

        public static final String TYPE = "_type";

        public static final String KEY = "_key";

        public static final String VALUE = "_value";

        public static final String VALUE_TYPE = "_value_type";

        public static final String VALUE_CLASS_NAME = "_value_class_name";

        public static final String EXPIRE_TIMESTAMP = "_expire_timestamp";

        public static final String TIMESTAMP = "_timestamp";
    }

    protected class Cache {

        public String json;

        public int valueType;

        public String valueClassname;

        public Cache(String json, int valueType, String valueClassname) {
            this.json = json;
            this.valueType = valueType;
            this.valueClassname = valueClassname;
        }
    }
}
