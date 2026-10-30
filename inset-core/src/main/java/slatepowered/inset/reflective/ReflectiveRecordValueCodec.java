package slatepowered.inset.reflective;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.codec.EncodeOutput;
import slatepowered.inset.codec.ValueCodec;
import slatepowered.inset.util.NotNullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ReflectiveRecordValueCodec<T> implements ValueCodec<T> {
  private final List<RecordFieldAccessor> fields;
  private final Map<String, RecordFieldAccessor> fieldMap;
  private final Constructor constructor;

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
      Object value = input.read(context, desc.getSerializedName(), desc.getType());
      if (value != null || (desc.getEtcFlags() & NotNullable.FLAG) == 0) {
        vals[i] = value;
      }
    }

    return (T) constructor.newInstance(vals);
  }

  @Override
  public void decode(CodecContext context, T instance, DecodeInput input) {
    // decode is done in construct with all values
  }

  @Override
  public <V> V getField(T instance, String field) {
    return (V) fieldMap.get(field).getAsObject(instance);
  }


}
