package java.util.function;

/**
 * Hack file to make AppKit work on JRE7
 */
public interface BiFunction<T,U,R> {
    R apply(T t, U u);
}
