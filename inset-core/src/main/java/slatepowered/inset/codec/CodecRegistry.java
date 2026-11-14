package slatepowered.inset.codec;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Stores {@link ValueCodec}s by class.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
@RequiredArgsConstructor
public class CodecRegistry {

  /**
   * The factory which creates new codecs.
   */
  private final CodecFactory factory;

  /**
   * The codec cache.
   */
  private final Map<Class, ValueCodec> codecMap = new ConcurrentHashMap<>();

  {
    codecMap.putAll(PrimitiveCodec.ALL);
  }

  @SuppressWarnings("unchecked")
  public <T> ValueCodec<T> getCodec(Class<T> klass) {
    return (ValueCodec<T>) codecMap.computeIfAbsent(klass, k -> factory.create(this, k));
  }

  @SuppressWarnings("unchecked")
  public <T, S extends ValueCodec<T>> ValueCodec<T> getCodec(Class<T> klass, BiFunction<CodecRegistry, Class<T>, S> factory) {
    return (ValueCodec<T>) codecMap.computeIfAbsent(klass, k -> factory.apply(this, klass));
  }

  public CodecRegistry register(Class<?> klass, ValueCodec<?> serializer) {
    codecMap.put(klass, serializer);
    return this;
  }

}
