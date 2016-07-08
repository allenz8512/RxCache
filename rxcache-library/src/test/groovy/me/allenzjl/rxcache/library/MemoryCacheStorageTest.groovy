package me.allenzjl.rxcache.library

import spock.lang.Specification

import java.util.concurrent.CountDownLatch

/**
 * Created by Allen on 2016/6/23.
 */
class MemoryCacheStorageTest extends Specification {

    static final def MAX_SIZE = 200

    CacheStorage cacheStorage

    def setup() {
        cacheStorage = new MemoryCacheStorage(MAX_SIZE)
    }

    def cleanup() {
        cacheStorage.clear()
        cacheStorage = null
    }

    def "put one object"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "address"), 0)

        then:
        cacheStorage.size == 35
        cacheStorage.getNow("test", "1") == new TestCacheObject("name", "address")
    }

    def "put three objects"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("test", "1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("test", "2"), 0)
        cacheStorage.putNow("test", "3", new TestCacheObject("test", "3"), 0)

        then:
        cacheStorage.size == 87
        cacheStorage.getNow("test", "1") == new TestCacheObject("test", "1")
        cacheStorage.getNow("test", "2") == new TestCacheObject("test", "2")
        cacheStorage.getNow("test", "3") == new TestCacheObject("test", "3")
    }

    def "overwrite object"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("test", "old1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("test", "old2"), 0)
        cacheStorage.putNow("test", "1", new TestCacheObject("test", "new1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("test", "new2"), 0)

        then:
        cacheStorage.size == 64
        cacheStorage.getNow("test", "1") == new TestCacheObject("test", "new1")
        cacheStorage.getNow("test", "2") == new TestCacheObject("test", "new2")
    }

    def "put array,list,map"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test_array", "1", [new TestCacheObject("test", "1"), new TestCacheObject("test",
                "2")] as TestCacheObject[], 0)
        cacheStorage.putNow("test_list", "1", [new TestCacheObject("test", "3"), new TestCacheObject("test",
                "4")], 0)
        cacheStorage.putNow("test_map", "1", ["5": new TestCacheObject("test", "5"), "6": new
                TestCacheObject("test", "6")], 0)

        then:
        cacheStorage.size == 191
        cacheStorage.getNow("test_array", "1") == [new TestCacheObject("test", "1"), new TestCacheObject("test", "2")] as TestCacheObject[]
        cacheStorage.getNow("test_list", "1") == [new TestCacheObject("test", "3"), new TestCacheObject("test", "4")]
        cacheStorage.getNow("test_map", "1") == ["5": new TestCacheObject("test", "5"), "6": new TestCacheObject("test", "6")]
    }

    def "remove object"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "1"), 0)
        cacheStorage.removeNow("test", "1")

        then:
        cacheStorage.size == 0
        cacheStorage.getNow("test", "1") == null

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("name", "2"), 0)
        cacheStorage.removeNow("test", "1")

        then:
        cacheStorage.size == 29
        cacheStorage.getNow("test", "1") == null
        cacheStorage.getNow("test", "2") == new TestCacheObject("name", "2")

        when:
        cacheStorage.removeNow("test", "2")

        then:
        cacheStorage.size == 0
        cacheStorage.getNow("test", "2") == null
    }

    def "overflow and remove the first cache"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("name", "2"), 0)
        cacheStorage.putNow("test", "3", new TestCacheObject("name", "3"), 0)
        cacheStorage.putNow("test", "4", new TestCacheObject("name", "4"), 0)
        cacheStorage.putNow("test", "5", new TestCacheObject("name", "5"), 0)
        cacheStorage.putNow("test", "6", new TestCacheObject("name", "6"), 0)
        cacheStorage.putNow("test", "7", new TestCacheObject("name", "7"), 0)

        then:
        cacheStorage.size == 174
        cacheStorage.getNow("test", "1") == null
        cacheStorage.getNow("test", "2") == new TestCacheObject("name", "2")
        cacheStorage.getNow("test", "3") == new TestCacheObject("name", "3")
        cacheStorage.getNow("test", "4") == new TestCacheObject("name", "4")
        cacheStorage.getNow("test", "5") == new TestCacheObject("name", "5")
        cacheStorage.getNow("test", "6") == new TestCacheObject("name", "6")
        cacheStorage.getNow("test", "7") == new TestCacheObject("name", "7")
    }

    def "overflow and remove the oldest cache"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("name", "2"), 0)
        cacheStorage.putNow("test", "3", new TestCacheObject("name", "3"), 0)
        cacheStorage.putNow("test", "4", new TestCacheObject("name", "4"), 0)
        cacheStorage.putNow("test", "5", new TestCacheObject("name", "5"), 0)
        cacheStorage.putNow("test", "6", new TestCacheObject("name", "6"), 0)
        cacheStorage.getNow("test", "1")
        cacheStorage.putNow("test", "7", new TestCacheObject("name", "7"), 0)

        then:
        cacheStorage.size == 174
        cacheStorage.getNow("test", "1") == new TestCacheObject("name", "1")
        cacheStorage.getNow("test", "2") == null
        cacheStorage.getNow("test", "3") == new TestCacheObject("name", "3")
        cacheStorage.getNow("test", "4") == new TestCacheObject("name", "4")
        cacheStorage.getNow("test", "5") == new TestCacheObject("name", "5")
        cacheStorage.getNow("test", "6") == new TestCacheObject("name", "6")
        cacheStorage.getNow("test", "7") == new TestCacheObject("name", "7")
    }

    def "multi-thread put"() {
        setup:
        def count = 5
        def latch = new CountDownLatch(count)
        def list = new ArrayList<>(count);

        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        count.times {
            def key = String.valueOf(it)
            def object = new TestCacheObject("name", key)
            list.add(object)
        }
        count.times {
            def key = String.valueOf(it)
            new Thread({
                println "Put thread-$key started"
                cacheStorage.putNow("test", key, list.get(key.toInteger()), 0)
                latch.countDown()
            }).start()
        }
        latch.await()

        then:
        cacheStorage.size == 145
        count.times {
            def key = String.valueOf(it)
            cacheStorage.getNow("test", key) == new TestCacheObject("name", key)
        }
    }

    def "multi-thread get"() {
        setup:
        def count = 5
        def latch = new CountDownLatch(count)

        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("name", "2"), 0)
        cacheStorage.putNow("test", "3", new TestCacheObject("name", "3"), 0)
        cacheStorage.putNow("test", "4", new TestCacheObject("name", "4"), 0)
        cacheStorage.putNow("test", "5", new TestCacheObject("name", "5"), 0)

        then:
        cacheStorage.size == 145
        count.times {
            def key = String.valueOf(it)
            new Thread({
                println "Get thread-$key started"
                cacheStorage.getNow("test", key) == new TestCacheObject("name", key)
                latch.countDown()
            }).start()
        }
        latch.await()
    }

    def "multi-thread remove"() {
        setup:
        def count = 5
        def latch = new CountDownLatch(count)

        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "0", new TestCacheObject("name", "0"), 0)
        cacheStorage.putNow("test", "1", new TestCacheObject("name", "1"), 0)
        cacheStorage.putNow("test", "2", new TestCacheObject("name", "2"), 0)
        cacheStorage.putNow("test", "3", new TestCacheObject("name", "3"), 0)
        cacheStorage.putNow("test", "4", new TestCacheObject("name", "4"), 0)
        cacheStorage.putNow("test", "5", new TestCacheObject("name", "5"), 0)
        count.times {
            def key = String.valueOf(it)
            new Thread({
                println "Remove thread-$key started"
                cacheStorage.removeNow("test", key)
                latch.countDown()
            }).start()
        }
        latch.await()

        then:
        cacheStorage.size == 29
        cacheStorage.getNow("test", "0") == null
        cacheStorage.getNow("test", "1") == null
        cacheStorage.getNow("test", "2") == null
        cacheStorage.getNow("test", "3") == null
        cacheStorage.getNow("test", "4") == null
        cacheStorage.getNow("test", "5") == new TestCacheObject("name", "5")
    }

    def "get expired cache"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "0", new TestCacheObject("name", "0"), System.currentTimeMillis() + 3000)

        then:
        cacheStorage.size == 29
        cacheStorage.getNow("test", "0") == new TestCacheObject("name", "0")
        sleep(3000)
        cacheStorage.size == 29
        cacheStorage.getNow("test", "0") == null
        cacheStorage.size == 0
    }

    def "clear"() {
        expect:
        cacheStorage.maxSize == MAX_SIZE
        cacheStorage.size == 0

        when:
        cacheStorage.putNow("test", "0", new TestCacheObject("test", "0"));
        cacheStorage.putNow("test", "1", new TestCacheObject("test", "1"));
        cacheStorage.putNow("test", "2", new TestCacheObject("test", "2"));
        cacheStorage.putNow("test", "3", new TestCacheObject("test", "3"));
        cacheStorage.putNow("test", "4", new TestCacheObject("test", "4"));

        then:
        cacheStorage.size == 145
        cacheStorage.getNow("test", "0") == new TestCacheObject("test", "0")
        cacheStorage.getNow("test", "1") == new TestCacheObject("test", "1")
        cacheStorage.getNow("test", "2") == new TestCacheObject("test", "2")
        cacheStorage.getNow("test", "3") == new TestCacheObject("test", "3")
        cacheStorage.getNow("test", "4") == new TestCacheObject("test", "4")

        when:
        cacheStorage.clear()

        then:
        cacheStorage.size == 0
        cacheStorage.getNow("test", "0") == null
        cacheStorage.getNow("test", "1") == null
        cacheStorage.getNow("test", "2") == null
        cacheStorage.getNow("test", "3") == null
        cacheStorage.getNow("test", "4") == null
    }
}
