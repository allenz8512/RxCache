package me.allenzjl.rxcache.compiler;

import com.google.auto.service.AutoService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import me.allenzjl.rxcache.annotation.Cacheable;
import me.allenzjl.rxcache.annotation.ExtraKey;

/**
 * 注解处理器
 *
 * @author Allenz
 * @since 0.1.0
 */
@AutoService(javax.annotation.processing.Processor.class)
public class Processor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        ProcessUtils.init(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Cacheable.class.getCanonicalName());
        types.add(ExtraKey.class.getCanonicalName());
        return Collections.unmodifiableSet(types);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return doProcess(roundEnv);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof ProcessException) {
                ProcessUtils.printError(e.getLocalizedMessage(), ((ProcessException) e).getElement());
            } else {
                ProcessUtils.printError(e.getLocalizedMessage(), null);
            }
        }
        return true;
    }

    protected boolean doProcess(RoundEnvironment roundEnv) {
        ExtraKeyHolderMarker extraKeyHolderMarker = new ExtraKeyHolderMarker();
        Set<? extends Element> elementsWithExtraKey = roundEnv.getElementsAnnotatedWith(ExtraKey.class);
        for (Element element : elementsWithExtraKey) {
            Element typeElement = element.getEnclosingElement();
            AnnotationValidator.verifyExtraKeyHolder(typeElement);
            AnnotationValidator.verifyExtraKey(element);
            extraKeyHolderMarker.addExtraKey((TypeElement) typeElement, element);
        }

        CacheableHolderMarker cacheableHolderMarker = new CacheableHolderMarker(extraKeyHolderMarker);
        Set<? extends Element> elementsWithCacheable = roundEnv.getElementsAnnotatedWith(Cacheable.class);
        for (Element element : elementsWithCacheable) {
            Element typeElement = element.getEnclosingElement();
            AnnotationValidator.verifyCacheable(element);
            AnnotationValidator.verifyCacheableHolder(typeElement);
            cacheableHolderMarker.addCacheable((TypeElement) typeElement, (ExecutableElement) element);
        }
        cacheableHolderMarker.generateCacheHolderFiles();
        return false;
    }

}
