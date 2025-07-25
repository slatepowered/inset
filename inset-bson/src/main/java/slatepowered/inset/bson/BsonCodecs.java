package slatepowered.inset.bson;

import slatepowered.inset.codec.support.ClassDistinctionOverride;

@ClassDistinctionOverride("")
public final class BsonCodecs {

    /**
     * The name to be given to the field which holds the class name present on
     * encoded abstract objects.
     */
    public static final String CLASS_NAME_FIELD = "__class";

}
