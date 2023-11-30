package slatepowered.store.reflect;

import lombok.RequiredArgsConstructor;
import slatepowered.store.serializer.DataSerializer;
import slatepowered.store.serializer.SerializationContext;
import slatepowered.store.serializer.SerializationInput;
import slatepowered.store.serializer.SerializationOutput;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;

/**
 * Utilizes reflection and Unsafe to quickly serialize object-only fields.
 */
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
class UnsafeReflectiveSerializer<T> implements DataSerializer<T> {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    protected final Class<T> tClass;
    protected final UnsafeFieldDesc[] fields;
    protected final MethodHandle constructor;

    @Override
    public final void serialize(SerializationContext context, T value, SerializationOutput output) {
        for (UnsafeFieldDesc desc : fields) {
            Object fieldValue = UNSAFE.getObject(value, desc.offset);
            output.set(context, desc.name, fieldValue);
        }
    }

    @Override
    public final T construct(SerializationContext context, SerializationInput input) {
        try {
            return (T) constructor.invoke();
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            throw new AssertionError(); // unreachable
        }
    }

    @Override
    public final void deserialize(SerializationContext context, T instance, SerializationInput input) {
        for (UnsafeFieldDesc desc : fields) {
            // TODO
        }
    }

}
