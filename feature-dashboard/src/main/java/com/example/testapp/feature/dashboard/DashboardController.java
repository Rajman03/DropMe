package com.example.testapp.feature.dashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
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

        /** Kod QR został usunięty. */
        default void onQrDeleted(UserQrItem item) {}

        /** Kod QR został zaktualizowany (np. ulubiony). */
        default void onQrUpdated(UserQrItem item) {}

        /** Zmieniły się dane, odśwież cały interfejs. */
        void refreshAll();
    }

    private final Activity activity;
    private final UserQrManager userQrManager;
    private final MultiLinkProfile multiLinkProfile;
    private final UserProfile userProfile;
    private final Host host;

    private final DashboardFilterController filterController;

    private View btnAddQrForm;
    private View btnEmptyAddQr;

    private LinearLayout userQrContainer;
    private View tvEmptyQrList;
    private TextView tvQrCounterBadge;
    private LinearLayout bannerLimitReached;

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
        setupAddQrActions();

        this.filterController = new DashboardFilterController(activity, this::refreshViews);

        setupMultiLinkActions();
        setupScannerActions();
    }

    private void bindViews() {
        btnAddQrForm = activity.findViewById(R.id.fabAddQr);
        btnEmptyAddQr = activity.findViewById(R.id.btnEmptyAddQr);

        userQrContainer = activity.findViewById(R.id.userQrContainer);
        tvEmptyQrList = activity.findViewById(R.id.tvEmptyQrList);
        tvQrCounterBadge = activity.findViewById(R.id.tvQrCounterBadge);
        bannerLimitReached = activity.findViewById(R.id.bannerLimitReached);
    }

    // ===================== FORMULARZ W MODALU (DIALOG) =====================

    private void setupAddQrActions() {
        if (btnAddQrForm != null) {
            btnAddQrForm.setOnClickListener(v -> showAddQrDialog());
        }
        if (btnEmptyAddQr != null) {
            btnEmptyAddQr.setOnClickListener(v -> showAddQrDialog());
        }
    }

    /** Pokazuje modalne okno dodawania kodu QR. */
    public void setFormVisible(boolean visible) {
        if (visible) {
            showAddQrDialog();
        }
    }

    /**
     * Otwiera modalne okno dialogowe z formularzem tworzenia nowego kodu QR.
     */
    public void showAddQrDialog() {
        if (activity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_add_qr, null, false);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        PlatformSelectorController dialogPlatformSelector = new PlatformSelectorController(dialogView);
        EditText etDialogName = dialogView.findViewById(R.id.etNewQrName);
        EditText etDialogUrl = dialogView.findViewById(R.id.etNewQrUrl);
        dialogPlatformSelector.attachAutoDetection(etDialogUrl, etDialogName, userProfile);

        final String[] dialogSelectedColor = new String[]{"#0F172A"};
        View btnObsidian = dialogView.findViewById(R.id.btnColorObsidian);
        View btnBlue = dialogView.findViewById(R.id.btnColorBlue);
        View btnGold = dialogView.findViewById(R.id.btnColorGold);
        View btnEmerald = dialogView.findViewById(R.id.btnColorEmerald);
        View btnViolet = dialogView.findViewById(R.id.btnColorViolet);

        Runnable updateDialogRings = () -> {
            setDialogRing(btnObsidian, "#0F172A", dialogSelectedColor[0]);
            setDialogRing(btnBlue, "#2563EB", dialogSelectedColor[0]);
            setDialogRing(btnGold, "#D97706", dialogSelectedColor[0]);
            setDialogRing(btnEmerald, "#059669", dialogSelectedColor[0]);
            setDialogRing(btnViolet, "#7C3AED", dialogSelectedColor[0]);
        };

        if (btnObsidian != null) btnObsidian.setOnClickListener(v -> { dialogSelectedColor[0] = "#0F172A"; updateDialogRings.run(); });
        if (btnBlue != null) btnBlue.setOnClickListener(v -> { dialogSelectedColor[0] = "#2563EB"; updateDialogRings.run(); });
        if (btnGold != null) btnGold.setOnClickListener(v -> { dialogSelectedColor[0] = "#D97706"; updateDialogRings.run(); });
        if (btnEmerald != null) btnEmerald.setOnClickListener(v -> { dialogSelectedColor[0] = "#059669"; updateDialogRings.run(); });
        if (btnViolet != null) btnViolet.setOnClickListener(v -> { dialogSelectedColor[0] = "#7C3AED"; updateDialogRings.run(); });
        updateDialogRings.run();

        Button btnCreate = dialogView.findViewById(R.id.btnCreateNewQr);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelNewQr);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnCreate != null) {
            btnCreate.setOnClickListener(v -> {
                String rawInput = etDialogUrl != null ? etDialogUrl.getText().toString().trim() : "";
                if (rawInput.isEmpty()) {
                    Toast.makeText(activity, "Wklej link lub wpisz nazwę użytkownika", Toast.LENGTH_SHORT).show();
                    return;
                }

                SocialPlatform platform = dialogPlatformSelector.getSelectedPlatform();
                String url = platform.buildUrl(rawInput);

                String name = etDialogName != null ? etDialogName.getText().toString().trim() : "";
                if (name.isEmpty()) {
                    name = platform.getDisplayName();
                }

                UserQrItem createdItem = userQrManager.addItem(name, platform.getKey(), url, dialogSelectedColor[0]);
                dialog.dismiss();
                refreshViews();
                Toast.makeText(activity, "Utworzono kod QR!", Toast.LENGTH_SHORT).show();

                if (host != null) host.onQrCreated(createdItem);
            });
        }

        dialog.show();
    }

    private void setDialogRing(View button, String hex, String selectedHex) {
        if (button == null) return;
        button.setBackgroundResource(hex.equalsIgnoreCase(selectedHex)
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
                            if (host != null) host.onQrUpdated(favItem);
                        }

                        @Override
                        public void onDeleteRequested(UserQrItem delItem) {
                            userQrManager.removeItem(delItem.getId());
                            if (host != null) {
                                host.onQrDeleted(delItem);
                                host.refreshAll();
                            }
                            Toast.makeText(activity, "Usunięto kod QR", Toast.LENGTH_SHORT).show();
                        }
                    });

            userQrContainer.addView(cardView);
        }
    }
}
