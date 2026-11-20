package core.time;

/** Puerto de tiempo monotónico. El origen es arbitrario; solo importan diferencias. */
@FunctionalInterface
public interface TimeSource {
    long nowInNanos();
}
