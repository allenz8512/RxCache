package me.allenzjl.rxcache.compiler

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import com.google.testing.compile.JavaSourcesSubjectFactory
import spock.lang.Specification

import javax.tools.JavaFileObject

/**
 * Created by Allen on 2016/7/7.
 */
class ExtraKeyTest extends Specification {

    def "@ExtraKey on non-static field"() {
        setup:
        JavaFileObject source1 = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                '',
                'public class Test {',
                '  @Cacheable(value = "test", expired = 5000, extraKeys = "test")',
                '  public Observable<String> testMethod(@Key("testId") long id) {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject source2 = JavaFileObjects.forSourceLines('test.ExtraKeys',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class ExtraKeys {',
                '  @ExtraKey("test")',
                '  public String extraKey;',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  protected ExtraKeys mExtraKeysField;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache, ExtraKeys testExtraKeys) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '    this.mExtraKeysField = testExtraKeys;',
                '  }',
                '',
                '  @Override',
                '  public Observable<String> testMethod(long id) {',
                '    String testMethodType = "test";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test", mExtraKeysField.extraKey);',
                '    testMethodKeyBuilder.add("testExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    testMethodKeyBuilder.add("testId", id);',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 5000;',
                '    int testMethodStrategy = 0;',
                '    Observable<String> testMethodSource = super.testMethod(id);',
                '    return CacheUtils.getObjectCacheableObservable(mTestCache, testMethodType, testMethodKey, testMethodExpired, ' +
                        'testMethodStrategy, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources()).that([source1, source2]).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }

