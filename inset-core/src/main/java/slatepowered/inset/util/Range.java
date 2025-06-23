package slatepowered.inset.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class Range {
    final long start;
    final long end;

    public boolean contains(long l) {
        return l >= start && l <= end;
    }
}
