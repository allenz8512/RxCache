package me.allenzjl.rxcache.library;

/**
 * Created by Allen on 2016/6/23.
 */

public class TestCacheObject {

    public String name;

    public String address;

    public TestCacheObject() {
    }

    public TestCacheObject(String name, String address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestCacheObject that = (TestCacheObject) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        return address != null ? address.equals(that.address) : that.address == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        return result;
    }
}
