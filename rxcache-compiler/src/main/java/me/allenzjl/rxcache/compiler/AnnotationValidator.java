package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import me.allenzjl.rxcache.annotation.Cacheable;
import me.allenzjl.rxcache.annotation.ExtraKey;

/**
 * 注解校验辅助类，缩窄注解的使用范围。
 *
 * @author Allenz
 * @since 0.1.0
 */
public class AnnotationValidator {

    public static void verifyCacheable(Element e) throws ProcessException {
        ExecutableElement methodElement = (ExecutableElement) e;
        if (!methodElement.getModifiers().contains(Modifier.PUBLIC) &&
                !methodElement.getModifiers().contains(Modifier.PROTECTED)) {
            throw new ProcessException("Method annotated by @Cacheable should be qualified by 'public' or 'protected'.", e);
        }
        if (methodElement.getModifiers().contains(Modifier.STATIC)) {
            throw new ProcessException("Method annotated by @Cacheable should not be qualified by 'static'.", e);
        }
        if (methodElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessException("Method annotated by @Cacheable should not be qualified by 'abstract'.", e);
        }
        if (methodElement.getModifiers().contains(Modifier.FINAL)) {
            throw new ProcessException("Method annotated by @Cacheable should not be qualified by 'final'.", e);
        }
        TypeMirror returnType = methodElement.getReturnType();
        TypeName returnTypeName = TypeName.get(returnType);
        if (returnTypeName instanceof ParameterizedTypeName) {
            ClassName rawTypeName = ((ParameterizedTypeName) returnTypeName).rawType;
            if (!rawTypeName.equals(Utils.RX_OBSERVABLE_TYPE_NAME)) {
                throw new ProcessException("Return type of method annotated by @Cacheable should be 'rx.Observable'.", e);
            }
            TypeName genericTypeName = ((ParameterizedTypeName) returnTypeName).typeArguments.get(0);
            if (genericTypeName.equals(TypeName.VOID.box())) {
                throw new ProcessException("Return type of method annotated by @Cacheable should not be 'rx.Observable<Void>'.",
                        e);
            }
            if (genericTypeName instanceof TypeVariableName || genericTypeName instanceof WildcardTypeName) {
                throw new ProcessException("Return type of method annotated by @Cacheable should not be 'rx.Observable<?>' or " +
                        "'rx.Observable<T>'.", e);
            }
            Cacheable cacheable = methodElement.getAnnotation(Cacheable.class);
            if (!Utils.isStringEmpty(cacheable.subType())) {
                if (genericTypeName instanceof ParameterizedTypeName) {
                    TypeElement rawTypeElement = ProcessUtils.getTypeElement(((ParameterizedTypeName) genericTypeName).rawType);
                    if (!ProcessUtils.isAssignable(rawTypeElement, "java.util.List") &&
                            !ProcessUtils.isAssignable(rawTypeElement, "java.util.Map")) {
                        throw new ProcessException("Method annotated by @Cacheable and 'subType' is not empty should return " +
                                "'Observable<List<T>>', 'Observable<Map<K,V>' or 'Observable<T[]>'.", e);
                    } else if (ProcessUtils.isAssignable(rawTypeElement, "java.util.Map")) {
                        TypeName keyTypeName = ((ParameterizedTypeName) genericTypeName).typeArguments.get(0);
                        if (!keyTypeName.equals(ClassName.get("java.lang", "Integer")) &&
                                !keyTypeName.equals(ClassName.get("java.lang", "Long")) &&
                                !keyTypeName.equals(ClassName.get("java.lang", "String"))) {
                            throw new ProcessException("Only support Map<Integer, V>, Map<Long, V> or Map<String, V> return " +
                                    "type when 'subType' is not empty.", e);
                        }
                    }
                } else {
                    if (!(genericTypeName instanceof ArrayTypeName)) {
                        throw new ProcessException("Method annotated by @Cacheable and 'subType' is not empty should return " +
                                "'Observable<List<T>>', 'Observable<Map<K,V>' or 'Observable<T[]>'.", e);
                    }
                }
            }
        } else {
            if (returnTypeName.equals(Utils.RX_OBSERVABLE_TYPE_NAME)) {
                throw new ProcessException(
                        "Return type of method annotated by @Cacheable should not be raw type 'rx.Observable'.", e);
            } else {
                throw new ProcessException("Return type of method annotated by @Cacheable should be 'rx.Observable'.", e);
            }
        }
    }

