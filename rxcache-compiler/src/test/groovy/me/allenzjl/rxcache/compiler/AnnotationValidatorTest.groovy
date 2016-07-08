package me.allenzjl.rxcache.compiler

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import spock.lang.Specification

import javax.tools.JavaFileObject

/**
 * Created by Allen on 2016/7/7.
 */
class AnnotationValidatorTest extends Specification {

    def "@ExtraKey on legal method 1"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public String getString() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError()
    }

    def "@ExtraKey on legal method 2"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public static final String getString() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor())
                .compilesWithoutError()
    }

    def "@ExtraKey on return void method"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public void method() {',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on return Void method"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public Void method() {',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on return TypeVar method 1"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public <T> T method() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on return TypeVar method 2"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import java.util.List;',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public <T> List<T> method() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on legal field 1"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public String mField;',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).compilesWithoutError()
    }

    def "@ExtraKey on legal field 2"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public static final String mField = null;',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).compilesWithoutError()
    }


    def "@ExtraKey on private field with get accessor"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  String mField;',
                '',
                '  public String getField() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).compilesWithoutError()
    }

    def "@ExtraKey on private static field with get accessor"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  static String mField;',
                '',
                '  public static String getField() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).compilesWithoutError()
    }

    def "@ExtraKey on private field"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  static String mField;',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on private field with private accessor"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  String mField;',
                '',
                '  String getField() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on non-static private field with static private accessor"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  String mField;',
                '',
                '  public static String getField() {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on private field with public accessor with parameters"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  String mField;',
                '',
                '  public String getField(String field) {',
                '    return null;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on private field with public accessor with different return type"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  String mField;',
                '',
                '  public int getField() {',
                '    return 0;',
                '  }',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }

    def "@ExtraKey on field is 'Void' type"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public Void mField;',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }
}
