package com.example.mainproj.shared_lists_adapter_placeholder;

public class PlaceholderContent {

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