package me.allenzjl.rxcache.compiler

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import spock.lang.Specification

import javax.tools.JavaFileObject

/**
 * Created by Allen on 2016/6/30.
 */
class ProcessTest extends Specification {

    def "legal cacheable method and class"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import me.allenzjl.rxcache.annotation.Key;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable(value = \"test\", expired = 5000)\n"
                + "  public Observable<String> testMethod(@Key(\"testId\") long id) {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )
        JavaFileObject cacheableHolderSource = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
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
                .compilesWithoutError().and().generatesSources(cacheableHolderSource)
    }

    def "cacheable method is default"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method is private"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  private Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method is static"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public static Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method is abstract"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public abstract Observable<String> testMethod();\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method is final"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public final Observable<String> testMethod();\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "return type of cacheable method is not Observable 1"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public String testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "return type of cacheable method is not Observable 2"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import java.util.List;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public List<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "return type of cacheable method is raw type Observable"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public Observable testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "return type of cacheable method is Observable<?>"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public Observable<?> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "return type of cacheable method is Observable<T>"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public <T> Observable<T> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "return type of cacheable method is Observable<Void>"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable\n"
                + "  public Observable<Void> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method's parent kind is not class"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public interface Test {\n"
                + "  @Cacheable\n"
                + "  Observable<String> testMethod();\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method's parent is not public"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "class Test {\n"
                + "  @Cacheable\n"
                + "  public Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method's parent is final"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public final class Test {\n"
                + "  @Cacheable\n"
                + "  public Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method's parent is nested"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  public static class Test2 {\n"
                + "    @Cacheable\n"
                + "    public Observable<String> testMethod() {\n"
                + "      return null;\n"
                + "    }\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "cacheable method's parent has no public constructor"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  private Test() {\n"
                + "  }\n"
                + "  @Cacheable\n"
                + "  public Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "with subType isn't empty but return type isn't Observable<List<>> or Observable<Map<,>> 1"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "public class Test {\n"
                + "  @Cacheable(subType = true)\n"
                + "  public Observable<String> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "with subType isn't empty but return type isn't Observable<List<>> or Observable<Map<,>> 2"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "import java.util.List;\n"
                + "public class Test {\n"
                + "  @Cacheable(subType = true)\n"
                + "  public Observable<List> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "with subType isn't empty and return type is Observable<List<>>"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "import java.util.List;\n"
                + "import me.allenzjl.rxcache.compiler.TestEntity;\n"
                + "public class Test {\n"
                + "  @Cacheable(subType = \"testEntity\")\n"
                + "  public Observable<List<TestEntity>> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )
        JavaFileObject cacheableHolderSource = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
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
                '    String testMethodKey = "";',
                '    long testMethodExpired = 0;',
                '    int testMethodStrategy = 0;',
                '    Observable<List<TestEntity>> testMethodSource = super.testMethod();',
                '    String testMethodSubType = "testEntity";',
                '    Func1<TestEntity, String> testMethodKeyMapper = new Func1<TestEntity, String>() {',
                '      @Override',
                '      public String call(TestEntity testMethodValue) {',
                '        JSONProcessor.Builder testMethodSubKeyBuilder = JSONProcessor.newBuilder();',
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
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(cacheableHolderSource)
    }

    def "with subType isn't empty and return type is Observable<Map<,>>"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "import java.util.Map;\n"
                + "import me.allenzjl.rxcache.compiler.TestEntity;\n"
                + "public class Test {\n"
                + "  @Cacheable(subType = \"testEntity\")\n"
                + "  public Observable<Map<Integer, TestEntity>> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )
        JavaFileObject cacheableHolderSource = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Integer;',
                'import java.lang.Override;',
                'import java.lang.String;',
                'import java.util.Map;',
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
                '  public Observable<Map<Integer, TestEntity>> testMethod() {',
                '    String testMethodType = "testMethod";',
                '    String testMethodKey = "";',
                '    long testMethodExpired = 0;',
                '    int testMethodStrategy = 0;',
                '    Observable<Map<Integer, TestEntity>> testMethodSource = super.testMethod();',
                '    String testMethodSubType = "testEntity";',
                '    Func1<TestEntity, String> testMethodKeyMapper = new Func1<TestEntity, String>() {',
                '      @Override',
                '      public String call(TestEntity testMethodValue) {',
                '        JSONProcessor.Builder testMethodSubKeyBuilder = JSONProcessor.newBuilder();',
                '        testMethodSubKeyBuilder.add("key", testMethodValue.getKey());',
                '        testMethodSubKeyBuilder.add("k3", testMethodValue.mKey3);',
                '        testMethodSubKeyBuilder.add("isKey2", testMethodValue.isKey2());',
                '        return testMethodSubKeyBuilder.build();',
                '      }',
                '    };',
                '    return CacheUtils.getMapCacheableObservable(mTestCache, testMethodType, testMethodKey,' +
                        ' testMethodExpired, testMethodStrategy, testMethodSubType, testMethodKeyMapper, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(cacheableHolderSource)
    }

    def "with subType isn't empty and return type is Observable<[]>"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceString("test.Test", ""
                + "package test;\n"
                + "import me.allenzjl.rxcache.annotation.Cacheable;\n"
                + "import rx.Observable;\n"
                + "import me.allenzjl.rxcache.compiler.TestEntity;\n"
                + "public class Test {\n"
                + "  @Cacheable(subType = \"testEntity\")\n"
                + "  public Observable<TestEntity[]> testMethod() {\n"
                + "    return null;\n"
                + "  }\n"
                + "}"
        )
        JavaFileObject cacheableHolderSource = JavaFileObjects.forSourceLines('test.Test$$CacheableHolder',
                'package test;',
                '',
                'import java.lang.Override;',
                'import java.lang.String;',
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
                '  public Observable<TestEntity[]> testMethod() {',
                '    String testMethodType = "testMethod";',
                '    String testMethodKey = "";',
                '    long testMethodExpired = 0;',
                '    int testMethodStrategy = 0;',
                '    Observable<TestEntity[]> testMethodSource = super.testMethod();',
                '    String testMethodSubType = "testEntity";',
                '    Func1<TestEntity, String> testMethodKeyMapper = new Func1<TestEntity, String>() {',
                '      @Override',
                '      public String call(TestEntity testMethodValue) {',
                '        JSONProcessor.Builder testMethodSubKeyBuilder = JSONProcessor.newBuilder();',
                '        testMethodSubKeyBuilder.add("key", testMethodValue.getKey());',
                '        testMethodSubKeyBuilder.add("k3", testMethodValue.mKey3);',
                '        testMethodSubKeyBuilder.add("isKey2", testMethodValue.isKey2());',
                '        return testMethodSubKeyBuilder.build();',
                '      }',
                '    };',
                '    return CacheUtils.getArrayCacheableObservable(mTestCache, testMethodType, testMethodKey,' +
                        ' testMethodExpired, testMethodStrategy, testMethodSubType, testMethodKeyMapper, testMethodSource);',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError().and().generatesSources(cacheableHolderSource)
    }


}
