package me.allenzjl.rxcache.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import me.allenzjl.rxcache.annotation.Cacheable;

/**
 * Created by Allen on 2016/6/30.
 */

public class CacheableHolderMarker {

    protected Map<String, Map<String, CacheableMarker>> cacheableHolderMap;

    protected ExtraKeyHolderMarker extraKeyHolderMarker;

    public CacheableHolderMarker(ExtraKeyHolderMarker extraKeyHolderMarker) {
        this.extraKeyHolderMarker = extraKeyHolderMarker;
        cacheableHolderMap = new HashMap<>();
    }

    public void addCacheable(TypeElement classElement, ExecutableElement methodElement) {
        String classQualifiedName = ProcessUtils.getTypeQualifiedName(classElement);
        Map<String, CacheableMarker> cacheableMap;
        if (cacheableHolderMap.containsKey(classQualifiedName)) {
            cacheableMap = cacheableHolderMap.get(classQualifiedName);
        } else {
            cacheableMap = new LinkedHashMap<>();
            cacheableHolderMap.put(classQualifiedName, cacheableMap);
        }
        String signature = methodElement.getSimpleName() + "(" + ProcessUtils.getExecutableSignature(methodElement) + ")";
        if (cacheableMap.containsKey(signature)) {
            throw new ProcessException("'" + classQualifiedName + "." + signature + "' is already marked.", methodElement);
        }
        Cacheable cacheable = methodElement.getAnnotation(Cacheable.class);
        String type = cacheable.value();
        String subType = cacheable.subType();
        long expired = cacheable.expired();
        int strategy = cacheable.strategy();
        String[] extraKeyNames = cacheable.extraKeys();
        Map<String, ExtraKeyMarker> extraKeyMarkers;
        if (Utils.isArrayEmpty(extraKeyNames)) {
            extraKeyMarkers = Collections.emptyMap();
        } else {
            extraKeyMarkers = new HashMap<>();
            for (String extraKeyName : extraKeyNames) {
                if (extraKeyHolderMarker.getExtraKeyMap().containsKey(extraKeyName)) {
                    extraKeyMarkers.put(extraKeyName, extraKeyHolderMarker.getExtraKeyMap().get(extraKeyName));
                } else {
                    throw new ProcessException("Extra key '" + extraKeyName + "' in 'extraKeys' have not been defined by " +
                            "@ExtraKey.", methodElement);
                }
            }
        }
        cacheableMap.put(signature,
                new CacheableMarker(methodElement, classQualifiedName, type, subType, expired, strategy, extraKeyMarkers));
    }

    public void generateCacheHolderFiles() {
        for (Map.Entry<String, Map<String, CacheableMarker>> entry : cacheableHolderMap.entrySet()) {
            String classQualifiedName = entry.getKey();
            Map<String, CacheableMarker> cacheableMarkers = entry.getValue();
            String[] names = ProcessUtils.splitPackageNameAndTypeName(classQualifiedName);
            String packageName = names[0];
            String superClassName = names[1];

            Set<ExtraKeyMarker> usedExtraKeyMarkers = new LinkedHashSet<>();
            for (CacheableMarker cacheableMarker : cacheableMarkers.values()) {
                usedExtraKeyMarkers.addAll(cacheableMarker.getExtraKeyMarkers().values());
            }
            TypeSpec.Builder builder = createCacheHolder(packageName, superClassName, usedExtraKeyMarkers);
            for (CacheableMarker cacheableMarker : cacheableMarkers.values()) {
                builder.addMethod(cacheableMarker.generateMethod());
            }
            ProcessUtils.createJavaFile(packageName, builder.build());
        }
    }

