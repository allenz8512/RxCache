package me.allenzjl.rxcache.library;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Allen on 2016/6/22.
 */

public class JSONProcessor {

    private static class LazyHolder {
        private static JSONProcessor INSTANCE = new JSONProcessor();
    }

    public static JSONProcessor getInstance() {
        return LazyHolder.INSTANCE;
    }

    private JSONProcessor() {
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public <T> String toJSON(T obj) {
        return JSON.toJSONString(obj);
    }

    public <T> T parse(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public <T> T[] parseArray(String json, Class<T> clazz) {
        List<T> list = JSON.parseArray(json, clazz);
        //noinspection unchecked
        T[] array = (T[]) Array.newInstance(clazz, list.size());
        return list.toArray(array);
    }

    public <T> List<T> parseList(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }

    public <K, V> Map<K, V> parseMap(String json, Class<K> keyClass, Class<V> valueClass) {
        JSONObject jsonObject = (JSONObject) JSON.parse(json);
        Map<K, V> map = new HashMap<>();
        for (String key : jsonObject.keySet()) {
            map.put(transformKey(key, keyClass), jsonObject.getObject(key, valueClass));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    protected <T> T transformKey(String key, Class<T> keyClass) {
        if (keyClass == String.class) {
            return (T) key;
        } else if (keyClass == Integer.class) {
            return (T) Integer.valueOf(key);
        } else if (keyClass == Long.class) {
            return (T) Long.valueOf(key);
        } else if (keyClass == Boolean.class) {
            return (T) Boolean.valueOf(key);
        } else {
            throw new IllegalArgumentException("Unsupported key class type:'" + keyClass.getName() + "'.");
        }
    }

    public static class Builder {

        private final JSONObject mJSONObject;

        private Builder() {
            mJSONObject = new JSONObject(true);
        }

        public Builder add(String name, Object value) {
            mJSONObject.put(name, value);
            return this;
        }

        public Object toObject() {
            return mJSONObject;
        }

        public String build() {
            return mJSONObject.toJSONString();
        }
    }

}
