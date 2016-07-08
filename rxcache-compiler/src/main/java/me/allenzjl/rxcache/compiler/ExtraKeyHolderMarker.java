package me.allenzjl.rxcache.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

import me.allenzjl.rxcache.annotation.ExtraKey;

/**
 * Created by Allen on 2016/7/7.
 */

public class ExtraKeyHolderMarker {

    protected Map<String, ExtraKeyMarker> extraKeyMap;

    public ExtraKeyHolderMarker() {
        extraKeyMap = new LinkedHashMap<>();
    }

    public void addExtraKey(TypeElement classElement, Element element) {
        String classQualifiedName = ProcessUtils.getTypeQualifiedName(classElement);
        ExtraKey extraKeyAnno = element.getAnnotation(ExtraKey.class);
        String extraKeyName = extraKeyAnno.value();
        if (extraKeyMap.containsKey(extraKeyName)) {
            throw new ProcessException("Extra key name '" + extraKeyName + "' is duplicate.", element);
        }
        if (element.getKind() == ElementKind.METHOD || element.getModifiers().contains(Modifier.PUBLIC)) {
            extraKeyMap.put(extraKeyName, new ExtraKeyMarker(classQualifiedName, element, extraKeyName));
        } else {
            ExecutableElement accessor = ProcessUtils.findFieldAccessor(classElement, (VariableElement) element);
            extraKeyMap.put(extraKeyName, new ExtraKeyMarker(classQualifiedName, accessor, extraKeyName));
        }
    }

    public Map<String, ExtraKeyMarker> getExtraKeyMap() {
        return extraKeyMap;
    }
}
