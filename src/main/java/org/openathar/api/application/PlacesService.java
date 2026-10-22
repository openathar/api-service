package org.openathar.api.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.openathar.api.domain.Place;
import org.openathar.api.port.in.PlacesUseCase;
import org.springframework.stereotype.Service;

/**
 * City lookup over a bundled GeoNames extract (cities above 15,000 people,
 * CC BY 4.0) — no external geocoder is called, so no request ever leaves the
 * server and nothing about a query is shared. The UTC offset is resolved from
 * the city's IANA zone for the requested date, daylight saving included.
 */
@Service
public class PlacesService implements PlacesUseCase {

    private record Row(String name, String country, double lat, double lon, String zone, long pop, String[] keys) {
    }

    private static volatile List<Row> rows;

    @Override
    public List<Place> search(String query, int limit, LocalDate at) {
        String q = normalize(query == null ? "" : query);
        if (q.length() < 2) throw new IllegalArgumentException("q must have at least 2 characters");
        if (limit < 1 || limit > 25) throw new IllegalArgumentException("limit must be between 1 and 25");
        record Hit(Row row, int score) {
        }
        List<Hit> hits = new ArrayList<>();
        for (Row r : rows()) {
            int best = 0;
            for (String k : r.keys()) {
                int s = k.equals(q) ? 3 : k.startsWith(q) ? 2 : k.contains(q) ? 1 : 0;
                if (s > best) best = s;
                if (best == 3) break;
            }
            if (best > 0) hits.add(new Hit(r, best));
        }
        hits.sort(Comparator.comparingInt(Hit::score).reversed().thenComparing((Hit h) -> -h.row().pop()));
        LocalDate day = at != null ? at : LocalDate.now(ZoneOffset.UTC);
        return hits.stream().limit(limit).map(h -> {
            Row r = h.row();
            double offset = ZoneId.of(r.zone()).getRules().getOffset(day.atTime(12, 0).toInstant(ZoneOffset.UTC)).getTotalSeconds() / 3600.0;
            return new Place(r.name(), r.country(), r.lat(), r.lon(), r.zone(), r.pop(), offset);
        }).toList();
    }

    static String normalize(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static List<Row> rows() {
        List<Row> r = rows;
        if (r == null) {
            synchronized (PlacesService.class) {
                if (rows == null) rows = load();
                r = rows;
            }
        }
        return r;
    }

    private static List<Row> load() {
        try (InputStream in = PlacesService.class.getResourceAsStream("/places/cities.tsv")) {
            if (in == null) throw new IllegalStateException("places/cities.tsv is missing");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<Row> out = new ArrayList<>(36_000);
            String line;
            while ((line = br.readLine()) != null) {
                String[] c = line.split("\t", -1);
                if (c.length < 8) continue;
                List<String> keys = new ArrayList<>();
                keys.add(normalize(c[0]));
                keys.add(normalize(c[1]));
                if (!c[2].isEmpty()) for (String a : c[2].split("\\|")) keys.add(normalize(a));
                out.add(new Row(c[0], c[5], Double.parseDouble(c[3]), Double.parseDouble(c[4]), c[7],
                    Long.parseLong(c[6]), keys.stream().distinct().toArray(String[]::new)));
            }
            return List.copyOf(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
