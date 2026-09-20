package com.example.testapp.feature.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testapp.core.MultiLinkProfile;
import com.example.testapp.core.QrDetailsDialog;
import com.example.testapp.core.SocialPlatform;
import com.example.testapp.core.UserProfile;
import com.example.testapp.core.UserQrItem;
import com.example.testapp.core.UserQrManager;

import java.util.List;

/**
 * Kontroler zakładki DASHBOARD (kody QR).
 *
 * <p>Cała logika tej zakładki - wyszukiwanie widoków, wybór koloru, tworzenie kodu QR,
 * lista kodów, wizytówka Multi-Link oraz skaner - żyje w tym module razem z layoutem
 * {@code layout_dashboard.xml}. Dzięki temu identyfikatory widoków ({@code R.id.*})
 * są lokalne dla modułu i nie wymagają sięgania do :app.</p>
 */
public class DashboardController {

    /** Zdarzenia, o których musi wiedzieć aktywność nadrzędna (:app). */
    public interface Host {
        /** Nowy kod QR został utworzony - np. pokaż szczegóły. */
        void onQrCreated(UserQrItem item);

        /** Zmieniły się dane, odśwież cały interfejs. */
        void refreshAll();
    }

    private final Activity activity;
    private final UserQrManager userQrManager;
    private final MultiLinkProfile multiLinkProfile;
    private final UserProfile userProfile;
    private final Host host;

    private final DashboardFilterController filterController;
    private final PlatformSelectorController platformSelector;

    private View btnAddQrForm;
    private View formAddQr;

    private LinearLayout userQrContainer;
    private View tvEmptyQrList;
    private TextView tvQrCounterBadge;
    private LinearLayout bannerLimitReached;
    private EditText etNewQrName;
    private EditText etNewQrUrl;

    private String selectedQrColor = "#0F172A";
    private View btnColorObsidian;
    private View btnColorBlue;
    private View btnColorGold;
    private View btnColorEmerald;
    private View btnColorViolet;

    public DashboardController(Activity activity,
                               UserQrManager userQrManager,
                               MultiLinkProfile multiLinkProfile,
                               UserProfile userProfile,
                               Host host) {
        this.activity = activity;
        this.userQrManager = userQrManager;
        this.multiLinkProfile = multiLinkProfile;
        this.userProfile = userProfile;
        this.host = host;

        bindViews();
        setupAddQrFormToggle();
        setupColorPicker();

        this.filterController = new DashboardFilterController(activity, this::refreshViews);
        this.platformSelector = new PlatformSelectorController(activity);
        this.platformSelector.attachAutoDetection(etNewQrUrl, etNewQrName, userProfile);

        setupMultiLinkActions();
        setupScannerActions();
        setupCreateQrAction();
    }

    private void bindViews() {
        btnAddQrForm = activity.findViewById(R.id.fabAddQr);
        formAddQr = activity.findViewById(R.id.formAddQr);

        userQrContainer = activity.findViewById(R.id.userQrContainer);
        tvEmptyQrList = activity.findViewById(R.id.tvEmptyQrList);
        tvQrCounterBadge = activity.findViewById(R.id.tvQrCounterBadge);
        bannerLimitReached = activity.findViewById(R.id.bannerLimitReached);

        etNewQrName = activity.findViewById(R.id.etNewQrName);
        etNewQrUrl = activity.findViewById(R.id.etNewQrUrl);

        btnColorObsidian = activity.findViewById(R.id.btnColorObsidian);
        btnColorBlue = activity.findViewById(R.id.btnColorBlue);
        btnColorGold = activity.findViewById(R.id.btnColorGold);
        btnColorEmerald = activity.findViewById(R.id.btnColorEmerald);
        btnColorViolet = activity.findViewById(R.id.btnColorViolet);
    }

    // ===================== FORMULARZ: POKAŻ / UKRYJ =====================

    /**
     * Niebieski przycisk "+" rozwija (i zwija) formularz tworzenia nowego kodu QR.
     * Formularz jest domyślnie schowany - widać tylko przycisk.
     */
    private void setupAddQrFormToggle() {
        if (btnAddQrForm == null || formAddQr == null) return;

        btnAddQrForm.setOnClickListener(v ->
                setFormVisible(formAddQr.getVisibility() != View.VISIBLE));

        setFormVisible(false);
    }

