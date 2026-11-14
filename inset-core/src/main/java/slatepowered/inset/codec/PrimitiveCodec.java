package slatepowered.inset.codec;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "unchecked", "rawtypes" })
@RequiredArgsConstructor
public class PrimitiveCodec<T> implements ValueCodec<T> {

  static PrimitiveCodec put(Class kl, PrimitiveCodec codec) {
    ALL.put(kl, codec);
    return codec;
  }

  static final Map<Class, ValueCodec> ALL = new HashMap<>();

  static {
    put(Byte.class, new PrimitiveCodec((byte) 0));
    put(Short.class, new PrimitiveCodec((short) 0));
    put(Character.class, new PrimitiveCodec('-'));
    put(Integer.class, new PrimitiveCodec((int) 0));
    put(Long.class, new PrimitiveCodec((long) 0));
    put(Float.class, new PrimitiveCodec(0F));
    put(Double.class, new PrimitiveCodec(0D));
  }

  final T defaultValue;

  @Override
  public void encode(CodecContext context, T value, EncodeOutput output) {
    throw new UnsupportedOperationException("Attempt to codec encode primitive, unsupported since `encode` is only meant for object types");
  }

  @Override
  public T construct(CodecContext context, DecodeInput input) {
    return defaultValue;
  }

  @Override
  public void decode(CodecContext context, T instance, DecodeInput input) {

  }

  @Override
  public <V> V getField(T instance, String field) {
    throw new UnsupportedOperationException("Attempt to codec `getField` primitive, unsupported since `getField` is only meant for object types");
  }

  @Override
  public void updateNotDecoded(CodecContext context, T instance) {

  }
}
