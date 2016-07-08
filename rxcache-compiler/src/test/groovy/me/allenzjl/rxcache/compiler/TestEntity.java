package me.allenzjl.rxcache.compiler;

import me.allenzjl.rxcache.annotation.Key;

/**
 * Created by Allen on 2016/7/1.
 */

public class TestEntity {

    @Key
    private int mKey;

    private boolean mKey2;

    @Key("k3")
    public String mKey3;

    public TestEntity(int key, boolean key2, String key3) {
        mKey = key;
        mKey2 = key2;
        mKey3 = key3;
    }

    public int getKey() {
        return mKey;
    }

    @Key
    public boolean isKey2() {
        return mKey2;
    }
}
