package slatepowered.inset.util;

import slatepowered.veru.misc.ANSI;

import java.util.function.Supplier;

/**
 * Provides debug logging utilities.
 */
public interface DebugLogging {

    /**
     * Whether ALL debug logging EVERYWHERE is enabled.
     */
    boolean ENABLED = true;

    default String compactString(Object value) {
        return ValueUtils.prettyCompact(value);
    }

    default void log(Supplier<String> supplier) {
        if (ENABLED) {
            System.out.println(ANSI.BRIGHT_GRAY + getClass().getSimpleName() + ": " + ANSI.RESET + supplier.get());
        }
    }


}
