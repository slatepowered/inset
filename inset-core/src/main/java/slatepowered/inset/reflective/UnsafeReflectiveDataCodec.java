package slatepowered.inset.reflective;

import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.datastore.DataItem;
import slatepowered.inset.codec.DataCodec;
import slatepowered.inset.operation.Projection;
import slatepowered.inset.query.constraint.FieldConstraint;
import slatepowered.inset.query.Query;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Extension of {@link UnsafeReflectiveValueCodec} to implement {@link DataCodec}.
 */
@SuppressWarnings("unchecked")
final class UnsafeReflectiveDataCodec<K, T> extends UnsafeReflectiveValueCodec<T> implements DataCodec<K, T> {

    // what the method name says lol
    private static UnsafeFieldDesc[] removePrimaryKeyFieldFromDefaultCodecFieldArray(UnsafeFieldDesc[] arr, String name) {
        int i = 0;
        for (; i < arr.length; i++)
            if (arr[i].name.equals(name))
                break;

        UnsafeFieldDesc[] result = new UnsafeFieldDesc[arr.length - 1];
        if (i > 0) // its not the first element
            System.arraycopy(arr, 0, result, 0, i);
        if (i < arr.length - 1) // its not the last element
            System.arraycopy(arr, i + 1, result, i, arr.length - i - 1);
        return result;
    }

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    /** The field containing the primary key. */
    final UnsafeFieldDesc[] allFields;
    final UnsafeFieldDesc primaryKeyField;

    public UnsafeReflectiveDataCodec(Class<T> tClass, UnsafeFieldDesc[] fields, MethodHandle constructor, UnsafeFieldDesc primaryKeyField) {
        super(tClass, removePrimaryKeyFieldFromDefaultCodecFieldArray(fields, primaryKeyField.name), constructor);
        this.allFields = fields;
        this.primaryKeyField = primaryKeyField;
    }

    @Override
    public void decode(CodecContext context, T instance, DecodeInput input) {
        super.decode(context, instance, input);

        // decode primary key
        primaryKeyField.setFromObject(instance, input.getOrReadKey(primaryKeyField.name, primaryKeyField.type));
    }

    @Override
    public K getPrimaryKey(T value) {
        return (K) primaryKeyField.getAsObject(value);
    }

    @Override
    public String getPrimaryKeyFieldName() {
        return primaryKeyField.name;
    }

    @Override
    public T createDefault(DataItem<K, T> item) {
        T instance = super.construct(null, null);
        primaryKeyField.setFromObject(instance, item.key());
        return instance;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Predicate<T> getFilterPredicate(Query query) {
        int i;
        final Map<String, FieldConstraint<?>> fieldConstraints = query.getFieldConstraints();
        final int constrainedFieldCount = fieldConstraints.size();

        // resolve constrained fields by name into an ordered array, assuming the
        // serialized objects don't have too many fields just iterating over
        // the array of fields and comparing each one should work fine
        // also resolve the values/comparators in order into the array
        UnsafeFieldDesc[] orderedFields = new UnsafeFieldDesc[constrainedFieldCount];
        FieldConstraint[] orderedConstrained = new FieldConstraint[constrainedFieldCount];
        i = 0;
        for (Map.Entry<String, FieldConstraint<?>> entry : fieldConstraints.entrySet()) {
            String fieldName = entry.getKey();

            UnsafeFieldDesc fieldDesc;
            UnsafeFieldDesc theField = null;
            for (int j = allFields.length - 1; j >= 0; j--) {
                if ((fieldDesc = allFields[j]).name.equals(fieldName)) {
                    theField = fieldDesc;
                    break;
                }
            }

            if (theField == null) {
                throw new IllegalArgumentException("Query field `" + fieldName + "` could not be resolved to a field on " + tClass);
            }

            orderedFields[i] = theField;
            orderedConstrained[i] = entry.getValue();

            i++;
        }

        /* the actual predicate, this code is run on each cached item
         * so it needs to be fast which is the reason we did all the
         * preparation above. */
        return value -> {
            try {
                for (int n = 0; n < constrainedFieldCount; n++) {
                    if (!orderedConstrained[n].test(UNSAFE.getObject(value, orderedFields[n].offset))) {
                        return false;
                    }
                }

                return true;
            } catch (Throwable t) {
                Throwables.sneakyThrow(t);
                throw new AssertionError(); // unreachable
            }
        };
    }

    @Override
    public Projection createExclusiveProjection(String primaryKeyName) {
        List<String> fields = new ArrayList<>();

        // add applicable data fields
        for (UnsafeFieldDesc fieldDesc : super.fields) {
            fields.add(fieldDesc.name);
        }

        // add primary key field
        if (primaryKeyName == null)
            primaryKeyName = primaryKeyField.getName();
        fields.add(primaryKeyName);

        return Projection.include(fields);
    }

}