    /** Pokazuje lub ukrywa formularz tworzenia kodu QR. */
    public void setFormVisible(boolean visible) {
        if (formAddQr == null) return;
        formAddQr.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) scrollToForm();
    }

    /** Przewija dashboard tak, aby rozwinięty formularz był widoczny. */
    private void scrollToForm() {
        View dashboard = activity.findViewById(R.id.layoutDashboard);
        if (dashboard instanceof ScrollView) {
            ScrollView scrollView = (ScrollView) dashboard;
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    // ===================== WYBÓR KOLORU KODU QR =====================

    private void setupColorPicker() {
        if (btnColorObsidian != null) btnColorObsidian.setOnClickListener(v -> selectQrColor("#0F172A"));
        if (btnColorBlue != null) btnColorBlue.setOnClickListener(v -> selectQrColor("#2563EB"));
        if (btnColorGold != null) btnColorGold.setOnClickListener(v -> selectQrColor("#D97706"));
        if (btnColorEmerald != null) btnColorEmerald.setOnClickListener(v -> selectQrColor("#059669"));
        if (btnColorViolet != null) btnColorViolet.setOnClickListener(v -> selectQrColor("#7C3AED"));
        updateColorRings();
    }

    private void selectQrColor(String hex) {
        this.selectedQrColor = hex;
        updateColorRings();
    }

    private void updateColorRings() {
        setRing(btnColorObsidian, "#0F172A");
        setRing(btnColorBlue, "#2563EB");
        setRing(btnColorGold, "#D97706");
        setRing(btnColorEmerald, "#059669");
        setRing(btnColorViolet, "#7C3AED");
    }

    private void setRing(View button, String hex) {
        if (button == null) return;
        button.setBackgroundResource(hex.equalsIgnoreCase(selectedQrColor)
                ? R.drawable.qr_color_ring_selected
                : R.drawable.qr_color_ring_unselected);
    }

    // ===================== WIZYTÓWKA MULTI-LINK =====================

    private void setupMultiLinkActions() {
        Button btnShowMultiLinkQr = activity.findViewById(R.id.btnShowMultiLinkQr);
        Button btnEditMultiLink = activity.findViewById(R.id.btnEditMultiLink);

        if (btnShowMultiLinkQr != null) {
            btnShowMultiLinkQr.setOnClickListener(v -> {
                multiLinkProfile.syncWithUserProfile(userProfile);
                MultiLinkHelper.showViewDialog(activity, multiLinkProfile);
            });
        }

        if (btnEditMultiLink != null) {
            btnEditMultiLink.setOnClickListener(v -> {
                multiLinkProfile.syncWithUserProfile(userProfile);
                MultiLinkHelper.showEditDialog(activity, multiLinkProfile,
                        () -> Toast.makeText(activity, "Wizytówka Multi-Link zaktualizowana!", Toast.LENGTH_SHORT).show());
            });
        }
    }

    // ===================== SKANER KODÓW QR =====================

    private void setupScannerActions() {
        View btnQuickScannerLaunch = activity.findViewById(R.id.btnQuickScannerLaunch);
        if (btnQuickScannerLaunch != null) {
            btnQuickScannerLaunch.setOnClickListener(v ->
                    QrScannerHelper.showScanner(activity, userQrManager, () -> {
                        if (host != null) host.refreshAll();
                    }));
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        QrScannerHelper.handleActivityResult(activity, requestCode, resultCode, data, userQrManager,
                () -> { if (host != null) host.refreshAll(); });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        QrScannerHelper.handlePermissionResult(activity, requestCode, permissions, grantResults);
    }

    // ===================== TWORZENIE KODU QR =====================

    private void setupCreateQrAction() {
        Button btnCreate = activity.findViewById(R.id.btnCreateNewQr);
        if (btnCreate == null) return;

        btnCreate.setOnClickListener(v -> {
            String rawInput = etNewQrUrl != null ? etNewQrUrl.getText().toString().trim() : "";
            if (rawInput.isEmpty()) {
                Toast.makeText(activity, "Wklej link lub wpisz nazwę użytkownika", Toast.LENGTH_SHORT).show();
                return;
            }

            SocialPlatform platform = platformSelector.getSelectedPlatform();
            String url = platform.buildUrl(rawInput);

            String name = etNewQrName != null ? etNewQrName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                name = platform.getDisplayName();
            }

            UserQrItem createdItem = userQrManager.addItem(name, platform.getKey(), url, selectedQrColor);

            if (etNewQrName != null) {
                etNewQrName.setText("");
                etNewQrName.clearFocus();
            }
            if (etNewQrUrl != null) {
                etNewQrUrl.setText("");
                etNewQrUrl.clearFocus();
            }

            // Formularz zwija się po zapisaniu kodu.
            setFormVisible(false);
            refreshViews();
            Toast.makeText(activity, "Utworzono kod QR!", Toast.LENGTH_SHORT).show();

            if (host != null) host.onQrCreated(createdItem);
        });
    }

    // ===================== LISTA KODÓW QR =====================

    /** Odświeża licznik, baner limitów oraz listę kodów QR. */
    public void refreshViews() {
        refreshCounterAndLimits();
        renderQrList();
    }

    private void refreshCounterAndLimits() {
        int count = userQrManager.getCount();
        if (tvQrCounterBadge != null) {
            tvQrCounterBadge.setText(count + " kodów (Nielimitowane)");
        }
        if (bannerLimitReached != null) {
            bannerLimitReached.setVisibility(View.GONE);
        }
    }

    private void renderQrList() {
        if (userQrContainer == null) return;
        userQrContainer.removeAllViews();

        List<UserQrItem> filtered = filterController.filter(userQrManager.getItems());

        if (filtered.isEmpty()) {
            if (tvEmptyQrList != null) tvEmptyQrList.setVisibility(View.VISIBLE);
            return;
        }

        if (tvEmptyQrList != null) tvEmptyQrList.setVisibility(View.GONE);

        for (UserQrItem item : filtered) {
            View cardView = QrCardViewBinder.createCardView(activity, userQrContainer, item,
                    new QrCardViewBinder.CardActionListener() {
                        @Override
                        public void onCardClicked(UserQrItem clickedItem) {
                            QrDetailsDialog.show(activity, clickedItem);
                        }

                        @Override
                        public void onFavoriteToggled(UserQrItem favItem, boolean isFavorite) {
                            userQrManager.saveItems();
                        }

                        @Override
                        public void onDeleteRequested(UserQrItem delItem) {
                            userQrManager.removeItem(delItem.getId());
                            if (host != null) host.refreshAll();
                            Toast.makeText(activity, "Usunięto kod QR", Toast.LENGTH_SHORT).show();
                        }
                    });

            userQrContainer.addView(cardView);
        }
    }
}
