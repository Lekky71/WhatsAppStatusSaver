package com.hashcode.whatsappstatussaver.data;

/**
 * Created by oluwalekefakorede on 04/09/2017.
 */

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

abstract class AbstractFileComparator implements Comparator<File> {

    /**
     * Sort an array of files.
     * <p>
     * This method uses {@link Arrays#sort(Object[], Comparator)}
     * and returns the original array.
     *
     * @param files The files to sort, may be null
     * @return The sorted array
     * @since 2.0
     */
    public File[] sort(final File... files) {
        if (files != null) {
            Arrays.sort(files, this);
        }
        return files;
    }

    /**
     * Sort a List of files.
     * <p>
     * This method uses {@link Collections#sort(List, Comparator)}
     * and returns the original list.
     *
     * @param files The files to sort, may be null
     * @return The sorted list
     * @since 2.0
     */
    public List<File> sort(final List<File> files) {
        if (files != null) {
            Collections.sort(files, this);
        }
        return files;
    }

    /**
     * String representation of this file comparator.
     *
     * @return String representation of this file comparator
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}