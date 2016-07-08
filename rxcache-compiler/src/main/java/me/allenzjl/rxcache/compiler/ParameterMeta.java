package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.TypeName;

/**
 * Created by Allen on 2016/7/2.
 */

public class ParameterMeta {

    public final TypeName typeName;

    public final String name;

    public final String fieldName;

    public final boolean finalModifier;

    public ParameterMeta(TypeName typeName, String name, String fieldName, boolean finalModifier) {
        this.typeName = typeName;
        this.name = name;
        this.fieldName = fieldName;
        this.finalModifier = finalModifier;
    }
}
