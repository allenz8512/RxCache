package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import me.allenzjl.rxcache.annotation.Key;

/**
 * Created by Allen on 2016/7/1.
 */

public class CacheableMarker {

    public static final int NO_SUB_TYPE = 0;

    public static final int SUB_TYPE_ARRAY = 1;

    public static final int SUB_TYPE_LIST = 2;

    public static final int SUB_TYPE_MAP = 3;

    protected ExecutableElement methodElement;

    protected String classQualifiedName;

    protected String type;

    protected String subType;

    protected int subTypeKind;

    protected long expired;

    protected int strategy;

    protected String name;

    protected String signature;

    protected ParameterizedTypeName returnTypeName;

    protected Map<String, ExtraKeyMarker> extraKeyMarkers;

    protected TypeName genericReturnTypeName;

    protected ClassName subTypeTypeName;

    protected Map<String, KeyMarker> keys;

    protected Map<String, KeyMarker> subTypeKeys;

    public CacheableMarker(ExecutableElement methodElement, String classQualifiedName, String type, String subType, long expired,
                           int strategy, Map<String, ExtraKeyMarker> extraKeyMarkers) {
        this.methodElement = methodElement;
        this.classQualifiedName = classQualifiedName;
        this.type = type;
        this.subType = subType;
        this.expired = expired;
        this.strategy = strategy;
        this.extraKeyMarkers = extraKeyMarkers;
        name = methodElement.getSimpleName().toString();
        signature = ProcessUtils.getExecutableSignature(methodElement);
        returnTypeName = initReturnTypeName(methodElement);
        genericReturnTypeName = initGenericReturnTypeName(returnTypeName);
        keys = initKeys(methodElement);
        if (!Utils.isStringEmpty(subType)) {
            subTypeTypeName = initSubTypeTypeName(genericReturnTypeName);
            subTypeKeys = initSubTypeKeys(subTypeTypeName);
        }
    }

    protected ParameterizedTypeName initReturnTypeName(ExecutableElement methodElement) {
        TypeMirror returnType = methodElement.getReturnType();
        return (ParameterizedTypeName) TypeName.get(returnType);
    }

    protected TypeName initGenericReturnTypeName(ParameterizedTypeName returnTypeName) {
        return returnTypeName.typeArguments.get(0);
    }

    protected ClassName initSubTypeTypeName(TypeName genericReturnTypeName) {
        TypeName entityTypeName;
        if (genericReturnTypeName instanceof ParameterizedTypeName) {
            ClassName rawType = ((ParameterizedTypeName) genericReturnTypeName).rawType;
            TypeElement rawTypeElement = ProcessUtils.getTypeElement(rawType);
            if (ProcessUtils.isAssignable(rawTypeElement, "java.util.List")) { // 列表
                entityTypeName = ((ParameterizedTypeName) genericReturnTypeName).typeArguments.get(0);
                subTypeKind = SUB_TYPE_LIST;
            } else { // 映射
                entityTypeName = ((ParameterizedTypeName) genericReturnTypeName).typeArguments.get(1);
                subTypeKind = SUB_TYPE_MAP;
            }
        } else { // 数组
            entityTypeName = ((ArrayTypeName) genericReturnTypeName).componentType;
            subTypeKind = SUB_TYPE_ARRAY;
        }
        if (!(entityTypeName instanceof ClassName)) {
            throw new ProcessException("Sub type should be a raw class.", methodElement);
        }
        //noinspection ConstantConditions
        return (ClassName) entityTypeName;
    }

    protected Map<String, KeyMarker> initKeys(ExecutableElement methodElement) {
        List<? extends VariableElement> parameters = methodElement.getParameters();
        Map<String, KeyMarker> keys = new LinkedHashMap<>();
        int keyCount = 0;
        for (VariableElement parameter : parameters) {
            if (parameter.getAnnotation(Key.class) != null) {
                keyCount++;
            }
        }
        if (keyCount > 0) {
            for (VariableElement parameter : parameters) {
                Key keyAnno = parameter.getAnnotation(Key.class);
                if (keyAnno != null) {
                    String keyName = keyAnno.value();
                    addKey(keys, parameter, keyName);
                }
            }
        } else {
            for (VariableElement parameter : parameters) {
                addKey(keys, parameter, "");
            }
        }
        return keys;
    }

