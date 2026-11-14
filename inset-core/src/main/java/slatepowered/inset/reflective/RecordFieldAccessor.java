package slatepowered.inset.reflective;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import slatepowered.inset.codec.support.ClassTreeInfo;
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
  final byte primitiveType;
  final Type type;
  final Class<?> typeAsClass;
  final long flags;

  String mark;
  ValuePolicy valuePolicy;
  boolean mustProcessDecoding;

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

  @Override
  public boolean mustProcessDecoding() {
    return true; // todo: determine so its not needed for every single field
  }

  @SneakyThrows
  public static RecordFieldAccessor of(RecordComponent field) {
    long flags = 0;

    SerializedName fieldNameAnnotation = field.getAnnotation(SerializedName.class);
    RecordFieldAccessor desc = new RecordFieldAccessor(field, field.getAccessor(), field.getName(), fieldNameAnnotation != null ? fieldNameAnnotation.value() : field.getName(),
        UnsafeFieldDesc.getPrimitiveType(field.getType()), field.getGenericType(), field.getType(), flags);

    ClassTreeInfo treeInfo = ClassTreeInfo.forClass(field.getType());
    desc.isPotentiallyTransient = treeInfo.isPotentiallyTransient();

    Mark markAnn = field.getAnnotation(Mark.class);
    if (markAnn != null) {
      desc.mark = markAnn.value();
    }

    Or orAnn = field.getAnnotation(Or.class);
    if (orAnn != null) {
      desc.valuePolicy = ValuePolicy.from(desc, orAnn);
    } else {
      if (field.isAnnotationPresent(NotNullable.class)) desc.valuePolicy = new ValuePolicy.Static(true, null);
    }

    return desc;
  }
}
