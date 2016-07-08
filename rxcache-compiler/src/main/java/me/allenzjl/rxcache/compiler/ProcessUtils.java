package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic;

/**
 * 注解处理帮助类
 */
public class ProcessUtils {

    public static final ClassName OVERRIDE = ClassName.get(Override.class);

    private static ProcessingEnvironment processingEnv;

    public static void init(ProcessingEnvironment processingEnv) {
        ProcessUtils.processingEnv = processingEnv;
    }

    public static ProcessingEnvironment getProcessingEnv() {
        return processingEnv;
    }

    /**
     * 返回一个覆盖了方法的构建器。
     *
     * @param method 方法元素
     * @return 方法构建器
     */
    public static MethodSpec.Builder overrideMethod(ExecutableElement method) {
        if (method == null) {
            throw new NullPointerException("method == null");
        }

        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.FINAL) || modifiers.contains(Modifier.STATIC)) {
            throw new IllegalArgumentException("cannot override method with modifiers: " + modifiers);
        }

        String methodName = method.getSimpleName().toString();
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

        methodBuilder.addAnnotation(OVERRIDE);

        modifiers = new LinkedHashSet<>(modifiers);
        modifiers.remove(Modifier.ABSTRACT);
        methodBuilder.addModifiers(modifiers);

        for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
            TypeVariable var = (TypeVariable) typeParameterElement.asType();
            methodBuilder.addTypeVariable(TypeVariableName.get(var));
        }

        methodBuilder.returns(TypeName.get(method.getReturnType()));

        List<? extends VariableElement> parameters = method.getParameters();
        for (VariableElement parameter : parameters) {
            TypeName type = TypeName.get(parameter.asType());
            String name = parameter.getSimpleName().toString();
            Set<Modifier> parameterModifiers = parameter.getModifiers();
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, name)
                    .addModifiers(parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
            methodBuilder.addParameter(parameterBuilder.build());
        }
        methodBuilder.varargs(method.isVarArgs());

        for (TypeMirror thrownType : method.getThrownTypes()) {
            methodBuilder.addException(TypeName.get(thrownType));
        }

        return methodBuilder;
    }

    public static MethodSpec.Builder inheritConstructor(ExecutableElement constructor, ParameterMeta... extraFields) {
        if (constructor == null) {
            throw new IllegalArgumentException("Parameter 'constructor' should not be null.");
        }
        if (constructor.getKind() != ElementKind.CONSTRUCTOR) {
            throw new IllegalArgumentException("Kind of parameter 'constructor' should be ElementKind.CONSTRUCTOR.");
        }
        Set<Modifier> modifiers = constructor.getModifiers();
        if (!modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.PROTECTED)) {
            throw new IllegalArgumentException("Cannot access super constructor without modifier 'public' or 'protected'.");
        }

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        constructorBuilder.addModifiers(modifiers);

        List<? extends VariableElement> parameters = constructor.getParameters();
        int size = parameters.size();
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < size; i++) {
            VariableElement parameter = parameters.get(i);
            TypeName type = TypeName.get(parameter.asType());
            String name = parameter.getSimpleName().toString();
            Set<Modifier> parameterModifiers = parameter.getModifiers();
            ParameterSpec.Builder paramBuilder =
                    ParameterSpec.builder(type, name, parameterModifiers.toArray(new Modifier[parameterModifiers.size()]));
            constructorBuilder.addParameter(paramBuilder.build());
            params.append(name);
            if (i < size - 1) {
                params.append(", ");
            }
        }

        constructorBuilder.addStatement("super($N)", params.toString());

        for (ParameterMeta paramMeta : extraFields) {
            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramMeta.typeName, paramMeta.name);
            if (paramMeta.finalModifier) {
                paramBuilder.addModifiers(Modifier.FINAL);
            }
            constructorBuilder.addParameter(paramBuilder.build());

            constructorBuilder.addStatement("this.$N = $N", paramMeta.fieldName, paramMeta.name);
        }

        return constructorBuilder;
    }

    public static String getTypePackageName(TypeElement typeElement) {
        return ProcessUtils.getProcessingEnv().getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
    }

    public static String getTypeQualifiedName(TypeElement typeElement) {
        String packageName = getTypePackageName(typeElement);
        String typeName = typeElement.getSimpleName().toString();
        return packageName + "." + typeName;
    }

    public static String[] splitPackageNameAndTypeName(String qualifiedName) {
        int index = qualifiedName.lastIndexOf(".");
        String packageName = qualifiedName.substring(0, index);
        String typeName = qualifiedName.substring(index + 1, qualifiedName.length());
        return new String[]{packageName, typeName};
    }

    public static String getExecutableSignature(ExecutableElement executableElement) {
        StringBuilder signature = new StringBuilder();
        List<? extends VariableElement> parameters = executableElement.getParameters();
        int size = parameters.size();
        for (int i = 0; i < size; i++) {
            VariableElement parameter = parameters.get(i);
            String nameOfType = TypeName.get(parameter.asType()).toString();
            signature.append(nameOfType);
            if (i < size - 1) {
                signature.append(",");
            }
        }
        return signature.toString();
    }

    public static String getCallExecutableString(ExecutableElement executableElement) {
        StringBuilder sb = new StringBuilder(executableElement.getSimpleName().toString());
        sb.append("(");
        List<? extends VariableElement> parameters = executableElement.getParameters();
        int size = parameters.size();
        for (int i = 0; i < size; i++) {
            VariableElement parameter = parameters.get(i);
            sb.append(parameter.getSimpleName().toString());
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    public static boolean isAssignable(TypeElement typeElement, String classCanonicalName) {
        TypeMirror t1 = typeElement.asType();
        TypeMirror t2 = processingEnv.getElementUtils().getTypeElement(classCanonicalName).asType();
        return processingEnv.getTypeUtils().isAssignable(t1, t2);
    }

    public static String getClassCanonicalName(ClassName className) {
        String packageName = className.packageName();
        List<String> names = className.simpleNames();
        StringBuilder canonicalName = new StringBuilder(packageName);
        for (String name : names) {
            canonicalName.append(".").append(name);
        }
        return canonicalName.toString();
    }

    public static ClassName getClassTypeName(String classQualifiedName) {
        String[] names = splitPackageNameAndTypeName(classQualifiedName);
        return ClassName.get(names[0], names[1]);
    }

    public static TypeElement getTypeElement(ClassName className) {
        return processingEnv.getElementUtils().getTypeElement(getClassCanonicalName(className));
    }

    public static ExecutableElement findFieldAccessor(TypeElement classElement, VariableElement fieldElement) {
        List<? extends Element> enclosedElements = classElement.getEnclosedElements();
        TypeName fieldTypeName = TypeName.get(fieldElement.asType());
        String accessorPrefix = "get";
        if (fieldTypeName.equals(TypeName.BOOLEAN) || fieldTypeName.equals(ClassName.get("java.lang", "Boolean"))) {
            accessorPrefix = "is";
        }
        String fieldName = getFieldNameWithoutMPrefix(fieldElement);
        String targetMethodName = accessorPrefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        for (Element element : enclosedElements) {
            if (element.getKind() == ElementKind.METHOD) {
                if (element.getSimpleName().toString().equals(targetMethodName) &&
                        element.getModifiers().contains(Modifier.PUBLIC)) {
                    ExecutableElement methodElement = (ExecutableElement) element;
                    if (TypeName.get(methodElement.getReturnType()).equals(fieldTypeName) &&
                            methodElement.getParameters().isEmpty()) {
                        return methodElement;
                    }
                }
            }
        }
        return null;
    }

    public static String getFieldNameWithoutMPrefix(VariableElement fieldElement) {
        String fieldName = fieldElement.getSimpleName().toString();
        if (fieldName.length() > 1 && fieldName.charAt(0) == 'm' && !Character.isLowerCase(fieldName.charAt(1))) {
            return fieldName.substring(1, 2).toLowerCase() + fieldName.substring(2);
        } else {
            return fieldName;
        }
    }

    public static void createJavaFile(String packageName, TypeSpec typeSpec) throws ProcessException {
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
        try {
            javaFile.writeTo(ProcessUtils.getProcessingEnv().getFiler());
        } catch (IOException e) {
            throw new ProcessException(e, null);
        }
    }

    public static void print(String message) {
        print(message, null);
    }

    public static void print(String message, Element e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message, e);
    }

    public static void printError(String message, Element e) throws ProcessException {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, e);
    }
}
