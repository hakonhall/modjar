package no.ion.jhms.modularizer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

public class Exceptions {
    public interface ThrowingSupplier<T, E extends Exception> {
        T get() throws E;
    }

    public static <T> T uncheckIO(ThrowingSupplier<T, IOException> supplier) {
        return uncheck(supplier, IOException.class, UncheckedIOException::new);
    }

    public static <T, E extends Exception> T uncheck(
            ThrowingSupplier<T, E> supplier,
            Class<E> clazz,
            Function<? super E, ? extends RuntimeException> toRuntimeMapper) {
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception exception) {
            E e = clazz.cast(exception);
            throw toRuntimeMapper.apply(e);
        }
    }

    public interface ThrowingRunnable<E extends Exception> {
        void get() throws E;
    }

    public static <T> void uncheckIO(ThrowingRunnable<IOException> runnable) {
        uncheck(runnable, IOException.class, UncheckedIOException::new);
    }

    public static <E extends Exception> void uncheck(
            ThrowingRunnable<E> runnable,
            Class<E> clazz,
            Function<? super E, ? extends RuntimeException> toRuntimeMapper) {
        try {
            runnable.get();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception exception) {
            E e = clazz.cast(exception);
            throw toRuntimeMapper.apply(e);
        }
    }
}
