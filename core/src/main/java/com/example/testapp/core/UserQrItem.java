package com.example.testapp.core;

import org.json.JSONException;
import org.json.JSONObject;

public class UserQrItem {
    private final long id;
    private String name;
    private String icon;
    private String url;
    private String date;
    private boolean favorite;
    private String qrColor = "";

    public UserQrItem(long id, String name, String icon, String url, String date, boolean favorite, String qrColor) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.url = url;
        this.date = date;
        this.favorite = favorite;
        this.qrColor = (qrColor != null) ? qrColor : "";
    }

    public UserQrItem(long id, String name, String icon, String url, String date, boolean favorite) {
        this(id, name, icon, url, date, favorite, "");
    }

    public UserQrItem(long id, String name, String icon, String url, String date) {
        this(id, name, icon, url, date, false, "");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getQrColor() {
        return (qrColor != null && !qrColor.isEmpty()) ? qrColor : "#0F172A";
    }

    public void setQrColor(String qrColor) {
        this.qrColor = (qrColor != null) ? qrColor : "";
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("icon", icon);
        json.put("url", url);
        json.put("date", date);
        json.put("favorite", favorite);
        json.put("qrColor", qrColor);
        return json;
    }

    public static UserQrItem fromJsonObject(JSONObject json) throws JSONException {
        return new UserQrItem(
            json.getLong("id"),
            json.getString("name"),
            json.getString("icon"),
            json.getString("url"),
            json.optString("date", ""),
            json.optBoolean("favorite", false),
            json.optString("qrColor", "")
        );
    }
}