    protected Map<String, KeyMarker> initSubTypeKeys(ClassName subTypeTypeName) {
        Map<String, KeyMarker> keys = new LinkedHashMap<>();
        TypeElement typeElement = ProcessUtils.getTypeElement(subTypeTypeName);
        List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
        int keyCount = 0;
        for (Element element : enclosedElements) {
            Key keyAnno = element.getAnnotation(Key.class);
            if (keyAnno != null) {
                keyCount++;
                String keyName = keyAnno.value();
                if (element.getKind() == ElementKind.FIELD) { // 字段
                    if (element.getModifiers().contains(Modifier.STATIC)) {
                        throw new ProcessException("Field annotated by @Key should not be qualified by 'static'.", element);
                    } else if (element.getModifiers().contains(Modifier.PUBLIC)) {
                        addKey(keys, element, keyName);
                    } else {
                        Element methodElement = ProcessUtils.findFieldAccessor(typeElement, (VariableElement) element);
                        if (methodElement == null) {
                            throw new ProcessException(
                                    "Field annotated by @Key is not qualified by 'public' and it's get method not found.",
                                    element);
                        } else {
                            if (Utils.isStringEmpty(keyName)) {
                                addKey(keys, methodElement, ProcessUtils.getFieldNameWithoutMPrefix((VariableElement) element));
                            } else {
                                addKey(keys, methodElement, keyName);
                            }
                        }
                    }
                } else { // 方法
                    if (element.getModifiers().contains(Modifier.STATIC)) {
                        throw new ProcessException("Method annotated by @Key should not be qualified by 'static'.", element);
                    } else if (!element.getModifiers().contains(Modifier.PUBLIC)) {
                        throw new ProcessException("Method annotated by @Key should be qualified by 'public'.", element);
                    } else if (!((ExecutableElement) element).getParameters().isEmpty()) {
                        throw new ProcessException("Method annotated by @Key should not have any parameter.", element);
                    } else {
                        addKey(keys, element, keyName);
                    }
                }
            }
        }
        if (keyCount == 0) {
            throw new ProcessException("No @Key is inside the define of the generic return type.", methodElement);
        }
        return keys;
    }

    protected void addKey(Map<String, KeyMarker> keys, Element element, String keyName) {
        String name;
        if (Utils.isStringEmpty(keyName)) {
            if (element.getKind() == ElementKind.FIELD) {
                name = ProcessUtils.getFieldNameWithoutMPrefix((VariableElement) element);
            } else {
                name = element.getSimpleName().toString();
            }
        } else {
            name = keyName;
        }
        if (keys.containsKey(name)) {
            throw new ProcessException("Duplicated key name '" + name + "' found.", element);
        } else {
            keys.put(name, new KeyMarker(element, name));
        }
    }

    public ExecutableElement getMethodElement() {
        return methodElement;
    }

