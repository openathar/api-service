package org.openathar.api.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.openathar.api.domain.PrayerTimes;
import org.openathar.api.domain.PrayerTimesQuery;
import org.openathar.api.port.in.CalendarFeedUseCase;
import org.openathar.api.port.in.PrayerTimesUseCase;
import org.springframework.stereotype.Service;

/**
 * iCalendar (RFC 5545) feed of prayer times. Event times are written in UTC
 * (the calculation yields local wall-clock times plus the given UTC offset),
 * so every calendar app shows the right moment without a time zone database.
 * Each prayer is a short event; calendars re-fetch the feed on their own, so
 * it stays current as long as the subscription URL is used.
 */
@Service
public class CalendarFeedService implements CalendarFeedUseCase {

    static final int MAX_DAYS = 62;
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final int EVENT_MINUTES = 10;

    private static final Map<String, Map<String, String>> NAMES = Map.of(
        "en", Map.of("fajr", "Fajr", "sunrise", "Sunrise", "dhuhr", "Dhuhr", "asr", "Asr", "maghrib", "Maghrib", "isha", "Isha"),
        "de", Map.of("fajr", "Fadschr", "sunrise", "Sonnenaufgang", "dhuhr", "Dhuhr", "asr", "Asr", "maghrib", "Maghrib", "isha", "Ischa"),
        "ar", Map.of("fajr", "الفجر", "sunrise", "الشروق", "dhuhr", "الظهر", "asr", "العصر", "maghrib", "المغرب", "isha", "العشاء"));
    private static final Map<String, String> CALNAME = Map.of(
        "en", "Prayer times", "de", "Gebetszeiten", "ar", "مواقيت الصلاة");

    private final PrayerTimesUseCase prayerTimes;

    public CalendarFeedService(PrayerTimesUseCase prayerTimes) {
        this.prayerTimes = prayerTimes;
    }

    @Override
    public String prayerTimes(double lat, double lon, LocalDate from, int days, String method, double utcOffsetHours,
            String lang, boolean includeSunrise) {
        if (days < 1 || days > MAX_DAYS) throw new IllegalArgumentException("days must be between 1 and " + MAX_DAYS);
        Map<String, String> names = NAMES.get(lang);
        if (names == null) throw new IllegalArgumentException("unknown lang: " + lang + " (valid: en, de, ar)");
        String[] order = includeSunrise
            ? new String[] {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"}
            : new String[] {"fajr", "dhuhr", "asr", "maghrib", "isha"};
        long offsetSeconds = Math.round(utcOffsetHours * 3600);
        String stamp = UTC.format(LocalDateTime.now(ZoneOffset.UTC));
        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//Athar//Prayer times//EN");
        line(sb, "CALSCALE:GREGORIAN");
        line(sb, "METHOD:PUBLISH");
        line(sb, "X-WR-CALNAME:" + CALNAME.get(lang));
        line(sb, "REFRESH-INTERVAL;VALUE=DURATION:PT12H");
        line(sb, "X-PUBLISHED-TTL:PT12H");
        for (int i = 0; i < days; i++) {
            LocalDate day = from.plusDays(i);
            PrayerTimes t = prayerTimes.calculate(new PrayerTimesQuery(lat, lon, day, method, utcOffsetHours));
            LocalTime prev = null;
            LocalDate date = day;
            for (String name : order) {
                String value = switch (name) {
                    case "fajr" -> t.fajr();
                    case "sunrise" -> t.sunrise();
                    case "dhuhr" -> t.dhuhr();
                    case "asr" -> t.asr();
                    case "maghrib" -> t.maghrib();
                    default -> t.isha();
                };
                if (value == null) continue;
                LocalTime time = LocalTime.parse(value);
                if (prev != null && time.isBefore(prev)) date = date.plusDays(1);
                prev = time;
                LocalDateTime start = LocalDateTime.of(date, time).minusSeconds(offsetSeconds);
                line(sb, "BEGIN:VEVENT");
                line(sb, "UID:" + name + "-" + day + "-" + lat + "," + lon + "-" + t.method() + "@openathar.org");
                line(sb, "DTSTAMP:" + stamp);
                line(sb, "DTSTART:" + UTC.format(start));
                line(sb, "DTEND:" + UTC.format(start.plusMinutes(EVENT_MINUTES)));
                line(sb, "SUMMARY:" + escape(names.get(name)));
                line(sb, "TRANSP:TRANSPARENT");
                line(sb, "END:VEVENT");
            }
        }
        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }

    /** RFC 5545 line: CRLF-terminated, folded at 75 octets (all our lines are short; fold defensively). */
    private static void line(StringBuilder sb, String s) {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= 75) {
            sb.append(s).append("\r\n");
            return;
        }
        int start = 0;
        boolean first = true;
        while (start < s.length()) {
            int budget = first ? 75 : 74;
            int end = start;
            int used = 0;
            while (end < s.length()) {
                int cp = s.codePointAt(end);
                int len = new String(Character.toChars(cp)).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                if (used + len > budget) break;
                used += len;
                end += Character.charCount(cp);
            }
            sb.append(first ? "" : " ").append(s, start, end).append("\r\n");
            start = end;
            first = false;
        }
    }
}
