package me.allenzjl.rxcache.compiler

import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import spock.lang.Specification

import javax.tools.JavaFileObject

/**
 * Created by Allen on 2016/7/7.
 */
class ExtraKeyHolderMarkerTest extends Specification {

    def "duplicate extra keys"() {
        setup:
        JavaFileObject source = JavaFileObjects.forSourceLines('test.Test',
                'package test;',
                '',
                'import me.allenzjl.rxcache.annotation.ExtraKey;',
                '',
                'public class Test {',
                '  @ExtraKey("test")',
                '  public String mField1;',
                '',
                '  @ExtraKey("test")',
                '  public String mField2;',
                '}'
        )

        expect:
        Truth.assertAbout(JavaSourceSubjectFactory.javaSource()).that(source).processedWith(new Processor()).failsToCompile()
    }
}
