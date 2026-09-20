package com.example.testapp;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.testapp.core.UserProfile;

public class NavigationController {

    public enum Tab { DASHBOARD, PAYWALL, PROFILE }

    private static final int COLOR_ACTIVE_ICON = 0xFFFFFFFF;
    private static final int COLOR_INACTIVE_ICON = 0xFF64748B;
    private static final int COLOR_ACTIVE_LABEL = 0xFFFFFFFF;
    private static final int COLOR_INACTIVE_LABEL = 0xFF94A3B8;

    private final Activity activity;
    private final UserProfile userProfile;

    private ScrollView layoutDashboard;
    private ScrollView layoutPaywall;
    private ScrollView layoutProfile;

    private View pillNavDash;
    private View pillNavPaywall;
    private View pillNavProf;

    private ImageView ivNavIconDash;
    private ImageView ivNavIconPaywall;
    private ImageView ivNavIconProf;

    private TextView tvNavLabelDash;
    private TextView tvNavLabelPaywall;
    private TextView tvNavLabelProf;

    private TextView tvHeaderTitle;
    private TextView tvHeaderSubtitle;

    private Tab currentTab = Tab.DASHBOARD;

    public NavigationController(Activity activity, UserProfile userProfile) {
        this.activity = activity;
        this.userProfile = userProfile;
        bindViews();
        setupClickListeners();
        updateNavUi();
    }

    private void bindViews() {
        layoutDashboard = activity.findViewById(R.id.layoutDashboard);
        layoutPaywall = activity.findViewById(R.id.layoutPaywall);
        layoutProfile = activity.findViewById(R.id.layoutProfile);

        pillNavDash = activity.findViewById(R.id.pillNavDash);
        pillNavPaywall = activity.findViewById(R.id.pillNavPaywall);
        pillNavProf = activity.findViewById(R.id.pillNavProf);

        ivNavIconDash = activity.findViewById(R.id.ivNavIconDash);
        ivNavIconPaywall = activity.findViewById(R.id.ivNavIconPaywall);
        ivNavIconProf = activity.findViewById(R.id.ivNavIconProf);

        tvNavLabelDash = activity.findViewById(R.id.tvNavLabelDash);
        tvNavLabelPaywall = activity.findViewById(R.id.tvNavLabelPaywall);
        tvNavLabelProf = activity.findViewById(R.id.tvNavLabelProf);

        tvHeaderTitle = activity.findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = activity.findViewById(R.id.tvHeaderSubtitle);
    }

    private void setupClickListeners() {
        View navDash = activity.findViewById(R.id.navDashboard);
        View navPay = activity.findViewById(R.id.navPaywall);
        View navProf = activity.findViewById(R.id.navProfile);
        View headerAvatar = activity.findViewById(R.id.tvHeaderAvatar);

        if (navDash != null) navDash.setOnClickListener(v -> switchTab(Tab.DASHBOARD));
        if (navPay != null) navPay.setOnClickListener(v -> switchTab(Tab.PAYWALL));
        if (navProf != null) navProf.setOnClickListener(v -> switchTab(Tab.PROFILE));
        if (headerAvatar != null) headerAvatar.setOnClickListener(v -> switchTab(Tab.PROFILE));
    }

    public void switchTab(Tab tab) {
        this.currentTab = tab;

        if (layoutDashboard != null) layoutDashboard.setVisibility(tab == Tab.DASHBOARD ? View.VISIBLE : View.GONE);
        if (layoutPaywall != null) layoutPaywall.setVisibility(tab == Tab.PAYWALL ? View.VISIBLE : View.GONE);
        if (layoutProfile != null) layoutProfile.setVisibility(tab == Tab.PROFILE ? View.VISIBLE : View.GONE);

        updateNavUi();
        updateHeader();
    }

    public void updateNavUi() {
        boolean isDash = (currentTab == Tab.DASHBOARD);
        if (pillNavDash != null) pillNavDash.setBackgroundResource(isDash ? R.drawable.dynamic_island_active_pill : 0);
        if (ivNavIconDash != null) ivNavIconDash.setColorFilter(isDash ? COLOR_ACTIVE_ICON : COLOR_INACTIVE_ICON);
        if (tvNavLabelDash != null) {
            tvNavLabelDash.setTextColor(isDash ? COLOR_ACTIVE_LABEL : COLOR_INACTIVE_LABEL);
            tvNavLabelDash.setTypeface(null, isDash ? Typeface.BOLD : Typeface.NORMAL);
        }

        boolean isPay = (currentTab == Tab.PAYWALL);
        if (pillNavPaywall != null) pillNavPaywall.setBackgroundResource(isPay ? R.drawable.dynamic_island_active_pill : 0);
        if (ivNavIconPaywall != null) {
            ivNavIconPaywall.setImageResource(R.drawable.ic_coffee);
            ivNavIconPaywall.setColorFilter(isPay ? COLOR_ACTIVE_ICON : COLOR_INACTIVE_ICON);
        }
        if (tvNavLabelPaywall != null) {
            tvNavLabelPaywall.setTextColor(isPay ? COLOR_ACTIVE_LABEL : COLOR_INACTIVE_LABEL);
            tvNavLabelPaywall.setTypeface(null, isPay ? Typeface.BOLD : Typeface.NORMAL);
            tvNavLabelPaywall.setText("Kawa");
        }

        boolean isProf = (currentTab == Tab.PROFILE);
        if (pillNavProf != null) pillNavProf.setBackgroundResource(isProf ? R.drawable.dynamic_island_active_pill : 0);
        if (ivNavIconProf != null) ivNavIconProf.setColorFilter(isProf ? COLOR_ACTIVE_ICON : COLOR_INACTIVE_ICON);
        if (tvNavLabelProf != null) {
            tvNavLabelProf.setTextColor(isProf ? COLOR_ACTIVE_LABEL : COLOR_INACTIVE_LABEL);
            tvNavLabelProf.setTypeface(null, isProf ? Typeface.BOLD : Typeface.NORMAL);
            tvNavLabelProf.setText("Profil");
        }
    }

    public void updateHeader() {
        updateNavUi();

        if (tvHeaderTitle == null || tvHeaderSubtitle == null) return;

        if (currentTab == Tab.DASHBOARD) {
            tvHeaderTitle.setText(userProfile.getName());
            tvHeaderSubtitle.setText("Kody wizytówek i profile");
        } else if (currentTab == Tab.PAYWALL) {
            tvHeaderTitle.setText("Wesprzyj Twórcę ☕");
            tvHeaderSubtitle.setText("DropMe jest w 100% bezpłatny");
        } else {
            tvHeaderTitle.setText("Profil Użytkownika");
            tvHeaderSubtitle.setText("Twoja wizytówka i ustawienia");
        }
    }

    public Tab getCurrentTab() {
        return currentTab;
    }
}
