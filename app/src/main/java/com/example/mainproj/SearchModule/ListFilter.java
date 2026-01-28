package com.example.mainproj.SearchModule;

import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ListFilter {

    private static final SimpleDateFormat DATE =
            new SimpleDateFormat("dd/MM/yy", Locale.getDefault());

    public static List<PlaceholderContent.PlaceholderItem> apply(
            List<PlaceholderContent.PlaceholderItem> source,
            String query
    ) {
        if (query == null || query.trim().isEmpty()) return source;

        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>(source);
        String[] parts = query.split(",");

        for (String raw : parts) {
            String part = raw.trim();

            if (part.startsWith("by:")) {
                out = filterBy(out, part.substring(3));
            } else if (part.startsWith("expiresBefore:")) {
                out = filterExpiresBefore(out, part.substring(14));
            } else if (part.startsWith("expiresAfter:")) {
                out = filterExpiresAfter(out, part.substring(13));
            } else if (part.startsWith("expires:")) {
                out = filterExpiresOn(out, part.substring(8));
            } else if (part.startsWith("addedBefore:")) {
                out = filterAddedBefore(out, part.substring(12));
            } else if (part.startsWith("addedAfter:")) {
                out = filterAddedAfter(out, part.substring(11));
            } else if (part.startsWith("added:")) {
                out = filterAddedOn(out, part.substring(6));
            }
        }
        return out;
    }

    // ---------- BY ----------
    private static List<PlaceholderContent.PlaceholderItem> filterBy(
            List<PlaceholderContent.PlaceholderItem> src,
            String csv
    ) {
        Set<String> allowed = new HashSet<>();
        for (String s : csv.split(",")) {
            allowed.add(s.trim().toLowerCase());
        }

        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (allowed.contains(i.byWho.toLowerCase())) {
                out.add(i);
            }
        }
        return out;
    }

    // ---------- EXPIRES ----------
    private static List<PlaceholderContent.PlaceholderItem> filterExpiresBefore(
            List<PlaceholderContent.PlaceholderItem> src, String date
    ) {
        long d = parse(date);
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampExpiresAt != -1 && i.timestampExpiresAt < d) {
                out.add(i);
            }
        }
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterExpiresAfter(
            List<PlaceholderContent.PlaceholderItem> src, String date
    ) {
        long d = parse(date);
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampExpiresAt != -1 && i.timestampExpiresAt > d) {
                out.add(i);
            }
        }
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterExpiresOn(
            List<PlaceholderContent.PlaceholderItem> src, String date
    ) {
        long day = parse(date);
        long next = day + 86_400_000L;

        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampExpiresAt >= day && i.timestampExpiresAt < next) {
                out.add(i);
            }
        }
        return out;
    }

    // ---------- ADDED ----------
    private static List<PlaceholderContent.PlaceholderItem> filterAddedBefore(
            List<PlaceholderContent.PlaceholderItem> src, String date
    ) {
        long d = parse(date);
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampWhenAdded < d) {
                out.add(i);
            }
        }
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterAddedAfter(
            List<PlaceholderContent.PlaceholderItem> src, String date
    ) {
        long d = parse(date);
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampWhenAdded > d) {
                out.add(i);
            }
        }
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterAddedOn(
            List<PlaceholderContent.PlaceholderItem> src, String date
    ) {
        long day = parse(date);
        long next = day + 86_400_000L;

        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampWhenAdded >= day && i.timestampWhenAdded < next) {
                out.add(i);
            }
        }
        return out;
    }

    private static long parse(String s) {
        try {
            return DATE.parse(s.trim()).getTime();
        } catch (ParseException e) {
            throw new RuntimeException("Bad date: " + s);
        }
    }
}
