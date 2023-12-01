package slatepowered.inset.bson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.BsonValue;
import org.bson.Document;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.EncodeOutput;

import java.util.*;

/**
 * Writes data to a {@link Document} input.
 */
@RequiredArgsConstructor
@Getter
public class DocumentEncodeOutput extends EncodeOutput {

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
            // todo: im too lazy to do this rn but like when are
            //  u going to put a whole map in your database which is
            //  basically a map
            throw new UnsupportedOperationException("im too lazy to implement this right now");
        }

        /* Primitives */
        else if (
                value instanceof Number  ||
                value instanceof String  ||
                value instanceof Boolean ||
                value instanceof Date    ||
                value instanceof UUID    ||
                value instanceof BsonValue
        ) {
            return value;
        }

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
