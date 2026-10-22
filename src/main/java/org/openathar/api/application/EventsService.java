package org.openathar.api.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.openathar.api.domain.IslamicEvent;
import org.openathar.api.port.in.EventsUseCase;
import org.openathar.core.Hijri;
import org.springframework.stereotype.Service;

/**
 * Islamic observances as pure calendar facts (Umm al-Qura dates). Two are
 * deliberately worded neutrally: the Mawlid is a date, not a call to observe
 * it (scholars differ on whether to), and the night of Laylat al-Qadr is
 * stated as unknown, with the 27th the common choice.
 */
@Service
public class EventsService implements EventsUseCase {

    private record Def(String key, int month, int day, Map<String, String> name, Map<String, String> note) {
    }

    private static final List<Def> EVENTS = List.of(
        new Def("new-year", 1, 1,
            Map.of("en", "Islamic New Year", "de", "Islamisches Neujahr", "ar", "رأس السنة الهجرية"),
            Map.of("en", "Start of the Hijri year.", "de", "Beginn des Hijri-Jahres.", "ar", "بداية السنة الهجرية.")),
        new Def("ashura", 1, 10,
            Map.of("en", "Ashura", "de", "Aschura", "ar", "عاشوراء"),
            Map.of("en", "10th of Muharram.", "de", "10. Muharram.", "ar", "العاشر من محرّم.")),
        new Def("mawlid", 3, 12,
            Map.of("en", "Mawlid al-Nabi", "de", "Mawlid an-Nabi", "ar", "المولد النبوي"),
            Map.of("en", "Birth date of the Prophet ﷺ. Observance of this date is debated among scholars.",
                "de", "Geburtsdatum des Propheten ﷺ. Ob dieser Tag begangen werden soll, ist unter Gelehrten umstritten.",
                "ar", "تاريخ مولد النبي ﷺ. إحياء هذا اليوم مسألة خلافية بين العلماء.")),
        new Def("ramadan-start", 9, 1,
            Map.of("en", "Start of Ramadan", "de", "Beginn des Ramadan", "ar", "بداية رمضان"),
            Map.of("en", "Subject to moon sighting; the calculated date may shift by a day.",
                "de", "Abhängig von der Mondsichtung; das berechnete Datum kann um einen Tag abweichen.",
                "ar", "مرتبط برؤية الهلال؛ قد يختلف التاريخ المحسوب بيوم واحد.")),
        new Def("laylat-al-qadr", 9, 27,
            Map.of("en", "Laylat al-Qadr (commonly observed)", "de", "Laylat al-Qadr (üblich beobachtet)",
                "ar", "ليلة القدر (المعتاد إحياؤها)"),
            Map.of("en", "The exact night is not specified in tradition — commonly the 27th, but any odd night of the last ten days of Ramadan.",
                "de", "Die genaue Nacht ist überliefert unbekannt — üblich ist die 27., überliefert sind aber alle ungeraden Nächte der letzten zehn Ramadan-Tage.",
                "ar", "لم تُحدَّد الليلة بعينها في النص — الشائع ليلة ٢٧، لكن الوارد أنها إحدى الليالي الوترية من العشر الأواخر.")),
        new Def("eid-al-fitr", 10, 1,
            Map.of("en", "Eid al-Fitr", "de", "Eid al-Fitr", "ar", "عيد الفطر"),
            Map.of("en", "1st of Shawwal, end of Ramadan.", "de", "1. Schawwal, Ende des Ramadan.", "ar", "الأول من شوّال، نهاية رمضان.")),
        new Def("eid-al-adha", 12, 10,
            Map.of("en", "Eid al-Adha", "de", "Eid al-Adha", "ar", "عيد الأضحى"),
            Map.of("en", "10th of Dhu al-Hijjah, during Hajj.", "de", "10. Dhu al-Hijjah, während der Hajj.",
                "ar", "العاشر من ذي الحجة، أثناء الحج.")));

    @Override
    public List<IslamicEvent> upcoming(LocalDate from, int count, String locale) {
        if (count < 1 || count > 20) throw new IllegalArgumentException("count must be between 1 and 20");
        String lang = Map.of("en", "en", "de", "de", "ar", "ar").getOrDefault(locale == null ? "en" : locale, null);
        if (lang == null) throw new IllegalArgumentException("unknown locale: " + locale + " (valid: en, ar, de)");
        int year = Hijri.gregorianToHijri(from).year();
        List<IslamicEvent> out = new ArrayList<>();
        // Three Hijri years are enough for any count up to 20 (seven observances a year).
        for (int y = year; y <= year + 3; y++) {
            for (Def d : EVENTS) {
                LocalDate date;
                try {
                    date = Hijri.hijriToGregorian(y, d.month(), d.day());
                } catch (RuntimeException e) {
                    continue; // beyond the calendar table
                }
                if (!date.isBefore(from)) {
                    out.add(new IslamicEvent(d.key(), d.name().get(lang), d.note().get(lang), d.day(), d.month(), y,
                        date, ChronoUnit.DAYS.between(from, date)));
                }
            }
        }
        out.sort(Comparator.comparing(IslamicEvent::gregorianDate));
        return out.stream().limit(count).toList();
    }
}
