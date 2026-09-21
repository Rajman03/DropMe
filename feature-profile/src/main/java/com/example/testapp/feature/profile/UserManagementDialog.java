package com.example.testapp.feature.profile;

import com.example.testapp.data.AuthManager;
import com.example.testapp.core.AuthUser;
import com.example.testapp.core.PinSecurityManager;
import com.example.testapp.core.UserProfile;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class UserManagementDialog {

    public static void show(Context context, AuthManager authManager, UserProfile userProfile, Runnable onUserUpdatedCallback) {
        AuthUser operator = authManager.getCurrentUser();
        if (operator == null) {
            String rank = userProfile.getRank();
            operator = new AuthUser(
                    userProfile.getName().toLowerCase().replace(" ", "_"),
                    userProfile.getName(),
                    "",
                    rank,
                    userProfile.getTitle(),
                    userProfile.getAvatarEmoji(),
                    userProfile.isPurchasedPremium()
            );
        } else {
            // Keep operator rank in sync with userProfile
            if (userProfile.isCreator() && !operator.isCreator()) {
                operator.setRank(UserProfile.RANK_CREATOR);
            } else if (userProfile.isAdmin() && !operator.isAdmin()) {
                operator.setRank(UserProfile.RANK_ADMIN);
            }
        }

        boolean canManage = operator.isAdmin() || userProfile.isAdmin() || userProfile.isCreator();
        if (!canManage) {
            Toast.makeText(context, "Dostęp tylko dla Creator lub Admin!", Toast.LENGTH_SHORT).show();
            return;
        }

        final AuthUser activeOperator = operator;
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user_management, null, false);
        dialogView.setFitsSystemWindows(true);
        dialogView.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottomInset = insets.getSystemWindowInsetBottom();
            int topInset = insets.getSystemWindowInsetTop();
            v.setPadding(
                    v.getPaddingLeft(),
                    topInset > 0 ? topInset : v.getPaddingTop(),
                    v.getPaddingRight(),
                    bottomInset > 0 ? bottomInset + 16 : 56
            );
            return insets;
        });

        Dialog dialog = new Dialog(context, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        dialog.setContentView(dialogView);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        TextView tvOperatorRole = dialogView.findViewById(R.id.tvManagementOperatorRole);
        TextView tvPermissionsTitle = dialogView.findViewById(R.id.tvPermissionsTitle);
        TextView tvPermissionsDesc = dialogView.findViewById(R.id.tvPermissionsDesc);
        TextView tvSupabaseSyncStatus = dialogView.findViewById(R.id.tvSupabaseSyncStatus);
        Button btnRefresh = dialogView.findViewById(R.id.btnRefreshFromSupabase);
        EditText etSearch = dialogView.findViewById(R.id.etSearchUserManagement);
        LinearLayout container = dialogView.findViewById(R.id.containerUserManagementItems);
        View btnClose = dialogView.findViewById(R.id.btnCloseManagementDialog);
        View btnBottomClose = dialogView.findViewById(R.id.btnBottomCloseManagement);

        if (activeOperator.isCreator()) {
            tvOperatorRole.setText("Zalogowano: Główny Twórca (Creator)");
            tvPermissionsTitle.setText("Uprawnienia Twórcy (Pełne)");
            tvPermissionsDesc.setText("Zarządzasz użytkownikami w chmurze Supabase. Zabezpieczenie: nie możesz usunąć rangi Premium osobie, która kupiła ją w Paywall.");
        } else {
            tvOperatorRole.setText("Zalogowano: Administrator (Admin)");
            tvPermissionsTitle.setText("Uprawnienia Administratora");
            tvPermissionsDesc.setText("Zgodnie z uprawnieniami możesz WYŁĄCZNIE nadawać rangę Premium użytkownikom, którzy jej jeszcze nie posiadają.");
        }

        final Runnable[] renderListHolder = new Runnable[1];
        final Runnable renderList = () -> {
            if (renderListHolder[0] != null) renderListHolder[0].run();
        };

        renderListHolder[0] = () -> {
            container.removeAllViews();
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim().toLowerCase() : "";
            List<AuthUser> allUsers = authManager.getAllUsers();

            int matchedCount = 0;
            for (AuthUser target : allUsers) {
                if (!query.isEmpty()) {
                    boolean matchName = target.getDisplayName().toLowerCase().contains(query);
                    boolean matchUser = target.getUsername().toLowerCase().contains(query);
                    if (!matchName && !matchUser) continue;
                }

                matchedCount++;
                View itemView = LayoutInflater.from(context).inflate(R.layout.item_user_management, container, false);

                TextView tvAvatar = itemView.findViewById(R.id.tvUserItemAvatar);
                TextView tvName = itemView.findViewById(R.id.tvUserItemDisplayName);
                TextView tvUsername = itemView.findViewById(R.id.tvUserItemUsername);
                TextView tvRankBadge = itemView.findViewById(R.id.tvUserItemRankBadge);
                TextView tvPurchasedBadge = itemView.findViewById(R.id.tvUserItemPurchasedBadge);
                TextView tvNotice = itemView.findViewById(R.id.tvUserItemNotice);
                Button btnToggleAdmin = itemView.findViewById(R.id.btnUserItemToggleAdmin);
                Button btnChangePin = itemView.findViewById(R.id.btnUserItemChangePin);
                Button btnTogglePremium = itemView.findViewById(R.id.btnUserItemTogglePremium);
                TextView tvLockedNotice = itemView.findViewById(R.id.tvUserItemLockedNotice);
                Button btnDelete = itemView.findViewById(R.id.btnUserItemDelete);

                tvAvatar.setText(target.getAvatar());
                tvName.setText(target.getDisplayName());
                tvUsername.setText("@" + target.getUsername());

                // Rank Badge style
                int badgeBg = R.drawable.badge_guest;
                if (target.isCreator()) badgeBg = R.drawable.badge_owner;
                else if (target.isAdmin()) badgeBg = R.drawable.badge_admin;
                else if (target.isPremium()) badgeBg = R.drawable.badge_premium;
                tvRankBadge.setBackgroundResource(badgeBg);
                tvRankBadge.setText(target.getRank());

                // Purchased status
                if (target.isPurchasedPremium()) {
                    tvPurchasedBadge.setVisibility(View.VISIBLE);
                    tvNotice.setText("Własny zakup w Paywall (Ochrona rangi)");
                } else {
                    tvPurchasedBadge.setVisibility(View.GONE);
                    if (target.isCreator()) tvNotice.setText("Konto Twórcy w Supabase");
                    else if (target.isAdmin()) tvNotice.setText("Konto Moderatora / Admina");
                    else if (target.isPremium()) tvNotice.setText("Ranga Premium w chmurze");
                    else tvNotice.setText("Standardowe konto użytkownika");
                }

                // Delete button (Creator only, cannot delete Creators)
                if (btnDelete != null) {
                    if (activeOperator.isCreator() && !target.isCreator()) {
                        btnDelete.setVisibility(View.VISIBLE);
                        btnDelete.setOnClickListener(v -> {
                            new AlertDialog.Builder(context)
                                    .setTitle("Usuwanie użytkownika")
                                    .setMessage("Czy na pewno chcesz usunąć konto @" + target.getUsername() + " z bazy Supabase?")
                                    .setPositiveButton("Usuń z bazy", (d, w) -> {
                                        String[] err = new String[1];
                                        if (authManager.deleteUser(target.getUsername(), activeOperator, err)) {
                                            Toast.makeText(context, "Usunięto użytkownika @" + target.getUsername() + " z Supabase", Toast.LENGTH_SHORT).show();
                                            renderList.run();
                                            if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                        } else {
                                            Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .setNegativeButton("Anuluj", null)
                                    .show();
                        });
                    } else {
                        btnDelete.setVisibility(View.GONE);
                    }
                }

                // ================= PERMISSION ACTION BUTTONS =================
                if (btnChangePin != null) btnChangePin.setVisibility(View.GONE);

                if (activeOperator.isCreator()) {
                    // Creator can manage everyone except other Creators
                    if (target.isCreator()) {
                        btnToggleAdmin.setVisibility(View.GONE);
                        btnTogglePremium.setVisibility(View.GONE);                            tvLockedNotice.setVisibility(View.VISIBLE);
                            tvLockedNotice.setText("Główny Twórca (Chroni system)");

                            if (btnChangePin != null) {
                                btnChangePin.setVisibility(View.VISIBLE);
                                btnChangePin.setText("Zmień PIN");
                            btnChangePin.setOnClickListener(v -> {
                                PinSecurityManager.promptSetAdminPinDialog(context, target.getUsername(), newPin -> {
                                    String[] err = new String[1];
                                    PinSecurityManager.setCreatorPin(context, newPin);
                                    if (authManager.updateUserPin(target.getUsername(), newPin, activeOperator, err)) {
                                        Toast.makeText(context, "Zmieniono kod PIN Twórcy", Toast.LENGTH_SHORT).show();
                                        renderList.run();
                                        if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                    } else {
                                        Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                        }
                    } else {
                        // Admin action button
                        btnToggleAdmin.setVisibility(View.VISIBLE);
                        if (target.isAdmin()) {
                            btnToggleAdmin.setText("Odbierz Admina");
                            btnToggleAdmin.setBackgroundResource(R.drawable.badge_expense);
                            btnToggleAdmin.setTextColor(context.getColor(R.color.expense_red));
                            btnToggleAdmin.setOnClickListener(v -> {
                                String newRank = target.isPurchasedPremium() ? UserProfile.RANK_PREMIUM : UserProfile.RANK_GUEST;
                                String[] err = new String[1];
                                if (authManager.updateUserRankWithPin(target.getUsername(), newRank, "", activeOperator, userProfile, err)) {
                                    Toast.makeText(context, "Odebrano uprawnienia Admina dla @" + target.getUsername(), Toast.LENGTH_SHORT).show();
                                    renderList.run();
                                    if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                } else {
                                    Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                }
                            });

                            if (btnChangePin != null) {
                                btnChangePin.setVisibility(View.VISIBLE);
                                btnChangePin.setText("Zmień PIN");
                                btnChangePin.setOnClickListener(v -> {
                                    PinSecurityManager.promptSetAdminPinDialog(context, target.getUsername(), newPin -> {
                                        String[] err = new String[1];
                                        if (authManager.updateUserPin(target.getUsername(), newPin, activeOperator, err)) {
                                            Toast.makeText(context, "Zmieniono PIN dla @" + target.getUsername(), Toast.LENGTH_SHORT).show();
                                            renderList.run();
                                            if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                        } else {
                                            Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                });
                            }
                        } else {
                            btnToggleAdmin.setText("Nadaj Admina");
                            btnToggleAdmin.setBackgroundResource(R.drawable.badge_admin);
                            btnToggleAdmin.setTextColor(context.getColor(R.color.white));
                            btnToggleAdmin.setOnClickListener(v -> {
                                PinSecurityManager.promptSetAdminPinDialog(context, target.getUsername(), newPin -> {
                                    String[] err = new String[1];
                                    if (authManager.updateUserRankWithPin(target.getUsername(), UserProfile.RANK_ADMIN, newPin, activeOperator, userProfile, err)) {
                                        Toast.makeText(context, "Nadano rangę Admin oraz kod PIN dla @" + target.getUsername(), Toast.LENGTH_SHORT).show();
                                        renderList.run();
                                        if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                    } else {
                                        Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                        }

                        // Premium action button
                        if (target.isAdmin()) {
                            btnTogglePremium.setVisibility(View.GONE);
                            tvLockedNotice.setVisibility(View.GONE);
                        } else if (target.isPremium()) {
                            if (target.isPurchasedPremium()) {
                                // CANNOT REVOKE PURCHASED PREMIUM!
                                btnTogglePremium.setVisibility(View.GONE);
                                tvLockedNotice.setVisibility(View.VISIBLE);
                                tvLockedNotice.setText("Zakupione w Paywall (Nie można odebrać)");
                            } else {
                                // Can revoke manual premium
                                btnTogglePremium.setVisibility(View.VISIBLE);
                                tvLockedNotice.setVisibility(View.GONE);
                                btnTogglePremium.setText("Odbierz Premium");
                                btnTogglePremium.setBackgroundResource(R.drawable.badge_expense);
                                btnTogglePremium.setTextColor(context.getColor(R.color.expense_red));
                                btnTogglePremium.setOnClickListener(v -> {
                                    String[] err = new String[1];
                                    if (authManager.updateUserRank(target.getUsername(), UserProfile.RANK_GUEST, activeOperator, userProfile, err)) {
                                        Toast.makeText(context, "Odebrano rangę Premium dla @" + target.getUsername(), Toast.LENGTH_SHORT).show();
                                        renderList.run();
                                        if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                    } else {
                                        Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } else {
                            // Target is Guest: Can grant Premium
                            btnTogglePremium.setVisibility(View.VISIBLE);
                            tvLockedNotice.setVisibility(View.GONE);
                            btnTogglePremium.setText("Nadaj Premium");
                            btnTogglePremium.setBackgroundResource(R.drawable.btn_pill_dark);
                            btnTogglePremium.setTextColor(context.getColor(R.color.white));
                            btnTogglePremium.setOnClickListener(v -> {
                                String[] err = new String[1];
                                if (authManager.updateUserRank(target.getUsername(), UserProfile.RANK_PREMIUM, activeOperator, userProfile, err)) {
                                    Toast.makeText(context, "Nadano rangę Premium dla @" + target.getUsername(), Toast.LENGTH_SHORT).show();
                                    renderList.run();
                                    if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                } else {
                                    Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                } else {
                    // Operator is Admin (NOT Creator):
                    btnToggleAdmin.setVisibility(View.GONE);

                    if (target.isCreator() || target.isAdmin()) {
                        btnTogglePremium.setVisibility(View.GONE);
                        tvLockedNotice.setVisibility(View.VISIBLE);
                        tvLockedNotice.setText("Brak uprawnień do edycji");

                        if (btnChangePin != null && !target.isCreator() && activeOperator.getUsername().equalsIgnoreCase(target.getUsername())) {
                            btnChangePin.setVisibility(View.VISIBLE);
                            btnChangePin.setText("Zmień Mój PIN");
                            btnChangePin.setOnClickListener(v -> {
                                PinSecurityManager.promptSetAdminPinDialog(context, target.getUsername(), newPin -> {
                                    String[] err = new String[1];
                                    PinSecurityManager.setAdminPin(context, newPin);
                                    if (authManager.updateUserPin(target.getUsername(), newPin, activeOperator, err)) {
                                        Toast.makeText(context, "Zaktualizowano Twój PIN!", Toast.LENGTH_SHORT).show();
                                        renderList.run();
                                        if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                                    } else {
                                        Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                                    }
                                });
                            });
                        }
                    } else if (target.isPremium()) {
                        btnTogglePremium.setVisibility(View.GONE);
                        tvLockedNotice.setVisibility(View.VISIBLE);
                        tvLockedNotice.setText(target.isPurchasedPremium() 
                                ? "Posiada zakupione Premium" 
                                : "Posiada Premium (Admin nie może odebrać)");
                    } else {
                        // User does NOT have Premium: Admin CAN grant it!
                        btnTogglePremium.setVisibility(View.VISIBLE);
                        tvLockedNotice.setVisibility(View.GONE);
                        btnTogglePremium.setText("Nadaj Premium");
                        btnTogglePremium.setBackgroundResource(R.drawable.badge_premium);
                        btnTogglePremium.setTextColor(context.getColor(R.color.white));
                        btnTogglePremium.setOnClickListener(v -> {
                            String[] err = new String[1];
                            if (authManager.updateUserRank(target.getUsername(), UserProfile.RANK_PREMIUM, activeOperator, userProfile, err)) {
                                Toast.makeText(context, "Admin nadał rangę Premium dla @" + target.getUsername(), Toast.LENGTH_SHORT).show();
                                renderList.run();
                                if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
                            } else {
                                Toast.makeText(context, err[0] != null ? err[0] : "Błąd", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                container.addView(itemView);
            }

            if (matchedCount == 0) {
                TextView tvEmpty = new TextView(context);
                tvEmpty.setText("Brak użytkowników do wyświetlenia w bazie");
                tvEmpty.setPadding(20, 40, 20, 40);
                tvEmpty.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tvEmpty.setTextColor(context.getColor(R.color.text_secondary));
                container.addView(tvEmpty);
            }
        };

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderList.run();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnRefresh.setOnClickListener(v -> {
            tvSupabaseSyncStatus.setText("Supabase: Pobieranie z chmury...");
            btnRefresh.setEnabled(false);
            authManager.fetchAndSyncUsersFromSupabase(() -> {
                btnRefresh.setEnabled(true);
                tvSupabaseSyncStatus.setText("Supabase: Zsynchronizowano (" + authManager.getAllUsers().size() + " kont)");
                renderList.run();
                if (onUserUpdatedCallback != null) onUserUpdatedCallback.run();
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnBottomClose.setOnClickListener(v -> dialog.dismiss());

        // Initial sync from Supabase in background
        tvSupabaseSyncStatus.setText("Supabase: Pobieranie...");
        authManager.fetchAndSyncUsersFromSupabase(() -> {
            tvSupabaseSyncStatus.setText("Supabase: Zsynchronizowano (" + authManager.getAllUsers().size() + " kont)");
            renderList.run();
        });

        renderList.run();
        dialog.show();
    }
}