    protected TypeSpec.Builder createCacheHolder(String packageName, String superClassName,
                                                 Set<ExtraKeyMarker> usedExtraKeyMarkers) {
        ClassName superClassTypeName = ClassName.get(packageName, superClassName);
        String superClassQualifiedName = packageName + "." + superClassName;
        TypeElement superClassElement = ProcessUtils.getTypeElement(superClassTypeName);
        String className = superClassName + Utils.CACHEABLE_CLASS_NAME_SUFFIX;
        // 扩展目标类
        TypeSpec.Builder classBuilder =
                TypeSpec.classBuilder(className).superclass(superClassTypeName).addModifiers(Modifier.PUBLIC);
        // 添加CacheRepository类型的字段
        String cacheFieldName = Utils.getCacheRepositoryFieldName(packageName, superClassName);
        classBuilder.addField(FieldSpec.builder(Utils.RX_CACHE_REPOSITORY_TYPE_NAME, cacheFieldName, Modifier.PROTECTED).build());
        // 添加拥有@ExtraKey的类型的字段
        // 记录已经添加过的类型
        Set<String> addedFieldClassQualifiedNames = new HashSet<>();
        Set<ExtraKeyMarker> needToAddFieldExtraKeys = new LinkedHashSet<>();
        for (ExtraKeyMarker extraKeyMarker : usedExtraKeyMarkers) {
            // 静态，不需要添加字段
            if (extraKeyMarker.isStatic()) {
                continue;
            }
            if (!addedFieldClassQualifiedNames.contains(extraKeyMarker.getClassQualifiedName())) {
                addedFieldClassQualifiedNames.add(extraKeyMarker.getClassQualifiedName());
                // 本类，不需要添加字段
                if (extraKeyMarker.getClassQualifiedName().equals(superClassQualifiedName)) {
                    continue;
                }
                needToAddFieldExtraKeys.add(extraKeyMarker);
                String[] names = ProcessUtils.splitPackageNameAndTypeName(extraKeyMarker.getClassQualifiedName());
                String fieldName = Utils.getExtraKeyHolderFieldName(names[0], names[1]);
                classBuilder
                        .addField(FieldSpec.builder(ClassName.get(names[0], names[1]), fieldName, Modifier.PROTECTED).build());
            }
        }
        // 添加构造函数
        String cacheParamName = superClassName.substring(0, 1).toLowerCase() + superClassName.substring(1) + "Cache";
        ParameterMeta cacheParamMeta =
                new ParameterMeta(Utils.RX_CACHE_REPOSITORY_TYPE_NAME, cacheParamName, cacheFieldName, false);
        for (Element element : superClassElement.getEnclosedElements()) {
            Set<Modifier> modifiers = element.getModifiers();
            if (element.getKind() == ElementKind.CONSTRUCTOR &&
                    (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED))) {
                createCacheableHolderConstructors(superClassName, needToAddFieldExtraKeys, classBuilder, cacheParamMeta,
                        (ExecutableElement) element);
            }
        }
        return classBuilder;
    }

    protected void createCacheableHolderConstructors(String superClassName, Set<ExtraKeyMarker> needToAddFieldExtraKeys,
                                                     TypeSpec.Builder classBuilder, ParameterMeta cacheParamMeta,
                                                     ExecutableElement element) {
        ParameterMeta[] parameterMetas = new ParameterMeta[needToAddFieldExtraKeys.size() + 1];
        parameterMetas[0] = cacheParamMeta;
        int i = 1;
        for (ExtraKeyMarker extraKeyMarker : needToAddFieldExtraKeys) {
            String[] names = ProcessUtils.splitPackageNameAndTypeName(extraKeyMarker.getClassQualifiedName());
            String extraKeyHolderPackageName = names[0];
            String extraKeyHolderClassName = names[1];
            String fieldName = Utils.getExtraKeyHolderFieldName(extraKeyHolderPackageName, extraKeyHolderClassName);
            String paramName = superClassName.substring(0, 1).toLowerCase() + superClassName.substring(1) +
                    extraKeyHolderClassName;
            TypeName typeName = ClassName.get(extraKeyHolderPackageName, extraKeyHolderClassName);
            ParameterMeta parameterMeta = new ParameterMeta(typeName, paramName, fieldName, false);
            parameterMetas[i] = parameterMeta;
            i++;
        }
        classBuilder.addMethod(ProcessUtils.inheritConstructor(element, parameterMetas).build());
    }
}
