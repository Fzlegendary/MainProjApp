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
        public long timestampExpiresAt;         // -1 = never expires
        public boolean isExpired;               // true once the expiry time has passed
        public long deleteAt;                   // -1 = no deletion scheduled

        public PlaceholderItem(
                String id,
                boolean isRemoved,
                long timestampWhenAdded,
                String name,
                String byWho,
                String reason,
                long timestampExpiresAt,
                boolean isExpired,
                long deleteAt
        ) {
            this.id = id;
            this.isRemoved = isRemoved;
            this.timestampWhenAdded = timestampWhenAdded;
            this.name = name;
            this.byWho = byWho;
            this.reason = reason;
            this.timestampExpiresAt = timestampExpiresAt;
            this.isExpired = isExpired;
            this.deleteAt = deleteAt;
        }

        /** Called when this item expires — updates state flags */
        public void onExpired() {
            this.isExpired = true;
            this.deleteAt = System.currentTimeMillis() + 3 * 3600 * 1000L;
        }
    }
}