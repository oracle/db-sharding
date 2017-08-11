package oracle.sharding.examples;

import oracle.sharding.tools.DirectPathLoadSink;
import oracle.sharding.tools.SeparatedString;
import oracle.util.metrics.Statistics;

import java.util.List;

/**
 * The version of DirectPathLoadSink with counting the inserted rows
 * and evaluating the performance.
 */
public class DirectPathLoadSinkCounted extends DirectPathLoadSink {
    private DirectPathLoadSinkCounted(Builder builder) {
        super(builder.getDpl(), builder.getColumnCount());
    }

    private final static Statistics.PerformanceMetric metric = Statistics.getGlobal()
            .createPerformanceMetric("DPLInserts", Statistics.PER_SECOND);

    @Override
    public void accept(List<SeparatedString> separatedStrings) {
        metric.addCurrent(separatedStrings.size());
        super.accept(separatedStrings);
    }
}
