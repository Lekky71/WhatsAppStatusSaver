package com.hashcode.whatsappstatussaver.data;

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

public class LastModifiedFileComparator extends AbstractFileComparator implements Serializable {

    private static final long serialVersionUID = 7372168004395734046L;

    /** Last modified comparator instance */
    public static final Comparator<File> LASTMODIFIED_COMPARATOR = new LastModifiedFileComparator();

    /** Reverse last modified comparator instance */
    public static final Comparator<File> LASTMODIFIED_REVERSE = new ReverseComparator(LASTMODIFIED_COMPARATOR);

    /**
     * Compare the last the last modified date/time of two files.
     *
     * @param file1 The first file to compare
     * @param file2 The second file to compare
     * @return a negative value if the first file's lastmodified date/time
     * is less than the second, zero if the lastmodified date/time are the
     * same and a positive value if the first files lastmodified date/time
     * is greater than the second file.
     *
     */
    @Override
    public int compare(final File file1, final File file2) {
        final long result = file1.lastModified() - file2.lastModified();
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
