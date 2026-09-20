package com.example.testapp.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class UserQrManager {
    private static final String PREFS_NAME = "custom_qr_manager_prefs";
    private static final String KEY_ITEMS = "qr_items_json";

    public static final int GUEST_LIMIT = 3;

    private final SharedPreferences prefs;
    private final List<UserQrItem> items = new ArrayList<>();

    public UserQrManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadItems();
    }

    private void loadItems() {
        items.clear();
        String jsonStr = prefs.getString(KEY_ITEMS, null);

        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return;
        }

        try {
            JSONArray array = new JSONArray(jsonStr);
            boolean purged = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                UserQrItem item = UserQrItem.fromJsonObject(obj);
                if (item != null) {
                    if (item.getId() == 1 || item.getId() == 2 
                            || "Domyślny".equalsIgnoreCase(item.getDate())
                            || (item.getUrl() != null && item.getUrl().contains("twoj_profil"))) {
                        purged = true;
                        continue;
                    }
                    items.add(item);
                }
            }
            if (purged) {
                saveItems();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void purgeDefaultItems() {
        boolean changed = false;
        java.util.Iterator<UserQrItem> it = items.iterator();
        while (it.hasNext()) {
            UserQrItem item = it.next();
            if (item.getId() == 1 || item.getId() == 2 
                    || "Domyślny".equalsIgnoreCase(item.getDate())
                    || (item.getUrl() != null && item.getUrl().contains("twoj_profil"))) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            saveItems();
        }
    }

    public void saveItems() {
        try {
            JSONArray array = new JSONArray();
            for (UserQrItem item : items) {
                array.put(item.toJsonObject());
            }
            prefs.edit().putString(KEY_ITEMS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<UserQrItem> getItems() {
        return items;
    }

    public int getCount() {
        return items.size();
    }

    public boolean canAddMore(boolean hasPremium) {
        // All users have unlimited QR code creations (100% free)
        return true;
    }

    public UserQrItem addItem(String name, String icon, String url, String qrColor) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        UserQrItem newItem = new UserQrItem(System.currentTimeMillis(), name, icon, url, dateStr, false, qrColor);
        items.add(0, newItem); // add to top
        saveItems();
        return newItem;
    }

    public UserQrItem addItem(String name, String icon, String url) {
        return addItem(name, icon, url, "#0F172A");
    }

    public void removeItem(long id) {
        UserQrItem target = null;
        for (UserQrItem item : items) {
            if (item.getId() == id) {
                target = item;
                break;
            }
        }
        if (target != null) {
            items.remove(target);
            saveItems();
        }
    }

    public boolean toggleFavorite(long id) {
        for (UserQrItem item : items) {
            if (item.getId() == id) {
                boolean nextState = !item.isFavorite();
                item.setFavorite(nextState);
                saveItems();
                return nextState;
            }
        }
        return false;
    }
}
