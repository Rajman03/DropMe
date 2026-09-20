package com.example.testapp.feature.paywall;

import com.example.testapp.data.AuthManager;
import com.example.testapp.core.UserProfile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class PaywallController {

    public interface OnPremiumPurchasedListener {
        void onPremiumPurchased();
    }

    private final Activity activity;
    private final UserProfile userProfile;
    private final AuthManager authManager;
    private final OnPremiumPurchasedListener listener;

    private LinearLayout planSmallCoffee;
    private LinearLayout planBigCoffee;
    private TextView tvRadioSmallCoffee;
    private TextView tvRadioBigCoffee;
    private Button btnTipPayPal;
    private TextView tvPaywallStatusMsg;

    private boolean isBigCoffee = true;

    public PaywallController(Activity activity, UserProfile userProfile, AuthManager authManager, OnPremiumPurchasedListener listener) {
        this.activity = activity;
        this.userProfile = userProfile;
        this.authManager = authManager;
        this.listener = listener;

        bindViews();
        setupActions();
        updateSelection(true);
    }

    private void bindViews() {
        planBigCoffee = activity.findViewById(R.id.planYearly);
        planSmallCoffee = activity.findViewById(R.id.planMonthly);
        tvRadioBigCoffee = activity.findViewById(R.id.tvRadioYearly);
        tvRadioSmallCoffee = activity.findViewById(R.id.tvRadioMonthly);
        btnTipPayPal = activity.findViewById(R.id.btnBuyPremium);
        tvPaywallStatusMsg = activity.findViewById(R.id.tvPaywallStatusMsg);
    }

    private void setupActions() {
        if (planBigCoffee != null) planBigCoffee.setOnClickListener(v -> updateSelection(true));
        if (planSmallCoffee != null) planSmallCoffee.setOnClickListener(v -> updateSelection(false));

        if (btnTipPayPal != null) {
            btnTipPayPal.setOnClickListener(v -> openPayPalSupport());
        }
    }

    private void updateSelection(boolean bigCoffee) {
        this.isBigCoffee = bigCoffee;
        if (planBigCoffee != null) {
            planBigCoffee.setBackgroundResource(bigCoffee ? R.drawable.plan_selected_bg : R.drawable.plan_unselected_bg);
        }
        if (planSmallCoffee != null) {
            planSmallCoffee.setBackgroundResource(bigCoffee ? R.drawable.plan_unselected_bg : R.drawable.plan_selected_bg);
        }
        if (tvRadioBigCoffee != null) {
            tvRadioBigCoffee.setText(bigCoffee ? "●" : "○");
        }
        if (tvRadioSmallCoffee != null) {
            tvRadioSmallCoffee.setText(bigCoffee ? "○" : "●");
        }
        if (btnTipPayPal != null) {
            btnTipPayPal.setText(bigCoffee ? "☕ POSTAW DUŻĄ KAWĘ (10,00 ZŁ)" : "☕ POSTAW MAŁĄ KAWĘ (5,00 ZŁ)");
        }
    }

    private void openPayPalSupport() {
        int amount = isBigCoffee ? 10 : 5;
        String url = "https://www.paypal.me/Rajman04/" + amount + "PLN";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            activity.startActivity(intent);
            Toast.makeText(activity, "Otwieram stronę PayPal. Dziękujemy za wsparcie! ❤️", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(activity, "Nie można otworzyć linku: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void refreshViews() {
        updateSelection(isBigCoffee);
    }
}
