package slatepowered.inset.bson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.util.ValueUtils;
import slatepowered.veru.reflect.ReflectUtil;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads data from a {@link Document} input.
 */
@Getter
@RequiredArgsConstructor
public class DocumentDecodeInput extends DecodeInput {

    protected final String keyFieldOverride;

    /**
     * The input document to read from.
     */
    final Document document;

    // decodes a value retrieved from a bson document
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object decodeDocumentValue(CodecContext context, Object value, Type expectedType) {
        /* Null */
        if (value == null) {
            return null;
        }

        Class<?> expectedClass = ReflectUtil.getClassForType(expectedType);

        /* Complex objects */
        //  only support primitives if context is
        //  null, because this is only ever used to decode
        //  the primary key field
        if (value instanceof Document) {
            if (context == null) {
                throw new IllegalArgumentException("Document contains non-primitive value for key field");
            }

            Document doc = (Document) value;

            // check for map
            if (Map.class.isAssignableFrom(expectedClass)) {
                // check for parameter types
                Type expectedKeyType;
                Type expectedValueType;
                if (expectedType instanceof ParameterizedType) {
                    expectedKeyType = ((ParameterizedType)expectedType).getActualTypeArguments()[0];
                    expectedValueType = ((ParameterizedType)expectedType).getActualTypeArguments()[0];
                } else {
                    expectedKeyType = Object.class;
                    expectedValueType = Object.class;
                }

                Map map = new HashMap();
                doc.forEach((k, v) -> map.put(
                        decodeDocumentValue(context, k, expectedKeyType),
                        decodeDocumentValue(context, v, expectedValueType)
                ));

                return map;
            }

            // decode nested object
            DocumentDecodeInput input = new DocumentDecodeInput(keyFieldOverride, doc);
            return context.findCodec(expectedClass).constructAndDecode(context, input);
        } else if (value instanceof List) {
            if (context == null) {
                throw new IllegalArgumentException("Document contains non-primitive value for key field");
            }

            // try to find accurate element type
            boolean isArrayExpected = false;
            Type expectedElementType = Object.class;
            if (expectedType instanceof ParameterizedType) {
                expectedElementType = ((ParameterizedType)expectedType).getActualTypeArguments()[0];
            } else if (expectedClass.isArray()) {
                expectedElementType = expectedClass.getComponentType();
                isArrayExpected = true;
            }

            // decode list
            List list = (List) value;
            final int length = list.size();
            List newList = new ArrayList(length);

            for (int i = 0; i < length; i++) {
                newList.add(decodeDocumentValue(context, list.get(i), expectedElementType));
            }

            return isArrayExpected ? newList : newList.toArray((Object[]) Array.newInstance(ReflectUtil.getClassForType(expectedElementType), list.size()));
        } else if (value instanceof Map) {
            Map map = (Map) value;

            // check for parameter types
            Type expectedKeyType;
            Type expectedValueType;
            if (Map.class.isAssignableFrom(expectedClass) && expectedType instanceof ParameterizedType) {
                expectedKeyType = ((ParameterizedType)expectedType).getActualTypeArguments()[0];
                expectedValueType = ((ParameterizedType)expectedType).getActualTypeArguments()[0];
            } else {
                expectedKeyType = Object.class;
                expectedValueType = Object.class;
            }

            Map convertedMap = new HashMap();
            map.forEach((k, v) -> convertedMap.put(
                    decodeDocumentValue(context, k, expectedKeyType),
                    decodeDocumentValue(context, v, expectedValueType)
            ));

            return convertedMap;
        }

        /* Primitives */
        else {
            if (Number.class.isAssignableFrom(expectedClass)) {
                return ValueUtils.castBoxedNumber((Number) value, expectedClass);
            }

            if (expectedClass.isPrimitive()) {
                return ValueUtils.castBoxedPrimitive(value, expectedClass);
            }

            return value;
        }

//        throw new IllegalArgumentException("Got unsupported value type to decode: " + value.getClass());
    }

    @Override
    public Object read(CodecContext context, String field, Type expectedType) {
        Object value = document.get(field);
        return decodeDocumentValue(context, value, expectedType);
    }

    @Override
    public Object readKey(String field, Type expectedType) {
        Object value = document.get(keyFieldOverride != null ? keyFieldOverride : field);
        return decodeDocumentValue(null, value, expectedType);
    }

}
