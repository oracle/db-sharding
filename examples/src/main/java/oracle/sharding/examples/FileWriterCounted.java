package oracle.sharding.examples;

import oracle.util.metrics.Statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by somestuff on 8/10/17.
 */
public class FileWriterCounted implements Consumer<List<String>>, AutoCloseable {
    public final Writer writer;

    public FileWriterCounted(String filename) {
        try {
            this.writer = new BufferedWriter(new FileWriter(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final static Statistics.PerformanceMetric metric = Statistics.getGlobal()
            .createPerformanceMetric("StringFileWrites", Statistics.PER_SECOND);

    @Override
    public void close() throws Exception {
        writer.close();
    }

    @Override
    public void accept(List<String> strings) {
        try {
            for (String x : strings) {
                writer.append(x).append('\n');
            }

            metric.addCurrent(strings.size());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
