package com.example.testapp.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SupabaseConfig {

    private static final String PREFS_NAME = "supabase_config_prefs";
    private static final String KEY_PROJECT_URL = "supabase_url";
    private static final String KEY_ANON_KEY = "supabase_anon_key";

    // User project Supabase credentials
    public static final String DEFAULT_URL = "https://zzzxlpgseqyaztvofwsb.supabase.co";
    public static final String DEFAULT_ANON_KEY = "sb_publishable_3JSfUks_PEmc6-TOOzNKIA_b8j5VfTo";

    public static final String TABLE_USERS = "profiles";
    public static final String TABLE_PURCHASES = "app_purchases";
    public static final String TABLE_QR_CODES = "user_qr_codes";

    private final SharedPreferences prefs;

    public SupabaseConfig(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getProjectUrl() {
        return prefs.getString(KEY_PROJECT_URL, DEFAULT_URL);
    }

    public void setProjectUrl(String url) {
        prefs.edit().putString(KEY_PROJECT_URL, url).apply();
    }

    public String getAnonKey() {
        return prefs.getString(KEY_ANON_KEY, DEFAULT_ANON_KEY);
    }

    public void setAnonKey(String key) {
        prefs.edit().putString(KEY_ANON_KEY, key).apply();
    }

    public String getRestEndpoint(String table) {
        String base = getProjectUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/rest/v1/" + table;
    }

    public String getAuthEndpoint(String path) {
        String base = getProjectUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/auth/v1/" + path;
    }
}
