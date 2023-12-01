package slatepowered.inset.reflective;

import lombok.Data;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@Data
final class UnsafeFieldDesc {

    final Field field; // The reflection field
    final String name; // The name of the field
    final long offset; // The object field offset
    final Type type;   // The generic type

}
