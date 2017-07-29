package oracle.sharding.tools;

import java.io.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by somestuff on 7/26/17.
 */
public class StreamOutputSink implements Consumer<List<String>>, AutoCloseable {
    private final Writer writer;

    public StreamOutputSink(File file) {
        try {
            this.writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public StreamOutputSink(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void accept(List<String> strings) {
        try {
            for (String string : strings) {
                writer.append(string).append('\n');
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
