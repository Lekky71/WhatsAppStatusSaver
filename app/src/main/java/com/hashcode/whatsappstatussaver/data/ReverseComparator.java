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
    public ReverseComparator(final Comparator<File> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate comparator is missing");
        }
        this.delegate = delegate;
    }
    @Override
    public int compare(final File file1, final File file2) {
        return delegate.compare(file2, file1); // parameters switched round
    }
    @Override
    public String toString() {
        return super.toString() + "[" + delegate.toString() + "]";
    }
}