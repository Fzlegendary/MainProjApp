package com.example.mainproj.SearchModule;

import com.example.mainproj.shared_lists_adapter_placeholder.PlaceholderContent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ListFilter {

    private static final SimpleDateFormat DATE =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Filters a list of PlaceholderItems based on the search query.
     * - "by:username" filters by who added the item.
     * - Free-text searches match the item's name.
     * - Empty or invalid date inputs are ignored safely.
     */
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
                out = filterByWho(out, part.substring(3));
            } else if (part.startsWith("expiresBefore:")) {
                Long d = safeParse(part.substring(14));
                if (d != null) out = filterExpiresBefore(out, d);
            } else if (part.startsWith("expiresAfter:")) {
                Long d = safeParse(part.substring(13));
                if (d != null) out = filterExpiresAfter(out, d);
            } else if (part.startsWith("expires:")) {
                Long d = safeParse(part.substring(8));
                if (d != null) out = filterExpiresOn(out, d);
            } else if (part.startsWith("addedBefore:")) {
                Long d = safeParse(part.substring(12));
                if (d != null) out = filterAddedBefore(out, d);
            } else if (part.startsWith("addedAfter:")) {
                Long d = safeParse(part.substring(11));
                if (d != null) out = filterAddedAfter(out, d);
            } else if (part.startsWith("added:")) {
                Long d = safeParse(part.substring(6));
                if (d != null) out = filterAddedOn(out, d);
            } else if (part.equals("expired:true")) {
                out = filterExpired(out, true);
            } else if (part.equals("expired:false")) {
                out = filterExpired(out, false);
            } else {
                // Free-text search → filter by name
                String lower = part.toLowerCase();
                List<PlaceholderContent.PlaceholderItem> filtered = new ArrayList<>();
                for (PlaceholderContent.PlaceholderItem item : out) {
                    if (item.name.toLowerCase().contains(lower)) {
                        filtered.add(item);
                    }
                }
                out = filtered;
            }
        }
        return out;
    }

    // ---------- HELPER: SAFE PARSE ----------
    private static Long safeParse(String s) {
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return DATE.parse(s).getTime();
        } catch (ParseException e) {
            // ignore invalid dates
            return null;
        }
    }

    // ---------- FILTER BY WHO ----------
    private static List<PlaceholderContent.PlaceholderItem> filterByWho(
            List<PlaceholderContent.PlaceholderItem> src,
            String csv
    ) {
        Set<String> allowed = new HashSet<>();
        for (String s : csv.split(",")) {
            s = s.trim();
            if (!s.isEmpty()) allowed.add(s.toLowerCase());
        }

        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (PlaceholderContent.PlaceholderItem i : src) {
            if (allowed.contains(i.byWho.toLowerCase())) {
                out.add(i);
            }
        }
        return out;
    }

    // ---------- FILTER EXPIRES ----------
    private static List<PlaceholderContent.PlaceholderItem> filterExpiresBefore(
            List<PlaceholderContent.PlaceholderItem> src, long date
    ) {
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampExpiresAt != -1 && i.timestampExpiresAt < date) out.add(i);
        }
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterExpiresAfter(
            List<PlaceholderContent.PlaceholderItem> src, long date
    ) {
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampExpiresAt == -1 || i.timestampExpiresAt > date) out.add(i);
        }
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterExpiresOn(
            List<PlaceholderContent.PlaceholderItem> src, long day
    ) {
        long next = day + 86_400_000L;
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.timestampExpiresAt != -1 &&
                    i.timestampExpiresAt >= day && i.timestampExpiresAt < next) {
                out.add(i);
            }
        }
        return out;
    }

    // ---------- FILTER EXPIRED ----------
    private static List<PlaceholderContent.PlaceholderItem> filterExpired(
            List<PlaceholderContent.PlaceholderItem> src, boolean expired) {
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) {
            if (i.isExpired == expired) out.add(i);
        }
        return out;
    }

    // ---------- FILTER ADDED ----------
    private static List<PlaceholderContent.PlaceholderItem> filterAddedBefore(
            List<PlaceholderContent.PlaceholderItem> src, long date
    ) {
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) if (i.timestampWhenAdded < date) out.add(i);
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterAddedAfter(
            List<PlaceholderContent.PlaceholderItem> src, long date
    ) {
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) if (i.timestampWhenAdded > date) out.add(i);
        return out;
    }

    private static List<PlaceholderContent.PlaceholderItem> filterAddedOn(
            List<PlaceholderContent.PlaceholderItem> src, long day
    ) {
        long next = day + 86_400_000L;
        List<PlaceholderContent.PlaceholderItem> out = new ArrayList<>();
        for (var i : src) if (i.timestampWhenAdded >= day && i.timestampWhenAdded < next) out.add(i);
        return out;
    }
}
