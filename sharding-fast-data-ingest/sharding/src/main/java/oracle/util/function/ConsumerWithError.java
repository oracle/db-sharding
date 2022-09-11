/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.util.function;

import java.util.function.Consumer;

/**
 * Variant of java.util.function.Consumer which can throw an exception and process it as side effect.
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
