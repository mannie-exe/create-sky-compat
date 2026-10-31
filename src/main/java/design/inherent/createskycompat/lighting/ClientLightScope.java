package design.inherent.createskycompat.lighting;

/** Mark only the duration of an authoritative engine query, including nested calls. */
public final class ClientLightScope {
    private static final ThreadLocal<Boolean> SERVER_QUERY = new ThreadLocal<>();
    private ClientLightScope() {}
    public static boolean serverQuery() { return SERVER_QUERY.get() != null; }
    public static Boolean enterServer() {
        Boolean previous = SERVER_QUERY.get();
        SERVER_QUERY.set(Boolean.TRUE);
        return previous;
    }
    public static void restore(Boolean previous) {
        if (previous == null) SERVER_QUERY.remove(); else SERVER_QUERY.set(previous);
    }
}
