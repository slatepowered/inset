package slatepowered.store.reflect;

import slatepowered.store.datastore.DataItem;
import slatepowered.store.marshaller.DataMarshaller;
import slatepowered.store.query.Query;
import slatepowered.store.query.QueryModel;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Extension of {@link UnsafeReflectiveSerializer} to implement {@link slatepowered.store.marshaller.DataMarshaller}.
 */
@SuppressWarnings("unchecked")
final class UnsafeReflectiveMarshaller<K, T> extends UnsafeReflectiveSerializer<T> implements DataMarshaller<K, T> {

    static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();

    final UnsafeFieldDesc primaryKeyField;

    public UnsafeReflectiveMarshaller(Class<T> tClass, UnsafeFieldDesc[] fields, MethodHandle constructor, UnsafeFieldDesc primaryKeyField) {
        super(tClass, fields, constructor);
        this.primaryKeyField = primaryKeyField;
    }

    @Override
    public K getPrimaryKey(T value) {
        return (K) UNSAFE.getObject(value, primaryKeyField.offset);
    }

    @Override
    public T createDefault(DataItem<K, T> item) {
        try {
            return (T) constructor.invoke();
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            throw new AssertionError(); // unreachable
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Predicate<T> getQueryComparator(Query query) {
        int i;
        final QueryModel queryModel = query.getModel();
        final Collection<String> constrainedFieldNames = queryModel.getConstrainedFields();
        final int constrainedFieldCount = constrainedFieldNames.size();

        // resolve constrained fields by name into an ordered array, assuming the
        // serialized objects don't have too many fields just iterating over
        // the array of fields and comparing each one should work fine
        // also resolve the values/comparators in order into the array
        UnsafeFieldDesc[] orderedFields = new UnsafeFieldDesc[constrainedFieldCount];
        Predicate[] orderedComparators = new Predicate[constrainedFieldCount];
        i = 0;
        for (String str : constrainedFieldNames) {
            UnsafeFieldDesc fieldDesc;
            UnsafeFieldDesc theField = null;
            for (int j = fields.length - 1; j >= 0; i--) {
                if ((fieldDesc = fields[j]).name.equals(str)) {
                    theField = fieldDesc;
                    break;
                }
            }

            if (theField == null) {
                throw new IllegalArgumentException("Query field `" + str + "` could not be resolved to a field on " + tClass);
            }

            orderedFields[i] = theField;
            orderedComparators[i] = query.getConstraint(str);

            i++;
        }

        /* the actual predicate, this code is run on each cached item
         * so it needs to be fast which is the reason we did all the
         * preparation above. */
        return value -> {
            try {
                for (int n = 0; n < constrainedFieldCount; n++) {
                    if (!orderedComparators[n].test(UNSAFE.getObject(value, orderedFields[n].offset))) {
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

}
