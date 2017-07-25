package oracle.util.function;

import java.util.function.Consumer;

/**
 * Created by somestuff on 4/6/17.
 */
@FunctionalInterface
public interface ConsumerWithError<T, E extends Exception> {
    void accept(T t) throws E;

    static<T, E extends Exception> ConsumerWithError<T, E> create(ConsumerWithError<T, E> input) {
        return input;
    }

    default Consumer<T> onError(Consumer<Exception> exceptionHandler) {
        return (T t) -> {
            try {
                this.accept(t);
            } catch (Exception exception) {
                exceptionHandler.accept(exception);
            }
        };
    }

    default Consumer<T> onErrorRuntimeException() {
        return (T t) -> {
            try {
                this.accept(t);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };
    }

    default Consumer<T> onErrorPrint() {
        return (T t) -> {
            try {
                this.accept(t);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }
}
