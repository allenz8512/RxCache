package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

/**
 * Created by Allen on 2016/7/7.
 */

public class ExtraKeyMarker {

    protected Element element;

    protected String extraKeyName;

    protected String classQualifiedName;

    protected String name;

    protected TypeName typeName;

    protected boolean isStatic;

    protected ClassName classTypeName;

    private boolean isField;

    public ExtraKeyMarker(String classQualifiedName, Element element, String extraKeyName) {
        this.element = element;
        this.extraKeyName = extraKeyName;
        this.classQualifiedName = classQualifiedName;
        this.name = element.getSimpleName().toString();
        if (element instanceof ExecutableElement) {
            typeName = TypeName.get(((ExecutableElement) element).getReturnType());
        } else {
            typeName = TypeName.get(element.asType());
        }
        isStatic = element.getModifiers().contains(Modifier.STATIC);
        classTypeName = ProcessUtils.getClassTypeName(classQualifiedName);
        isField = element.getKind() == ElementKind.FIELD;
    }

    public Element getElement() {
        return element;
    }

    public String getExtraKeyName() {
        return extraKeyName;
    }

    public String getClassQualifiedName() {
        return classQualifiedName;
    }

    public String getName() {
        return name;
    }

    public TypeName getTypeName() {
        return typeName;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public ClassName getClassTypeName() {
        return classTypeName;
    }

    public boolean isField() {
        return isField;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ExtraKeyMarker that = (ExtraKeyMarker) o;

        return extraKeyName != null ? extraKeyName.equals(that.extraKeyName) : that.extraKeyName == null;

    }

    @Override
    public int hashCode() {
        return extraKeyName != null ? extraKeyName.hashCode() : 0;
    }
}
