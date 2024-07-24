package slatepowered.inset.util;

import slatepowered.veru.misc.ANSI;
import slatepowered.veru.misc.Throwables;
import slatepowered.veru.reflect.UnsafeUtil;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * Provides debug logging utilities.
 */
public interface DebugLogging {

    /**
     * The global debug logging level.
     *
     * Change this to a constant expression to inline at compile time, allowing
     * for performance benefits but making it impossible to change at runtime.
     */
    int DEBUG_LOGGING_LEVEL =
//            Integer.parseInt(System.getProperty("slatepowered.inset.debugLoggingLevel", "0")); boolean CONSTANT = false;
            0; boolean CONSTANT = true;

    /** Set the {@link DebugLogging#DEBUG_LOGGING_LEVEL} using Unsafe. */
    static void setDebugLoggingLevel(int level) {
        if (CONSTANT) {
            throw new UnsupportedOperationException("Library has been compiled with the debug logging level as a constant expression, meaning it" +
                    " has been inlined for all internal cases and can not be changed");
        }

        try {
            Unsafe unsafe = UnsafeUtil.getUnsafe();
            Field field = DebugLogging.class.getField("DEBUG_LOGGING_LEVEL");

            long offset = unsafe.staticFieldOffset(field);
            Object base = unsafe.staticFieldBase(field);
            unsafe.putInt(base, offset, level);
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
        }
    }

    /* Debug Logging Levels */
    int SUMMARY = 1;
    int DEBUG = 2;
    int VERBOSE = 3;
    int TRACE = 4;

    default String compactString(Object value) {
        return ValueUtils.prettyCompact(value);
    }

    default void log(String string, int level) {
        if (DEBUG_LOGGING_LEVEL >= level) {
            log(string);
        }
    }

    default void log(Supplier<String> supplier, int level) {
        if (DEBUG_LOGGING_LEVEL >= level) {
            log(supplier);
        }
    }

    default void log(String string) {
        System.out.println(ANSI.BRIGHT_GRAY + getClass().getSimpleName() + ": " + ANSI.RESET + string);
    }

    default void log(Supplier<String> supplier) {
        System.out.println(ANSI.BRIGHT_GRAY + getClass().getSimpleName() + ": " + ANSI.RESET + supplier.get());
    }

}
