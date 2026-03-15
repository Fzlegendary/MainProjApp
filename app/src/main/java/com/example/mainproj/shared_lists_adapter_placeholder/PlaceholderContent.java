package com.example.mainproj.shared_lists_adapter_placeholder;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderContent {

    // ✅ Whitelist placeholder items
    public static final List<PlaceholderItem> ITEMS = new ArrayList<>();

    // ✅ Blacklist placeholder items
    public static final List<PlaceholderItem> BLACKLIST_ITEMS = new ArrayList<>();

    static {
        long now = System.currentTimeMillis();

        // Initialize Whitelist items
        for (int i = 1; i <= 7; i++) {
            ITEMS.add(new PlaceholderItem(
                    null,                       // id (will be set after server add)
                    false,                      // isRemoved
                    now,                        // timestampWhenAdded
                    "Whitelist Item " + i,      // name
                    "System",                   // byWho
                    "Initial placeholder",      // reason
                    -1                           // timestampExpiresAt (-1 = never)
            ));
        }

        // Initialize Blacklist items
        for (int i = 1; i <= 5; i++) {
            BLACKLIST_ITEMS.add(new PlaceholderItem(
                    null,
                    false,
                    now,
                    "Blacklist Item " + i,
                    "System",
                    "Initial placeholder",
                    -1
            ));
        }
    }

    /** Represents a single list item (Whitelist or Blacklist) */
    public static class PlaceholderItem {

        public String id;                       // Firebase key (added for server sync)
        public boolean isRemoved;
        public long timestampWhenAdded;
        public String name;
        public String byWho;
        public String reason;
        public long timestampExpiresAt; // -1 = never expires

        public PlaceholderItem(
                String id,
                boolean isRemoved,
                long timestampWhenAdded,
                String name,
                String byWho,
                String reason,
                long timestampExpiresAt
        ) {
            this.id = id;
            this.isRemoved = isRemoved;
            this.timestampWhenAdded = timestampWhenAdded;
            this.name = name;
            this.byWho = byWho;
            this.reason = reason;
            this.timestampExpiresAt = timestampExpiresAt;
        }
    }
}