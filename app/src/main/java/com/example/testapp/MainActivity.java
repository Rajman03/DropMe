package com.example.testapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Toast;

import com.example.testapp.core.MultiLinkProfile;
import com.example.testapp.core.QrDetailsDialog;
import com.example.testapp.core.UserProfile;
import com.example.testapp.core.UserQrItem;
import com.example.testapp.core.UserQrManager;
import com.example.testapp.data.AuthManager;
import com.example.testapp.feature.auth.AuthController;
import com.example.testapp.feature.dashboard.DashboardController;
import com.example.testapp.feature.paywall.PaywallController;
import com.example.testapp.feature.profile.ProfileController;

/**
 * Aktywność nadrzędna aplikacji - cienki orkiestrator.
 *
 * <p>MainActivity nie zawiera już logiki poszczególnych zakładek. Każda zakładka
 * (auth, dashboard, paywall, profil) jest osobnym modułem Gradle, który wystawia
 * własny kontroler i własny layout. Tutaj tylko spina się je w całość.</p>
 */
public class MainActivity extends Activity {

    // ===== Warstwa danych (:core + :data) =====
    private AuthManager authManager;
    private UserProfile userProfile;
    private UserQrManager userQrManager;
    private MultiLinkProfile multiLinkProfile;

    // ===== Kontrolery modułów funkcyjnych =====
    private AuthController authController;
    private NavigationController navigationController;
    private DashboardController dashboardController;
    private ProfileController profileController;
    private PaywallController paywallController;

    private View layoutMainApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applySystemBarInsets();
        initDataManagers();
        layoutMainApp = findViewById(R.id.layoutMainApp);
        initControllers();

        authManager.purgeLegacyMockRajman(userProfile);
        userQrManager.purgeDefaultItems();

        if (!authManager.isLoggedIn()) {
            authController.showAuthScreen();
        } else {
            authManager.applyCurrentUserToProfile(userProfile);
            authController.dismissAuth();
        }

        refreshAllUi();

        if (authManager.isLoggedIn()) {
            authManager.syncWithSupabase(userProfile, this::refreshAllUi);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authManager != null && authManager.isLoggedIn()) {
            authManager.syncWithSupabase(userProfile, this::refreshAllUi);
        }
    }

    /**
     * Od Androida 15 (API 35) aplikacje z targetSdk >= 35 działają w trybie
     * edge-to-edge - zawartość rysowana jest pod paskami systemowymi.
     *
     * <p>Bez obsługi insetów dolny dock chował się pod paskiem nawigacji,
     * a nagłówek wjeżdżał pod pasek statusu. Tutaj dokładamy odpowiednie
     * odstępy: nagłówkowi padding u góry (żeby jego tło zostało pod paskiem
     * statusu), a dockowi margines u dołu (żeby cały był widoczny).</p>
     */
    private void applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return; // poniżej API 35 okno samo omija paski systemowe
        }

        final View root = findViewById(R.id.rootLayout);
        final View header = findViewById(R.id.headerBar);
        final View dock = findViewById(R.id.dockDynamicIsland);
        if (root == null) return;

        // Ekran bez paska nagłówkowego (PIN, logowanie): treść musi się podnieść
        // nad pasek gestów, inaczej ostatni link chowa się pod nawigacją systemową.
        final View pinContent = findViewById(R.id.pinAuthContent);
        final View authContent = findViewById(R.id.authContent);
        final int pinBasePaddingBottom = pinContent != null ? pinContent.getPaddingBottom() : 0;
        final int authBasePaddingBottom = authContent != null ? authContent.getPaddingBottom() : 0;

        final int headerBasePaddingTop = header != null ? header.getPaddingTop() : 0;

        final int dockBaseMarginBottom;
        if (dock != null && dock.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            dockBaseMarginBottom = ((ViewGroup.MarginLayoutParams) dock.getLayoutParams()).bottomMargin;
        } else {
            dockBaseMarginBottom = 0;
        }

        root.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars());

            if (header != null) {
                header.setPadding(header.getPaddingLeft(),
                        headerBasePaddingTop + bars.top,
                        header.getPaddingRight(),
                        header.getPaddingBottom());
            }

            if (dock != null && dock.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) dock.getLayoutParams();
                params.bottomMargin = dockBaseMarginBottom + bars.bottom;
                dock.setLayoutParams(params);
            }

            if (pinContent != null) {
                pinContent.setPadding(pinContent.getPaddingLeft(), pinContent.getPaddingTop(),
                        pinContent.getPaddingRight(), pinBasePaddingBottom + bars.bottom);
            }

            if (authContent != null) {
                authContent.setPadding(authContent.getPaddingLeft(), authContent.getPaddingTop(),
                        authContent.getPaddingRight(), authBasePaddingBottom + bars.bottom);
            }

            return WindowInsets.CONSUMED;
        });

        root.requestApplyInsets();
    }

    private void initDataManagers() {
        userProfile = new UserProfile(this);
        userQrManager = new UserQrManager(this);
        authManager = new AuthManager(this);
        multiLinkProfile = new MultiLinkProfile(this);
        multiLinkProfile.syncWithUserProfile(userProfile);
    }

    private void initControllers() {
        navigationController = new NavigationController(this, userProfile);

        authController = new AuthController(this, authManager, userProfile, this::refreshAllUi);
        authController.attachMainAppView(layoutMainApp);

        dashboardController = new DashboardController(this, userQrManager, multiLinkProfile, userProfile,
                new DashboardController.Host() {
                    @Override
                    public void onQrCreated(UserQrItem item) {
                        QrDetailsDialog.show(MainActivity.this, item);
                    }

                    @Override
                    public void refreshAll() {
                        refreshAllUi();
                    }
                });

        profileController = new ProfileController(this, userProfile, authManager,
                new ProfileController.ProfileActionCallback() {
                    @Override
                    public void onProfileUpdated() {
                        if (multiLinkProfile != null) {
                            multiLinkProfile.syncWithUserProfile(userProfile);
                        }
                        refreshAllUi();
                    }

                    @Override
                    public void onLogoutRequested() {
                        authManager.logout(userProfile);
                        authController.showAuthScreen();
                        Toast.makeText(MainActivity.this, "Wylogowano pomyślnie", Toast.LENGTH_SHORT).show();
                    }
                });
        profileController.attachHeaderViews(
                findViewById(R.id.tvHeaderAvatar),
                findViewById(R.id.tvHeaderRankBadge));

        paywallController = new PaywallController(this, userProfile, authManager, () -> {
            navigationController.switchTab(NavigationController.Tab.DASHBOARD);
            profileController.refreshViews();
            refreshAllUi();
        });
    }

    /** Odświeża wszystkie zakładki. */
    public void refreshAllUi() {
        navigationController.updateHeader();
        profileController.refreshViews();
        paywallController.refreshViews();
        dashboardController.refreshViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dashboardController.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        dashboardController.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
