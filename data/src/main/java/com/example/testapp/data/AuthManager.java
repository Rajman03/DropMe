package com.example.testapp.data;

import com.example.testapp.core.AuthUser;
import com.example.testapp.core.CredentialHasher;
import com.example.testapp.core.UserProfile;
import com.example.testapp.core.UserQrItem;
import com.example.testapp.core.UserQrManager;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class AuthManager {

    private static final String PREFS_NAME = "auth_accounts_prefs";
    private static final String KEY_USERS = "users_list_json";
    private static final String KEY_CURRENT_USER = "current_logged_in_username";
    private static final String KEY_CLEAN_RESET_V6 = "v6_supabase_clean_reset_done";

    private final SharedPreferences prefs;
    private final List<AuthUser> users = new ArrayList<>();
    private AuthUser currentUser;
    private final SupabaseClient supabaseClient;

    public AuthManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.supabaseClient = new SupabaseClient(context);

        boolean cleanResetDone = prefs.getBoolean(KEY_CLEAN_RESET_V6, false);
        if (!cleanResetDone) {
            prefs.edit().clear().putBoolean(KEY_CLEAN_RESET_V6, true).apply();
            try {
                context.getSharedPreferences("vcard_user_prefs", Context.MODE_PRIVATE)
                       .edit().clear().apply();
            } catch (Exception ignored) {}
            users.clear();
            currentUser = null;
        } else {
            loadUsers();
            loadSession();
        }
    }

    public void purgeLegacyMockRajman() {
        purgeLegacyMockRajman(null);
    }

    public void purgeLegacyMockRajman(UserProfile profile) {
        if (currentUser != null && "rajman".equalsIgnoreCase(currentUser.getUsername()) && "secret".equals(currentUser.getPassword())) {
            currentUser = null;
            prefs.edit().remove(KEY_CURRENT_USER).apply();
        }
        boolean changed = false;
        Iterator<AuthUser> it = users.iterator();
        while (it.hasNext()) {
            AuthUser u = it.next();
            if ("rajman".equalsIgnoreCase(u.getUsername()) && ("secret".equals(u.getPassword()) && "5829".equals(u.getPin()) && u.isCreator())) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            saveUsers();
        }
    }

    private void loadUsers() {
        users.clear();
        String jsonStr = prefs.getString(KEY_USERS, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            // Do NOT pre-seed fake mock users. Supabase is the single source of truth.
            return;
        }

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                AuthUser u = AuthUser.fromJsonObject(obj);
                if (u != null) {
                    // Filter out legacy mock user from earlier builds
                    if ("rajman".equalsIgnoreCase(u.getUsername()) && ("secret".equals(u.getPassword()) && "5829".equals(u.getPin()) && u.isCreator())) {
                        continue;
                    }
                    users.add(u);
                }
            }
            // Bez zbędnego zapisu przy każdym starcie (bylo: saveUsers() tutaj).
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveUsers() {
        try {
            JSONArray arr = new JSONArray();
            for (AuthUser u : users) {
                arr.put(u.toJsonObject());
            }
            prefs.edit().putString(KEY_USERS, arr.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSession() {
        String loggedUsername = prefs.getString(KEY_CURRENT_USER, null);
        if ("rajman".equalsIgnoreCase(loggedUsername)) {
            prefs.edit().remove(KEY_CURRENT_USER).apply();
            this.currentUser = null;
            return;
        }
        if (loggedUsername != null) {
            this.currentUser = findUser(loggedUsername);
        }
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public AuthUser getCurrentUser() {
        return currentUser;
    }

    public AuthUser findUser(String username) {
        if (username == null) return null;
        String normalized = username.trim().toLowerCase();
        for (AuthUser u : users) {
            if (u.getUsername().equals(normalized)) {
                return u;
            }
        }
        return null;
    }

    public AuthUser findUserByEmail(String email) {
        if (email == null) return null;
        String normalized = email.trim().toLowerCase();
        for (AuthUser u : users) {
            if (normalized.equalsIgnoreCase(u.getEmail())) {
                return u;
            }
        }
        return null;
    }

    public void registerInSupabase(String username, String email, String password, UserProfile profile, SupabaseClient.Callback<AuthUser> callback) {
        if (username == null || username.trim().isEmpty()) {
            if (callback != null) callback.onError("Podaj nazwę użytkownika (Username)");
            return;
        }
        String normalizedUser = username.trim().toLowerCase();
        if (!normalizedUser.matches("^[a-z0-9._-]{3,32}$")) {
            if (callback != null) callback.onError("Username musi mieć 3-32 znaki (litery, cyfry, . _ -)");
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            if (callback != null) callback.onError("Podaj adres e-mail");
            return;
        }
        String cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            if (callback != null) callback.onError("Podaj prawidłowy adres e-mail (np. nazwa@domena.pl)");
            return;
        }

        // Jedno konto na jeden mail!
        AuthUser existingWithEmail = findUserByEmail(cleanEmail);
        if (existingWithEmail != null) {
            if (callback != null) callback.onError("Ten adres e-mail jest już przypisany do innego konta!");
            return;
        }

        // Unikalność nazwy użytkownika
        AuthUser existingWithUsername = findUser(normalizedUser);
        if (existingWithUsername != null) {
            if (callback != null) callback.onError("Użytkownik o takim loginie już istnieje!");
            return;
        }

        if (password == null || password.length() < 6) {
            if (callback != null) callback.onError("Hasło musi mieć co najmniej 6 znaków");
            return;
        }

        String rank = UserProfile.RANK_GUEST;
        String avatar = "👤";
        String realName = username.trim();
        AuthUser newUser = new AuthUser(normalizedUser, realName, password, rank, "Użytkownik", avatar);
        newUser.setEmail(cleanEmail);

        // Push to Supabase - Wymóg: konto musi zostać zapisane w Supabase!
        supabaseClient.registerUser(newUser, new SupabaseClient.Callback<AuthUser>() {
            @Override
            public void onSuccess(AuthUser result) {
                AuthUser ex = findUser(normalizedUser);
                if (ex != null) users.remove(ex);
                users.add(newUser);
                saveUsers();
                setCurrentUser(newUser, profile);
                if (callback != null) callback.onSuccess(newUser);
            }

            @Override
            public void onError(String error) {
                // Nie ukrywaj błędu - poinformuj użytkownika o przyczynie (np. blokada RLS w Supabase)
                if (callback != null) callback.onError(error != null ? error : "Błąd rejestracji w Supabase");
            }
        });
    }

    public void loginInSupabase(String username, String password, UserProfile profile, SupabaseClient.Callback<AuthUser> callback) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            if (callback != null) callback.onError("Podaj login i hasło");
            return;
        }

        String norm = username.trim().toLowerCase();
        AuthUser foundUser = findUser(norm);
        if (foundUser == null && norm.contains("@")) {
            foundUser = findUserByEmail(norm);
        }
        final AuthUser local = foundUser;
        if (local != null && local.verifyPassword(password)) {
            setCurrentUser(local, profile);
            if (callback != null) callback.onSuccess(local);
            syncWithSupabase(profile, null);
            return;
        }

        // Try Supabase fetch if not cached locally.
        // SECURITY: fetchUser WERYFIKUJE hasło po stronie danych (loginUser-
        // -like flow) - tu NIE wolno nadpisywać hasła wpisanym tekstem.
        // Wcześniej: cloudUser.setPassword(password) + setCurrentUser = przejęcie
        // dowolnego konta znajomością samego username (hasło nadpisywało hash).
        supabaseClient.loginUser(norm, password, new SupabaseClient.Callback<AuthUser>() {
            @Override
            public void onSuccess(AuthUser cloudUser) {
                AuthUser ex = findUser(norm);
                if (ex != null) users.remove(ex);
                users.add(cloudUser);
                saveUsers();
                setCurrentUser(cloudUser, profile);
                if (callback != null) callback.onSuccess(cloudUser);
            }

            @Override
            public void onError(String error) {
                // Konto istnieje lokalnie (offline) - pozwól na lokalne hasło,
                // ale błąd z Supabase (nieznany użytkownik) traktuj jako błąd logowania.
                if (local != null && local.verifyPassword(password)) {
                    setCurrentUser(local, profile);
                    if (callback != null) callback.onSuccess(local);
                } else {
                    if (callback != null) callback.onError("Błędny login lub hasło");
                }
            }
        });
    }

    public boolean register(String username, String password, String displayName, UserProfile profile) {
        return register(username, password, displayName, profile, null);
    }

    public boolean register(String username, String password, String displayName, UserProfile profile, SupabaseClient.Callback<Boolean> syncCallback) {
        if (username == null || username.trim().isEmpty()) return false;
        String normalizedUser = username.trim().toLowerCase();
        if (!normalizedUser.matches("^[a-z0-9._-]{3,32}$")) return false;
        if ("creator".equals(normalizedUser) || "admin".equals(normalizedUser)) return false;
        if (password == null || password.length() < 6) return false;

        if (findUser(normalizedUser) != null) {
            return false; // already exists
        }

        String rank = UserProfile.RANK_GUEST;
        String avatar = "👤";
        String realName = (displayName != null && !displayName.trim().isEmpty()) ? displayName.trim() : username;
        AuthUser newUser = new AuthUser(normalizedUser, realName, password, rank, "Użytkownik", avatar);
        users.add(newUser);
        saveUsers();

        // Log in immediately
        setCurrentUser(newUser, profile);
        supabaseClient.syncUser(newUser, syncCallback);
        return true;
    }

    public boolean register(String username, String password, String displayName, String pin, UserProfile profile) {
        return register(username, password, displayName, profile);
    }

    public SupabaseClient getSupabaseClient() {
        return supabaseClient;
    }

    public List<AuthUser> getAllUsers() {
        return new ArrayList<>(users);
    }

    public boolean recordSelfPurchase(String plan, double amount, UserProfile profile) {
        if (profile != null) {
            profile.setPurchasedPremium(true);
            if (!profile.isAdmin()) {
                profile.setRank(UserProfile.RANK_PREMIUM);
                profile.setAvatarEmoji("⭐");
            }
        }

        if (currentUser == null) return true;
        currentUser.setPurchasedPremium(true);
        if (!currentUser.isAdmin()) {
            currentUser.setRank(UserProfile.RANK_PREMIUM);
            currentUser.setAvatar("⭐");
            supabaseClient.updateUserRank(currentUser.getUsername(), UserProfile.RANK_PREMIUM, null);
        }
        saveUsers();

        supabaseClient.recordPurchase(currentUser.getUsername(), plan, amount, null);
        return true;
    }

    /**
     * Updates rank based on business rules:
     * - Creator can grant/revoke Admin (with PIN setup).
     * - Creator can grant Premium and revoke Premium (EXCEPT if user bought it themselves).
     * - Admin can ONLY grant Premium to users who do not have it.
     */
    public boolean updateUserRank(String targetUsername, String newRank, AuthUser operator, UserProfile activeProfile, String[] outError) {
        return updateUserRankWithPin(targetUsername, newRank, null, operator, activeProfile, outError);
    }

    public boolean updateUserRankWithPin(String targetUsername, String newRank, String pin, AuthUser operator, UserProfile activeProfile, String[] outError) {
        if (operator == null) {
            if (outError != null && outError.length > 0) outError[0] = "Brak zalogowanego operatora";
            return false;
        }
        AuthUser target = findUser(targetUsername);
        if (target == null) {
            if (outError != null && outError.length > 0) outError[0] = "Nie odnaleziono użytkownika: " + targetUsername;
            return false;
        }

        // Safety: Cannot demote or alter Creator accounts
        if (target.isCreator() && !target.getUsername().equals(operator.getUsername())) {
            if (outError != null && outError.length > 0) outError[0] = "Konto Creator jest chronione i nie może być modyfikowane";
            return false;
        }

        // Case 1: Operator is Admin (NOT Creator)
        if (!operator.isCreator() && operator.isAdmin()) {
            if (!UserProfile.RANK_PREMIUM.equalsIgnoreCase(newRank)) {
                if (outError != null && outError.length > 0) outError[0] = "Admin może wyłącznie nadawać rangę Premium!";
                return false;
            }
            if (target.isPremium()) {
                if (outError != null && outError.length > 0) outError[0] = "Użytkownik posiada już rangę Premium!";
                return false;
            }
            // Admin grants Premium
            target.setRank(UserProfile.RANK_PREMIUM);
            if ("👤".equals(target.getAvatar())) target.setAvatar("⭐");
            saveUsers();
            supabaseClient.updateUserRank(target.getUsername(), UserProfile.RANK_PREMIUM, null);
            return true;
        }

        // Case 2: Operator is Creator
        if (operator.isCreator()) {
            // Cannot demote yourself
            if (target.getUsername().equals(operator.getUsername()) && !UserProfile.RANK_CREATOR.equalsIgnoreCase(newRank)) {
                if (outError != null && outError.length > 0) outError[0] = "Nie możesz odebrać samemu sobie roli Creator";
                return false;
            }

            // Check if revoking premium when purchased
            boolean isRevokingPremium = !UserProfile.RANK_PREMIUM.equalsIgnoreCase(newRank) 
                    && !UserProfile.RANK_ADMIN.equalsIgnoreCase(newRank) 
                    && !UserProfile.RANK_CREATOR.equalsIgnoreCase(newRank);

            if (isRevokingPremium && target.isPurchasedPremium()) {
                if (outError != null && outError.length > 0) {
                    outError[0] = "Użytkownik @" + target.getUsername() + " kupił Premium samemu! Nie można go usunąć.";
                }
                return false;
            }

            target.setRank(newRank);
            if (UserProfile.RANK_ADMIN.equalsIgnoreCase(newRank)) {
                if ("👤".equals(target.getAvatar())) target.setAvatar("🛡️");
                if (pin != null && !pin.trim().isEmpty()) {
                    target.setPin(pin.trim());
                }
            } else if (UserProfile.RANK_PREMIUM.equalsIgnoreCase(newRank)) {
                if ("👤".equals(target.getAvatar())) target.setAvatar("⭐");
            } else if (UserProfile.RANK_GUEST.equalsIgnoreCase(newRank)) {
                if ("🛡️".equals(target.getAvatar()) || "⭐".equals(target.getAvatar())) target.setAvatar("👤");
                target.setPin("");
            }

            saveUsers();

            if (currentUser != null && currentUser.getUsername().equals(target.getUsername()) && activeProfile != null) {
                activeProfile.setRank(newRank);
                activeProfile.setAvatarEmoji(target.getAvatar());
            }

            supabaseClient.updateUserRank(target.getUsername(), newRank, null);
            return true;
        }

        if (outError != null && outError.length > 0) outError[0] = "Brak wymaganych uprawnień do zmiany rang";
        return false;
    }

    public void applyCurrentUserToProfile(UserProfile profile) {
        if (currentUser != null && profile != null) {
            profile.setName(currentUser.getDisplayName());
            profile.setRank(currentUser.getRank());
            profile.setAvatarEmoji(currentUser.getAvatar());
            profile.setTitle(currentUser.getTitle());
            profile.setPurchasedPremium(currentUser.isPurchasedPremium());
        }
    }

    public boolean updateUserPin(String targetUsername, String newPin, AuthUser operator, String[] outError) {
        if (operator == null) {
            if (outError != null && outError.length > 0) outError[0] = "Brak zalogowanego operatora";
            return false;
        }
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            if (outError != null && outError.length > 0) outError[0] = "Nie podano nazwy użytkownika";
            return false;
        }
        String cleanTarget = targetUsername.trim().toLowerCase();
        boolean isSelf = operator.getUsername().equalsIgnoreCase(cleanTarget);

        if (!operator.isAdmin() && !operator.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Opcja dostępna wyłącznie dla rangi Administrator lub Twórca";
            return false;
        }

        AuthUser target = findUser(cleanTarget);
        if (target != null && target.isCreator() && !operator.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Administrator nie ma uprawnień do zmiany PINu Twórcy!";
            return false;
        }

        if (!operator.isCreator() && !isSelf) {
            if (outError != null && outError.length > 0) outError[0] = "Administrator może zmieniać kod PIN wyłącznie na własnym koncie!";
            return false;
        }
        if (target != null && !target.isAdmin() && !target.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Kod PIN jest zastrzeżony wyłącznie dla rangi Administrator lub Twórca";
            return false;
        }

        if (newPin == null || newPin.trim().length() != 4 || !newPin.trim().matches("\\d{4}")) {
            if (outError != null && outError.length > 0) outError[0] = "PIN musi składać się z 4 cyfr";
            return false;
        }

        if (CredentialHasher.isTrivialPin(newPin)) {
            if (outError != null && outError.length > 0) outError[0] = "PIN jest zbyt prosty (np. 1234, 0000)";
            return false;
        }

        if (target != null) {
            target.setPin(newPin);
            saveUsers();
            supabaseClient.syncUser(target, null);
        }

        if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(cleanTarget)) {
            currentUser.setPin(newPin);
            saveUsers();
        }

        return true;
    }

    public boolean deleteUser(String targetUsername, AuthUser operator, String[] outError) {
        if (operator == null || !operator.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Tylko Creator może usuwać użytkowników";
            return false;
        }
        AuthUser target = findUser(targetUsername);
        if (target == null) return false;
        if (target.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Nie można usunąć konta Creator";
            return false;
        }

        users.remove(target);
        saveUsers();
        supabaseClient.deleteUser(targetUsername, null);
        return true;
    }

    public boolean createUserByCreator(String username, String displayName, String rank, String pin, AuthUser operator, String[] outError) {
        if (operator == null || !operator.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Tylko Creator może dodawać użytkowników";
            return false;
        }
        if (username == null || username.trim().isEmpty()) {
            if (outError != null && outError.length > 0) outError[0] = "Wpisz nazwę użytkownika";
            return false;
        }
        String cleanUser = username.trim().toLowerCase();
        if (findUser(cleanUser) != null) {
            if (outError != null && outError.length > 0) outError[0] = "Użytkownik @" + cleanUser + " już istnieje!";
            return false;
        }
        String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName.trim() : cleanUser;
        String avatar = "👤";
        if (UserProfile.RANK_CREATOR.equalsIgnoreCase(rank)) avatar = "👑";
        else if (UserProfile.RANK_ADMIN.equalsIgnoreCase(rank)) avatar = "🛡️";
        else if (UserProfile.RANK_PREMIUM.equalsIgnoreCase(rank)) avatar = "⭐";

        AuthUser newUser = new AuthUser(cleanUser, name, "", rank, "Użytkownik", avatar, false);
        if (pin != null && !pin.trim().isEmpty()) {
            newUser.setPin(pin.trim());
        }
        users.add(newUser);
        saveUsers();
        supabaseClient.syncUser(newUser, null);
        return true;
    }

    public void syncWithSupabase(UserProfile profile, Runnable onComplete) {
        // Push local accounts to Supabase first
        if (currentUser != null) {
            supabaseClient.syncUser(currentUser, null);
        }
        for (AuthUser u : users) {
            if (!u.isCreator() || !"secret".equals(u.getPassword())) {
                supabaseClient.syncUser(u, null);
            }
        }

        supabaseClient.fetchUsers(new SupabaseClient.Callback<List<AuthUser>>() {
            @Override
            public void onSuccess(List<AuthUser> cloudUsers) {
                if (cloudUsers != null) {
                    boolean changed = false;

                    // Collect all valid usernames present in Supabase
                    Set<String> cloudUsernames = new HashSet<>();
                    for (AuthUser cu : cloudUsers) {
                        if (cu.getUsername() != null && !cu.getUsername().trim().isEmpty()) {
                            cloudUsernames.add(cu.getUsername().trim().toLowerCase());
                        }
                    }

                    // Safely update or add cloud users without deleting offline local accounts
                    for (AuthUser cloudUser : cloudUsers) {
                        if (cloudUser.getUsername() == null || cloudUser.getUsername().isEmpty()) continue;
                        AuthUser local = findUser(cloudUser.getUsername());
                        if (local != null) {
                            if (!local.getRank().equalsIgnoreCase(cloudUser.getRank())) {
                                local.setRank(cloudUser.getRank());
                                changed = true;
                            }
                            // TWO-WAY SYNC: update purchased_premium to true OR false
                            if (local.isPurchasedPremium() != cloudUser.isPurchasedPremium()) {
                                local.setPurchasedPremium(cloudUser.isPurchasedPremium());
                                changed = true;
                            }
                            if (cloudUser.getDisplayName() != null && !cloudUser.getDisplayName().isEmpty() 
                                    && !cloudUser.getDisplayName().equals(local.getDisplayName())) {
                                local.setDisplayName(cloudUser.getDisplayName());
                                changed = true;
                            }
                            if (cloudUser.isCreator()) {
                                local.setAvatar("👑");
                            } else if (cloudUser.isAdmin()) {
                                local.setAvatar("🛡️");
                            } else if (cloudUser.isPremium()) {
                                local.setAvatar("⭐");
                            } else if (UserProfile.RANK_GUEST.equalsIgnoreCase(cloudUser.getRank())) {
                                if ("👑".equals(local.getAvatar()) || "⭐".equals(local.getAvatar()) || "🛡️".equals(local.getAvatar())) {
                                    local.setAvatar("👤");
                                }
                            }
                        } else {
                            users.add(cloudUser);
                            local = cloudUser;
                            changed = true;
                        }

                        // Also update current active session if this is the logged-in user!
                        if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(cloudUser.getUsername())) {
                            currentUser.setRank(cloudUser.getRank());
                            currentUser.setPurchasedPremium(cloudUser.isPurchasedPremium());
                            currentUser.setDisplayName(cloudUser.getDisplayName());
                            if (cloudUser.isCreator()) {
                                currentUser.setAvatar("👑");
                            } else if (cloudUser.isAdmin()) {
                                currentUser.setAvatar("🛡️");
                            } else if (cloudUser.isPremium()) {
                                currentUser.setAvatar("⭐");
                            } else if (UserProfile.RANK_GUEST.equalsIgnoreCase(cloudUser.getRank())) {
                                if ("👑".equals(currentUser.getAvatar()) || "⭐".equals(currentUser.getAvatar()) || "🛡️".equals(currentUser.getAvatar())) {
                                    currentUser.setAvatar("👤");
                                }
                            }

                            if (profile != null) {
                                profile.setRank(cloudUser.getRank());
                                profile.setPurchasedPremium(cloudUser.isPurchasedPremium());
                                profile.setName(cloudUser.getDisplayName());
                                profile.setAvatarEmoji(currentUser.getAvatar());
                            }
                            changed = true;
                        }
                    }

                    if (currentUser == null && profile != null) {
                        profile.setRank(UserProfile.RANK_GUEST);
                        profile.setPurchasedPremium(false);
                        profile.setAvatarEmoji("👤");
                        profile.setName("Użytkownik");
                    }

                    if (changed) {
                        saveUsers();
                    }
                }

                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onError(String error) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public void syncUserFromSupabase(String username, Runnable onComplete) {
        if (username == null || username.trim().isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        supabaseClient.fetchUser(username.trim().toLowerCase(), new SupabaseClient.Callback<AuthUser>() {
            @Override
            public void onSuccess(AuthUser cloudUser) {
                if (cloudUser != null) {
                    AuthUser local = findUser(cloudUser.getUsername());
                    if (local != null) {
                        local.setRank(cloudUser.getRank());
                        local.setPurchasedPremium(cloudUser.isPurchasedPremium());
                        if (cloudUser.getDisplayName() != null && !cloudUser.getDisplayName().isEmpty()) {
                            local.setDisplayName(cloudUser.getDisplayName());
                        }
                        if (cloudUser.isCreator()) {
                            local.setAvatar("👑");
                        } else if (cloudUser.isAdmin()) {
                            local.setAvatar("🛡️");
                        } else if (cloudUser.isPremium()) {
                            local.setAvatar("⭐");
                        } else if (UserProfile.RANK_GUEST.equalsIgnoreCase(cloudUser.getRank())) {
                            if ("👑".equals(local.getAvatar()) || "⭐".equals(local.getAvatar()) || "🛡️".equals(local.getAvatar())) {
                                local.setAvatar("👤");
                            }
                        }
                    } else {
                        users.add(cloudUser);
                    }

                    if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(cloudUser.getUsername())) {
                        currentUser.setRank(cloudUser.getRank());
                        currentUser.setPurchasedPremium(cloudUser.isPurchasedPremium());
                        currentUser.setDisplayName(cloudUser.getDisplayName());
                    }

                    saveUsers();
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onError(String error) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    public void fetchAndSyncUsersFromSupabase(Runnable onComplete) {
        syncWithSupabase(null, onComplete);
    }

    public boolean login(String username, String password, UserProfile profile) {
        if (username == null || password == null) return false;
        AuthUser user = findUser(username);
        if (user != null && user.getPassword().equals(password)) {
            setCurrentUser(user, profile);
            return true;
        }
        return false;
    }

    public void loginAsGuest(UserProfile profile) {
        AuthUser guestUser = new AuthUser("guest", "Gość", "", UserProfile.RANK_GUEST, "Konto Gościa", "👤");
        setCurrentUser(guestUser, profile);
    }

    public void setCurrentUser(AuthUser user, UserProfile profile) {
        this.currentUser = user;
        prefs.edit().putString(KEY_CURRENT_USER, user.getUsername()).apply();

        if (profile != null) {
            profile.setName(user.getDisplayName());
            profile.setRank(user.getRank());
            profile.setAvatarEmoji(user.getAvatar());
            profile.setTitle(user.getTitle());
            profile.setPurchasedPremium(user.isPurchasedPremium());
        }
    }

    public void logout(UserProfile profile) {
        this.currentUser = null;
        prefs.edit().remove(KEY_CURRENT_USER).apply();
        if (profile != null) {
            profile.setName("Użytkownik");
            profile.setTitle("Profil wizytówki DropMe");
            profile.setRank(UserProfile.RANK_GUEST);
            profile.setPurchasedPremium(false);
            profile.setAvatarEmoji("👤");
        }
    }

    public boolean deleteSelfAccount(UserProfile profile, String[] outError) {
        if (currentUser == null) {
            if (outError != null && outError.length > 0) outError[0] = "Brak zalogowanego użytkownika";
            return false;
        }
        if (currentUser.isCreator()) {
            if (outError != null && outError.length > 0) outError[0] = "Konto Głównego Twórcy nie może zostać usunięte!";
            return false;
        }

        String username = currentUser.getUsername();
        users.remove(currentUser);
        saveUsers();
        supabaseClient.deleteUser(username, null);
        logout(profile);
        return true;
    }

    public boolean updateCurrentUserPassword(String newPassword, String[] outError) {
        if (currentUser == null) {
            if (outError != null && outError.length > 0) outError[0] = "Brak zalogowanego użytkownika";
            return false;
        }
        if (newPassword == null || newPassword.length() < 6) {
            if (outError != null && outError.length > 0) outError[0] = "Hasło musi mieć co najmniej 6 znaków";
            return false;
        }

        currentUser.setPassword(newPassword);
        saveUsers();
        supabaseClient.syncUser(currentUser, null);
        return true;
    }

    public boolean resetPasswordByEmail(String email, String newPassword, String[] outError) {
        if (email == null || email.trim().isEmpty()) {
            if (outError != null && outError.length > 0) outError[0] = "Podaj adres e-mail";
            return false;
        }
        AuthUser user = findUserByEmail(email.trim().toLowerCase());
        if (user == null) {
            if (outError != null && outError.length > 0) outError[0] = "Nie znaleziono konta powiązanego z tym adresem e-mail";
            return false;
        }
        if (newPassword == null || newPassword.length() < 6) {
            if (outError != null && outError.length > 0) outError[0] = "Hasło musi mieć co najmniej 6 znaków";
            return false;
        }

        user.setPassword(newPassword);
        saveUsers();
        supabaseClient.syncUser(user, null);
        return true;
    }

    public void syncQrCodesWithCloud(UserQrManager qrManager, Runnable onDone) {
        if (currentUser == null || qrManager == null) {
            if (onDone != null) onDone.run();
            return;
        }

        String username = currentUser.getUsername();
        supabaseClient.fetchQrCodes(username, new SupabaseClient.Callback<List<UserQrItem>>() {
            @Override
            public void onSuccess(List<UserQrItem> cloudItems) {
                List<UserQrItem> localItems = qrManager.getItems();
                boolean changed = false;
                if (cloudItems != null) {
                    for (UserQrItem cloudItem : cloudItems) {
                        boolean exists = false;
                        for (UserQrItem localItem : localItems) {
                            if (localItem.getId() == cloudItem.getId()) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            localItems.add(cloudItem);
                            changed = true;
                        }
                    }
                }

                for (UserQrItem localItem : localItems) {
                    boolean inCloud = false;
                    if (cloudItems != null) {
                        for (UserQrItem c : cloudItems) {
                            if (c.getId() == localItem.getId()) {
                                inCloud = true;
                                break;
                            }
                        }
                    }
                    if (!inCloud) {
                        supabaseClient.saveQrCode(username, localItem, null);
                    }
                }

                if (changed) {
                    qrManager.saveItems();
                }
                if (onDone != null) onDone.run();
            }

            @Override
            public void onError(String error) {
                if (onDone != null) onDone.run();
            }
        });
    }

    public void saveQrCodeToCloud(UserQrItem item) {
        if (currentUser == null || item == null) return;
        supabaseClient.saveQrCode(currentUser.getUsername(), item, null);
    }

    public void deleteQrCodeFromCloud(long qrId) {
        if (currentUser == null) return;
        supabaseClient.deleteQrCode(qrId, null);
    }

    /** Supabase Auth wysyła 6-cyfrowy kod na e-mail. */
    public void sendEmailOtp(String email, SupabaseClient.Callback<Boolean> callback) {
        supabaseClient.sendEmailOtp(email, callback);
    }

    /** Supabase Auth sprawdza kod wpisany przez użytkownika. */
    public void verifyEmailOtp(String email, String code, SupabaseClient.Callback<Boolean> callback) {
        supabaseClient.verifyEmailOtp(email, code, callback);
    }

    /** Reset hasła: wysyła kod z szablonu "Reset Password" (endpoint /auth/v1/recover). */
    public void sendPasswordRecoveryOtp(String email, SupabaseClient.Callback<Boolean> callback) {
        supabaseClient.sendPasswordRecoveryOtp(email, callback);
    }

    /** Reset hasła: weryfikuje kod z e-maila (verify z type=recovery). */
    public void verifyRecoveryOtp(String email, String code, SupabaseClient.Callback<Boolean> callback) {
        supabaseClient.verifyRecoveryOtp(email, code, callback);
    }

    /**
     * Waliduje nowy adres e-mail przed rozpoczęciem procedury zmiany.
     * Zwraca znormalizowany adres lub null (z opisem błędu w outError).
     */
    public String validateNewEmail(String newEmail, String[] outError) {
        if (currentUser == null) {
            if (outError != null && outError.length > 0) outError[0] = "Brak zalogowanego użytkownika";
            return null;
        }
        if (currentUser.isGuest()) {
            if (outError != null && outError.length > 0) outError[0] = "Konto gościa nie posiada adresu e-mail. Zarejestruj się!";
            return null;
        }
        if (newEmail == null || newEmail.trim().isEmpty()) {
            if (outError != null && outError.length > 0) outError[0] = "Podaj nowy adres e-mail";
            return null;
        }
        String clean = newEmail.trim().toLowerCase();
        if (!clean.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            if (outError != null && outError.length > 0) outError[0] = "Podaj prawidłowy adres e-mail (np. nazwa@domena.pl)";
            return null;
        }
        String currentEmail = currentUser.getEmail();
        if (currentEmail != null && currentEmail.equalsIgnoreCase(clean)) {
            if (outError != null && outError.length > 0) outError[0] = "To jest Twój aktualny adres e-mail";
            return null;
        }
        AuthUser taken = findUserByEmail(clean);
        if (taken != null && !taken.getUsername().equals(currentUser.getUsername())) {
            if (outError != null && outError.length > 0) outError[0] = "Ten adres e-mail jest już przypisany do innego konta!";
            return null;
        }
        return clean;
    }

    /**
     * Zapisuje nowy adres e-mail po poprawnej weryfikacji kodu ze STAREGO e-maila.
     * Aktualizuje konto lokalnie oraz w Supabase.
     */
    public boolean applyEmailChange(String newEmail, String[] outError) {
        if (currentUser == null) {
            if (outError != null && outError.length > 0) outError[0] = "Brak zalogowanego użytkownika";
            return false;
        }
        if (newEmail == null || newEmail.trim().isEmpty()) {
            if (outError != null && outError.length > 0) outError[0] = "Brak nowego adresu e-mail";
            return false;
        }
        String clean = newEmail.trim().toLowerCase();
        AuthUser taken = findUserByEmail(clean);
        if (taken != null && !taken.getUsername().equals(currentUser.getUsername())) {
            if (outError != null && outError.length > 0) outError[0] = "Ten adres e-mail jest już przypisany do innego konta!";
            return false;
        }

        currentUser.setEmail(clean);
        saveUsers();
        supabaseClient.updateUserEmail(currentUser.getUsername(), clean, null);
        return true;
    }
}
