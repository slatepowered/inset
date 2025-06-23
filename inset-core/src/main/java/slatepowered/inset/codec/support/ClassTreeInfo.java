package slatepowered.inset.codec.support;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Information on the class tree of a class.
 */
@RequiredArgsConstructor
@Getter
public class ClassTreeInfo {

    static final WeakHashMap<Class<?>, ClassTreeInfo> MAP = new WeakHashMap<>();

    static boolean hasAbstractParentsOrIsAbstract(Class<?> klass) {
        if ((
                klass.isInterface() ||
                        Modifier.isAbstract(klass.getModifiers()) ||
                        klass.getInterfaces().length != 0
        ) && (
                // Special classes
                klass != Enum.class
        )) {
            return true;
        }

        Class<?> superclass = klass.getSuperclass();
        return superclass != null && superclass != Object.class &&
                hasAbstractParentsOrIsAbstract(klass.getSuperclass());
    }

    public static ClassTreeInfo forClass(Class<?> klass) {
        if (klass == null || klass.isPrimitive() || klass.isArray()) {
            return null;
        }

        ClassTreeInfo info = MAP.get(klass);
        if (info != null) {
            return info;
        }

        info = new ClassTreeInfo(klass);

        // parse own properties
        info.abstractOrHasAbstractParents = hasAbstractParentsOrIsAbstract(klass);
        info.classDistinctionOverride = klass.getAnnotation(ClassDistinctionOverride.class);
        for (Annotation ann : klass.getAnnotations()) {
            info.annotationMap.put(ann.getClass(), ann);
        }

        // load superclass properties
        Class<?> superclass = klass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            ClassTreeInfo scInfo = forClass(superclass);
            info.annotationMap.putAll(scInfo.getAnnotationMap());
            if (scInfo.isAbstractOrHasAbstractParents()) info.abstractOrHasAbstractParents = true;
            if (scInfo.getClassDistinctionOverride() != null) info.classDistinctionOverride = scInfo.getClassDistinctionOverride();
        }

        return info;
    }

    private final Class<?> klass;

    /* Annotations and other properties */
    private Map<Class<? extends Annotation>, Annotation> annotationMap = new HashMap<>();
    private boolean abstractOrHasAbstractParents;
    private ClassDistinctionOverride classDistinctionOverride;

    public boolean shouldWriteSeparateClassName() {
        return classDistinctionOverride == null && abstractOrHasAbstractParents;
    }

}
