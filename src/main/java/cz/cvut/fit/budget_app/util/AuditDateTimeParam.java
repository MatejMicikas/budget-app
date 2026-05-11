package cz.cvut.fit.budget_app.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Parses audit log query datetime params from browsers ({@code datetime-local}, ISO variants). */
public final class AuditDateTimeParam {

    private AuditDateTimeParam() {}

    /** Order matters: try explicit patterns before ISO (browser often omits seconds). */
    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

    /**
     * @param raw query parameter (may be URL-decoded); blank → null
     * @throws IllegalArgumentException if non-blank and not parseable
     */
    public static LocalDateTime parseOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter f : FORMATTERS) {
            try {
                return LocalDateTime.parse(s, f);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        throw new IllegalArgumentException(
                "Invalid datetime parameter: \"" + raw + "\". Use ISO local format, e.g. 2026-05-11T14:30 or 2026-05-11T14:30:00.");
    }
}
