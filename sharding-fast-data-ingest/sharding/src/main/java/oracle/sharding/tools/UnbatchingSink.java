/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.tools;

import oracle.util.function.ConsumerWithError;

import java.io.Writer;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UnbatchingSink<T> implements Consumer<List<T>>, AutoCloseable {
    public final AutoCloseable closable;
    public final Consumer<T> writer;

    private UnbatchingSink(Consumer<T> writer, AutoCloseable closable) {
        this.closable = closable;
        this.writer = writer;
    }

    public static<U> Consumer<List<U>> unbatchToStrings(Writer writer) {
        return unbatchToStrings(writer, Object::toString);
    }

    public static<U> Consumer<List<U>> unbatchToStrings(Writer writer, Function<U, String> convert) {
        return new UnbatchingSink<U>(
                ConsumerWithError.create((U x) -> writer.append(convert.apply(x)).append('\n'))
                        .onErrorRuntimeException(), writer);
    }

    public static<U> Consumer<List<U>> unbatch(Consumer<U> writer, AutoCloseable onClose) {
        return new UnbatchingSink<U>(writer, onClose);
    }

    public static<U> Consumer<List<U>> unbatch(Consumer<U> writer) {
        return new UnbatchingSink<U>(writer, null);
    }

    @Override
    public void close() throws Exception {
        if (closable != null) {
            closable.close();
        }
    }

    @Override
    public void accept(List<T> ts) {
        for (T obj : ts) {
            writer.accept(obj);
        }
    }
}