    public String getClassQualifiedName() {
        return classQualifiedName;
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public int getSubTypeKind() {
        return subTypeKind;
    }

    public long getExpired() {
        return expired;
    }

    public int getStrategy() {
        return strategy;
    }

    public Map<String, ExtraKeyMarker> getExtraKeyMarkers() {
        return extraKeyMarkers;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public ParameterizedTypeName getReturnTypeName() {
        return returnTypeName;
    }

    public TypeName getGenericReturnTypeName() {
        return genericReturnTypeName;
    }

    public ClassName getSubTypeTypeName() {
        return subTypeTypeName;
    }

    public Map<String, KeyMarker> getKeys() {
        return keys;
    }

    public Map<String, KeyMarker> getSubTypeKeys() {
        return subTypeKeys;
    }

    public MethodSpec generateMethod() {
        MethodSpec.Builder methodBuilder = ProcessUtils.overrideMethod(methodElement);
        // type local variable
        String typeVarName = name + "Type";
        String cacheType = Utils.isStringEmpty(type) ? name : type;
        methodBuilder.addStatement("String $N = $S", typeVarName, cacheType);
        // key local variable
        String keyVarName = name + "Key";
        if (keys.isEmpty() && extraKeyMarkers.isEmpty()) {
            methodBuilder.addStatement("String $N = \"\"", keyVarName);
        } else {
            String cacheKeyBuilderVarName = name + "KeyBuilder";
            methodBuilder.addStatement("$T $N = $T.newBuilder()", Utils.RX_JSON_BUILDER_TYPE_NAME, cacheKeyBuilderVarName,
                    Utils.RX_JSON_PROCESSER_TYPE_NAME);
            String extraKeysBuilderVarName = name + "ExtraKeysBuilder";
            if (!extraKeyMarkers.isEmpty()) {
                generateExtraKeys(methodBuilder, extraKeysBuilderVarName, extraKeyMarkers);
                String extraKeysKeyName = cacheType + "ExtraKeys";
                methodBuilder.addStatement("$N.add($S, $N.toObject())", cacheKeyBuilderVarName, extraKeysKeyName,
                        extraKeysBuilderVarName);
            }
            for (KeyMarker key : keys.values()) {
                methodBuilder.addStatement("$N.add($S, $N)", cacheKeyBuilderVarName, key.getKeyName(), key.getName());
            }
            methodBuilder.addStatement("String $N = $N.build()", keyVarName, cacheKeyBuilderVarName);
        }
        // expired local variable
        String expiredVarName = name + "Expired";
        methodBuilder.addStatement("long $N = $L", expiredVarName, expired);
        // strategy local variable
        String strategyVarName = name + "Strategy";
        methodBuilder.addStatement("int $N = $L", strategyVarName, strategy);
        // return cache observable
        String[] names = ProcessUtils.splitPackageNameAndTypeName(classQualifiedName);
        String cacheFieldName = Utils.getCacheRepositoryFieldName(names[0], names[1]);
        String callSuper = ProcessUtils.getCallExecutableString(methodElement);
        String sourceVarName = name + "Source";
        methodBuilder.addStatement("$T $N = super.$L", returnTypeName, sourceVarName, callSuper);
        if (subTypeKind == NO_SUB_TYPE) {
            methodBuilder.addStatement("return $T.getObjectCacheableObservable($N, $N, $N, $N, $N, $N)",
                    Utils.RX_CACHE_UTILS_TYPE_NAME, cacheFieldName, typeVarName, keyVarName, expiredVarName, strategyVarName,
                    sourceVarName);
        } else {
            String subTypeVarName = name + "SubType";
            methodBuilder.addStatement("String $N = $S", subTypeVarName, subType);

            TypeSpec.Builder keyMapperBuilder = TypeSpec.anonymousClassBuilder("");
            ParameterizedTypeName keyMapperTypeName =
                    ParameterizedTypeName.get(Utils.RX_FUNC1_TYPE_NAME, subTypeTypeName, Utils.STRING_TYPE_NAME);
            keyMapperBuilder.addSuperinterface(keyMapperTypeName);
            MethodSpec.Builder callBuilder = MethodSpec.methodBuilder("call");
            String valueVarName = name + "Value";
            callBuilder.addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).addParameter(subTypeTypeName, valueVarName)
                    .returns(Utils.STRING_TYPE_NAME);

            String subKeyBuilderVarName = name + "SubKeyBuilder";
            callBuilder.addStatement("$T $N = $T.newBuilder()", Utils.RX_JSON_BUILDER_TYPE_NAME, subKeyBuilderVarName,
                    Utils.RX_JSON_PROCESSER_TYPE_NAME);
            String extraKeysBuilderVarName = name + "SubExtraKeysBuilder";
            if (!extraKeyMarkers.isEmpty()) {
                generateExtraKeys(callBuilder, extraKeysBuilderVarName, extraKeyMarkers);
                String extraKeysKeyName = subType + "ExtraKeys";
                callBuilder.addStatement("$N.add($S, $N.toObject())", subKeyBuilderVarName, extraKeysKeyName,
                        extraKeysBuilderVarName);
            }
            for (KeyMarker key : subTypeKeys.values()) {
                if (key.getElement().getKind() == ElementKind.FIELD) {
                    callBuilder.addStatement("$N.add($S, $N.$N)", subKeyBuilderVarName, key.getKeyName(), valueVarName,
                            key.getName());
                } else {
                    callBuilder.addStatement("$N.add($S, $N.$N())", subKeyBuilderVarName, key.getKeyName(), valueVarName,
                            key.getName());
                }
            }
            callBuilder.addStatement("return $N.build()", subKeyBuilderVarName);

            keyMapperBuilder.addMethod(callBuilder.build());
            String keyMapperVarName = name + "KeyMapper";
            methodBuilder.addStatement("$T $N = $L", keyMapperTypeName, keyMapperVarName, keyMapperBuilder.build());
            String utilsMethodName;
            switch (subTypeKind) {
                case SUB_TYPE_ARRAY:
                    utilsMethodName = "getArrayCacheableObservable";
                    break;
                case SUB_TYPE_LIST:
                    utilsMethodName = "getListCacheableObservable";
                    break;
                case SUB_TYPE_MAP:
                    utilsMethodName = "getMapCacheableObservable";
                    break;
                default:
                    throw new IllegalStateException("Should not go here!");
            }
            methodBuilder
                    .addStatement("return $T.$N($N, $N, $N, $N, $N, $N, $N, $N)", Utils.RX_CACHE_UTILS_TYPE_NAME, utilsMethodName,
                            cacheFieldName, typeVarName, keyVarName, expiredVarName, strategyVarName, subTypeVarName,
                            keyMapperVarName, sourceVarName);
        }
        return methodBuilder.build();
    }

    protected void generateExtraKeys(MethodSpec.Builder methodBuilder, String extraKeysBuilderVarName,
                                     Map<String, ExtraKeyMarker> extraKeyMarkers) {
        methodBuilder.addStatement("$T $N = $T.newBuilder()", Utils.RX_JSON_BUILDER_TYPE_NAME, extraKeysBuilderVarName,
                Utils.RX_JSON_PROCESSER_TYPE_NAME);
        for (ExtraKeyMarker extraKey : extraKeyMarkers.values()) {
            String[] holderNames = ProcessUtils.splitPackageNameAndTypeName(extraKey.getClassQualifiedName());
            String packageName = holderNames[0];
            String className = holderNames[1];
            String fieldName = Utils.getExtraKeyHolderFieldName(packageName, className);

            Object objectParameter;
            StringBuilder statement = new StringBuilder();
            statement.append("$N.add($S, ");
            if (extraKey.isStatic()) {
                statement.append("$T");
                objectParameter = extraKey.getClassTypeName();
            } else {
                if (extraKey.getClassQualifiedName().equals(classQualifiedName)) {
                    statement.append("this");
                    objectParameter = null;
                } else {
                    statement.append("$N");
                    objectParameter = fieldName;
                }
            }
            statement.append(".$N");
            if (!extraKey.isField()) {
                statement.append("()");
            }
            if (extraKey.getTypeName() instanceof ParameterizedTypeName &&
                    ((ParameterizedTypeName) extraKey.getTypeName()).rawType.equals(Utils.RX_OBSERVABLE_TYPE_NAME)) {
                statement.append(".toBlocking().single()");
            }
            statement.append(")");

            if (extraKey.getClassQualifiedName().equals(classQualifiedName) && !extraKey.isStatic()) {
                methodBuilder.addStatement(statement.toString(), extraKeysBuilderVarName, extraKey.getExtraKeyName(),
                        extraKey.getName());
            } else {
                methodBuilder
                        .addStatement(statement.toString(), extraKeysBuilderVarName, extraKey.getExtraKeyName(), objectParameter,
                                extraKey.getName());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CacheableMarker that = (CacheableMarker) o;

        if (classQualifiedName != null ? !classQualifiedName.equals(that.classQualifiedName) : that.classQualifiedName != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (signature != null ? !signature.equals(that.signature) : that.signature != null) {
            return false;
        }
        return genericReturnTypeName != null ? genericReturnTypeName.equals(that.genericReturnTypeName) :
                that.genericReturnTypeName == null;

    }

    @Override
    public int hashCode() {
        int result = classQualifiedName != null ? classQualifiedName.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        result = 31 * result + (genericReturnTypeName != null ? genericReturnTypeName.hashCode() : 0);
        return result;
    }
}
