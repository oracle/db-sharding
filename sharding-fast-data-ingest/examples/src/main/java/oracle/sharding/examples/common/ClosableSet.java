/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at
**   http://oss.oracle.com/licenses/upl
*/

package oracle.sharding.examples.common;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of closable objects, which are closed with a single close,
 * but masking close() exceptions.
 *
 * (Oh no! What do we do!? What if close() throws a meaningful exception? ... he-he)
 */
public class ClosableSet implements AutoCloseable {
    private final Set<AutoCloseable> backedObjects = new HashSet<>();

    public ClosableSet(Collection<? extends AutoCloseable> objects) {
        backedObjects.addAll(objects);
    }

    public boolean add(AutoCloseable autoCloseable) {
        return backedObjects.add(autoCloseable);
    }

    public boolean remove(Object o) {
        return backedObjects.remove(o);
    }

    @Override
    public void close() {
        for (AutoCloseable x : backedObjects) {
            try { x.close(); } catch (Exception ignore) { }
        }
    }
}
