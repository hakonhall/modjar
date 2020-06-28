package no.ion.jhms.modularizer;

import java.util.EnumSet;

public enum RequiresFlag {
    TRANSITIVE(0x0020), STATIC_PHASE(0x0040), SYNTHETIC(0x1000), MANDATED(0x8000);

    private final int flag;

    RequiresFlag(int flag) {
        this.flag = flag;
    }

    public static int toRequiresFlag(EnumSet<RequiresFlag> flags) {
        int requiresFlags = 0;
        for (var flag : flags) {
            requiresFlags |= flag.flag;
        }
        return requiresFlags;
    }

    public int value() { return flag; }
    public boolean hasFlag(int flags) { return (flags & flag) != 0; }
}
