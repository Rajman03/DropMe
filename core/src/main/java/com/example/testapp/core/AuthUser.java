package com.example.testapp.core;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthUser {
    private final String username;
    private String displayName;
    private String email;
    private String password;
    private String rank;
    private String title;
    private String avatar;
    private boolean purchasedPremium;
    private String pin;

    public AuthUser(String username, String displayName, String password, String rank, String title, String avatar, boolean purchasedPremium, String pin) {
        this.username = username != null ? username.trim().toLowerCase() : "";
        this.displayName = displayName != null ? displayName.trim() : "";
        this.email = "";
        this.password = password != null ? password : "";
        this.rank = rank != null ? rank : UserProfile.RANK_GUEST;
        this.title = title != null ? title : "Użytkownik";
        this.avatar = avatar != null ? avatar : "👤";
        this.purchasedPremium = purchasedPremium;
        this.pin = pin != null ? pin.trim() : "";
    }

    public AuthUser(String username, String displayName, String password, String rank, String title, String avatar, boolean purchasedPremium) {
        this(username, displayName, password, rank, title, avatar, purchasedPremium, "");
    }

    public AuthUser(String username, String displayName, String password, String rank, String title, String avatar) {
        this(username, displayName, password, rank, title, avatar, false, "");
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            this.password = "";
        } else if (CredentialHasher.isHashed(password)) {
            this.password = password;
        } else {
            this.password = CredentialHasher.hash(password);
        }
    }

    public boolean verifyPassword(String rawPassword) {
        if (rawPassword == null || password == null || password.isEmpty()) return false;
        return CredentialHasher.verify(rawPassword, password);
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isPurchasedPremium() {
        return purchasedPremium;
    }

    public void setPurchasedPremium(boolean purchasedPremium) {
        this.purchasedPremium = purchasedPremium;
    }

    public String getPin() {
        return pin != null ? pin : "";
    }

    public void setPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            this.pin = "";
        } else if (CredentialHasher.isHashed(pin.trim())) {
            this.pin = pin.trim();
        } else {
            this.pin = CredentialHasher.hash(pin.trim());
        }
    }

    public boolean verifyPin(String rawPin) {
        if (rawPin == null || pin == null || pin.isEmpty()) return false;
        return CredentialHasher.verify(rawPin.trim(), pin);
    }

    public boolean isCreator() {
        return UserProfile.RANK_CREATOR.equalsIgnoreCase(rank) || "Owner (Creator)".equalsIgnoreCase(rank);
    }

    public boolean isAdmin() {
        return UserProfile.RANK_ADMIN.equalsIgnoreCase(rank) || isCreator();
    }

    public boolean isPremium() {
        return UserProfile.RANK_PREMIUM.equalsIgnoreCase(rank) || isPurchasedPremium() || isAdmin();
    }

    public boolean isGuest() {
        return "guest".equalsIgnoreCase(username) || UserProfile.RANK_GUEST.equalsIgnoreCase(rank);
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : "";
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("username", username);
        obj.put("displayName", displayName);
        obj.put("email", getEmail());
        obj.put("password", password);
        obj.put("rank", rank);
        obj.put("title", title);
        obj.put("avatar", avatar);
        obj.put("purchasedPremium", purchasedPremium);
        obj.put("pin", pin);
        return obj;
    }

    public static AuthUser fromJsonObject(JSONObject obj) throws JSONException {
        AuthUser user = new AuthUser(
                obj.getString("username"),
                obj.optString("displayName", "Użytkownik"),
                obj.getString("password"),
                obj.optString("rank", UserProfile.RANK_GUEST),
                obj.optString("title", "Użytkownik"),
                obj.optString("avatar", "👤"),
                obj.optBoolean("purchasedPremium", false),
                obj.optString("pin", "")
        );
        user.setEmail(obj.optString("email", ""));
        return user;
    }
}
