package slatepowered.inset.reflective;

import lombok.Builder;
import slatepowered.inset.codec.CodecFactory;
import slatepowered.inset.codec.CodecRegistry;
import slatepowered.inset.codec.ValueCodec;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates reflective value and data codecs.
 */
@Builder
public final class ReflectiveCodecFactory implements CodecFactory {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();
    static final MethodHandles.Lookup LOOKUP = UnsafeUtil.getInternalLookup();

    @Override
    public <T> ValueCodec<T> create(CodecRegistry registry, Class<T> klass) {
        try {
            List<UnsafeFieldDesc> unsafeFields = new ArrayList<>();
            Field[] fields = klass.getDeclaredFields();

            UnsafeFieldDesc primaryKeyField = null; // Data for data codecs: primary key field

            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers())) continue;

                // TODO: uhh support primitives but im too lazy rn
                if (field.getType().isPrimitive()) {
                    throw new UnsupportedOperationException("Primitive fields in values are not supported yet");
                }

                UnsafeFieldDesc fieldDesc = new UnsafeFieldDesc(field, field.getName(), UNSAFE.objectFieldOffset(field), field.getGenericType());
                unsafeFields.add(fieldDesc);

                // check for primary key field
                Key keyAnnotation = field.getAnnotation(Key.class);
                if (keyAnnotation != null) {
                    primaryKeyField = fieldDesc;
                }
            }

            UnsafeFieldDesc[] unsafeFieldArr = unsafeFields.toArray(new UnsafeFieldDesc[0]);

            // find constructor or null
            MethodHandle constructorHandle;

            try {
                Constructor<?> constructor = klass.getDeclaredConstructor();
                constructorHandle = LOOKUP.unreflectConstructor(constructor);
            } catch (NoSuchMethodException ignored) {
                constructorHandle = null;
            }

            // check if we should build a data codec or
            // a value codec based on the collected data
            if (
                    primaryKeyField != null
            ) {
                return new UnsafeReflectiveDataCodec<>(klass, unsafeFieldArr, constructorHandle, primaryKeyField);
            } else {
                return new UnsafeReflectiveValueCodec<>(klass, unsafeFieldArr, constructorHandle);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create reflective codec for " + klass, t);
        }
    }

}
