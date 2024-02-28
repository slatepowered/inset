package slatepowered.inset.bson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.*;
import org.bson.codecs.UuidCodec;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.EncodeOutput;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Writes data to a {@link Document} input.
 */
@RequiredArgsConstructor
@Getter
public class DocumentEncodeOutput extends EncodeOutput {

    protected static final Map<Class<?>, Boolean> classesWithAbstractParents = new WeakHashMap<>();

    protected static boolean hasAbstractParents(Class<?> klass) {
        Boolean result = classesWithAbstractParents.get(klass);
        if (result != null) {
            return result;
        }

        for (Class<?> kl = klass; kl != Object.class; kl = kl.getSuperclass()) {
            if (Modifier.isAbstract(kl.getModifiers())) {
                return true;
            }
        }

        return false;
    }

    /*
     * TODO: EXPORT EVERYTHING AS BSON VALUES TO SAVE MEMORY AND PERFORMANCE
     *  THIS SHOULD BE SIMPLE BC WE ARE ALREADY ENCODING ALL COMPLEX DATA STRUCTURES
     *  MANUALLY AND WOULD JUST REQUIRE MINOR EXTENSIONS TO THE ENCODE VALUE METHOD
     *
     * THE AIM IS TO EVENTUALLY HAVE THAT METHOD RETURN A BSON VALUE SO ALL COMPLEX DATA STRUCTURES
     * CAN BE ENCODED DIRECTLY INTO A BSON MEMORY REPRESENTATION WITHOUT HAVING TO GO THROUGH MONGO CODECS
     */

    /*
     * CRITERIA FOR A TARGET CLASS NAME TO BE WRITTEN:
     * - class has ABSTRACT parents
     */

    static final UuidCodec UUID_CODEC = new UuidCodec(UuidRepresentation.STANDARD);

    protected final String keyFieldOverride;

    /**
     * The output document to write to.
     */
    protected final BsonDocument outputDocument;

    // check if the class name of an object of the given
    // class should be written to the database
    private boolean shouldWriteClassName(Class<?> klass) {
        return hasAbstractParents(klass);
    }

    // encode the given value to a bson supported document value
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private BsonValue encodeValue(CodecContext context, Object value, Class<?> baseType) {
        /* Null */
        if (value == null) {
            return new BsonNull();
        }

        /* Complex Types */
        else if (value.getClass().isEnum()) {
            return new BsonString(((Enum)value).name()); // encode as name
        } else if (value.getClass().isArray()) {
            int l = Array.getLength(value);
            BsonArray outArr = new BsonArray(new ArrayList<>(l));
            for (int i = 0; i < l; i++)
                outArr.add(i, encodeValue(context, Array.get(value, i), null));

            return outArr;
        } else if (value instanceof Collection) {
            Collection collection = (Collection<?>) value;
            BsonArray outArr = new BsonArray(new ArrayList<>(collection.size()));
            for (Object o : collection)
                outArr.add(encodeValue(context, o, null));

            return outArr;
        } else if (value instanceof Map) {
            /*
             * Maps are encoded as arrays with each entry being a pair of key and value
             * represented in BSON as another array:
             *
             * { a = 6, b = 7 }
             * becomes
             * [ ["a", 6], ["b", 7] ]
             */

            Map map = (Map) value;
            BsonArray convertedMap = new BsonArray(new ArrayList<>(map.size()));

            map.forEach((k, v) -> convertedMap.add(new BsonArray(Arrays.asList(
                    encodeValue(context, k, null),
                    encodeValue(context, v, null)
            ))));

            return convertedMap;
        }

        /* Primitives */
        else if (value instanceof Long) return new BsonInt64((Long)value);
        else if (value instanceof Integer) return new BsonInt32((Integer)value);
        else if (value instanceof Number) return new BsonDouble(((Number)value).doubleValue());
        else if (value instanceof String) return new BsonString((String) value);
        else if (value instanceof Boolean) return new BsonBoolean((Boolean) value);
        else if (value instanceof Date) return new BsonDateTime(((Date)value).getTime());
        else if (value instanceof OffsetDateTime) return new BsonDateTime(((OffsetDateTime)value).toInstant().toEpochMilli());
        else if (value instanceof Instant) return new BsonDateTime(((Instant)value).toEpochMilli());
        else if (value instanceof UUID) return encodeUUID((UUID) value);
        else if (value instanceof BsonValue) return (BsonValue) value;

        /* Objects */
        else {
            Class<?> klass = value.getClass();

            BsonDocument document = new BsonDocument();
            DocumentEncodeOutput output = new DocumentEncodeOutput(keyFieldOverride, document);
            context.findCodec((Class<Object>) klass).encode(context, value, output);

            if (shouldWriteClassName(klass)) {
                document.put(BsonCodecs.CLASS_NAME_FIELD, new BsonString(klass.getName()));
            }

            return document;
        }

//        throw new IllegalArgumentException("Got unsupported value type to encode: " + value.getClass());
    }

    private static void writeLongToArrayBigEndian(final byte[] bytes, final int offset, final long x) {
        bytes[offset + 7] = (byte) (0xFFL & (x));
        bytes[offset + 6] = (byte) (0xFFL & (x >> 8));
        bytes[offset + 5] = (byte) (0xFFL & (x >> 16));
        bytes[offset + 4] = (byte) (0xFFL & (x >> 24));
        bytes[offset + 3] = (byte) (0xFFL & (x >> 32));
        bytes[offset + 2] = (byte) (0xFFL & (x >> 40));
        bytes[offset + 1] = (byte) (0xFFL & (x >> 48));
        bytes[offset] = (byte) (0xFFL & (x >> 56));
    }

    private static BsonValue encodeUUID(UUID uuid) {
        byte[] binaryData = new byte[16];
        writeLongToArrayBigEndian(binaryData, 0, uuid.getMostSignificantBits());
        writeLongToArrayBigEndian(binaryData, 8, uuid.getLeastSignificantBits());
        return new BsonBinary(BsonBinarySubType.UUID_STANDARD, binaryData);
    }

    @Override
    protected void registerKey(CodecContext context, String name, Object key) {
        setKeyField = keyFieldOverride != null ? keyFieldOverride : setKeyField;
        outputDocument.append(setKeyField, encodeValue(context, key, null));
    }

    @Override
    public void set(CodecContext context, String field, Object value) {
        outputDocument.append(field, encodeValue(context, value, null));
    }

}
