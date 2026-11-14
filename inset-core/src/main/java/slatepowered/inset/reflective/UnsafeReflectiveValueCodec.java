package slatepowered.inset.reflective;

import slatepowered.inset.codec.ValueCodec;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.util.Special;
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
public class UnsafeReflectiveValueCodec<T> implements ValueCodec<T> {

  static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

  protected final Class<T> tClass;
  protected final FieldAccessor[] fields;
  protected final MethodHandle constructor;
  protected final Map<String, FieldAccessor> fieldMap = new HashMap<>();

  public UnsafeReflectiveValueCodec(Class<T> tClass, FieldAccessor[] fields, MethodHandle constructor) {
    this.tClass = tClass;
    this.fields = fields;
    this.constructor = constructor;
    for (int i = fields.length - 1; i >= 0; i--) {
      FieldAccessor fieldDesc = fields[i];
      fieldMap.put(fieldDesc.getName(), fieldDesc);
      fieldMap.put(fieldDesc.getSerializedName(), fieldDesc);
    }
  }

  @Override
  public void encode(CodecContext context, T value, EncodeOutput output) {
    for (FieldAccessor desc : fields) {
      Object fieldValue = desc.getAsObject(value);
      output.set(context, desc.getSerializedName(), fieldValue, desc.getType());
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
    for (FieldAccessor desc : fields) {
      if (desc.getMark() != null) {
        context.marked().put(desc.getMark(), new DecodeFrame(context, instance, input));
      }

      Object value = desc.decodeAndConform(context, input, instance);
      if (value != Special.IGNORE) {
        desc.setFromObject(instance, value);
      }
    }
  }

  @Override
  public <V> V getField(T instance, String field) {
    FieldAccessor fieldDesc = fieldMap.get(field);
    if (fieldDesc == null)
      throw new IllegalArgumentException("No field by name `" + field + "` on " + tClass);
    return (V) fieldDesc.getAsObject(instance);
  }

  @Override
  public void updateNotDecoded(CodecContext context, T instance) {
    for (FieldAccessor desc : fields) {
      Object updated = desc.updateNotDecoded(context, instance);
      if (updated != Special.IGNORE) {
        desc.setFromObject(instance, updated);
      }
    }
  }

  @Override
  public String toSerializedName(String name) {
    FieldAccessor field = fieldMap.get(name);
    return field != null ? field.getSerializedName() : name;
  }

}
