package java.time;

import java.time.format.DateTimeFormatter;

/**
 * Hack file to make AppKit work on JRE7
 */
public class LocalDate {
    public static LocalDate parse(String date, DateTimeFormatter formatter) {
        throw new UnsupportedOperationException();
    }
}
