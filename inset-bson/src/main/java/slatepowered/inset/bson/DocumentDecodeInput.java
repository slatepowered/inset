package slatepowered.inset.bson;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.UuidRepresentation;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.util.Reflections;
import slatepowered.inset.util.ValueUtils;
import slatepowered.veru.reflect.ReflectUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
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

    // decodes a map-valid key retrieved from a bson document
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object decodeDocumentKey(CodecContext context, String value, Type expectedType) {
        Class<?> expectedClass = ReflectUtil.getClassForType(expectedType);
        if (expectedClass == String.class) {
            return value;
        }

        /* Convert floating point numbers */
        if (
                expectedClass == Float.class || expectedClass == Double.class ||
                expectedClass == float.class || expectedClass == double.class
        ) {
            return Double.longBitsToDouble(Long.parseLong(value));
        }

        /* Convert boxed numbers */
        if (Number.class.isAssignableFrom(expectedClass)) {
            return ValueUtils.castBoxedNumber(Long.parseLong(value), expectedClass);
        }

        /* Convert primitive numbers */
        if (expectedClass.isPrimitive()) {
            return ValueUtils.castBoxedPrimitive(Long.parseLong(value), expectedClass);
        }

        throw new IllegalArgumentException("Got unsupported map key type to decode: " + value.getClass());
    }

    // decodes a value retrieved from a bson document
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object decodeDocumentValue(CodecContext context, Object value, Type expectedType) {
        /* Null */
        if (value == null) {
            return null;
        }

        Class<?> expectedClass = ReflectUtil.getClassForType(expectedType);

        if (expectedClass.isEnum() && value instanceof String) {
            String str = (String) value;
            for (Object constant : expectedClass.getEnumConstants()) {
                if (((Enum)constant).name().equalsIgnoreCase(str)) {
                    return constant;
                }
            }

            throw new IllegalArgumentException("Could not resolve `" + value + "` to an enum value of " + expectedClass);
        }

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
                    expectedValueType = ((ParameterizedType)expectedType).getActualTypeArguments()[1];
                } else {
                    expectedKeyType = Object.class;
                    expectedValueType = Object.class;
                }

                Map map = new HashMap();
                doc.forEach((k, v) -> map.put(
                        decodeDocumentKey(context, k, expectedKeyType),
                        decodeDocumentValue(context, v, expectedValueType)
                ));

                return map;
            }

            // decode nested object
            String className = doc.getString("$class");
            if (className != null) {
                // decode with an alternate target type
                Class<?> klass = Reflections.findClass(className);
                DocumentDecodeInput input = new DocumentDecodeInput(keyFieldOverride, doc);
                return context.findCodec(klass).constructAndDecode(context, input);
            }

            DocumentDecodeInput input = new DocumentDecodeInput(keyFieldOverride, doc);
            return context.findCodec(expectedClass).constructAndDecode(context, input);
        } else if (value instanceof List && Map.class.isAssignableFrom(expectedClass)) {
            List<List> encodedMap = (List<List>) value;

            /*
             * Maps are encoded as arrays with each entry being a pair of key and value
             * represented in BSON as another array:
             *
             * { a = 6, b = 7 }
             * becomes
             * [ ["a", 6], ["b", 7] ]
             */

            // check for parameter types
            Type expectedKeyType;
            Type expectedValueType;
            if (Map.class.isAssignableFrom(expectedClass) && expectedType instanceof ParameterizedType) {
                expectedKeyType = ((ParameterizedType)expectedType).getActualTypeArguments()[0];
                expectedValueType = ((ParameterizedType)expectedType).getActualTypeArguments()[1];
            } else {
                expectedKeyType = Object.class;
                expectedValueType = Object.class;
            }

            Map convertedMap = new HashMap();
            encodedMap.forEach(pair -> convertedMap.put(
                    decodeDocumentValue(context, pair.get(0), expectedKeyType),  // key
                    decodeDocumentValue(context, pair.get(1), expectedValueType) // value
            ));

            return convertedMap;
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
