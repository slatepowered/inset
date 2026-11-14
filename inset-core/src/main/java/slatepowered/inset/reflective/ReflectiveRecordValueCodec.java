package slatepowered.inset.reflective;

import lombok.SneakyThrows;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.codec.ValueCodec;
import slatepowered.inset.util.Special;
import slatepowered.inset.util.ValueUtils;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ReflectiveRecordValueCodec<T> implements ValueCodec<T> {
  private final List<RecordFieldAccessor> fields;
  private final Map<String, RecordFieldAccessor> fieldMap;
  private final Constructor constructor;
  private final Object[] defaults;

  public ReflectiveRecordValueCodec(List<RecordFieldAccessor> fields, Constructor constructor) {
    this.fields = fields;
    this.fieldMap = new HashMap<>();
    this.constructor = constructor;

    for (var f : fields) {
      fieldMap.put(f.getName(), f);
      if (f.getSerializedName() != null) {
        fieldMap.put(f.getSerializedName(), f);
      }
    }

    defaults = new Object[fields.size()];
    for (int i = 0; i < defaults.length; i++) {
      final var desc = fields.get(i);
      defaults[i] = ValueUtils.defaultValueForPrimitive(desc.getTypeAsClass());
    }
  }

  @Override
  public void encode(CodecContext context, T value, EncodeOutput output) {
    for (FieldAccessor desc : fields) {
      Object fieldValue = desc.getAsObject(value);
      output.set(context, desc.getSerializedName(), fieldValue, desc.getType());
    }
  }

  @SneakyThrows
  @Override
  public T construct(CodecContext context, DecodeInput input) {
    final int len = fields.size();
    Object[] vals = new Object[len];
    for (int i = 0; i < len; i++) {
      final RecordFieldAccessor desc = fields.get(i);
      Object value = desc.decodeAndConform(context, input, null);
      if (value != Special.IGNORE) {
        vals[i] = value;
      } else {
        vals[i] = defaults[i];
      }
    }

    return (T) constructor.newInstance(vals);
  }

  @Override
  public void decode(CodecContext context, T instance, DecodeInput input) {
    // decode is done in construct with all values
  }

  @Override
  public void updateNotDecoded(CodecContext context, T instance) {
    for (RecordFieldAccessor accessor : fields) {
      Object updated = accessor.updateNotDecoded(context, instance);
    }
  }

  @Override
  public <V> V getField(T instance, String field) {
    return (V) fieldMap.get(field).getAsObject(instance);
  }


}
