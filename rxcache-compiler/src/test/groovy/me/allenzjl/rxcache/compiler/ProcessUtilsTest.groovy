package me.allenzjl.rxcache.compiler

import spock.lang.Specification

/**
 * Created by Allen on 2016/7/1.
 */
class ProcessUtilsTest extends Specification {

    void setup() {
        ProcessUtils.init()
    }

    def "SplitPackageNameAndTypeName"() {
        when:
        def names = ProcessUtils.splitPackageNameAndTypeName('me.allenzjl.rxcache.Test$$CacheableHolder')

        then:
        names.length == 2
        names[0] == 'me.allenzjl.rxcache'
        names[1] == 'Test$$CacheableHolder'
    }

}
