/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.examples.loading;

import oracle.sharding.tools.DirectPathLoadSink;
import oracle.sharding.tools.SeparatedString;
import oracle.util.metrics.Statistics;

import java.util.List;

/**
 * The version of DirectPathLoadSink with counting the inserted rows
 * and evaluating the performance.
 */
public class DirectPathLoadSinkCounted extends DirectPathLoadSink {
    public DirectPathLoadSinkCounted(Builder builder) {
        super(builder.getDpl(), builder.getColumnCount());
    }

    private final static Statistics.PerformanceMetric metric = Statistics.getGlobal()
            .createPerformanceMetric("DPLInserts", Statistics.PER_SECOND);

    @Override
    public void accept(List<SeparatedString> separatedStrings) {
        super.accept(separatedStrings);
        metric.addCurrent(separatedStrings.size());
    }
}
