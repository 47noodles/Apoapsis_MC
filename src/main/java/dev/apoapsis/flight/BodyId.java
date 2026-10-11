package dev.apoapsis.flight;

/** Bodies in the prototype. The ordinal is sent over the network; append only. */
public enum BodyId {
    VERDANCE("verdance"),
    PALE("pale");

    public final String id;

    BodyId(String id) {
        this.id = id;
    }

    public static BodyId byOrdinal(int i) {
        return values()[Math.floorMod(i, values().length)];
    }
}
