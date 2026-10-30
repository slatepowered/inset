package slatepowered.inset.reflective;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import slatepowered.inset.codec.support.PotentiallyTransient;
import slatepowered.inset.util.NotNullable;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;

@RequiredArgsConstructor
@Getter
public class RecordFieldAccessor implements FieldAccessor {
  final RecordComponent component;
  final Method accessor;
  final String name;
  final String serializedName;
  final int primitiveType;
  final Type type;
  final long etcFlags;

  boolean isPotentiallyTransient;

  @SneakyThrows
  @Override
  public Object getAsObject(Object instance) {
    return accessor.invoke(instance);
  }

  @Override
  public void setFromObject(Object instance, Object value) {
    throw new UnsupportedOperationException();
  }

  @SneakyThrows
  public static RecordFieldAccessor of(RecordComponent field) {
    long flags = 0;
    if (field.isAnnotationPresent(NotNullable.class)) flags |= NotNullable.FLAG;

    SerializedName fieldNameAnnotation = field.getAnnotation(SerializedName.class);
    RecordFieldAccessor desc = new RecordFieldAccessor(field, field.getAccessor(), field.getName(), fieldNameAnnotation != null ? fieldNameAnnotation.value() : field.getName(),
        UnsafeFieldDesc.getPrimitiveType(field.getType()), field.getGenericType(), flags);

    if (PotentiallyTransient.class.isAssignableFrom(field.getType())) {
      desc.isPotentiallyTransient = true;
    }

    return desc;
  }
}
