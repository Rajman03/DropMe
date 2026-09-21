package com.example.testapp.feature.dashboard;

import com.example.testapp.core.SocialPlatform;
import com.example.testapp.core.UserProfile;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

public class PlatformSelectorController {

    private final Activity activity;
    private final View rootView;
    private final Map<SocialPlatform, PlatformItemViews> itemViewsMap = new HashMap<>();
    private SocialPlatform selectedPlatform = SocialPlatform.INSTAGRAM;

    private static class PlatformItemViews {
        final LinearLayout layout;
        final TextView label;

        PlatformItemViews(LinearLayout layout, TextView label) {
            this.layout = layout;
            this.label = label;
        }
    }

    public PlatformSelectorController(Activity activity) {
        this.activity = activity;
        this.rootView = activity != null ? activity.getWindow().getDecorView() : null;
        bindViews();
        selectPlatform(SocialPlatform.INSTAGRAM);
    }

    public PlatformSelectorController(View rootView) {
        this.activity = null;
        this.rootView = rootView;
        bindViews();
        selectPlatform(SocialPlatform.INSTAGRAM);
    }

    private void bindViews() {
        register(SocialPlatform.INSTAGRAM, R.id.optPlatformInstagram, R.id.tvLabelInstagram);
        register(SocialPlatform.FACEBOOK, R.id.optPlatformFacebook, R.id.tvLabelFacebook);
        register(SocialPlatform.TIKTOK, R.id.optPlatformTikTok, R.id.tvLabelTikTok);
        register(SocialPlatform.SNAPCHAT, R.id.optPlatformSnapchat, R.id.tvLabelSnapchat);
        register(SocialPlatform.YOUTUBE, R.id.optPlatformYouTube, R.id.tvLabelYouTube);
        register(SocialPlatform.LINKEDIN, R.id.optPlatformLinkedIn, R.id.tvLabelLinkedIn);
        register(SocialPlatform.TWITTER, R.id.optPlatformTwitter, R.id.tvLabelTwitter);
        register(SocialPlatform.DISCORD, R.id.optPlatformDiscord, R.id.tvLabelDiscord);
        register(SocialPlatform.GITHUB, R.id.optPlatformGitHub, R.id.tvLabelGitHub);
        register(SocialPlatform.SPOTIFY, R.id.optPlatformSpotify, R.id.tvLabelSpotify);
        register(SocialPlatform.CUSTOM, R.id.optPlatformWeb, R.id.tvLabelWeb);
    }

    private void register(SocialPlatform platform, int layoutId, int labelId) {
        if (rootView == null) return;
        LinearLayout layout = rootView.findViewById(layoutId);
        TextView label = rootView.findViewById(labelId);
        if (layout != null && label != null) {
            itemViewsMap.put(platform, new PlatformItemViews(layout, label));
            layout.setOnClickListener(v -> selectPlatform(platform));
        }
    }

    public void selectPlatform(SocialPlatform platform) {
        this.selectedPlatform = platform;

        for (Map.Entry<SocialPlatform, PlatformItemViews> entry : itemViewsMap.entrySet()) {
            boolean isSelected = (entry.getKey() == platform);
            PlatformItemViews views = entry.getValue();

            views.layout.setBackgroundResource(isSelected ? R.drawable.platform_item_selected : R.drawable.platform_item_unselected);
            views.label.setTextColor(isSelected ? 0xFF0D6EFD : 0xFF334155);
            views.label.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    public void attachAutoDetection(EditText urlInput, EditText nameInput, UserProfile userProfile) {
        if (urlInput == null) return;

        urlInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                SocialPlatform detected = SocialPlatform.detectFromUrl(input);
                if (detected != SocialPlatform.CUSTOM) {
                    selectPlatform(detected);
                    if (nameInput != null && nameInput.getText().toString().trim().isEmpty()) {
                        String userHandle = userProfile != null ? userProfile.getName().toLowerCase().replace(" ", "_") : "profil";
                        nameInput.setHint(detected.getDisplayName() + " @" + userHandle);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public SocialPlatform getSelectedPlatform() {
        return selectedPlatform;
    }
}