    def "@ExtraKey on static field and with sub type"() {
        setup:
        JavaFileObject source1 = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                'import java.util.List;',
                'import me.allenzjl.rxcache.compiler.TestEntity;',
                '',
                'public class Test {',
                '  @Cacheable(subType = "testEntity", extraKeys = "test")',
                '  public Observable<List<TestEntity>> testMethod() {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject source2 = JavaFileObjects.forSourceLines('test.ExtraKeys',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class ExtraKeys {',
                '  @ExtraKey("test")',
                '  private static String extraKey;',
                '',
                '  public static String getExtraKey() {',
                '    return extraKey;',
                '  }',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import java.util.List;',
                'import me.allenzjl.rxcache.compiler.TestEntity;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                'import rx.functions.Func1;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '  }',
                '',
                '  @Override',
                '  public Observable<List<TestEntity>> testMethod() {',
                '    String testMethodType = "testMethod";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test", ExtraKeys.getExtraKey());',
                '    testMethodKeyBuilder.add("testMethodExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 0;',
                '    int testMethodStrategy = 0;',
                '    Observable<List<TestEntity>> testMethodSource = super.testMethod();',
                '    String testMethodSubType = "testEntity";',
                '    Func1<TestEntity, String> testMethodKeyMapper = new Func1<TestEntity, String>() {',
                '      @Override',
                '      public String call(TestEntity testMethodValue) {',
                '        JSONProcessor.Builder testMethodSubKeyBuilder = JSONProcessor.newBuilder();',
                '        JSONProcessor.Builder testMethodSubExtraKeysBuilder = JSONProcessor.newBuilder();',
                '        testMethodSubExtraKeysBuilder.add("test", ExtraKeys.getExtraKey());',
                '        testMethodSubKeyBuilder.add("testEntityExtraKeys", testMethodSubExtraKeysBuilder.toObject());',
                '        testMethodSubKeyBuilder.add("key", testMethodValue.getKey());',
                '        testMethodSubKeyBuilder.add("k3", testMethodValue.mKey3);',
                '        testMethodSubKeyBuilder.add("isKey2", testMethodValue.isKey2());',
                '        return testMethodSubKeyBuilder.build();',
                '      }',
                '    };',
                '    return CacheUtils.getListCacheableObservable(mTestCache, testMethodType, testMethodKey,' +
                        ' testMethodExpired, testMethodStrategy, testMethodSubType, testMethodKeyMapper, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources()).that([source1, source2]).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }

    def "@ExtraKey on static and non-static fields together"() {
        setup:
        JavaFileObject source1 = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                'import java.util.List;',
                'import me.allenzjl.rxcache.compiler.TestEntity;',
                '',
                'public class Test {',
                '  @Cacheable(subType = "testEntity", extraKeys = {"test", "test2"})',
                '  public Observable<List<TestEntity>> testMethod() {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject source2 = JavaFileObjects.forSourceLines('test.ExtraKeys',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class ExtraKeys {',
                '  @ExtraKey("test")',
                '  private static String extraKey;',
                '',
                '  public static String getExtraKey() {',
                '    return extraKey;',
                '  }',
                '',
                '  @ExtraKey("test2")',
                '  public String extraKey2;',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import java.util.List;',
                'import me.allenzjl.rxcache.compiler.TestEntity;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                'import rx.functions.Func1;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  protected ExtraKeys mExtraKeysField;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache, ExtraKeys testExtraKeys) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '    this.mExtraKeysField = testExtraKeys;',
                '  }',
                '',
                '  @Override',
                '  public Observable<List<TestEntity>> testMethod() {',
                '    String testMethodType = "testMethod";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test2", mExtraKeysField.extraKey2);',
                '    testMethodExtraKeysBuilder.add("test", ExtraKeys.getExtraKey());',
                '    testMethodKeyBuilder.add("testMethodExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 0;',
                '    int testMethodStrategy = 0;',
                '    Observable<List<TestEntity>> testMethodSource = super.testMethod();',
                '    String testMethodSubType = "testEntity";',
                '    Func1<TestEntity, String> testMethodKeyMapper = new Func1<TestEntity, String>() {',
                '      @Override',
                '      public String call(TestEntity testMethodValue) {',
                '        JSONProcessor.Builder testMethodSubKeyBuilder = JSONProcessor.newBuilder();',
                '        JSONProcessor.Builder testMethodSubExtraKeysBuilder = JSONProcessor.newBuilder();',
                '        testMethodSubExtraKeysBuilder.add("test2", mExtraKeysField.extraKey2);',
                '        testMethodSubExtraKeysBuilder.add("test", ExtraKeys.getExtraKey());',
                '        testMethodSubKeyBuilder.add("testEntityExtraKeys", testMethodSubExtraKeysBuilder.toObject());',
                '        testMethodSubKeyBuilder.add("key", testMethodValue.getKey());',
                '        testMethodSubKeyBuilder.add("k3", testMethodValue.mKey3);',
                '        testMethodSubKeyBuilder.add("isKey2", testMethodValue.isKey2());',
                '        return testMethodSubKeyBuilder.build();',
                '      }',
                '    };',
                '    return CacheUtils.getListCacheableObservable(mTestCache, testMethodType, testMethodKey,' +
                        ' testMethodExpired, testMethodStrategy, testMethodSubType, testMethodKeyMapper, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources()).that([source1, source2]).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }

    def "@ExtraKey on static and non-static methods together"() {
        setup:
        JavaFileObject source1 = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                'import java.util.List;',
                'import me.allenzjl.rxcache.compiler.TestEntity;',
                '',
                'public class Test {',
                '  @Cacheable(subType = "testEntity", extraKeys = {"test", "test2"})',
                '  public Observable<List<TestEntity>> testMethod() {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject source2 = JavaFileObjects.forSourceLines('test.ExtraKeys',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class ExtraKeys {',
                '  @ExtraKey("test")',
                '  public static String getExtraKey() {',
                '    return null;',
                '  }',
                '',
                '  @ExtraKey("test2")',
                '  public String extraKey2() {',
                '     return null;',
                '  }',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import java.util.List;',
                'import me.allenzjl.rxcache.compiler.TestEntity;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                'import rx.functions.Func1;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  protected ExtraKeys mExtraKeysField;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache, ExtraKeys testExtraKeys) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '    this.mExtraKeysField = testExtraKeys;',
                '  }',
                '',
                '  @Override',
                '  public Observable<List<TestEntity>> testMethod() {',
                '    String testMethodType = "testMethod";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test2", mExtraKeysField.extraKey2());',
                '    testMethodExtraKeysBuilder.add("test", ExtraKeys.getExtraKey());',
                '    testMethodKeyBuilder.add("testMethodExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 0;',
                '    int testMethodStrategy = 0;',
                '    Observable<List<TestEntity>> testMethodSource = super.testMethod();',
                '    String testMethodSubType = "testEntity";',
                '    Func1<TestEntity, String> testMethodKeyMapper = new Func1<TestEntity, String>() {',
                '      @Override',
                '      public String call(TestEntity testMethodValue) {',
                '        JSONProcessor.Builder testMethodSubKeyBuilder = JSONProcessor.newBuilder();',
                '        JSONProcessor.Builder testMethodSubExtraKeysBuilder = JSONProcessor.newBuilder();',
                '        testMethodSubExtraKeysBuilder.add("test2", mExtraKeysField.extraKey2());',
                '        testMethodSubExtraKeysBuilder.add("test", ExtraKeys.getExtraKey());',
                '        testMethodSubKeyBuilder.add("testEntityExtraKeys", testMethodSubExtraKeysBuilder.toObject());',
                '        testMethodSubKeyBuilder.add("key", testMethodValue.getKey());',
                '        testMethodSubKeyBuilder.add("k3", testMethodValue.mKey3);',
                '        testMethodSubKeyBuilder.add("isKey2", testMethodValue.isKey2());',
                '        return testMethodSubKeyBuilder.build();',
                '      }',
                '    };',
                '    return CacheUtils.getListCacheableObservable(mTestCache, testMethodType, testMethodKey,' +
                        ' testMethodExpired, testMethodStrategy, testMethodSubType, testMethodKeyMapper, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources()).that([source1, source2]).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }

    def "@ExtraKey on two fields in one object"() {
        setup:
        JavaFileObject source1 = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                '',
                'public class Test {',
                '  @Cacheable(value = "test", expired = 5000, extraKeys = {"test", "test2"})',
                '  public Observable<String> testMethod(@Key("testId") long id) {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject source2 = JavaFileObjects.forSourceLines('test.ExtraKeys',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class ExtraKeys {',
                '  @ExtraKey("test")',
                '  public String extraKey;',
                '',
                '  @ExtraKey("test2")',
                '  public String extraKey2;',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  protected ExtraKeys mExtraKeysField;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache, ExtraKeys testExtraKeys) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '    this.mExtraKeysField = testExtraKeys;',
                '  }',
                '',
                '  @Override',
                '  public Observable<String> testMethod(long id) {',
                '    String testMethodType = "test";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test2", mExtraKeysField.extraKey2);',
                '    testMethodExtraKeysBuilder.add("test", mExtraKeysField.extraKey);',
                '    testMethodKeyBuilder.add("testExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    testMethodKeyBuilder.add("testId", id);',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 5000;',
                '    int testMethodStrategy = 0;',
                '    Observable<String> testMethodSource = super.testMethod(id);',
                '    return CacheUtils.getObjectCacheableObservable(mTestCache, testMethodType, testMethodKey, testMethodExpired, ' +
                        'testMethodStrategy, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources()).that([source1, source2]).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }

    def "@ExtraKey on field in same object and is Observable"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public Observable<Long> extraKey;',
                '',
                '  @Cacheable(value = "test", expired = 5000, extraKeys = "test")',
                '  public Observable<String> testMethod(@Key("testId") long id) {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '  }',
                '',
                '  @Override',
                '  public Observable<String> testMethod(long id) {',
                '    String testMethodType = "test";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test", this.extraKey.toBlocking().single());',
                '    testMethodKeyBuilder.add("testExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    testMethodKeyBuilder.add("testId", id);',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 5000;',
                '    int testMethodStrategy = 0;',
                '    Observable<String> testMethodSource = super.testMethod(id);',
                '    return CacheUtils.getObjectCacheableObservable(mTestCache, testMethodType, testMethodKey, testMethodExpired, ' +
                        'testMethodStrategy, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }

