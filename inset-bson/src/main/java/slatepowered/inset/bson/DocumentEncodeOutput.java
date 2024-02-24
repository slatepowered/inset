package slatepowered.inset.bson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.*;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.EncodeOutput;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Writes data to a {@link Document} input.
 */
@RequiredArgsConstructor
@Getter
public class DocumentEncodeOutput extends EncodeOutput {

    /*
     * TODO: EXPORT EVERYTHING AS BSON VALUES TO SAVE MEMORY AND PERFORMANCE
     *  THIS SHOULD BE SIMPLE BC WE ARE ALREADY ENCODING ALL COMPLEX DATA STRUCTURES
     *  MANUALLY AND WOULD JUST REQUIRE MINOR EXTENSIONS TO THE ENCODE VALUE METHOD
     *
     * THE AIM IS TO EVENTUALLY HAVE THAT METHOD RETURN A BSON VALUE SO ALL COMPLEX DATA STRUCTURES
     * CAN BE ENCODED DIRECTLY INTO A BSON MEMORY REPRESENTATION WITHOUT HAVING TO GO THROUGH MONGO CODECS
     */

    protected final String keyFieldOverride;

    /**
     * The output document to write to.
     */
    protected final Document outputDocument;

    // encode the given value to a bson supported document value
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object encodeValue(CodecContext context, Object value) {
        /* Null */
        if (value == null) {
            return null;
        }

        /* Complex Types */
        else if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            int l = arr.length;
            Object[] outArr = new Object[l];
            for (int i = 0; i < l; i++)
                outArr[i] = encodeValue(context, arr[i]);

            return outArr;
        } else if (value instanceof Collection) {
            Collection collection = (Collection<?>) value;
            List list = new ArrayList<>();
            for (Object o : collection)
                list.add(encodeValue(context, o));

            return list;
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
            List convertedMap = new ArrayList();

            map.forEach((k, v) -> convertedMap.add(Arrays.asList(
                    encodeValue(context, k),
                    encodeValue(context, v)
            )));

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
        else if (value instanceof UUID) return value; // TODO
        else if (value instanceof BsonValue) return value;

        /* Objects */
        else {
            Document document = new Document();
            DocumentEncodeOutput output = new DocumentEncodeOutput(keyFieldOverride, document);
            context.findCodec((Class<Object>) value.getClass()).encode(context, value, output);
            return document;
        }

//        throw new IllegalArgumentException("Got unsupported value type to encode: " + value.getClass());
    }

    @Override
    protected void registerKey(CodecContext context, String name, Object key) {
        setKeyField = keyFieldOverride != null ? keyFieldOverride : setKeyField;
        outputDocument.append(setKeyField, encodeValue(context, key));
    }

    @Override
    public void set(CodecContext context, String field, Object value) {
        outputDocument.append(field, encodeValue(context, value));
    }

}
