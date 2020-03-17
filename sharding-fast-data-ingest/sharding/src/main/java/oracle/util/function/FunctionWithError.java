/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.util.function;

import java.util.function.Function;

/**
 * Variant of java.util.function.Function which can throw an exception and process it as side effect.
 */
@FunctionalInterface
public interface FunctionWithError<T, R, E extends Exception> {
    R applyWithError(T t) throws E;

    static<T, R, E extends Exception> FunctionWithError<T, R, E> create(FunctionWithError<T, R, E> input) {
        return input;
    }

    default Function<T, R> onError(Function<Exception, R> exceptionHandler) {
        return (T t) -> {
            try {
                return this.applyWithError(t);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                return exceptionHandler.apply(exception);
            }
        };
    }

    default Function<T, R> onErrorRuntimeException() {
        return (T t) -> {
            try {
                return this.applyWithError(t);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        };
    }

    default Function<T, R> onErrorNull() {
        return (T t) -> {
            try {
                return this.applyWithError(t);
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        };
    }

}
