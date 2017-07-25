package oracle.util.metrics;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by somestuff on 4/13/17.
 */
public class ProgressInputStream extends InputStream {
    private final InputStream originalStream;
    private final Statistics.Metric metric;

    public ProgressInputStream(InputStream originalStream, Statistics.Metric statistics) {
        this.originalStream = originalStream;
        this.metric = statistics;
    }

    @Override
    public int read() throws IOException {
        metric.addCurrent(1);
        return originalStream.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        metric.addCurrent(len);
        return originalStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        metric.addCurrent(n);
        return originalStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return originalStream.available();
    }

    @Override
    public void close() throws IOException {
        metric.setValue("Finished");
        originalStream.close();
    }

    @Override
    public void mark(int readlimit) {
        originalStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        originalStream.reset();
    }

    @Override
    public boolean markSupported() {
        return originalStream.markSupported();
    }

}