    public static void verifyCacheableHolder(Element e) throws ProcessException {
        if (e.getKind() != ElementKind.CLASS) {
            throw new ProcessException("Element has methods annotated by @Cacheable should be a class.", e);
        }
        TypeElement classElement = (TypeElement) e;
        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ProcessException("Class has method annotated by @Cacheable should be qualified by 'public'.", e);
        }
        if (classElement.getModifiers().contains(Modifier.FINAL)) {
            throw new ProcessException("Class has method annotated by @Cacheable should not be qualified by 'final'.", e);
        }
        if (classElement.getNestingKind().isNested()) {
            throw new ProcessException("Class has method annotated by @Cacheable should not be nested.", e);
        }
        List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        int inheritableConstructorCount = 0;
        int uninheritableConstructorCount = 0;
        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.CONSTRUCTOR) {
                Set<Modifier> modifiers = element.getModifiers();
                if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED)) {
                    inheritableConstructorCount++;
                } else {
                    uninheritableConstructorCount++;
                }
            }
        }
        if (uninheritableConstructorCount > 0 && inheritableConstructorCount == 0) {
            throw new ProcessException(
                    "Class has methods annotated by @Cacheable should have 'public' or 'protected' constructor.", e);
        }
    }

    public static void verifyExtraKeyHolder(Element e) throws ProcessException {
        if (e.getKind() != ElementKind.CLASS) {
            throw new ProcessException("Element has sub element annotated by @ExtraKey should be a class.", e);
        }
        if (!e.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ProcessException("Class has sub element annotated by @ExtraKey should be qualified by 'public'.", e);
        }
    }

    public static void verifyExtraKey(Element e) throws ProcessException {
        ExtraKey extraKey = e.getAnnotation(ExtraKey.class);
        if (Utils.isStringEmpty(extraKey.value())) {
            throw new ProcessException("'Value' on @ExtraKey should not be empty.", e);
        }
        if (e.getKind() == ElementKind.FIELD) {
            VariableElement fieldElement = (VariableElement) e;
            TypeName returnTypeName = TypeName.get(fieldElement.asType());
            if (returnTypeName.equals(ClassName.get("java.lang", "Void"))) {
                throw new ProcessException("Field annotated by @ExtraKey should not be 'Void' type.", e);
            }
            if (!e.getModifiers().contains(Modifier.PUBLIC)) {
                TypeElement classElement = ((TypeElement) fieldElement.getEnclosingElement());
                boolean isStatic = e.getModifiers().contains(Modifier.STATIC);
                ExecutableElement getAccessor = ProcessUtils.findFieldAccessor(classElement, fieldElement);
                if (getAccessor == null || getAccessor.getModifiers().contains(Modifier.STATIC) != isStatic) {
                    throw new ProcessException(
                            "Field annotated by @ExtraKey is not qualified by 'public' and no get accessor found.", e);
                }
            }
        } else {
            if (!e.getModifiers().contains(Modifier.PUBLIC)) {
                throw new ProcessException("Method annotated by @ExtraKey should be qualified by 'public'.", e);
            }
            ExecutableElement methodElement = (ExecutableElement) e;
            if (!methodElement.getParameters().isEmpty()) {
                throw new ProcessException("Method annotated by @ExtraKey should be have any parameters.", e);
            }
            TypeName returnTypeName = TypeName.get(methodElement.getReturnType());
            if (returnTypeName.equals(TypeName.VOID) || returnTypeName.equals(ClassName.get("java.lang", "Void"))) {
                throw new ProcessException("Method annotated by @ExtraKey should not return 'void'.", e);
            }
            if (!methodElement.getTypeParameters().isEmpty()) {
                throw new ProcessException("Method annotated by @ExtraKey should return a specify type.", e);
            }
        }
    }
}
