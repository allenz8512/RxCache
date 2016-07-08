package me.allenzjl.rxcache.library;

/**
 * Created by Allen on 2016/6/22.
 */

public class JSONProcessor {

    public static Builder newBuilder() {
        return null;
    }

    public interface Builder {

        public Builder add(String name, Object value);

        public Object toObject();

        public String build();
    }

}
