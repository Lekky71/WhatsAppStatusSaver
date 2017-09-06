package com.hashcode.whatsappstatussaver.data;

/**
 * Created by oluwalekefakorede on 04/09/2017.
 */

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

class ReverseComparator extends AbstractFileComparator implements Serializable {

    private static final long serialVersionUID = -4808255005272229056L;
    private final Comparator<File> delegate;

    /**
     * Construct an instance with the specified delegate {@link Comparator}.
     *
     * @param delegate The comparator to delegate to
     */
    public ReverseComparator(final Comparator<File> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate comparator is missing");
        }
        this.delegate = delegate;
    }

    /**
     * Compare using the delegate Comparator, but reversing the result.
     *
     * @param file1 The first file to compare
     * @param file2 The second file to compare
     * @return the result from the delegate {@link Comparator#compare(Object, Object)}
     * reversing the value (i.e. positive becomes negative and vice versa)
     */
    @Override
    public int compare(final File file1, final File file2) {
        return delegate.compare(file2, file1); // parameters switched round
    }

    /**
     * String representation of this file comparator.
     *
     * @return String representation of this file comparator
     */
    @Override
    public String toString() {
        return super.toString() + "[" + delegate.toString() + "]";
    }

}