package com.example.mainproj.shared_lists_adapter_placeholder;

import java.util.*;

public class PlaceholderContent {

    public static final List<PlaceholderItem> ITEMS = new ArrayList<>();

    static {
        long now = System.currentTimeMillis();

        for (int i = 1; i <= 7; i++) {
            ITEMS.add(new PlaceholderItem(
                    false,                      // isRemoved
                    now,                        // timestampWhenAdded
                    "Item " + i,                // name
                    "System",                   // byWho
                    "Initial placeholder",      // reason
                    -1                           // timestampExpiresAt (-1 = never)
            ));
        }
    }

    public static class PlaceholderItem {

        public boolean isRemoved;
        public long timestampWhenAdded;
        public String name;
        public String byWho;
        public String reason;
        public long timestampExpiresAt; // -1 means never expires

        public PlaceholderItem(
                boolean isRemoved,
                long timestampWhenAdded,
                String name,
                String byWho,
                String reason,
                long timestampExpiresAt
        ) {
            this.isRemoved = isRemoved;
            this.timestampWhenAdded = timestampWhenAdded;
            this.name = name;
            this.byWho = byWho;
            this.reason = reason;
            this.timestampExpiresAt = timestampExpiresAt;
        }
    }
}
