package java.util.function;

/**
 * Hack file to make AppKit work on JRE7
 */
public interface Predicate<T> {
    boolean test(T t);
}