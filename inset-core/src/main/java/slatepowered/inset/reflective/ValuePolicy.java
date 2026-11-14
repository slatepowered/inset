package slatepowered.inset.reflective;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.inset.codec.CodecContext;
import slatepowered.inset.codec.DecodeInput;
import slatepowered.inset.codec.ValueCodec;
import slatepowered.inset.util.Special;

import java.util.Arrays;

public abstract class ValuePolicy {

  /**
   * Whether to accept the given decoded value for this field.
   */
  public abstract boolean shouldAcceptValue(CodecContext context, FieldAccessor accessor, Object parent, Object decoded);

  /**
   * Whether to invoke {@link #updateValue(CodecContext, FieldAccessor, Object, Object)}.
   */
  public abstract boolean shouldUpdateValue(CodecContext context, FieldAccessor accessor, Object parent, Object current);

  /**
   * Get or create the default value for the given field. This expects an updated instance
   * on which, importantly, `updateNotDecoded` should not have to be called.
   */
  public abstract Object updateValue(CodecContext context, FieldAccessor accessor, Object parent, Object current);

  public static ValuePolicy from(FieldAccessor accessor, Or ann) {
    return fromString(accessor, ann.value());
  }

  public static ValuePolicy fromString(FieldAccessor accessor, String str) {
    switch (str) {
      case "declineAbsent+decodeDefaultIfNull": return new Static(true, DefaultProviders.DECODE_EMPTY);
      case "declineAbsent+ignoreNull": return new Static(true, null);
      case "acceptAbsent+decodeDefaultIfNull": return new Static(false, DefaultProviders.DECODE_EMPTY);
    }

    // parse simple command
    int space = str.indexOf(' ');
    String alias = str.substring(0, space);
    String remaining = str.substring(space + 1);

    switch (alias) {
      case "from": return new Static(true, inputFromPathDefault(accessor, remaining));
    }

    throw new IllegalArgumentException("Unsupported ValuePolicy string `" + str + "`");
  }

  public interface DefaultProvider {
    Object defaultFor(CodecContext context, FieldAccessor accessor, Object parent, Object current);
  }

  public enum DefaultProviders implements DefaultProvider {
    DECODE_EMPTY
    ;

    @Override
    public Object defaultFor(CodecContext context, FieldAccessor accessor, Object parent, Object current) {
      return context.findCodec(accessor.getTypeAsClass()).constructAndDecode(context, DecodeInput.empty());
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class Static extends ValuePolicy {
    final boolean declineAbsent;
    final DefaultProvider defaultProvider;

    @Override
    public boolean shouldAcceptValue(CodecContext context, FieldAccessor accessor, Object parent, Object decoded) {
      return !declineAbsent || decoded != null;
    }

    @Override
    public boolean shouldUpdateValue(CodecContext context, FieldAccessor accessor, Object parent, Object current) {
      return defaultProvider != null && (current == null || current == Special.IGNORE);
    }

    @Override
    public Object updateValue(CodecContext context, FieldAccessor accessor, Object parent, Object current) {
      try {
        return defaultProvider.defaultFor(context, accessor, parent, current);
      } catch (Exception ex) {
        throw new RuntimeException("In resolution of default value for field `" + accessor.getName() + "`", ex);
      }
    }
  }

  static DefaultProvider inputFromPathDefault(FieldAccessor accessor, String remaining) {
    final String[] path = remaining.split("\\.");
    final String first = path[0];
    final String last = path[path.length - 1];
    final String[] traverse = Arrays.copyOfRange(path, 1, path.length - 1);
    return (context, accessor1, parent, current) -> {
      DecodeFrame frame = context.marked().get(first);
      if (frame == null) {
        throw new IllegalArgumentException("No marked decode frame by path `" + first + "`");
      }

      DecodeInput curr = frame.getInput();
      if (curr == null) {
        throw new IllegalArgumentException("Marked decode frame by path `" + first + "` has missing or invalid object decode input");
      }

      for (String elem : traverse) {
        String fieldName = elem;
        curr = curr.inputObject(fieldName);
        if (curr == null) {
          return null;
        }
      }

      return curr.read(context, last, accessor1.getType());
    };
  }
}
