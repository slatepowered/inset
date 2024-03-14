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
import java.util.HashMap;
import java.util.Map;

/**
 * Utilizes reflection and Unsafe to quickly serialize object-only fields.
 */
@SuppressWarnings("unchecked")
class UnsafeReflectiveValueCodec<T> implements ValueCodec<T> {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    protected final Class<T> tClass;
    protected final UnsafeFieldDesc[] fields;
    protected final MethodHandle constructor;
    protected final Map<String, UnsafeFieldDesc> fieldMap = new HashMap<>();

    public UnsafeReflectiveValueCodec(Class<T> tClass, UnsafeFieldDesc[] fields, MethodHandle constructor) {
        this.tClass = tClass;
        this.fields = fields;
        this.constructor = constructor;
        for (int i = fields.length - 1; i >= 0; i--) {
            UnsafeFieldDesc fieldDesc = fields[i];
            fieldMap.put(fieldDesc.name, fieldDesc);
        }
    }

    @Override
    public void encode(CodecContext context, T value, EncodeOutput output) {
        for (UnsafeFieldDesc desc : fields) {
            Object fieldValue = desc.getAsObject(value);
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
        System.out.println("decoding values for instance of " + tClass + " unsafe-reflectively");
        for (UnsafeFieldDesc desc : fields) {
            System.out.println("reading field " + desc.name + " with offset " + desc.offset + " and ptype " + desc.type);
            Object value = input.read(context, desc.name, desc.type);
            desc.setFromObject(instance, value);
        }
    }

    @Override
    public <V> V getField(T instance, String field) {
        UnsafeFieldDesc fieldDesc = fieldMap.get(field);
        if (fieldDesc == null)
            throw new IllegalArgumentException("No field by name `" + field + "` on " + tClass);
        return (V) fieldDesc.getAsObject(instance);
    }

}
