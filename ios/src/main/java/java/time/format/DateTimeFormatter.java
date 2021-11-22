package java.time.format;

/**
 * Hack file to make AppKit work on JRE7
 */
public class DateTimeFormatter {
    public static DateTimeFormatter ISO_LOCAL_DATE;
    public static DateTimeFormatter ISO_OFFSET_DATE_TIME;

    public boolean format(Object date) {
        throw new UnsupportedOperationException();
    }
}
