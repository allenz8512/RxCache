package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

/**
 * Created by Allen on 2016/7/1.
 */

public class KeyMarker {

    protected Element element;

    protected String keyName;

    protected TypeName typeName;

    protected String name;

    public KeyMarker(Element element, String keyName) {
        this.element = element;
        this.keyName = keyName;
        if (element instanceof ExecutableElement) {
            typeName = TypeName.get(((ExecutableElement) element).getReturnType());
        } else {
            typeName = TypeName.get(element.asType());
        }
        name = element.getSimpleName().toString();
    }

    public Element getElement() {
        return element;
    }

    public String getKeyName() {
        return keyName;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public String getName() {
        return name;
    }
}