    def "@ExtraKey static field in same object"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.Cacheable;',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                'import me.allenzjl.rxcache.annotation.Key;',
                'import rx.Observable;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public static long extraKey;',
                '',
                '  @Cacheable(value = "test", expired = 5000, extraKeys = "test")',
                '  public Observable<String> testMethod(@Key("testId") long id) {',
                '    return null;',
                '  }',
                '}'
        )

        JavaFileObject output = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import me.allenzjl.rxcache.library.CacheRepository;',
                'import me.allenzjl.rxcache.library.CacheUtils;',
                'import me.allenzjl.rxcache.library.JSONProcessor;',
                'import rx.Observable;',
                '',
                'public class Test$$CacheableHolder extends Test {',
                '  protected CacheRepository mTestCache;',
                '',
                '  public Test$$CacheableHolder(CacheRepository testCache) {',
                '    super();',
                '    this.mTestCache = testCache;',
                '  }',
                '',
                '  @Override',
                '  public Observable<String> testMethod(long id) {',
                '    String testMethodType = "test";',
                '    JSONProcessor.Builder testMethodKeyBuilder = JSONProcessor.newBuilder();',
                '    JSONProcessor.Builder testMethodExtraKeysBuilder = JSONProcessor.newBuilder();',
                '    testMethodExtraKeysBuilder.add("test", Test.extraKey);',
                '    testMethodKeyBuilder.add("testExtraKeys", testMethodExtraKeysBuilder.toObject());',
                '    testMethodKeyBuilder.add("testId", id);',
                '    String testMethodKey = testMethodKeyBuilder.build();',
                '    long testMethodExpired = 5000;',
                '    int testMethodStrategy = 0;',
                '    Observable<String> testMethodSource = super.testMethod(id);',
                '    return CacheUtils.getObjectCacheableObservable(mTestCache, testMethodType, testMethodKey, testMethodExpired, ' +
                        'testMethodStrategy, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(output)
    }
}
