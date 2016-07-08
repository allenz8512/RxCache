package me.allenzjl.rxcache.library

import me.allenzjl.library.BuildConfig
import me.allenzjl.rxcache.annotation.Cacheable
import org.robolectric.annotation.Config
import org.robospock.GradleRoboSpecification
import rx.Observable
import rx.functions.Func1
import rx.observers.TestSubscriber

/**
 * Created by Allen on 2016/7/6.
 */
@Config(sdk = 21, constants = BuildConfig)
class CacheUtilsTest extends GradleRoboSpecification {

    static final def MEMORY_MAX_SIZE = 200

    CacheRepository cacheStorage

    def setup() {
        cacheStorage = new CacheRepository(MEMORY_MAX_SIZE)
    }

    def cleanup() {
        cacheStorage = null
    }

    def "object cacheable with strategy 'read cache only if exist'"() {
        setup:
        def type = "test"
        def key = "1"
        def expired = 0
        def strategy = Cacheable.STRATEGY_READ_CACHE_ONLY_IF_EXIST
        def source = Observable.just(new TestCacheObject(type, key))
        def ob
        def subscriber
        def cacheStatus

        when:
        ob = CacheUtils.getObjectCacheableObservable(cacheStorage, type, key, expired, strategy, source)
        subscriber = new TestSubscriber()
        ob.subscribe(subscriber)
        cacheStatus = cacheStorage.getCacheStatus()

        then:
        cacheStatus.getAccess == 1
        cacheStatus.putAccess == 1
        cacheStatus.hit == 0
        cacheStatus.miss == 1
        subscriber.assertValue(new TestCacheObject(type, key))
        cacheStorage.getNow(type, key) == new TestCacheObject(type, key)

        when:
        subscriber = new TestSubscriber()
        ob.subscribe(subscriber)
        cacheStatus = cacheStorage.getCacheStatus()

        then:
        cacheStatus.getAccess == 3
        cacheStatus.putAccess == 1
        cacheStatus.hit == 2
        cacheStatus.miss == 1
        subscriber.assertValue(new TestCacheObject(type, key))
    }

    def "object cacheable with strategy 'load source after read cache'"() {
        setup:
        def type = "test"
        def key = "1"
        def expired = 0
        def strategy = Cacheable.STRATEGY_LOAD_SOURCE_AFTER_READ_CACHE
        def source = Observable.just(new TestCacheObject("test", "1"))
        def ob
        def subscriber
        def cacheStatus

        when:
        ob = CacheUtils.getObjectCacheableObservable(cacheStorage, type, key, expired, strategy, source)
        subscriber = new TestSubscriber();
        ob.subscribe(subscriber)
        cacheStatus = cacheStorage.getCacheStatus()

        then:
        cacheStatus.getAccess == 1
        cacheStatus.putAccess == 1
        cacheStatus.hit == 0
        cacheStatus.miss == 1
        subscriber.assertValue(new TestCacheObject("test", "1"))
        cacheStorage.getNow(type, key) == new TestCacheObject("test", "1")

        when:
        source = Observable.just(new TestCacheObject("test", "2"))
        ob = CacheUtils.getObjectCacheableObservable(cacheStorage, type, key, expired, strategy, source)
        subscriber = new TestSubscriber()
        ob.subscribe(subscriber)
        cacheStatus = cacheStorage.getCacheStatus()

        then:
        cacheStatus.getAccess == 3
        cacheStatus.putAccess == 2
        cacheStatus.hit == 2
        cacheStatus.miss == 1
        subscriber.assertValues(new TestCacheObject("test", "1"), new TestCacheObject("test", "2"))
        cacheStorage.getNow(type, key) == new TestCacheObject("test", "2")
    }

    def "list cacheable"() {
        setup:
        def type = "test"
        def key = "1"
        def expired = 0
        def strategy = Cacheable.STRATEGY_READ_CACHE_ONLY_IF_EXIST
        def subType = "subTest"
        Func1<TestCacheObject, String> keyMapper = { TestCacheObject testCacheObject ->
            return testCacheObject.address
        }
        def source, ob, subscriber, cacheStatus

        when:
        source = Observable.just([new TestCacheObject("test", "1"), new TestCacheObject("test", "2")])
        ob = CacheUtils.getListCacheableObservable(cacheStorage, type, key, expired, strategy, subType, keyMapper, source)
        subscriber = new TestSubscriber()
        ob.subscribe(subscriber)
        cacheStatus = cacheStorage.getCacheStatus()

        then:
        cacheStatus.getAccess == 1
        cacheStatus.putAccess == 3
        cacheStatus.hit == 0
        cacheStatus.miss == 1
        subscriber.assertValues([new TestCacheObject("test", "1"), new TestCacheObject("test", "2")])
        cacheStorage.getNow(type, key) == [new TestCacheObject("test", "1"), new TestCacheObject("test", "2")]
        cacheStorage.getNow(subType, "1") == new TestCacheObject("test", "1")
        cacheStorage.getNow(subType, "2") == new TestCacheObject("test", "2")
    }

}
