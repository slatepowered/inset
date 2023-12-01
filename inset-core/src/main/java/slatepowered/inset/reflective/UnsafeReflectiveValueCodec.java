package slatepowered.inset.reflective;

import lombok.RequiredArgsConstructor;
import slatepowered.inset.codec.ValueCodec;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;

/**
 * Utilizes reflection and Unsafe to quickly serialize object-only fields.
 */
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
class UnsafeReflectiveValueCodec<T> implements ValueCodec<T> {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    protected final Class<T> tClass;
    protected final UnsafeFieldDesc[] fields;
    protected final MethodHandle constructor;

    @Override
    public void encode(CodecContext context, T value, EncodeOutput output) {
        for (UnsafeFieldDesc desc : fields) {
            Object fieldValue = UNSAFE.getObject(value, desc.offset);
            output.set(context, desc.name, fieldValue);
        }
    }

    @Override
    public T construct(CodecContext context, DecodeInput input) {
        try {
            return (T) (constructor != null ? constructor.invoke() : UNSAFE.allocateInstance(tClass));
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            throw new AssertionError(); // unreachable
        }
    }

    @Override
    public void decode(CodecContext context, T instance, DecodeInput input) {
        for (UnsafeFieldDesc desc : fields) {
            Object value = input.read(context, desc.name, desc.type);
            UNSAFE.putObject(instance, desc.offset, value);
        }
    }

}
