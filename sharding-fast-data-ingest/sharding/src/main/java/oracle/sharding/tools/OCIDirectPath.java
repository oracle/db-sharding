/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.tools;

/**
 * JNI bindings for OCI Direct Path operations
 */
public final class OCIDirectPath implements AutoCloseable {
    private long nativeObjectAddress;

    private static native void initialize();

    private native void createInternal();
    private native void connectInternal(String connectionString, String user, byte [] password);
    private native void closeInternal();

    /**
     * Set direct load target.
     *
     * @param schema Schema name.
     * @param table Table name.
     * @param partition Partition or subpartition name or null if not required.
     */
    public native void setTarget(String schema, String table, String partition);

    /**
     * Save buffer for data loading without assigning any column values.
     *
     * @param data buffer with row data
     */
    public native void setData(byte [] data);

    /**
     * Assign column value with data from the buffer already saved by setData call
     *
     * @param column Column id
     * @param offset Offset in saved buffer
     * @param length Length of the piece in saved buffer
     */
    public native void setValue(int column, int offset, int length);

    /**
     * Assign column value with the data from the buffer provided by data
     *
     * @param column Column id
     * @param data Buffer to use
     * @param offset Offset in the buffer provided
     * @param length Length of the piece in the buffer provided
     */
    public native void setValue(int column, byte [] data, int offset, int length);


    /**
     * Assign column value with the data from the buffer provided
     *
     * @param column Column id
     * @param data Buffer to use
     */
    public native void setValue(int column, byte [] data);

    /**
     * Go to the next row
     */
    public native void nextRow();

    /**
     * Add column definition for a target being added.
     * This function affects column id.
     *
     * @param name Name of the column in the table
     * @param dty Internal Oracle type
     * @param maxSize maximum size of the column (for OCI internal purposes only)
     */
    public native void addColumnDefinition(String name, int dty, int maxSize);

    /**
     * Add column definition for a target being added.
     * This function affects column id.
     * dty is always SQLT_CHR, since in most cases anything else does not make sense.
     *
     * @param name Name of the column in the table
     * @param maxSize maximum size of the column (for OCI internal purposes only)
     */
    public void addColumnDefinition(String name, int maxSize) {
        addColumnDefinition(name, 1, maxSize);
    }

    /**
     * Closes the current Direct Path Load Handle and creates a new one within the same connection.
     * All the properties, starting from target must be reset.
     * With the new handle, another loading target can be specified.
     *
     * Reopen method cannot be called after close() was called.
     * close() method closes the connection entirely.
     *
     * Implicitly calls finish()
     */
    public native void reopen();

    /**
     * Commits all the changes and closes the current Direct Path Load Handle.
     * Load cannot be performed after this method is called.
     * However, the same connection can be reused for another load if reopen() method is called.
     */
    public native void finish();

    /**
     * Reset the Direct Path Load Handle. Effectively the same as finish, but without saving changes.
     * Connection can be used later if reopen() is explicitly called.
     */
    public native void discard();

    /**
     * Initialize Direct Path Handles and begin load.
     * After calling being, you can no longer set properties.
     * NOTE: reopen() does not do it automatically.
     */
    public native void begin();

    public native void setAttribute(String attributeName, String value);

    public OCIDirectPath(String connectionString, String user, byte[] password)
    {
        createInternal();
        connectInternal(connectionString, user, password);
    }

    @Override
    public void close() throws Exception {
        try {
            finish();
        } catch (Exception ignore) {
        /* Ignore any exceptions if finish failed,
           we still have to close the connection */
        }

        closeInternal();
    }

    @Override
    protected void finalize() throws Throwable {
        closeInternal();
    }

    static {
        System.loadLibrary("dpjni");
        initialize();
    }
}
