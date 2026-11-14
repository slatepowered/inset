package slatepowered.inset.codec.support;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.reflective.Mark;

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

    // load superclass properties
    Class<?> superclass = klass.getSuperclass();
    ClassTreeInfo scInfo = superclass != null && superclass != Object.class ?
        forClass(superclass) :
        null;
    if (scInfo != null) {
      info.annotationMap.putAll(scInfo.getAnnotationMap());
      info.defaultMark = scInfo.defaultMark;
    }

    // parse own properties
    info.abstractOrHasAbstractParents = hasAbstractParentsOrIsAbstract(klass);
    info.classDistinctionOverride = klass.getAnnotation(ClassDistinctionOverride.class);
    for (Annotation ann : klass.getAnnotations()) {
      info.annotationMap.put(ann.getClass(), ann);
    }

    info.isPotentiallyTransient = info.isPotentiallyTransient || PotentiallyTransient.class.isAssignableFrom(klass);
    if (klass.isAnnotationPresent(Mark.class)) {
      info.defaultMark = klass.getAnnotation(Mark.class).value();
    }

    // load overriding superclass properties
    if (scInfo != null) {
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
  private boolean isPotentiallyTransient;
  private String defaultMark;

  public boolean shouldWriteSeparateClassName() {
    return classDistinctionOverride == null && abstractOrHasAbstractParents;
  }

}
