package slatepowered.inset.reflective;

import java.lang.reflect.Type;

public interface FieldAccessor {

  Object getAsObject(Object instance);
  void setFromObject(Object instance, Object value);
  String getName();
  String getSerializedName();
  Type getType();
  int getPrimitiveType();
  long getEtcFlags();
  boolean isPotentiallyTransient();

}
