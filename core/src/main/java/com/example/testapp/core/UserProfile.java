package com.example.testapp.core;

import android.content.Context;
import android.content.SharedPreferences;

public class UserProfile {
    public static final String RANK_CREATOR = "Creator";
    public static final String RANK_OWNER = RANK_CREATOR;
    public static final String RANK_ADMIN = "Admin";
    public static final String RANK_PREMIUM = "Premium";
    public static final String RANK_GUEST = "Guest";

    private static final String PREFS_NAME = "vcard_user_prefs";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_TITLE = "user_title";
    private static final String KEY_RANK = "user_rank";
    private static final String KEY_BIO = "user_bio";
    private static final String KEY_AVATAR = "user_avatar";
    private static final String KEY_PURCHASED_PREMIUM = "user_purchased_premium";

    private final SharedPreferences prefs;

    public UserProfile(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getName() {
        String n = prefs.getString(KEY_NAME, "Użytkownik");
        if (n == null || n.trim().isEmpty()) {
            return "Użytkownik";
        }
        return n;
    }

    public void purgeLegacyRajman() {
        // Only reset if it was unauthenticated default
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "Użytkownik";
        }
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    public String getTitle() {
        String t = prefs.getString(KEY_TITLE, "Profil wizytówki DropMe");
        if (t != null && t.toLowerCase().contains("rajman")) {
            t = "Profil wizytówki DropMe";
            setTitle(t);
        }
        return t;
    }

    public void setTitle(String title) {
        prefs.edit().putString(KEY_TITLE, title).apply();
    }

    public String getRank() {
        String r = prefs.getString(KEY_RANK, RANK_GUEST);
        if ("Owner (Creator)".equals(r)) {
            return RANK_CREATOR;
        }
        return r;
    }

    public void setRank(String rank) {
        prefs.edit().putString(KEY_RANK, rank).apply();
    }

    public boolean isCreator() {
        String r = getRank();
        return RANK_CREATOR.equals(r) || "Owner (Creator)".equals(r);
    }

    public boolean isOwner() {
        return isCreator();
    }

    public boolean isAdmin() {
        return RANK_ADMIN.equals(getRank()) || isCreator();
    }

    public boolean isPremium() {
        return RANK_PREMIUM.equals(getRank()) || isPurchasedPremium();
    }

    public boolean isPurchasedPremium() {
        return prefs.getBoolean(KEY_PURCHASED_PREMIUM, false);
    }

    public void setPurchasedPremium(boolean purchased) {
        prefs.edit().putBoolean(KEY_PURCHASED_PREMIUM, purchased).apply();
    }

    public boolean hasPremiumAccess() {
        return isCreator() || isAdmin() || isPremium() || isPurchasedPremium();
    }

    public boolean isGuest() {
        return RANK_GUEST.equals(getRank());
    }

    public String getBio() {
        return prefs.getString(KEY_BIO, "Wirtualna wizytówka z kodami QR do moich profili społecznościowych. Zeskanuj i bądźmy w kontakcie!");
    }

    public void setBio(String bio) {
        prefs.edit().putString(KEY_BIO, bio).apply();
    }

    public String getAvatarEmoji() {
        String defaultEmoji = isCreator() ? "👑" : (isPremium() ? "⭐" : (isAdmin() ? "🛡️" : "👤"));
        return prefs.getString(KEY_AVATAR, defaultEmoji);
    }

    public void setAvatarEmoji(String avatarEmoji) {
        prefs.edit().putString(KEY_AVATAR, avatarEmoji).apply();
    }
}
