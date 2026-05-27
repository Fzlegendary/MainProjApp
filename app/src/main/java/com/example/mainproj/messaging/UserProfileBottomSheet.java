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

/**
 * UserProfileBottomSheet
 *
 * Opens at 2/3 screen height, draggable to full screen.
 * Dismisses when tapping outside or dragging down.
 *
 * Usage from adapter:
 *
 *   UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
 *       username, bio, lastSeen, isOnline
 *   );
 *   sheet.setActionListener(...);
 *   sheet.show(fragmentManager, UserProfileBottomSheet.TAG);
 */
public class UserProfileBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "UserProfileBottomSheet";

    private static final String ARG_USERNAME  = "username";
    private static final String ARG_BIO       = "bio";
    private static final String ARG_LAST_SEEN = "last_seen";
    private static final String ARG_IS_ONLINE = "is_online";
    private static final String ARG_SAFE_EMAIL = "safe_email";
    private static final String ARG_ROLE_LEVEL = "role_level";

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------
    public static UserProfileBottomSheet newInstance(
            String username, String bio, String lastSeen, boolean isOnline
    ) {
        return newInstance(username, bio, lastSeen, isOnline, "", -1);
    }

    public static UserProfileBottomSheet newInstance(
            String username, String bio, String lastSeen, boolean isOnline,
            String safeEmail, int roleLevel
    ) {
        UserProfileBottomSheet sheet = new UserProfileBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_BIO, bio);
        args.putString(ARG_LAST_SEEN, lastSeen);
        args.putBoolean(ARG_IS_ONLINE, isOnline);
        args.putString(ARG_SAFE_EMAIL, safeEmail);
        args.putInt(ARG_ROLE_LEVEL, roleLevel);
        sheet.setArguments(args);
        return sheet;
    }

    // -------------------------------------------------------------------------
    // Listener
    // -------------------------------------------------------------------------
    public interface ProfileActionListener {
        void onSendMessage(String username);
        void onViewProfile(String username);
        void onBlockUser(String username);
        default void onManageRole(String username, String safeEmail) {}
        default void onManageGatePermissions(String username, String safeEmail) {}
    }

    private ProfileActionListener actionListener;

    public void setActionListener(ProfileActionListener listener) {
        this.actionListener = listener;
    }

    // -------------------------------------------------------------------------
    // Set up dialog: 2/3 height, expandable, dismiss on outside tap
    // -------------------------------------------------------------------------
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;

            // Get the bottom sheet FrameLayout inside the dialog
            FrameLayout bottomSheet = d.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );

            if (bottomSheet != null) {
                // Set height to 2/3 of screen
                DisplayMetrics metrics = new DisplayMetrics();
                requireActivity().getWindowManager()
                        .getDefaultDisplay().getMetrics(metrics);
                int peekHeight = (int) (metrics.heightPixels * 0.66);

                BottomSheetBehavior<FrameLayout> behavior =
                        BottomSheetBehavior.from(bottomSheet);

                // Start at 2/3, allow dragging up to full screen
                behavior.setPeekHeight(peekHeight);
                behavior.setMaxHeight(metrics.heightPixels);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                // Drag down to dismiss
                behavior.setHideable(true);
                behavior.setSkipCollapsed(false);
            }
        });

        // Tapping outside the sheet dismisses it
        dialog.setCanceledOnTouchOutside(true);

        return dialog;
    }

    // -------------------------------------------------------------------------
    // Inflate layout
    // -------------------------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_user_profile, container, false);
    }

    // -------------------------------------------------------------------------
    // Bind views
    // -------------------------------------------------------------------------
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args     = getArguments();
        String username = args != null ? args.getString(ARG_USERNAME, "Unknown") : "Unknown";
        String bio      = args != null ? args.getString(ARG_BIO, "") : "";
        String lastSeen = args != null ? args.getString(ARG_LAST_SEEN, "") : "";
        boolean online  = args != null && args.getBoolean(ARG_IS_ONLINE, false);
        String safeEmail = args != null ? args.getString(ARG_SAFE_EMAIL, "") : "";
        int targetRoleLevel = args != null ? args.getInt(ARG_ROLE_LEVEL, -1) : -1;

        TextView  tvUsername  = view.findViewById(R.id.bsUsername);
        TextView  tvBio       = view.findViewById(R.id.bsBio);
        TextView  tvLastSeen  = view.findViewById(R.id.bsLastSeen);
        View      onlineDot   = view.findViewById(R.id.bsOnlineDot);

        MaterialButton btnMessage         = view.findViewById(R.id.bsBtnSendMessage);
        MaterialButton btnViewProfile     = view.findViewById(R.id.bsBtnViewProfile);
        MaterialButton btnBlock           = view.findViewById(R.id.bsBtnBlock);
        MaterialButton btnManageRole      = view.findViewById(R.id.bsBtnManageRole);
        MaterialButton btnManageGatePerms = view.findViewById(R.id.bsBtnManageGatePerms);

        tvUsername.setText(username);
        tvBio.setText(bio.isEmpty() ? "No bio yet." : bio);

        if (online) {
            tvLastSeen.setText("Online");
            onlineDot.setVisibility(View.VISIBLE);
        } else {
            tvLastSeen.setText(lastSeen.isEmpty() ? "Last seen recently" : "Last seen " + lastSeen);
            onlineDot.setVisibility(View.INVISIBLE);
        }

        // Admin buttons: visible only if caller provides safeEmail and current user has authority
        if (!safeEmail.isEmpty() && SessionManager.canModifyUser(targetRoleLevel)) {
            btnManageRole.setVisibility(View.VISIBLE);
            btnManageGatePerms.setVisibility(View.VISIBLE);
        } else {
            btnManageRole.setVisibility(View.GONE);
            btnManageGatePerms.setVisibility(View.GONE);
        }

        btnMessage.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onSendMessage(username);
            dismiss();
        });

        btnViewProfile.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onViewProfile(username);
            dismiss();
        });

        btnBlock.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onBlockUser(username);
            dismiss();
        });

        btnManageRole.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onManageRole(username, safeEmail);
            dismiss();
        });

        btnManageGatePerms.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onManageGatePermissions(username, safeEmail);
            dismiss();
        });
    }
}

/*
 * ─────────────────────────────────────────────
 * HOW TO CALL FROM YOUR ADAPTER
 * ─────────────────────────────────────────────
 *
 * 1. Add FragmentManager to your adapter constructor:
 *
 *   private final FragmentManager fragmentManager;
 *
 *   public ChatAdapter(List<ChatItem> items, FragmentManager fm) {
 *       this.fragmentManager = fm;
 *   }
 *
 * 2. On item click in onBindViewHolder:
 *
 *   holder.itemView.setOnClickListener(v -> {
 *       ChatItem chat = items.get(holder.getAdapterPosition());
 *
 *       UserProfileBottomSheet sheet = UserProfileBottomSheet.newInstance(
 *           chat.getUsername(),
 *           chat.getBio(),
 *           chat.getLastSeen(),
 *           chat.isOnline()
 *       );
 *
 *       sheet.setActionListener(new UserProfileBottomSheet.ProfileActionListener() {
 *           @Override public void onSendMessage(String username) { }
 *           @Override public void onViewProfile(String username) { }
 *           @Override public void onBlockUser(String username) { }
 *       });
 *
 *       sheet.show(fragmentManager, UserProfileBottomSheet.TAG);
 *   });
 *
 * 3. Pass FragmentManager when creating the adapter in your Activity:
 *
 *   adapter = new ChatAdapter(chatList, getSupportFragmentManager());
 *
 * ─────────────────────────────────────────────
 */