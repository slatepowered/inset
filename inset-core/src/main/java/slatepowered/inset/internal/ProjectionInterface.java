package slatepowered.inset.internal;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.operation.Projection;
import slatepowered.inset.util.Reflections;
import slatepowered.veru.reflect.ReflectUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Getter
public class ProjectionInterface implements ProjectionType {

    /**
     * The interface class.
     */
    protected final Class<?> klass;

    /**
     * The method representing the key field.
     */
    protected final Method keyMethod;

    /**
     * The other data field methods.
     */
    protected final List<Method> fieldMethods;

    /**
     * Create a new proxy for this interface with the given parameters.
     *
     * @return The proxy instance.
     */
    public Object createProxy(Supplier<Object> keySupplier,
                              BiFunction<String, Type, Object> fieldGetter) {
        return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[] { klass }, (proxy, method, args) -> {
            if (method.isDefault()) {
                return ReflectUtil.invokeDefault(proxy, method, args);
            }

            if (method.equals(keyMethod)) {
                return keySupplier.get();
            }

            if (method.equals(Reflections.METHOD_OBJECT_EQUALS)) {
                return false; // todo
            } else if (method.equals(Reflections.METHOD_OBJECT_TOSTRING)) {
                return "partial projection of key " + keySupplier.get().toString();
            } else if (method.equals(Reflections.METHOD_OBJECT_HASHCODE)) {
                return keySupplier.get().hashCode();
            }

            return fieldGetter.apply(method.getName(), method.getGenericReturnType());
        });
    }

    @Override
    public Projection createExclusiveProjection(String primaryKeyNameOverride) {
        List<String> fields = new ArrayList<>();

        // add applicable data fields
        for (Method method : fieldMethods) {
            fields.add(method.getName());
        }

        // add primary key field
        if (primaryKeyNameOverride == null)
            primaryKeyNameOverride = keyMethod.getName();
        fields.add(primaryKeyNameOverride);

        return Projection.include(fields);
    }

}
