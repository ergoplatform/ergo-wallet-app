package java.util.function;

/**
 * Hack file to make AppKit work on JRE7
 */
public interface Function<T, R> {
    R apply (T t);
}
