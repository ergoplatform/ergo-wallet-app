package scala.collection.immutable;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * MobiVM 3.0.0 compatibility override for Scala 2.12's {@code scala.collection.immutable.VM}.
 * MobiVM's {@code robovm-rt} provides native {@code sun.misc.Unsafe#storeFence()} directly, whereas
 * {@code java.lang.invoke.MethodHandles} is an AOT compile-time stub without {@code lookup()}.
 * Declared {@code public} so callers on the application classpath can invoke {@link #releaseFence()}
 * across MobiVM's bootclasspath boundary without triggering {@link IllegalAccessError}.
 */
public abstract class VM {
    private static final Unsafe UNSAFE = findUnsafe();

    private VM() {
    }

    public static void releaseFence() throws Throwable {
        UNSAFE.storeFence();
    }

    private static Unsafe findUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (Throwable ignored) {
            // Fallback for standard JVM test environments where getUnsafe() checks ClassLoader
        }
        try {
            for (Field field : Unsafe.class.getDeclaredFields()) {
                if (field.getType() == Unsafe.class) {
                    field.setAccessible(true);
                    Object instance = field.get(null);
                    if (instance != null) {
                        return (Unsafe) instance;
                    }
                }
            }
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
        throw new IllegalStateException("No instance of sun.misc.Unsafe found");
    }
}
