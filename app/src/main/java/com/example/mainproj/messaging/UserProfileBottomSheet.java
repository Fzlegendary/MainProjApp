package com.example.mainproj.messaging;

import android.app.Dialog;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mainproj.R;
import com.example.mainproj.auth.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "UserProfileBottomSheet";

    private static final String ARG_USERNAME  = "username";
    private static final String ARG_BIO       = "bio";
    private static final String ARG_IS_ONLINE = "is_online";
    private static final String ARG_SAFE_EMAIL = "safe_email";
    private static final String ARG_ROLE_LEVEL = "role_level";
    private static final String ARG_LAST_SEEN_TIMESTAMP = "last_seen_timestamp";

    public static UserProfileBottomSheet newInstance(
            String username, String bio, long lastSeenTimestamp, boolean isOnline
    ) {
        return newInstance(username, bio, lastSeenTimestamp, isOnline, "", -1);
    }

    public static UserProfileBottomSheet newInstance(
            String username, String bio, long lastSeenTimestamp, boolean isOnline,
            String safeEmail, int roleLevel
    ) {
        UserProfileBottomSheet sheet = new UserProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_BIO, bio);
        args.putLong(ARG_LAST_SEEN_TIMESTAMP, lastSeenTimestamp);
        args.putBoolean(ARG_IS_ONLINE, isOnline);
        args.putString(ARG_SAFE_EMAIL, safeEmail);
        args.putInt(ARG_ROLE_LEVEL, roleLevel);
        sheet.setArguments(args);
        return sheet;
    }

    public interface ProfileActionListener {
        void onSendMessage(String username);
        void onViewProfile(String username);
        default void onManageRole(String username, String safeEmail) {}
        default void onManageGatePermissions(String username, String safeEmail) {}
    }

    private ProfileActionListener actionListener;

    public void setActionListener(ProfileActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            FrameLayout bottomSheet = d.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );

            if (bottomSheet != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                requireActivity().getWindowManager()
                        .getDefaultDisplay().getMetrics(metrics);
                int peekHeight = (int) (metrics.heightPixels * 0.66);

                BottomSheetBehavior<FrameLayout> behavior =
                        BottomSheetBehavior.from(bottomSheet);

                behavior.setPeekHeight(peekHeight);
                behavior.setMaxHeight(metrics.heightPixels);
                behavior.setFitToContents(false);
                behavior.setHalfExpandedRatio(0.66f);
                behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);

                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);
            }
        });

        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args     = getArguments();
        String username = args != null ? args.getString(ARG_USERNAME, "Unknown") : "Unknown";
        String bio      = args != null ? args.getString(ARG_BIO, "") : "";
        long lastSeenTimestamp = args != null ? args.getLong(ARG_LAST_SEEN_TIMESTAMP, 0L) : 0L;
        boolean online  = args != null && args.getBoolean(ARG_IS_ONLINE, false);
        String safeEmail = args != null ? args.getString(ARG_SAFE_EMAIL, "") : "";

        TextView  tvUsername  = view.findViewById(R.id.bsUsername);
        TextView  tvBio       = view.findViewById(R.id.bsBio);
        TextView  tvLastSeen  = view.findViewById(R.id.bsLastSeen);
        View      onlineDot   = view.findViewById(R.id.bsOnlineDot);

        MaterialButton btnMessage         = view.findViewById(R.id.bsBtnSendMessage);
        MaterialButton btnViewProfile     = view.findViewById(R.id.bsBtnViewProfile);

        tvUsername.setText(username);
        tvBio.setText(bio.isEmpty() ? "No bio yet." : bio);

        if (online) {
            tvLastSeen.setText("Online");
            onlineDot.setVisibility(View.VISIBLE);
        } else {
            tvLastSeen.setText(formatLastSeen(lastSeenTimestamp));
            onlineDot.setVisibility(View.INVISIBLE);
        }

        btnMessage.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onSendMessage(username);
            dismiss();
        });

        btnViewProfile.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onViewProfile(username);
            dismiss();
        });
    }

    private String formatLastSeen(long timestamp) {
        if (timestamp <= 0) return "Last seen recently";
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60_000) return "Just now";
        if (diff < 3_600_000) return (diff / 60_000) + "m ago";
        if (diff < 86_400_000) return (diff / 3_600_000) + "h ago";
        if (diff < 604_800_000) return (diff / 86_400_000) + " days ago";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }
}
